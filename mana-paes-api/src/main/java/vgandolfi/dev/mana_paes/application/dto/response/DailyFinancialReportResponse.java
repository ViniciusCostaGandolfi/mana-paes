package vgandolfi.dev.mana_paes.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyFinancialReportResponse(
        LocalDate date,
        BigDecimal totalAmount,
        int totalOrders) {
}