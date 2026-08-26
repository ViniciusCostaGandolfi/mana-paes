package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Configuração de notificações do tenant. A conexão WhatsApp é GLOBAL
 * (instância única "mana-paes") — o token/estado fica em
 * {@code /api/v1/whatsapp/status}, não aqui.
 */
public record NotificationConfigResponse(
        UUID id,
        String adminWhatsappNumber,
        String adminEmail,
        LocalTime dailyReportTime,
        boolean whatsappEnabled,
        boolean emailEnabled) {

    public static NotificationConfigResponse from(NotificationConfig config) {
        return new NotificationConfigResponse(
                config.getId(),
                config.getAdminWhatsappNumber(),
                config.getAdminEmail(),
                config.getDailyReportTime(),
                config.isWhatsappEnabled(),
                config.isEmailEnabled());
    }
}