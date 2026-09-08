package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Pedido público via link mágico (sem autenticação). O vendedor é resolvido
 * pelo telefone ({@code vendorPhone}); {@code vendorName} é informativo
 * (exibido no formulário pelo frontend).
 */
public record PublicOrderRequest(
        @NotBlank @Size(max = 20) String vendorPhone,
        @Size(max = 150) String vendorName,
        @NotNull @FutureOrPresent LocalDate deliveryDate,
        @NotEmpty List<@Valid OrderItemRequest> items) {
}