package vgandolfi.dev.mana_paes.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportDispatchResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse;
import vgandolfi.dev.mana_paes.domain.model.DailyReport;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.repository.DailyReportRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReportDispatcherTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private NotificationConfigRepository configRepository;
    @Mock
    private DailyReportRepository dailyReportRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ReportService reportService;
    @Mock
    private NotificationService notificationService;

    private final UUID tenantId = UUID.randomUUID();
    private final LocalDate date = LocalDate.now();

    private DailyReportDispatcher dispatcher() {
        return new DailyReportDispatcher(tenantRepository, configRepository, dailyReportRepository,
                productRepository, reportService, notificationService);
    }

    private DailyProductionReportResponse production() {
        return new DailyProductionReportResponse(date,
                List.of(new DailyReportItemResponse(UUID.randomUUID(), "Pão", UnitMeasure.UN,
                        new BigDecimal("10"), new BigDecimal("5.00"))),
                new BigDecimal("5.00"));
    }

    @Test
    void dispatchForTenantAlreadySentSkips() {
        DailyReport sent = new DailyReport();
        sent.setSent(true);
        when(dailyReportRepository.findByTenantIdAndReportDate(tenantId, date)).thenReturn(Optional.of(sent));

        DailyReportDispatchResponse response = dispatcher().dispatchForTenant(tenantId, date);

        assertThat(response.dispatched()).isFalse();
        verifyNoInteractions(reportService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void dispatchForTenantPersistsSendsAndMarksSent() {
        when(dailyReportRepository.findByTenantIdAndReportDate(tenantId, date)).thenReturn(Optional.empty());
        when(reportService.dailyFinancial(tenantId, date))
                .thenReturn(new DailyFinancialReportResponse(date, new BigDecimal("5.00"), 1));
        when(reportService.dailyProduction(tenantId, date)).thenReturn(production());
        when(productRepository.getReferenceById(any(UUID.class)))
                .thenAnswer(inv -> new vgandolfi.dev.mana_paes.domain.model.Product());
        when(tenantRepository.getReferenceById(tenantId)).thenAnswer(inv -> {
            Tenant tenant = new Tenant();
            tenant.setId(inv.getArgument(0));
            return tenant;
        });
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.sendDailyReportNotifications(any(), any(), any()))
                .thenReturn(new NotificationService.DailyReportSendResult(true, true, "wa", "em"));

        DailyReportDispatchResponse response = dispatcher().dispatchForTenant(tenantId, date);

        assertThat(response.dispatched()).isTrue();
        assertThat(response.whatsappSent()).isTrue();
        assertThat(response.emailSent()).isTrue();

        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        verify(dailyReportRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        DailyReport saved = captor.getAllValues().get(1);
        assertThat(saved.isSent()).isTrue();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("5.00");
        assertThat(saved.getTotalOrders()).isEqualTo(1);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getTenant().getId()).isEqualTo(tenantId);
        verify(notificationService).sendDailyReportNotifications(any(), any(), any());
    }

    @Test
    void dispatchDueTenantsOnlyDispatchesThoseWhoseTimeHasPassed() {
        UUID dueTenant = UUID.randomUUID();
        UUID notDueTenant = UUID.randomUUID();
        Tenant t1 = new Tenant();
        t1.setId(dueTenant);
        Tenant t2 = new Tenant();
        t2.setId(notDueTenant);
        when(tenantRepository.findByActiveTrue()).thenReturn(List.of(t1, t2));

        NotificationConfig config1 = new NotificationConfig();
        config1.setDailyReportTime(LocalTime.of(10, 0));
        NotificationConfig config2 = new NotificationConfig();
        config2.setDailyReportTime(LocalTime.of(20, 0));
        when(configRepository.findByTenantId(dueTenant)).thenReturn(Optional.of(config1));
        when(configRepository.findByTenantId(notDueTenant)).thenReturn(Optional.of(config2));

        when(dailyReportRepository.findByTenantIdAndReportDate(dueTenant, date)).thenReturn(Optional.empty());
        when(reportService.dailyFinancial(dueTenant, date))
                .thenReturn(new DailyFinancialReportResponse(date, BigDecimal.ZERO, 0));
        when(reportService.dailyProduction(dueTenant, date))
                .thenReturn(new DailyProductionReportResponse(date, List.of(), BigDecimal.ZERO));
        when(tenantRepository.getReferenceById(dueTenant)).thenAnswer(inv -> {
            Tenant tenant = new Tenant();
            tenant.setId(inv.getArgument(0));
            return tenant;
        });
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.sendDailyReportNotifications(any(), any(), any()))
                .thenReturn(new NotificationService.DailyReportSendResult(false, false, "x", "y"));

        List<DailyReportDispatchResponse> results = dispatcher().dispatchDueTenants(date, LocalTime.of(12, 0));

        assertThat(results).hasSize(1);
        // apenas o tenant "due" foi persistido
        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        verify(dailyReportRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(report -> report.getTenant().getId().equals(dueTenant));
        verify(dailyReportRepository, never()).findByTenantIdAndReportDate(notDueTenant, date);
    }

    @Test
    void dispatchDueTenantsIgnoresTenantsWithoutConfig() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepository.findByActiveTrue()).thenReturn(List.of(tenant));
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        List<DailyReportDispatchResponse> results = dispatcher().dispatchDueTenants(date, LocalTime.of(12, 0));

        assertThat(results).isEmpty();
        verifyNoInteractions(dailyReportRepository);
    }
}