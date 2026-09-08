package vgandolfi.dev.mana_paes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;

import java.util.UUID;

/**
 * Conexão WhatsApp da Evolution API — UMA por usuário ADMIN (instância
 * própria). {@code user_id} é nullable para suportar a conexão global legada
 * (sem dono); quando presente, é único (1 instância por admin).
 *
 * <p>O {@code instance_name} é único e pode ser derivado do userId
 * (auto-criado na produção) ou pré-cadastrado pelo seed (ex: "VGandolfi Dev"
 * do env). O {@code instance_api_key} (token da instância) é armazenado
 * CRIPTOGRAFADO (AES-256/GCM via {@code TextEncryptor}); nunca em texto puro.</p>
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

    /** Dono da conexão (ADMIN). Nulo apenas para a conexão global legada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Nome da instância na Evolution API (único). */
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