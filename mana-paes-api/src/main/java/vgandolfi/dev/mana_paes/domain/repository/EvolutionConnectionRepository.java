package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso às conexões WhatsApp da Evolution API. Cada ADMIN tem UMA conexão
 * própria ({@code user_id} único); {@code user_id} nulo representa a conexão
 * global legada.
 */
public interface EvolutionConnectionRepository extends JpaRepository<EvolutionConnection, UUID> {

    /** Conexão global legada (a primeira criada sem {@code user_id}). */
    Optional<EvolutionConnection> findFirstByOrderByCreatedAtAsc();

    Optional<EvolutionConnection> findByInstanceName(String instanceName);

    Optional<EvolutionConnection> findByUserId(UUID userId);

    /** Todas as conexões por ADMIN (para sincronização de estado na subida). */
    List<EvolutionConnection> findAllByUserIdNotNull();
}