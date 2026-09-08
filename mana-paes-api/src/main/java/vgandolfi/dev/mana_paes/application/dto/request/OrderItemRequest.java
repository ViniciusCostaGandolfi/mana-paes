package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Item de pedido com quantidades por turno (manhã/tarde/noite) e opções de
 * produto selecionadas. Pelo menos um dos turnos deve ser maior que zero
 * (validado no service).
 */
public record OrderItemRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin("0") BigDecimal quantidadeManha,
        @NotNull @DecimalMin("0") BigDecimal quantidadeTarde,
        @NotNull @DecimalMin("0") BigDecimal quantidadeNoite,
        List<@Valid SelectionRequest> selections) {

    public BigDecimal getTotalQuantity() {
        return quantidadeManha.add(quantidadeTarde).add(quantidadeNoite);
    }
}