package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;

import java.util.Optional;
import java.util.UUID;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, UUID> {

    Optional<NotificationConfig> findByTenantId(UUID tenantId);
}