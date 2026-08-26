package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.service.NotificationService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EmailNotificationService emailService;
    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppNotificationAdapter whatsAppAdapter;
    private final AppProperties appProperties;

    public NotificationServiceImpl(NotificationConfigRepository configRepository,
                                   NotificationLogRepository logRepository,
                                   UserRepository userRepository,
                                   OrderRepository orderRepository,
                                   EmailNotificationService emailService,
                                   EvolutionApiClient evolutionApiClient,
                                   WhatsAppNotificationAdapter whatsAppAdapter,
                                   AppProperties appProperties) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.evolutionApiClient = evolutionApiClient;
        this.whatsAppAdapter = whatsAppAdapter;
        this.appProperties = appProperties;
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
        User requester = userRepository.findById(event.requesterId()).orElse(null);
        String requesterName = requester != null ? requester.getName() : "Solicitante";
        String requesterWhatsapp = requester != null ? requester.getWhatsappNumber() : null;
        String requesterEmail = requester != null ? requester.getEmail() : null;

        String confirmationContent = whatsAppAdapter.orderConfirmation(event, requesterName);
        String adminAlertContent = whatsAppAdapter.newOrderAdminAlert(event, requesterName);

        if (config.isWhatsappEnabled() && hasText(config.getAdminWhatsappNumber())) {
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.WHATSAPP,
                    NotificationType.NEW_ORDER_ADMIN_ALERT,
                    config.getAdminWhatsappNumber(), adminAlertContent,
                    () -> sendWhatsApp(config, config.getAdminWhatsappNumber(), adminAlertContent));
        }
        if (config.isWhatsappEnabled() && hasText(requesterWhatsapp)) {
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.WHATSAPP,
                    NotificationType.ORDER_CONFIRMATION_REQUESTER,
                    requesterWhatsapp, confirmationContent,
                    () -> sendWhatsApp(config, requesterWhatsapp, confirmationContent));
        }
        if (config.isEmailEnabled() && hasText(requesterEmail)) {
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.EMAIL,
                    NotificationType.ORDER_CONFIRMATION_REQUESTER,
                    requesterEmail, confirmationContent,
                    () -> emailService.sendOrderConfirmation(requesterEmail, requesterName, event.orderId(),
                            event.deliveryDate(), event.totalAmount(), toItemMaps(event)));
        }
        if (config.isEmailEnabled() && hasText(config.getAdminEmail())) {
            sendWithLog(event.tenantId(), event.orderId(), NotificationChannel.EMAIL,
                    NotificationType.NEW_ORDER_ADMIN_ALERT,
                    config.getAdminEmail(), adminAlertContent,
                    () -> emailService.sendNewOrderAlert(config.getAdminEmail(), config.getTenant().getName(),
                            requesterName, event.orderId(), event.deliveryDate(), event.totalAmount(),
                            toItemMaps(event)));
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
            evolutionApiClient.sendText(config.getEvolutionApiInstanceName(), config.getEvolutionApiKey(),
                    config.getAdminWhatsappNumber(), content);
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            log.info("whatsapp_test_sent tenantId={} recipient={}", tenantId, config.getAdminWhatsappNumber());
            return new WhatsAppTestResult(true, "Mensagem de teste enviada para " + config.getAdminWhatsappNumber());
        } catch (Exception ex) {
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorMessage(truncate(ex.getMessage(), ERROR_MAX_LENGTH));
            log.warn("whatsapp_test_failed tenantId={} recipient={} reason={}",
                    tenantId, config.getAdminWhatsappNumber(), ex.getMessage());
            return new WhatsAppTestResult(false, "Falha ao enviar mensagem de teste: " + ex.getMessage());
        }
    }

    private boolean sendWhatsApp(NotificationConfig config, String number, String text) {
        evolutionApiClient.sendText(config.getEvolutionApiInstanceName(), config.getEvolutionApiKey(), number, text);
        return true;
    }

    @Override
    @Transactional
    public DailyReportSendResult sendDailyReportNotifications(UUID tenantId,
                                                             DailyProductionReportResponse production,
                                                             DailyFinancialReportResponse financial) {
        NotificationConfig config = configRepository.findByTenantId(tenantId).orElse(null);
        if (config == null) {
            return new DailyReportSendResult(false, false,
                    "config de notificações não encontrada", "config de notificações não encontrada");
        }
        String content = whatsAppAdapter.dailyReport(production, financial);

        boolean whatsappSent = false;
        String whatsappMessage = "whatsapp desabilitado ou número do admin ausente";
        if (config.isWhatsappEnabled() && hasText(config.getAdminWhatsappNumber())) {
            whatsappSent = sendWithLog(tenantId, null, NotificationChannel.WHATSAPP, NotificationType.DAILY_REPORT,
                    config.getAdminWhatsappNumber(), content,
                    () -> sendWhatsApp(config, config.getAdminWhatsappNumber(), content));
            whatsappMessage = whatsappSent ? "whatsapp enviado para o admin" : "falha no envio do whatsapp (ver logs)";
        }

        boolean emailSent = false;
        String emailMessage = "email desabilitado ou email do admin ausente";
        if (config.isEmailEnabled() && hasText(config.getAdminEmail())) {
            emailSent = sendWithLog(tenantId, null, NotificationChannel.EMAIL, NotificationType.DAILY_REPORT,
                    config.getAdminEmail(), content,
                    () -> emailService.sendDailyReport(config.getAdminEmail(), production, financial));
            emailMessage = emailSent ? "email enviado para o admin" : "falha no envio do email (ver logs)";
        }

        log.info("daily_report_notifications tenantId={} whatsapp={} email={}",
                tenantId, whatsappSent, emailSent);
        return new DailyReportSendResult(whatsappSent, emailSent, whatsappMessage, emailMessage);
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

    private List<Map<String, Object>> toItemMaps(OrderCreatedEvent event) {
        return event.items().stream()
                .map(item -> Map.<String, Object>of(
                        "productName", item.productName(),
                        "quantity", item.quantity().stripTrailingZeros().toPlainString(),
                        "unitMeasure", item.unitMeasure().name()))
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}