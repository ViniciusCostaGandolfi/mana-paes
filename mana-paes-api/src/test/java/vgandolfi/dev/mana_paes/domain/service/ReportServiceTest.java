package vgandolfi.dev.mana_paes.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReportService reportService;

    private final UUID tenantId = UUID.randomUUID();
    private final LocalDate date = LocalDate.now();

    @Test
    void dailyProductionAggregatesRows() {
        UUID paoId = UUID.randomUUID();
        UUID boloId = UUID.randomUUID();
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{paoId, "Pão Francês", UnitMeasure.UN, new BigDecimal("10"), new BigDecimal("5.00")});
        rows.add(new Object[]{boloId, "Bolo", UnitMeasure.UN, new BigDecimal("2"), new BigDecimal("50.00")});
        when(orderRepository.sumByProductForDate(tenantId, date, OrderStatus.CANCELLED)).thenReturn(rows);

        DailyProductionReportResponse report = reportService.dailyProduction(tenantId, date);

        assertThat(report.items()).hasSize(2);
        assertThat(report.items().get(0).productName()).isEqualTo("Pão Francês");
        assertThat(report.totalAmount()).isEqualByComparingTo("55.00");
        verify(orderRepository).sumByProductForDate(tenantId, date, OrderStatus.CANCELLED);
    }

    @Test
    void dailyProductionEmptyReturnsZeroTotal() {
        when(orderRepository.sumByProductForDate(tenantId, date, OrderStatus.CANCELLED)).thenReturn(List.of());

        DailyProductionReportResponse report = reportService.dailyProduction(tenantId, date);

        assertThat(report.items()).isEmpty();
        assertThat(report.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void dailyFinancialWithRow() {
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{3L, new BigDecimal("120.50")});
        when(orderRepository.countAndSumForDate(tenantId, date, OrderStatus.CANCELLED)).thenReturn(rows);

        DailyFinancialReportResponse report = reportService.dailyFinancial(tenantId, date);

        assertThat(report.totalOrders()).isEqualTo(3);
        assertThat(report.totalAmount()).isEqualByComparingTo("120.50");
    }

    @Test
    void dailyFinancialEmptyReturnsZeros() {
        when(orderRepository.countAndSumForDate(tenantId, date, OrderStatus.CANCELLED)).thenReturn(List.of());

        DailyFinancialReportResponse report = reportService.dailyFinancial(tenantId, date);

        assertThat(report.totalOrders()).isZero();
        assertThat(report.totalAmount()).isEqualByComparingTo("0.00");
    }
}