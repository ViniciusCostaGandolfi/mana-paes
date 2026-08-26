package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração de notificações: canais por tenant, retry, NotificationLog
 * (PENDING → SENT/FAILED) e relatório diário. Falhas de canal nunca propagam.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationConfigRepository configRepository;
    @Mock
    private NotificationLogRepository logRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private EvolutionApiClient evolutionApiClient;
    @Mock
    private EvolutionConnectionRepository connectionRepository;
    @Mock
    private TextEncryptor textEncryptor;

    private final WhatsAppNotificationAdapter whatsAppAdapter = new WhatsAppNotificationAdapter();
    private final AppProperties appProperties = new AppProperties(
            new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
            new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
            new AppProperties.Evolution("", "", 0L),
            new AppProperties.Backend("http://localhost:8080"),
            new AppProperties.Frontend("http://localhost"),
            new AppProperties.Mail(false),
            new AppProperties.Notifications(false, 2),
            new AppProperties.Scheduler(false));

    private NotificationServiceImpl service() {
        return new NotificationServiceImpl(configRepository, logRepository, userRepository, orderRepository,
                emailService, evolutionApiClient, whatsAppAdapter, appProperties,
                connectionRepository, textEncryptor);
    }

    private Tenant tenant(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Padaria Teste");
        return tenant;
    }

    private NotificationConfig config(UUID tenantId, boolean whatsapp, boolean email) {
        NotificationConfig config = new NotificationConfig();
        config.setTenant(tenant(tenantId));
        config.setAdminWhatsappNumber("5511999999999");
        config.setAdminEmail("admin@example.com");
        config.setWhatsappEnabled(whatsapp);
        config.setEmailEnabled(email);
        return config;
    }

    private User requester(UUID requesterId) {
        User user = new User();
        user.setId(requesterId);
        user.setName("Solicitante");
        user.setEmail("requester@example.com");
        user.setWhatsappNumber("5511888888888");
        user.setRole(UserRole.ROLE_REQUESTER);
        user.setTenant(tenant(UUID.randomUUID()));
        return user;
    }

    private OrderCreatedEvent event(UUID tenantId, UUID orderId, UUID requesterId) {
        return new OrderCreatedEvent(orderId, tenantId, requesterId, new BigDecimal("55.00"),
                LocalDate.now().plusDays(3),
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), "Pão", new BigDecimal("10"), UnitMeasure.UN)));
    }

    @Test
    void sendOrderNotificationsSendsAllChannels() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester(requesterId)));
        when(emailService.sendOrderConfirmation(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(emailService.sendNewOrderAlert(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);

        service().sendOrderNotifications(event(tenantId, orderId, requesterId));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(8)).save(captor.capture());
        // 8 saves = 4 canais x (PENDING + final); a mesma entidade é mutada para SENT
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getStatus() == NotificationStatus.SENT);
        verify(evolutionApiClient, times(2)).sendText(any(), any(), any(), any());
        verify(emailService).sendOrderConfirmation(any(), any(), any(), any(), any(), any());
        verify(emailService).sendNewOrderAlert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendOrderNotificationsWithDisabledChannelsSendsNothing() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(config(tenantId, false, false)));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester(requesterId)));

        service().sendOrderNotifications(event(tenantId, orderId, requesterId));

        verify(logRepository, never()).save(any());
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void sendOrderNotificationsWithoutConfigSkips() {
        when(configRepository.findByTenantId(any())).thenReturn(Optional.empty());

        service().sendOrderNotifications(event(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        verify(logRepository, never()).save(any());
    }

    @Test
    void sendOrderNotificationsWithEvolutionFailureMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        User requesterWithoutWhatsapp = requester(requesterId);
        requesterWithoutWhatsapp.setWhatsappNumber(null);
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, false)));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requesterWithoutWhatsapp));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(any(), any(), any(), any());

        // não lança — registra FAILED e segue
        service().sendOrderNotifications(event(tenantId, UUID.randomUUID(), requesterId));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        // 1 canal ativo (whatsapp admin) x (PENDING + final)
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getAllValues().get(0).getRetryCount()).isEqualTo(2);
        assertThat(captor.getAllValues().get(0).getErrorMessage()).contains("Evolution API não configurada");
    }

    @Test
    void sendTestWhatsAppWithoutNumberReturnsFalse() {
        NotificationConfig config = config(UUID.randomUUID(), true, true);
        config.setAdminWhatsappNumber(null);
        when(configRepository.findByTenantId(any())).thenReturn(Optional.of(config));

        NotificationService.WhatsAppTestResult result =
                service().sendTestWhatsApp(UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("adminWhatsappNumber");
    }

    @Test
    void sendTestWhatsAppSuccess() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        // a mesma entidade é mutada para SENT após o save do PENDING
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void sendTestWhatsAppUsesGlobalConnectionKey() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceApiKey("encrypted-token");
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(connection));
        when(textEncryptor.decrypt("encrypted-token")).thenReturn("global-api-key");

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isTrue();
        verify(evolutionApiClient).sendText(eq("mana-paes"), eq("global-api-key"), eq("5511999999999"), anyString());
    }

    @Test
    void sendTestWhatsAppWithUndecryptableKeyMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceApiKey("corrupted-token");
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(connection));
        when(textEncryptor.decrypt("corrupted-token"))
                .thenThrow(new IllegalStateException("BadPaddingException: given final block not properly padded"));
        // chave indecifrável -> resolveGlobalApiKey() devolve null -> client lança NotConfigured (sem derrubar o fluxo)
        doThrow(new EvolutionApiNotConfiguredException("API key da Evolution não configurada"))
                .when(evolutionApiClient).sendText(eq("mana-paes"), isNull(), eq("5511999999999"), anyString());

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Falha ao enviar");
        verify(evolutionApiClient).sendText(eq("mana-paes"), isNull(), eq("5511999999999"), anyString());
    }

    @Test
    void sendTestWhatsAppFailureMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(any(), any(), any(), any());

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isFalse();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("Evolution API não configurada");
    }

    @Test
    void sendDailyReportNotificationsSendsWhatsAppAndEmail() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        DailyProductionReportResponse production = new DailyProductionReportResponse(LocalDate.now(),
                List.of(new vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse(
                        UUID.randomUUID(), "Pão", UnitMeasure.UN, new BigDecimal("10"), new BigDecimal("5.00"))),
                new BigDecimal("5.00"));
        DailyFinancialReportResponse financial = new DailyFinancialReportResponse(LocalDate.now(),
                new BigDecimal("5.00"), 1);
        when(emailService.sendDailyReport(any(), any(), any())).thenReturn(true);

        NotificationService.DailyReportSendResult result =
                service().sendDailyReportNotifications(tenantId, production, financial);

        assertThat(result.whatsappSent()).isTrue();
        assertThat(result.emailSent()).isTrue();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getType() == NotificationType.DAILY_REPORT);
        assertThat(captor.getAllValues()).anyMatch(entry -> entry.getChannel() == NotificationChannel.WHATSAPP);
        assertThat(captor.getAllValues()).anyMatch(entry -> entry.getChannel() == NotificationChannel.EMAIL);
    }

    @Test
    void sendDailyReportNotificationsWithoutConfigReportsNotFound() {
        when(configRepository.findByTenantId(any())).thenReturn(Optional.empty());

        NotificationService.DailyReportSendResult result = service().sendDailyReportNotifications(
                UUID.randomUUID(), mock(DailyProductionReportResponse.class), mock(DailyFinancialReportResponse.class));

        assertThat(result.whatsappSent()).isFalse();
        assertThat(result.emailSent()).isFalse();
        assertThat(result.whatsappMessage()).contains("não encontrada");
    }
}