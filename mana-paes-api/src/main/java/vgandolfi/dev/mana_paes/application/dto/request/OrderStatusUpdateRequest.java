package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status,
        @Size(max = 500) String reason) {
}