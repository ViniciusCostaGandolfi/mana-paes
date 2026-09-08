package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Criação/atualização de um {@code ScheduledReminder} (lembrete para
 * vendedores). Campos nulos de {@code enabled}/{@code whatsappEnabled}/
 * {@code emailEnabled} recebem os defaults no service (true, false, true);
 * {@code deliveryDayOffset} default 1.
 */
public record ScheduledReminderRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 500) String message,
        @NotNull LocalTime reminderTime,
        Boolean enabled,
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        Integer deliveryDayOffset) {
}