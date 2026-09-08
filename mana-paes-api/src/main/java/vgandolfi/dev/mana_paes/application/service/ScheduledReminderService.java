package vgandolfi.dev.mana_paes.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.ScheduledReminderRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ScheduledReminderResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.ScheduledReminderRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de lembretes agendados para motoboys, restrito ao tenant do usuário
 * autenticado (multi-tenant). Remoção é física ({@code delete}); o flag
 * {@code enabled} serve para pausar/reativar o disparo recorrente.
 */
@Service
public class ScheduledReminderService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReminderService.class);

    private final ScheduledReminderRepository reminderRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final NotificationService notificationService;

    public ScheduledReminderService(ScheduledReminderRepository reminderRepository,
                                    TenantRepository tenantRepository,
                                    UserRepository userRepository,
                                    VendorRepository vendorRepository,
                                    NotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ScheduledReminderResponse create(UUID tenantId, UUID actorUserId, ScheduledReminderRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));
        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

        ScheduledReminder reminder = new ScheduledReminder();
        reminder.setTenant(tenant);
        reminder.setName(request.name().trim());
        reminder.setMessage(request.message().trim());
        reminder.setReminderTime(request.reminderTime());
        reminder.setEnabled(request.enabled() == null || request.enabled());
        reminder.setWhatsappEnabled(request.whatsappEnabled() != null && request.whatsappEnabled());
        reminder.setEmailEnabled(request.emailEnabled() == null || request.emailEnabled());
        reminder.setDeliveryDayOffset(request.deliveryDayOffset() == null ? 1 : request.deliveryDayOffset());
        reminder.setCreatedBy(actor);
        ScheduledReminder saved = reminderRepository.save(reminder);

        log.info("scheduled_reminder_created id={} tenantId={} time={}",
                saved.getId(), tenantId, saved.getReminderTime());
        return ScheduledReminderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledReminderResponse> list(UUID tenantId, Pageable pageable) {
        return reminderRepository.findByTenantId(tenantId, pageable).map(ScheduledReminderResponse::from);
    }

    @Transactional(readOnly = true)
    public ScheduledReminderResponse getById(UUID tenantId, UUID reminderId) {
        return ScheduledReminderResponse.from(findScoped(tenantId, reminderId));
    }

    @Transactional
    public ScheduledReminderResponse update(UUID tenantId, UUID reminderId, ScheduledReminderRequest request) {
        ScheduledReminder reminder = findScoped(tenantId, reminderId);
        reminder.setName(request.name().trim());
        reminder.setMessage(request.message().trim());
        reminder.setReminderTime(request.reminderTime());
        reminder.setEnabled(request.enabled() == null || request.enabled());
        reminder.setWhatsappEnabled(request.whatsappEnabled() != null && request.whatsappEnabled());
        reminder.setEmailEnabled(request.emailEnabled() == null || request.emailEnabled());
        reminder.setDeliveryDayOffset(request.deliveryDayOffset() == null ? reminder.getDeliveryDayOffset()
                : request.deliveryDayOffset());
        ScheduledReminder saved = reminderRepository.save(reminder);

        log.info("scheduled_reminder_updated id={} tenantId={} enabled={}",
                reminderId, tenantId, saved.isEnabled());
        return ScheduledReminderResponse.from(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID reminderId) {
        ScheduledReminder reminder = findScoped(tenantId, reminderId);
        reminderRepository.delete(reminder);
        log.info("scheduled_reminder_deleted id={} tenantId={}", reminderId, tenantId);
    }

    /**
     * Dispara o lembrete imediatamente para todos os vendedores ativos do
     * tenant (útil para validar o conteúdo/canais antes do horário agendado).
     */
    @Transactional
    public NotificationService.VendorReminderResult sendTest(UUID tenantId, UUID reminderId) {
        ScheduledReminder reminder = findScoped(tenantId, reminderId);
        List<Vendor> vendors = vendorRepository.findByTenantIdAndActiveTrue(tenantId);
        if (vendors.isEmpty()) {
            throw new BusinessException("Nenhum vendedor ativo neste tenant para receber o lembrete");
        }
        LocalDate deliveryDate = LocalDate.now().plusDays(reminder.getDeliveryDayOffset());
        NotificationService.VendorReminderResult result = notificationService.sendVendorReminder(reminder, vendors, deliveryDate);
        log.info("scheduled_reminder_test id={} tenantId={} recipients={}",
                reminderId, tenantId, result.recipients());
        return result;
    }

    private ScheduledReminder findScoped(UUID tenantId, UUID reminderId) {
        return reminderRepository.findByIdAndTenantId(reminderId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Lembrete", reminderId));
    }
}