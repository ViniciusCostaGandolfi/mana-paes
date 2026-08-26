package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Atualização parcial da {@code NotificationConfig} do tenant: campos nulos não
 * são alterados. A conexão WhatsApp é GLOBAL (instância única "mana-paes") —
 * configurada em {@code /api/v1/whatsapp}, não aqui.
 */
public record NotificationConfigRequest(
        @Size(max = 20) String adminWhatsappNumber,
        @Email @Size(max = 150) String adminEmail,
        LocalTime dailyReportTime,
        Boolean whatsappEnabled,
        Boolean emailEnabled) {
}