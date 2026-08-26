package vgandolfi.dev.mana_paes.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyProductionReportResponse(
        LocalDate date,
        List<DailyReportItemResponse> items,
        BigDecimal totalAmount) {
}