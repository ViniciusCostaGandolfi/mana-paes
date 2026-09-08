package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.OrderItem;
import vgandolfi.dev.mana_paes.domain.model.OrderItemOption;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        UnitMeasure unitMeasure,
        BigDecimal quantidadeManha,
        BigDecimal quantidadeTarde,
        BigDecimal quantidadeNoite,
        BigDecimal totalQuantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        List<OptionResponse> options) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getUnitMeasure(),
                item.getQuantidadeManha(),
                item.getQuantidadeTarde(),
                item.getQuantidadeNoite(),
                item.getTotalQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getOptions().stream()
                        .map(o -> new OptionResponse(o.getOptionId(), o.getOptionName(), o.getPriceAdjustment()))
                        .toList());
    }

    public record OptionResponse(UUID optionId, String optionName, BigDecimal priceAdjustment) {
    }
}