package vgandolfi.dev.mana_paes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;

import java.util.UUID;

/**
 * Conexão WhatsApp global da aplicação (instância única "mana-paes" da
 * Evolution API), compartilhada por todos os tenants.
 *
 * <p>É um SINGLETON global: não possui {@code tenant_id}. A linha é criada na
 * primeira conexão (admin escaneia o QR) e reaproveitada nas seguintes. O
 * {@code instance_api_key} (token da instância) é armazenado CRIPTOGRAFADO
 * (AES-256/GCM via {@code TextEncryptor}); nunca em texto puro.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evolution_connections")
public class EvolutionConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Nome fixo da instância global ("mana-paes"). */
    @Column(name = "instance_name", nullable = false, unique = true, length = 100)
    private String instanceName;

    /** Token da instância (instance_api_key) — SEMPRE criptografado em repouso. */
    @Column(name = "instance_api_key", nullable = false, length = 512)
    private String instanceApiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_state", nullable = false, length = 20)
    private ConnectionState connectionState = ConnectionState.CLOSE;

    /** Número WhatsApp conectado (ex: 5511999999999) — preenchido quando OPEN. */
    @Column(name = "connected_number", length = 20)
    private String connectedNumber;

    /** QR code pendente (data URI base64) — preenchido quando CONNECTING. */
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;
}