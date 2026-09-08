package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vgandolfi.dev.mana_paes.application.dto.request.ScheduledReminderRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ScheduledReminderResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.ScheduledReminderRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledReminderServiceTest {

    @Mock
    private ScheduledReminderRepository reminderRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ScheduledReminderService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID reminderId = UUID.randomUUID();

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
    }

    private ScheduledReminder reminder(UUID id) {
        ScheduledReminder reminder = new ScheduledReminder();
        reminder.setId(id);
        reminder.setTenant(tenant());
        reminder.setName("Lembrete de pedidos");
        reminder.setMessage("Faça seus pedidos");
        reminder.setReminderTime(LocalTime.of(8, 0));
        reminder.setEnabled(true);
        reminder.setWhatsappEnabled(true);
        reminder.setEmailEnabled(false);
        reminder.setDeliveryDayOffset(1);
        return reminder;
    }

    private Vendor vendor(UUID id) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setName("Everton");
        vendor.setPhone("5511900000001");
        vendor.setTenant(tenant());
        vendor.setActive(true);
        return vendor;
    }

    private ScheduledReminderRequest request(LocalTime time, Boolean enabled, Boolean whatsapp, Boolean email,
                                             Integer offset) {
        return new ScheduledReminderRequest("Lembrete de pedidos", "Faça seus pedidos", time, enabled, whatsapp, email, offset);
    }

    @Test
    void createAppliesDefaultsAndSetsTenant() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(reminderRepository.save(any(ScheduledReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduledReminderResponse response = service.create(tenantId, null,
                request(LocalTime.of(8, 0), null, null, null, null));

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.enabled()).isTrue();
        assertThat(response.whatsappEnabled()).isFalse();
        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.deliveryDayOffset()).isEqualTo(1);
        ArgumentCaptor<ScheduledReminder> captor = ArgumentCaptor.forClass(ScheduledReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertThat(captor.getValue().getTenant().getId()).isEqualTo(tenantId);
    }

    @Test
    void createHonorsExplicitChannelsAndOffset() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(reminderRepository.save(any(ScheduledReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduledReminderResponse response = service.create(tenantId, null,
                request(LocalTime.of(9, 30), false, true, false, 2));

        assertThat(response.enabled()).isFalse();
        assertThat(response.whatsappEnabled()).isTrue();
        assertThat(response.emailEnabled()).isFalse();
        assertThat(response.deliveryDayOffset()).isEqualTo(2);
    }

    @Test
    void createWithMissingTenantThrows() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, null, request(LocalTime.of(8, 0), null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listReturnsTenantScopedPage() {
        when(reminderRepository.findByTenantId(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reminder(reminderId))));

        Page<ScheduledReminderResponse> page = service.list(tenantId, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(reminderId);
    }

    @Test
    void getByIdFromAnotherTenantThrows() {
        when(reminderRepository.findByIdAndTenantId(reminderId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(tenantId, reminderId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateModifiesFieldsKeepingTenantAndId() {
        ScheduledReminder existing = reminder(reminderId);
        when(reminderRepository.findByIdAndTenantId(reminderId, tenantId)).thenReturn(Optional.of(existing));
        when(reminderRepository.save(any(ScheduledReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduledReminderResponse response = service.update(tenantId, reminderId,
                request(LocalTime.of(14, 0), true, false, true, 3));

        assertThat(response.reminderTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.whatsappEnabled()).isFalse();
        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.deliveryDayOffset()).isEqualTo(3);
        assertThat(response.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void deleteRemovesReminder() {
        ScheduledReminder existing = reminder(reminderId);
        when(reminderRepository.findByIdAndTenantId(reminderId, tenantId)).thenReturn(Optional.of(existing));

        service.delete(tenantId, reminderId);

        verify(reminderRepository).delete(existing);
    }

    @Test
    void sendTestDispatchesToActiveVendors() {
        ScheduledReminder existing = reminder(reminderId);
        List<Vendor> vendors = List.of(vendor(UUID.randomUUID()), vendor(UUID.randomUUID()));
        when(reminderRepository.findByIdAndTenantId(reminderId, tenantId)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(vendors);
        when(notificationService.sendVendorReminder(existing, vendors, java.time.LocalDate.now().plusDays(1)))
                .thenReturn(new NotificationService.VendorReminderResult(2, 2));

        NotificationService.VendorReminderResult result = service.sendTest(tenantId, reminderId);

        assertThat(result.recipients()).isEqualTo(2);
        assertThat(result.whatsappSent()).isEqualTo(2);
        verify(notificationService).sendVendorReminder(existing, vendors, java.time.LocalDate.now().plusDays(1));
    }

    @Test
    void sendTestWithoutVendorsThrows() {
        ScheduledReminder existing = reminder(reminderId);
        when(reminderRepository.findByIdAndTenantId(reminderId, tenantId)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.sendTest(tenantId, reminderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhum vendedor ativo");
        verify(notificationService, never()).sendVendorReminder(any(), any(), any());
    }
}