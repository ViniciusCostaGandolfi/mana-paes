package vgandolfi.dev.mana_paes.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vgandolfi.dev.mana_paes.application.dto.request.NotificationConfigRequest;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationConfigResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationLogResponse;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConfigServiceTest {

    @Mock
    private NotificationConfigRepository configRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private NotificationLogRepository logRepository;
    @Mock
    private NotificationService notificationService;

    private final UUID tenantId = UUID.randomUUID();

    private NotificationConfigService service() {
        return new NotificationConfigService(configRepository, tenantRepository, logRepository, notificationService);
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
    }

    private NotificationConfig config() {
        NotificationConfig config = new NotificationConfig();
        config.setId(UUID.randomUUID());
        config.setTenant(tenant());
        config.setDailyReportTime(LocalTime.of(18, 0));
        return config;
    }

    @Test
    void getConfigCreatesDefaultWhenAbsent() {
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(configRepository.save(any(NotificationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationConfigResponse response = service().getConfig(tenantId);

        assertThat(response.whatsappEnabled()).isFalse();
        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.dailyReportTime()).isEqualTo(LocalTime.of(18, 0));
        verify(configRepository).save(any(NotificationConfig.class));
    }

    @Test
    void updateConfigAppliesOnlyNonNullFields() {
        NotificationConfig config = config();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));
        when(configRepository.save(any(NotificationConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationConfigResponse response = service().updateConfig(tenantId,
                new NotificationConfigRequest("5511999999999", null, null, true, null));

        assertThat(response.adminWhatsappNumber()).isEqualTo("5511999999999");
        assertThat(response.whatsappEnabled()).isTrue();
        assertThat(response.emailEnabled()).isTrue(); // inalterado
        assertThat(response.dailyReportTime()).isEqualTo(LocalTime.of(18, 0)); // inalterado
    }

    @Test
    void sendTestWhatsAppWithoutNumberReturnsGuidance() {
        NotificationConfig config = config();
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

        MessageResponse response = service().sendTestWhatsApp(tenantId);

        assertThat(response.message()).contains("adminWhatsappNumber");
    }

    @Test
    void listLogsMapsToResponse() {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setId(UUID.randomUUID());
        logEntry.setTenantId(tenantId);
        logEntry.setChannel(NotificationChannel.WHATSAPP);
        logEntry.setType(NotificationType.TEST);
        logEntry.setRecipient("5511999999999");
        logEntry.setStatus(NotificationStatus.FAILED);
        when(logRepository.search(tenantId, NotificationStatus.FAILED, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(logEntry), PageRequest.of(0, 20), 1));

        Page<NotificationLogResponse> page =
                service().listLogs(tenantId, NotificationStatus.FAILED, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(page.getContent().get(0).channel()).isEqualTo(NotificationChannel.WHATSAPP);
    }
}