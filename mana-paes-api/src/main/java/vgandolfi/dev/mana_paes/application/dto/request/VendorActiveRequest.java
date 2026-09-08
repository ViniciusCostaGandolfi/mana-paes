package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record VendorActiveRequest(
        @NotNull Boolean active) {
}