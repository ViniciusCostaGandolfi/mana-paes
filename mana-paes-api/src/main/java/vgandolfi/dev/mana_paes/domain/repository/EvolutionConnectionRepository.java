package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;

import java.util.Optional;
import java.util.UUID;

/**
 * Acesso à conexão WhatsApp GLOBAL (instância única). Como não há
 * {@code tenant_id}, a "linha única" é localizada pela ordem de criação
 * (singleton) ou pelo nome da instância.
 */
public interface EvolutionConnectionRepository extends JpaRepository<EvolutionConnection, UUID> {

    /** Retorna a conexão global (a primeira criada — singleton). */
    Optional<EvolutionConnection> findFirstByOrderByCreatedAtAsc();

    Optional<EvolutionConnection> findByInstanceName(String instanceName);
}