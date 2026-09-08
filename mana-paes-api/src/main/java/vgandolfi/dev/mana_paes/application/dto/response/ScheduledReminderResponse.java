package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Lembrete agendado de um tenant. Os canais são expostos como flags
 * {@code whatsappEnabled}/{@code emailEnabled} (mesmo padrão da
 * {@code NotificationConfig}); {@code deliveryDayOffset} define a data de
 * entrega do link (hoje + offset).
 */
public record ScheduledReminderResponse(
        UUID id,
        String name,
        String message,
        LocalTime reminderTime,
        boolean enabled,
        boolean whatsappEnabled,
        boolean emailEnabled,
        int deliveryDayOffset,
        LocalDate lastSentDate,
        UUID tenantId,
        UUID createdById) {

    public static ScheduledReminderResponse from(ScheduledReminder reminder) {
        return new ScheduledReminderResponse(
                reminder.getId(),
                reminder.getName(),
                reminder.getMessage(),
                reminder.getReminderTime(),
                reminder.isEnabled(),
                reminder.isWhatsappEnabled(),
                reminder.isEmailEnabled(),
                reminder.getDeliveryDayOffset(),
                reminder.getLastSentDate(),
                reminder.getTenant().getId(),
                reminder.getCreatedBy() != null ? reminder.getCreatedBy().getId() : null);
    }
}