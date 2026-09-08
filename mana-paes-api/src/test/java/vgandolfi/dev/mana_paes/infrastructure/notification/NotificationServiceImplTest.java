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
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
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
import vgandolfi.dev.mana_paes.application.service.NotificationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
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
            new AppProperties.Evolution("", "", 0L, "mana-paes", false),
            new AppProperties.Backend("http://localhost:8080"),
            new AppProperties.Frontend("http://localhost"),
            new AppProperties.Mail(false),
            new AppProperties.Notifications(false, 2),
            new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin("vinicius@vgandolfi.dev", "admin123"));

    private NotificationServiceImpl service() {
        return new NotificationServiceImpl(configRepository, logRepository, orderRepository,
                userRepository, emailService, evolutionApiClient, whatsAppAdapter, appProperties,
                connectionRepository, textEncryptor);
    }

    private AppProperties appPropertiesWithMockSend(boolean mockSend) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution("", "", 0L, "mana-paes", mockSend),
                new AppProperties.Backend("http://localhost:8080"),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin("vinicius@vgandolfi.dev", "admin123"));
    }

    private NotificationServiceImpl serviceWithMockSend(boolean mockSend) {
        return new NotificationServiceImpl(configRepository, logRepository, orderRepository,
                userRepository, emailService, evolutionApiClient, whatsAppAdapter,
                appPropertiesWithMockSend(mockSend), connectionRepository, textEncryptor);
    }


    /**
     * Stub da conexão global legada (findFirst) + decrypt do token.
     */
    private void stubLegacyInstance(String instanceName, String apiKey) {
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName(instanceName);
        connection.setInstanceApiKey("enc-" + apiKey);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(connection));
        when(textEncryptor.decrypt("enc-" + apiKey)).thenReturn(apiKey);
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
        user.setRole(UserRole.ROLE_ADMIN);
        user.setTenant(tenant(UUID.randomUUID()));
        return user;
    }

    private OrderCreatedEvent event(UUID tenantId, UUID orderId, String vendorName, String vendorPhone) {
        return new OrderCreatedEvent(orderId, tenantId, UUID.randomUUID(), vendorName, vendorPhone,
                new BigDecimal("55.00"), LocalDate.now().plusDays(3),
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), "Pão", new BigDecimal("10"), UnitMeasure.UN)));
    }

    @Test
    void sendOrderNotificationsSendsAdminAndVendorWhatsApp() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        service().sendOrderNotifications(event(tenantId, orderId, "Everton", "5511900000001"));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        // 2 canais WhatsApp (admin + vendedor) x (PENDING + final)
        verify(logRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getStatus() == NotificationStatus.SENT);
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getChannel() == NotificationChannel.WHATSAPP);
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getType() == NotificationType.NEW_ORDER_ADMIN_ALERT
                || entry.getType() == NotificationType.ORDER_CONFIRMATION_REQUESTER);
        verify(evolutionApiClient, times(2)).sendText(eq("mana-paes"), eq("token"), any(), any());
    }

    void sendOrderNotificationsWithDisabledChannelsSendsNothing() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(config(tenantId, false, false)));

        service().sendOrderNotifications(event(tenantId, orderId, "Everton", "5511900000001"));

        verify(logRepository, never()).save(any());
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void sendOrderNotificationsWithoutConfigSkips() {
        when(configRepository.findByTenantId(any())).thenReturn(Optional.empty());

        service().sendOrderNotifications(event(UUID.randomUUID(), UUID.randomUUID(), "Everton", "5511900000001"));

        verify(logRepository, never()).save(any());
    }

    @Test
    void sendOrderNotificationsWithEvolutionFailureMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, false)));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(eq("mana-paes"), eq("token"), any(), any());

        // pedido sem vendedor (manual): apenas o alerta ao admin (WhatsApp)
        service().sendOrderNotifications(event(tenantId, UUID.randomUUID(), null, null));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        // 1 canal ativo (whatsapp admin) x (PENDING + final)
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getAllValues().get(0).getRetryCount()).isEqualTo(2);
        assertThat(captor.getAllValues().get(0).getErrorMessage()).contains("Evolution API não configurada");
    }

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
        stubLegacyInstance("mana-paes", "token");
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    void sendTestWhatsAppUsesGlobalConnectionKey() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName("mana-paes");
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
        connection.setInstanceName("mana-paes");
        connection.setInstanceApiKey("corrupted-token");
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(connection));
        when(textEncryptor.decrypt("corrupted-token"))
                .thenThrow(new IllegalStateException("BadPaddingException: given final block not properly padded"));

        // token indecifrável -> sem instância conectada -> FAILED sem chamar a API
        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Nenhuma instância Evolution conectada");
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void sendTestWhatsAppFailureMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(eq("mana-paes"), eq("token"), any(), any());

        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isFalse();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("Evolution API não configurada");
    }

    void sendDailyReportNotificationsSendsWhatsAppOnly() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        DailyProductionReportResponse production = new DailyProductionReportResponse(LocalDate.now(),
                List.of(new vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse(
                        UUID.randomUUID(), "Pão", UnitMeasure.UN, new BigDecimal("10"), new BigDecimal("0"),
                        new BigDecimal("0"), new BigDecimal("10"), new BigDecimal("5.00"))),
                new BigDecimal("5.00"), List.of("Everton"));
        DailyFinancialReportResponse financial = new DailyFinancialReportResponse(LocalDate.now(),
                new BigDecimal("5.00"), 1);

        NotificationService.DailyReportSendResult result =
                service().sendDailyReportNotifications(tenantId, production, financial);

        assertThat(result.whatsappSent()).isTrue();
        assertThat(result.emailSent()).isFalse();
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getType() == NotificationType.DAILY_REPORT);
        assertThat(captor.getAllValues()).allMatch(entry -> entry.getChannel() == NotificationChannel.WHATSAPP);
    }

    void sendDailyReportNotificationsWithoutConfigReportsNotFound() {
        when(configRepository.findByTenantId(any())).thenReturn(Optional.empty());

        NotificationService.DailyReportSendResult result = service().sendDailyReportNotifications(
                UUID.randomUUID(), mock(DailyProductionReportResponse.class), mock(DailyFinancialReportResponse.class));

        assertThat(result.whatsappSent()).isFalse();
        assertThat(result.emailSent()).isFalse();
        assertThat(result.whatsappMessage()).contains("não encontrada");
    }

    private ScheduledReminder reminder(UUID tenantId, boolean whatsapp, boolean email) {
        ScheduledReminder reminder = new ScheduledReminder();
        reminder.setId(UUID.randomUUID());
        reminder.setTenant(tenant(tenantId));
        reminder.setName("Lembrete de pedidos");
        reminder.setMessage("Faça seus pedidos");
        reminder.setReminderTime(LocalTime.of(8, 0));
        reminder.setWhatsappEnabled(whatsapp);
        reminder.setEmailEnabled(email);
        return reminder;
    }

    private Vendor vendor(UUID id, String whatsapp, String phone) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setName("Everton");
        vendor.setPhone(phone);
        vendor.setWhatsappNumber(whatsapp);
        vendor.setTenant(tenant(UUID.randomUUID()));
        return vendor;
    }

    @Test
    void sendVendorReminderSendsWhatsAppToVendor() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), "5511888888888", "5511900000001");

        NotificationService.VendorReminderResult result =
                service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        assertThat(result.recipients()).isEqualTo(1);
        assertThat(result.whatsappSent()).isEqualTo(1);
        verify(evolutionApiClient).sendText(eq("mana-paes"), eq("token"), eq("5511888888888"),
                contains("/pedido?telefone=5511900000001"));
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(captor.getAllValues().get(0).getChannel()).isEqualTo(NotificationChannel.WHATSAPP);
    }

    void sendVendorReminderUsesPhoneWhenWhatsAppAbsent() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), null, "5511900000001");

        NotificationService.VendorReminderResult result =
                service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        assertThat(result.whatsappSent()).isEqualTo(1);
        verify(evolutionApiClient).sendText(eq("mana-paes"), eq("token"), eq("5511900000001"), anyString());
    }

    void sendVendorReminderWithEvolutionFailureMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        stubLegacyInstance("mana-paes", "token");
        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), "5511888888888", "5511900000001");
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(eq("mana-paes"), eq("token"), any(), any());

        NotificationService.VendorReminderResult result =
                service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        assertThat(result.whatsappSent()).isEqualTo(0);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getAllValues().get(0).getErrorMessage()).contains("Evolution API não configurada");
    }

    void sendVendorReminderSkipsVendorWithoutContact() {
        UUID tenantId = UUID.randomUUID();
        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), null, null);

        NotificationService.VendorReminderResult result =
                service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        assertThat(result.whatsappSent()).isEqualTo(0);
        verify(logRepository, never()).save(any());
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void sendVendorReminderUsesAdminConnectionInstanceForTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);
        when(userRepository.findByTenantIdAndRoleAndActiveTrue(tenantId, UserRole.ROLE_ADMIN))
                .thenReturn(List.of(admin));
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName("VGandolfi Dev");
        connection.setInstanceApiKey("enc-admin-token");
        when(connectionRepository.findByUserId(adminId)).thenReturn(Optional.of(connection));
        when(textEncryptor.decrypt("enc-admin-token")).thenReturn("admin-token");

        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), "5511888888888", "5511900000001");
        service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        verify(evolutionApiClient).sendText(eq("VGandolfi Dev"), eq("admin-token"),
                eq("5511888888888"), anyString());
    }

    @Test
    void sendVendorReminderWithoutConnectedInstanceMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findByTenantIdAndRoleAndActiveTrue(tenantId, UserRole.ROLE_ADMIN))
                .thenReturn(List.of());
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), "5511888888888", "5511900000001");
        NotificationService.VendorReminderResult result =
                service().sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        // sem instância conectada: falha graciosa (FAILED no log), não chama a API
        assertThat(result.whatsappSent()).isZero();
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getAllValues().get(0).getErrorMessage()).contains("Nenhuma instância Evolution conectada");
    }

    @Test
    void sendTestWhatsAppWithoutConnectedInstanceMarksFailed() {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));
        User admin = new User();
        admin.setId(adminId);
        when(userRepository.findByTenantIdAndRoleAndActiveTrue(tenantId, UserRole.ROLE_ADMIN))
                .thenReturn(List.of(admin));
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName("VGandolfi Dev");
        connection.setInstanceApiKey("corrupted");
        when(connectionRepository.findByUserId(adminId)).thenReturn(Optional.of(connection));
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(textEncryptor.decrypt("corrupted"))
                .thenThrow(new IllegalStateException("bad padding"));

        // decrypt do admin falhou e sem conexão legada -> nenhuma instância conectada
        NotificationService.WhatsAppTestResult result = service().sendTestWhatsApp(tenantId);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Nenhuma instância Evolution conectada");
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void sendTestWhatsAppWithMockSendLogsAndReturnsSuccess() {
        UUID tenantId = UUID.randomUUID();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config(tenantId, true, true)));

        NotificationService.WhatsAppTestResult result = serviceWithMockSend(true).sendTestWhatsApp(tenantId);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("mock");
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void sendVendorReminderWithMockSendLogsAndMarksSent() {
        UUID tenantId = UUID.randomUUID();
        ScheduledReminder reminder = reminder(tenantId, true, false);
        Vendor vendor = vendor(UUID.randomUUID(), "5511888888888", "5511900000001");

        NotificationService.VendorReminderResult result =
                serviceWithMockSend(true).sendVendorReminder(reminder, List.of(vendor), LocalDate.of(2026, 9, 2));

        assertThat(result.whatsappSent()).isEqualTo(1);
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getAllValues().get(0).getType()).isEqualTo(NotificationType.REMINDER);
    }
}