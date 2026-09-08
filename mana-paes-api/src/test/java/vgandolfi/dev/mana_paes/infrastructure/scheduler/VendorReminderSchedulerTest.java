package vgandolfi.dev.mana_paes.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.service.NotificationService;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.ScheduledReminderRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorReminderSchedulerTest {

    @Mock
    private ScheduledReminderRepository reminderRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private NotificationService notificationService;

    private VendorReminderScheduler scheduler() {
        return new VendorReminderScheduler(reminderRepository, vendorRepository, notificationService);
    }

    private Tenant tenant(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        return tenant;
    }

    private ScheduledReminder reminder(UUID id, UUID tenantId, LocalDate lastSentDate, int offset) {
        ScheduledReminder reminder = new ScheduledReminder();
        reminder.setId(id);
        reminder.setTenant(tenant(tenantId));
        reminder.setName("Lembrete de pedidos");
        reminder.setMessage("Faça seus pedidos");
        reminder.setReminderTime(LocalTime.of(8, 0));
        reminder.setEnabled(true);
        reminder.setLastSentDate(lastSentDate);
        reminder.setDeliveryDayOffset(offset);
        return reminder;
    }

    private Vendor vendor(UUID id) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setName("Everton");
        vendor.setPhone("5511900000001");
        return vendor;
    }

    @Test
    void runAtDispatchesDueReminderAndMarksLastSentDate() {
        UUID tenantId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();
        ScheduledReminder due = reminder(reminderId, tenantId, null, 1);
        when(reminderRepository.findByEnabledTrueAndReminderTimeLessThanEqual(LocalTime.of(12, 0)))
                .thenReturn(List.of(due));
        List<Vendor> vendors = List.of(vendor(UUID.randomUUID()), vendor(UUID.randomUUID()));
        when(vendorRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(vendors);
        when(notificationService.sendVendorReminder(due, vendors, LocalDate.of(2026, 9, 2)))
                .thenReturn(new NotificationService.VendorReminderResult(2, 1));

        LocalDate today = LocalDate.of(2026, 9, 1);
        scheduler().runAt(today, LocalTime.of(12, 0));

        verify(notificationService).sendVendorReminder(due, vendors, LocalDate.of(2026, 9, 2));
        verify(reminderRepository).save(due);
        assertThat(due.getLastSentDate()).isEqualTo(today);
    }

    @Test
    void runAtSkipsReminderAlreadySentToday() {
        LocalDate today = LocalDate.of(2026, 9, 1);
        ScheduledReminder sentToday = reminder(UUID.randomUUID(), UUID.randomUUID(), today, 1);
        when(reminderRepository.findByEnabledTrueAndReminderTimeLessThanEqual(LocalTime.of(12, 0)))
                .thenReturn(List.of(sentToday));

        scheduler().runAt(today, LocalTime.of(12, 0));

        verify(notificationService, never()).sendVendorReminder(any(), any(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void runAtSkipsReminderWithoutActiveVendors() {
        UUID tenantId = UUID.randomUUID();
        ScheduledReminder due = reminder(UUID.randomUUID(), tenantId, null, 1);
        when(reminderRepository.findByEnabledTrueAndReminderTimeLessThanEqual(LocalTime.of(12, 0)))
                .thenReturn(List.of(due));
        when(vendorRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of());

        scheduler().runAt(LocalDate.of(2026, 9, 1), LocalTime.of(12, 0));

        verify(notificationService, never()).sendVendorReminder(any(), any(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void runAtContinuesWhenOneReminderFails() {
        UUID failingTenant = UUID.randomUUID();
        UUID okTenant = UUID.randomUUID();
        ScheduledReminder failing = reminder(UUID.randomUUID(), failingTenant, null, 1);
        ScheduledReminder ok = reminder(UUID.randomUUID(), okTenant, null, 1);
        when(reminderRepository.findByEnabledTrueAndReminderTimeLessThanEqual(LocalTime.of(12, 0)))
                .thenReturn(List.of(failing, ok));

        when(vendorRepository.findByTenantIdAndActiveTrue(failingTenant))
                .thenThrow(new RuntimeException("boom"));
        List<Vendor> vendors = List.of(vendor(UUID.randomUUID()));
        when(vendorRepository.findByTenantIdAndActiveTrue(okTenant)).thenReturn(vendors);
        when(notificationService.sendVendorReminder(ok, vendors, LocalDate.of(2026, 9, 2)))
                .thenReturn(new NotificationService.VendorReminderResult(1, 1));

        scheduler().runAt(LocalDate.of(2026, 9, 1), LocalTime.of(12, 0));

        verify(notificationService).sendVendorReminder(ok, vendors, LocalDate.of(2026, 9, 2));
        verify(notificationService, times(1)).sendVendorReminder(any(), any(), any());
        verify(reminderRepository).save(ok);
    }
}