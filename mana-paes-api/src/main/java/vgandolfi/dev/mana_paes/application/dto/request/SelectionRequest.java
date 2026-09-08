package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Seleção de uma opção de um grupo de produto no pedido (ex.: grupo
 * "Tamanho" → opção "Grande").
 */
public record SelectionRequest(
        @NotNull UUID groupId,
        @NotNull UUID optionId) {
}