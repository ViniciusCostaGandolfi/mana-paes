package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        UnitMeasure unitMeasure,
        boolean active,
        Instant createdAt,
        List<OptionGroupResponse> optionGroups) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getUnitMeasure(),
                product.isActive(),
                product.getCreatedAt(),
                product.getOptionGroups().stream()
                        .sorted(java.util.Comparator.comparingInt(ProductOptionGroup::getSortOrder))
                        .map(ProductResponse::toGroup)
                        .toList());
    }

    private static OptionGroupResponse toGroup(ProductOptionGroup group) {
        return new OptionGroupResponse(
                group.getId(),
                group.getName(),
                group.isRequired(),
                group.getOptions().stream()
                        .filter(ProductOption::isActive)
                        .sorted(java.util.Comparator.comparingInt(ProductOption::getSortOrder))
                        .map(option -> new OptionResponse(option.getId(), option.getName(), option.getPriceAdjustment()))
                        .toList());
    }

    public record OptionGroupResponse(UUID id, String name, boolean required, List<OptionResponse> options) {
    }

    public record OptionResponse(UUID id, String name, BigDecimal priceAdjustment) {
    }
}