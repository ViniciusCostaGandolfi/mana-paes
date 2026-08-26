package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Atualização parcial da {@code NotificationConfig} do tenant: campos nulos não
 * são alterados (o segredo {@code evolutionApiKey} só é atualizado quando
 * informado).
 */
public record NotificationConfigRequest(
        @Size(max = 20) String adminWhatsappNumber,
        @Email @Size(max = 150) String adminEmail,
        LocalTime dailyReportTime,
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        @Size(max = 100) String evolutionApiInstanceName,
        @Size(max = 255) String evolutionApiKey) {
}