package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull @FutureOrPresent LocalDate deliveryDate,
        @NotEmpty List<@Valid OrderItemRequest> items,
        UUID requesterId) {
}