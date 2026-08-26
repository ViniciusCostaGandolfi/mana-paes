package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID orderId,
        NotificationChannel channel,
        NotificationType type,
        String recipient,
        NotificationStatus status,
        String content,
        String errorMessage,
        int retryCount,
        Instant sentAt,
        Instant createdAt) {

    public static NotificationLogResponse from(NotificationLog entry) {
        return new NotificationLogResponse(
                entry.getId(),
                entry.getOrder() != null ? entry.getOrder().getId() : null,
                entry.getChannel(),
                entry.getType(),
                entry.getRecipient(),
                entry.getStatus(),
                entry.getContent(),
                entry.getErrorMessage(),
                entry.getRetryCount(),
                entry.getSentAt(),
                entry.getCreatedAt());
    }
}