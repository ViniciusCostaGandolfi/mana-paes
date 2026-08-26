package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vgandolfi.dev.mana_paes.domain.model.NotificationLog;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    List<NotificationLog> findByOrderId(UUID orderId);

    /**
     * Logs do tenant com filtros opcionais por status e canal.
     */
    @Query("""
            select n from NotificationLog n
            where n.tenantId = :tenantId
              and (:status is null or n.status = :status)
              and (:channel is null or n.channel = :channel)
            """)
    Page<NotificationLog> search(@Param("tenantId") UUID tenantId,
                                 @Param("status") NotificationStatus status,
                                 @Param("channel") NotificationChannel channel,
                                 Pageable pageable);
}