package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Configuração de notificações do tenant. O segredo {@code evolutionApiKey} não
 * é devolvido — apenas um booleano indicando se está configurado.
 */
public record NotificationConfigResponse(
        UUID id,
        String adminWhatsappNumber,
        String adminEmail,
        LocalTime dailyReportTime,
        boolean whatsappEnabled,
        boolean emailEnabled,
        String evolutionApiInstanceName,
        boolean evolutionApiKeyConfigured) {

    public static NotificationConfigResponse from(NotificationConfig config) {
        return new NotificationConfigResponse(
                config.getId(),
                config.getAdminWhatsappNumber(),
                config.getAdminEmail(),
                config.getDailyReportTime(),
                config.isWhatsappEnabled(),
                config.isEmailEnabled(),
                config.getEvolutionApiInstanceName(),
                config.getEvolutionApiKey() != null && !config.getEvolutionApiKey().isBlank());
    }
}