package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.application.service.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Implementação da porta {@link NotificationService}: orquestra os envios de um
 * pedido criado (WhatsApp/e-mail para admin e solicitante), respeitando a
 * {@code NotificationConfig} do tenant, com retry simples por canal e registro
 * de {@code NotificationLog} (PENDING → SENT/FAILED).
 *
 * <p>Falhas de canal (Evolution API sem configurar, SMTP fora do ar) são
 * sempre capturadas e registradas como FAILED — o fluxo de criação de pedido
 * nunca é afetado.</p>
 */
@Component
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final long BACKOFF_BASE_MS = 500L;
    private static final int CONTENT_MAX_LENGTH = 4000;
    private static final int ERROR_MAX_LENGTH = 1000;

    private final NotificationConfigRepository configRepository;
    private final NotificationLogRepository logRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;
    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppNotificationAdapter whatsAppAdapter;
    private final AppProperties appProperties;
    private final EvolutionConnectionRepository connectionRepository;
    private final TextEncryptor textEncryptor;

    public NotificationServiceImpl(NotificationConfigRepository configRepository,
                                   NotificationLogRepository logRepository,
                                   OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   EmailNotificationService emailService,
                                   EvolutionApiClient evolutionApiClient,
                                   WhatsAppNotificationAdapter whatsAppAdapter,
                                   AppProperties appProperties,
                                   EvolutionConnectionRepository connectionRepository,
                                   TextEncryptor textEncryptor) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.evolutionApiClient = evolutionApiClient;
        this.whatsAppAdapter = whatsAppAdapter;
        this.appProperties = appProperties;
        this.connectionRepository = connectionRepository;
        this.textEncryptor = textEncryptor;
    }

    @Override
    public void sendForgotPasswordEmail(User user, String token, Instant expiryDate) {
        emailService.sendForgotPasswordEmail(user, token, expiryDate);
    }

    @Override
    @Transactional
    public void sendOrderNotifications(OrderCreatedEvent event) {
        NotificationConfig config = configRepository.findByTenantId(event.tenantId()).orElse(null);
        if (config == null) {
            log.info("skip_order_notifications tenantId={} reason=notification_config_not_found", event.tenantId());
            return;
        }

        // alerta ao admin via WhatsApp (instância do ADMIN do tenant)
        if (config.isWhatsappEnabled() && hasText(config.getAdminWhatsappNumber())) {
            String adminContent = whatsAppAdapter.newOrderAdminAlert(event);
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.WHATSAPP,
                    NotificationType.NEW_ORDER_ADMIN_ALERT,
                    config.getAdminWhatsappNumber(), adminContent,
                    () -> sendWhatsApp(event.tenantId(), config.getAdminWhatsappNumber(), adminContent));
        }

        // confirmação ao vendedor via WhatsApp
        if (config.isWhatsappEnabled() && hasText(event.vendorPhone())) {
            String vendorContent = whatsAppAdapter.orderConfirmation(event);
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.WHATSAPP,
                    NotificationType.ORDER_CONFIRMATION_REQUESTER,
                    event.vendorPhone(), vendorContent,
                    () -> sendWhatsAppTo(event.tenantId(), event.vendorPhone(), vendorContent));
        }
    }

    @Override
    @Transactional
    public WhatsAppTestResult sendTestWhatsApp(UUID tenantId) {
        NotificationConfig config = configRepository.findByTenantId(tenantId).orElse(null);
        if (config == null || !hasText(config.getAdminWhatsappNumber())) {
            return new WhatsAppTestResult(false, "Configure adminWhatsappNumber antes de testar o WhatsApp");
        }
        String content = whatsAppAdapter.testMessage();

        NotificationLog entry = new NotificationLog();
        entry.setTenantId(tenantId);
        entry.setChannel(NotificationChannel.WHATSAPP);
        entry.setType(NotificationType.TEST);
        entry.setRecipient(config.getAdminWhatsappNumber());
        entry.setContent(content);
        entry.setStatus(NotificationStatus.PENDING);
        logRepository.save(entry);

        try {
            if (appProperties.evolution().mockSend()) {
                // dev: console-print em vez de envio real
                log.info("whatsapp_mock_send to={} text={}", config.getAdminWhatsappNumber(), content);
                entry.setStatus(NotificationStatus.SENT);
                entry.setSentAt(Instant.now());
                log.info("whatsapp_test_sent (mock) tenantId={} recipient={}",
                        tenantId, config.getAdminWhatsappNumber());
                return new WhatsAppTestResult(true,
                        "Mensagem de teste enviada para " + config.getAdminWhatsappNumber() + " (mock)");
            }
            EvolutionTarget target = resolveInstance(tenantId);
            requireInstance(target);
            evolutionApiClient.sendText(target.instanceName(),
                    target.apiKey(), config.getAdminWhatsappNumber(), content);
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            log.info("whatsapp_test_sent tenantId={} recipient={} instance={}",
                    tenantId, config.getAdminWhatsappNumber(), target.instanceName());
            return new WhatsAppTestResult(true, "Mensagem de teste enviada para " + config.getAdminWhatsappNumber());
        } catch (Exception ex) {
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage(truncate(ex.getMessage(), ERROR_MAX_LENGTH));
            log.warn("whatsapp_test_failed tenantId={} recipient={} reason={}",
                    tenantId, config.getAdminWhatsappNumber(), ex.getMessage());
            return new WhatsAppTestResult(false, "Falha ao enviar mensagem de teste: " + ex.getMessage());
        }
    }

    private boolean sendWhatsApp(UUID tenantId, String number, String text) {
        if (appProperties.evolution().mockSend()) {
            log.info("whatsapp_mock_send to={} text={}", number, text);
            return true;
        }
        EvolutionTarget target = resolveInstance(tenantId);
        requireInstance(target);
        evolutionApiClient.sendText(target.instanceName(), target.apiKey(), number, text);
        return true;
    }

    /**
     * Envia WhatsApp para um destinatário arbitrário (ex.: vendedor no fluxo de
     * lembrete), usando a instância do ADMIN do tenant.
     */
    private boolean sendWhatsAppTo(UUID tenantId, String number, String text) {
        if (appProperties.evolution().mockSend()) {
            log.info("whatsapp_mock_send to={} text={}", number, text);
            return true;
        }
        EvolutionTarget target = resolveInstance(tenantId);
        requireInstance(target);
        evolutionApiClient.sendText(target.instanceName(), target.apiKey(), number, text);
        return true;
    }

    /**
     * Resolve a instância + chave da Evolution API usada no envio do TENANT: a
     * conexão do ADMIN do tenant (1 instância por ADMIN); senão a conexão
     * global legada (token no banco). Sem nenhuma conexão → alvo vazio (envio
     * falha graciosamente com FAILED). A chave global do env é usada APENAS no
     * createInstance — nunca como chave de envio.
     */
    private EvolutionTarget resolveInstance(UUID tenantId) {
        EvolutionConnection adminConnection = userRepository.findByTenantIdAndRoleAndActiveTrue(
                        tenantId, UserRole.ROLE_ADMIN).stream()
                .map(User::getId)
                .flatMap(adminId -> connectionRepository.findByUserId(adminId).stream())
                .findFirst()
                .orElse(null);

        if (adminConnection != null && hasText(adminConnection.getInstanceApiKey())) {
            String decrypted = decryptSafely(adminConnection.getInstanceApiKey());
            if (hasText(decrypted)) {
                return new EvolutionTarget(adminConnection.getInstanceName(), decrypted);
            }
        }

        EvolutionConnection legacy = connectionRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (legacy != null && hasText(legacy.getInstanceApiKey())) {
            String decrypted = decryptSafely(legacy.getInstanceApiKey());
            if (hasText(decrypted)) {
                return new EvolutionTarget(legacy.getInstanceName(), decrypted);
            }
        }

        return new EvolutionTarget("", "");
    }

    private void requireInstance(EvolutionTarget target) {
        if (!hasText(target.instanceName()) || !hasText(target.apiKey())) {
            throw new EvolutionApiNotConfiguredException("Nenhuma instância Evolution conectada para o tenant");
        }
    }

    private String decryptSafely(String encrypted) {
        try {
            return textEncryptor.decrypt(encrypted);
        } catch (Exception ex) {
            log.warn("evolution_api_key_decrypt_failed reason={}", ex.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public DailyReportSendResult sendDailyReportNotifications(UUID tenantId,
                                                             DailyProductionReportResponse production,
                                                             DailyFinancialReportResponse financial) {
        NotificationConfig config = configRepository.findByTenantId(tenantId).orElse(null);
        if (config == null) {
            return new DailyReportSendResult(false, false,
                    "config de notificações não encontrada", "");
        }
        String content = whatsAppAdapter.dailyReport(production, financial, appProperties.frontend().url());

        boolean whatsappSent = false;
        String whatsappMessage = "whatsapp desabilitado ou número do admin ausente";
        if (config.isWhatsappEnabled() && hasText(config.getAdminWhatsappNumber())) {
            whatsappSent = sendWithLog(tenantId, null, NotificationChannel.WHATSAPP, NotificationType.DAILY_REPORT,
                    config.getAdminWhatsappNumber(), content,
                    () -> sendWhatsApp(tenantId, config.getAdminWhatsappNumber(), content));
            whatsappMessage = whatsappSent ? "whatsapp enviado para o admin" : "falha no envio do whatsapp (ver logs)";
        }

        log.info("daily_report_notifications tenantId={} whatsapp={}", tenantId, whatsappSent);
        return new DailyReportSendResult(whatsappSent, false, whatsappMessage, "");
    }

    @Override
    @Transactional
    public VendorReminderResult sendVendorReminder(ScheduledReminder reminder, List<Vendor> vendors, LocalDate deliveryDate) {
        UUID tenantId = reminder.getTenant().getId();
        int whatsappSent = 0;

        for (Vendor vendor : vendors) {
            String target = firstText(vendor.getWhatsappNumber(), vendor.getPhone());
            if (!hasText(target)) {
                continue;
            }
            String content = whatsAppAdapter.vendorReminder(vendor.getName(), publicOrderLink(vendor, deliveryDate));
            boolean sent = sendWithLog(tenantId, null, NotificationChannel.WHATSAPP, NotificationType.REMINDER,
                    target, content,
                    () -> sendWhatsAppTo(tenantId, target, content));
            if (sent) {
                whatsappSent++;
            }
        }

        log.info("vendor_reminder_sent tenantId={} reminderId={} recipients={} whatsapp={}",
                tenantId, reminder.getId(), vendors.size(), whatsappSent);
        return new VendorReminderResult(vendors.size(), whatsappSent);
    }

    /**
     * Link público de pedido do vendedor (consumido pelo frontend para abrir o
     * formulário de pedido pré-preenchido).
     */
    private String publicOrderLink(Vendor vendor, LocalDate deliveryDate) {
        return appProperties.frontend().url() + "/pedido"
                + "?telefone=" + vendor.getPhone()
                + "&nome=" + java.net.URLEncoder.encode(vendor.getName(), java.nio.charset.StandardCharsets.UTF_8)
                + "&dia=" + deliveryDate;
    }

    /**
     * Registra um {@code NotificationLog} (PENDING) e tenta o envio com retry
     * simples (até {@code app.notifications.max-retries}, backoff 500ms/1s).
     * O status final (SENT/FAILED) é persistido; exceções de canal nunca
     * propagam para o consumidor.
     *
     * @return {@code true} quando o envio foi efetivado (SENT)
     */
    private boolean sendWithLog(UUID tenantId, UUID orderId, NotificationChannel channel, NotificationType type,
                                String recipient, String content, BooleanSupplier sendAction) {
        NotificationLog entry = new NotificationLog();
        entry.setTenantId(tenantId);
        if (orderId != null) {
            entry.setOrder(orderRepository.getReferenceById(orderId));
        }
        entry.setChannel(channel);
        entry.setType(type);
        entry.setRecipient(recipient);
        entry.setContent(truncate(content, CONTENT_MAX_LENGTH));
        entry.setStatus(NotificationStatus.PENDING);
        entry.setRetryCount(0);
        logRepository.save(entry);

        int maxRetries = Math.max(1, appProperties.notifications().maxRetries());
        Throwable lastError = null;
        boolean sent = false;
        for (int attempt = 1; attempt <= maxRetries && !sent; attempt++) {
            try {
                sent = sendAction.getAsBoolean();
                if (!sent) {
                    lastError = new IllegalStateException("canal indisponivel (envio desabilitado ou recusado)");
                }
            } catch (Exception ex) {
                lastError = ex;
            }
            entry.setRetryCount(attempt);
            if (!sent && attempt < maxRetries) {
                sleep(BACKOFF_BASE_MS * (1L << (attempt - 1)));
            }
        }

        if (sent) {
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            entry.setErrorMessage(null);
            log.info("notification_sent channel={} type={} recipient={} tenantId={}",
                    channel, type, recipient, tenantId);
        } else {
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage(truncate(messageOf(lastError), ERROR_MAX_LENGTH));
            log.warn("notification_failed channel={} type={} recipient={} tenantId={} attempts={} reason={}",
                    channel, type, recipient, tenantId, maxRetries, messageOf(lastError));
        }
        logRepository.save(entry);
        return sent;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String messageOf(Throwable ex) {
        if (ex == null) {
            return "envio sem sucesso";
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Instância + chave da Evolution API usada no envio (por tenant).
     */
    record EvolutionTarget(String instanceName, String apiKey) {
    }
}