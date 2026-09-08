package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.util.List;

/**
 * Criação/atualização de produto com grupos de opções. Cada grupo exige ao
 * menos uma opção; o {@code priceAdjustment} soma ao preço base no pedido.
 */
public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @NotNull UnitMeasure unitMeasure,
        List<@Valid OptionGroupRequest> optionGroups) {

    public record OptionGroupRequest(
            @NotBlank @Size(max = 100) String name,
            Boolean required,
            Integer sortOrder,
            @NotEmpty List<@Valid OptionRequest> options) {
    }

    public record OptionRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin("0") BigDecimal priceAdjustment,
            Integer sortOrder) {
    }
}