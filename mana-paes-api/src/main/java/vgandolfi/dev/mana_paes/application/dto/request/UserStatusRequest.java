package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull Boolean active) {
}