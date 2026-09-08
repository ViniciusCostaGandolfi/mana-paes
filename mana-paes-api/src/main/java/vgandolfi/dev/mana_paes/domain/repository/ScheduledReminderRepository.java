package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledReminderRepository extends JpaRepository<ScheduledReminder, UUID> {

    Page<ScheduledReminder> findByTenantId(UUID tenantId, Pageable pageable);

    List<ScheduledReminder> findByTenantId(UUID tenantId);

    Optional<ScheduledReminder> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ScheduledReminder> findByEnabledTrue();

    /**
     * Lembretes habilitados cujo horário já foi igualado/ultrapassado. A
     * idempotência diária ({@code lastSentDate != hoje}) é filtrada em código
     * pelo scheduler (JPQL não lida bem com {@code NULL != data}).
     */
    List<ScheduledReminder> findByEnabledTrueAndReminderTimeLessThanEqual(LocalTime time);
}