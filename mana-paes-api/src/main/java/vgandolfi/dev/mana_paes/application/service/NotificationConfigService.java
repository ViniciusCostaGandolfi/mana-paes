package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.NotificationConfigRequest;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationConfigResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationLogResponse;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.util.UUID;

/**
 * Configuração de notificações por tenant + consulta de logs.
 */
@Service
public class NotificationConfigService {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfigService.class);

    private final NotificationConfigRepository configRepository;
    private final TenantRepository tenantRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationService notificationService;

    public NotificationConfigService(NotificationConfigRepository configRepository,
                                     TenantRepository tenantRepository,
                                     NotificationLogRepository logRepository,
                                     NotificationService notificationService) {
        this.configRepository = configRepository;
        this.tenantRepository = tenantRepository;
        this.logRepository = logRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public NotificationConfigResponse getConfig(UUID tenantId) {
        return NotificationConfigResponse.from(getOrCreate(tenantId));
    }

    @Transactional
    public NotificationConfigResponse updateConfig(UUID tenantId, NotificationConfigRequest request) {
        NotificationConfig config = getOrCreate(tenantId);
        if (request.adminWhatsappNumber() != null) {
            config.setAdminWhatsappNumber(request.adminWhatsappNumber());
        }
        if (request.adminEmail() != null) {
            config.setAdminEmail(request.adminEmail());
        }
        if (request.dailyReportTime() != null) {
            config.setDailyReportTime(request.dailyReportTime());
        }
        if (request.whatsappEnabled() != null) {
            config.setWhatsappEnabled(request.whatsappEnabled());
        }
        if (request.emailEnabled() != null) {
            config.setEmailEnabled(request.emailEnabled());
        }
        NotificationConfig saved = configRepository.save(config);

        log.info("notification_config_updated tenantId={}", tenantId);
        return NotificationConfigResponse.from(saved);
    }

    @Transactional
    public MessageResponse sendTestWhatsApp(UUID tenantId) {
        NotificationConfig config = getOrCreate(tenantId);
        if (config.getAdminWhatsappNumber() == null || config.getAdminWhatsappNumber().isBlank()) {
            return new MessageResponse("Configure adminWhatsappNumber antes de testar o WhatsApp");
        }
        NotificationService.WhatsAppTestResult result = notificationService.sendTestWhatsApp(tenantId);
        return new MessageResponse(result.message());
    }

    @Transactional(readOnly = true)
    public Page<NotificationLogResponse> listLogs(UUID tenantId, NotificationStatus status,
                                                  NotificationChannel channel, Pageable pageable) {
        return logRepository.search(tenantId, status, channel, pageable)
                .map(NotificationLogResponse::from);
    }

    private NotificationConfig getOrCreate(UUID tenantId) {
        return configRepository.findByTenantId(tenantId).orElseGet(() -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));
            NotificationConfig config = new NotificationConfig();
            config.setTenant(tenant);
            return configRepository.save(config);
        });
    }
}