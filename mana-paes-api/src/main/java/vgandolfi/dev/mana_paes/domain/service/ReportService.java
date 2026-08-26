package vgandolfi.dev.mana_paes.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Relatórios diários gerados sob demanda via queries agregadas.
 * A persistência em {@code DailyReport} entra com o scheduler (Fase 5).
 */
@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public DailyProductionReportResponse dailyProduction(UUID tenantId, LocalDate date) {
        List<Object[]> rows = orderRepository.sumByProductForDate(tenantId, date, OrderStatus.CANCELLED);
        List<DailyReportItemResponse> items = rows.stream()
                .map(row -> new DailyReportItemResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (UnitMeasure) row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4]))
                .toList();

        BigDecimal total = items.stream()
                .map(DailyReportItemResponse::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new DailyProductionReportResponse(date, items, total);
    }

    @Transactional(readOnly = true)
    public DailyFinancialReportResponse dailyFinancial(UUID tenantId, LocalDate date) {
        List<Object[]> rows = orderRepository.countAndSumForDate(tenantId, date, OrderStatus.CANCELLED);
        Object[] row = rows.isEmpty() ? null : rows.get(0);
        long totalOrders = row == null || row[0] == null ? 0L : ((Number) row[0]).longValue();
        BigDecimal totalAmount = row == null || row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
        return new DailyFinancialReportResponse(date, totalAmount.setScale(2, RoundingMode.HALF_UP), (int) totalOrders);
    }
}