package vgandolfi.dev.mana_paes.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.service.NotificationService;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.ScheduledReminderRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Agendador de lembretes para vendedores: roda de hora em hora e, para cada
 * lembrete habilitado cujo {@code reminderTime} já foi igualado/ultrapassado,
 * dispara notificações WhatsApp para os vendedores ativos do tenant. A
 * idempotência diária (máx. 1 envio por dia) é garantida por
 * {@code lastSentDate}.
 *
 * <p>Condicional a {@code app.scheduler.enabled=true} (default em dev/test:
 * {@code false}) — em testes o bean não existe e nada dispara. Falha de um
 * lembrete nunca impede os demais.</p>
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class VendorReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(VendorReminderScheduler.class);

    private final ScheduledReminderRepository reminderRepository;
    private final VendorRepository vendorRepository;
    private final NotificationService notificationService;

    public VendorReminderScheduler(ScheduledReminderRepository reminderRepository,
                                   VendorRepository vendorRepository,
                                   NotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.vendorRepository = vendorRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        runAt(LocalDate.now(), LocalTime.now());
    }

    /**
     * Núcleo do disparo, separado para permitir testes determinísticos
     * (injeção de data/hora).
     */
    void runAt(LocalDate today, LocalTime now) {
        log.info("vendor_reminder_scheduler_run at={}", now);

        List<ScheduledReminder> due = reminderRepository.findByEnabledTrueAndReminderTimeLessThanEqual(now);
        for (ScheduledReminder reminder : due) {
            try {
                if (today.equals(reminder.getLastSentDate())) {
                    continue; // já enviado hoje
                }
                UUID tenantId = reminder.getTenant().getId();
                List<Vendor> vendors = vendorRepository.findByTenantIdAndActiveTrue(tenantId);
                if (vendors.isEmpty()) {
                    log.info("vendor_reminder_skip id={} tenantId={} reason=no_active_vendors", reminder.getId(), tenantId);
                    continue;
                }
                LocalDate deliveryDate = today.plusDays(reminder.getDeliveryDayOffset());
                notificationService.sendVendorReminder(reminder, vendors, deliveryDate);
                reminder.setLastSentDate(today);
                reminderRepository.save(reminder);
                log.info("vendor_reminder_dispatched id={} tenantId={} recipients={} deliveryDate={}",
                        reminder.getId(), tenantId, vendors.size(), deliveryDate);
            } catch (Exception ex) {
                // falha de um lembrete não impede os demais
                log.error("vendor_reminder_failed id={} tenantId={}", reminder.getId(),
                        reminder.getTenant() != null ? reminder.getTenant().getId() : null, ex);
            }
        }
    }
}