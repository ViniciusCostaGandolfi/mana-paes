package vgandolfi.dev.mana_paes.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.service.DailyReportDispatcher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReportSchedulerTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private NotificationConfigRepository configRepository;
    @Mock
    private DailyReportDispatcher dispatcher;

    private DailyReportScheduler scheduler() {
        return new DailyReportScheduler(tenantRepository, configRepository, dispatcher);
    }

    @Test
    void runAtDispatchesOnlyTenantsWhoseTimeHasPassed() {
        UUID due = UUID.randomUUID();
        UUID notDue = UUID.randomUUID();
        Tenant t1 = new Tenant();
        t1.setId(due);
        Tenant t2 = new Tenant();
        t2.setId(notDue);
        when(tenantRepository.findByActiveTrue()).thenReturn(List.of(t1, t2));

        NotificationConfig c1 = new NotificationConfig();
        c1.setDailyReportTime(LocalTime.of(10, 0));
        NotificationConfig c2 = new NotificationConfig();
        c2.setDailyReportTime(LocalTime.of(20, 0));
        when(configRepository.findByTenantId(due)).thenReturn(Optional.of(c1));
        when(configRepository.findByTenantId(notDue)).thenReturn(Optional.of(c2));

        LocalDate today = LocalDate.now();
        scheduler().runAt(today, LocalTime.of(12, 0));

        verify(dispatcher).dispatchForTenant(due, today);
        verify(dispatcher, never()).dispatchForTenant(notDue, today);
    }

    @Test
    void runAtContinuesWhenOneTenantFails() {
        UUID failing = UUID.randomUUID();
        UUID ok = UUID.randomUUID();
        Tenant t1 = new Tenant();
        t1.setId(failing);
        Tenant t2 = new Tenant();
        t2.setId(ok);
        when(tenantRepository.findByActiveTrue()).thenReturn(List.of(t1, t2));

        NotificationConfig c1 = new NotificationConfig();
        c1.setDailyReportTime(LocalTime.of(10, 0));
        NotificationConfig c2 = new NotificationConfig();
        c2.setDailyReportTime(LocalTime.of(10, 0));
        when(configRepository.findByTenantId(failing)).thenReturn(Optional.of(c1));
        when(configRepository.findByTenantId(ok)).thenReturn(Optional.of(c2));

        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(dispatcher).dispatchForTenant(failing, LocalDate.now());

        LocalDate today = LocalDate.now();
        scheduler().runAt(today, LocalTime.of(12, 0));

        verify(dispatcher).dispatchForTenant(ok, today);
    }
}