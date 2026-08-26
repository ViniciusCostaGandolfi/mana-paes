package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.util.UUID;

public record DailyReportItemResponse(
        UUID productId,
        String productName,
        UnitMeasure unitMeasure,
        BigDecimal totalQuantity,
        BigDecimal totalAmount) {
}