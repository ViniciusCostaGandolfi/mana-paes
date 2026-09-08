package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;

import java.util.UUID;

/**
 * Porta de gerenciamento da conexão WhatsApp — UMA instância por usuário ADMIN.
 *
 * <p>Duas implementações (exatamente uma ativa):</p>
 * <ul>
 *   <li>{@link EvolutionConnectionServiceImpl} (REAL) — ativa quando
 *       {@code app.evolution.url} está preenchida; cria a instância na Evolution
 *       API com nome derivado do userId e persiste o estado;</li>
 *   <li>{@link MockEvolutionConnectionServiceImpl} (DEV/TEST) — ativa quando
 *       {@code app.evolution.url} está em branco; simula o fluxo em memória por
 *       usuário.</li>
 * </ul>
 */
public interface EvolutionConnectionManager {

    /**
     * Inicia/retoma a conexão do usuário: cria a instância (primeira vez) com
     * nome único derivado, pede o QR code e entra em {@code CONNECTING}.
     */
    WhatsAppStatus startConnection(UUID userId);

    /** Estado atual (CLOSE | CONNECTING | OPEN) + QR/número conforme o caso. */
    WhatsAppStatus getStatus(UUID userId);

    /** Encerra a conexão do usuário e limpa estado, número e QR. */
    void disconnect(UUID userId);

    /** Envia mensagem de teste para o número conectado do usuário. */
    TestMessage testConnection(UUID userId);

    /** Resultado do teste de conexão. */
    record TestMessage(boolean success, String message) {
    }
}