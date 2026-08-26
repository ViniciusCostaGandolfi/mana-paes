package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;

/**
 * Porta de gerenciamento da conexão WhatsApp GLOBAL (instância única
 * "mana-paes" da Evolution API), compartilhada pela aplicação inteira.
 *
 * <p>Duas implementações (exatamente uma ativa):</p>
 * <ul>
 *   <li>{@link EvolutionConnectionServiceImpl} (REAL) — ativa quando
 *       {@code app.evolution.url} está preenchida; chama a Evolution API e
 *       persiste o estado na tabela {@code evolution_connections};</li>
 *   <li>{@link MockEvolutionConnectionServiceImpl} (DEV/TEST) — ativa quando
 *       {@code app.evolution.url} está em branco; simula o fluxo em memória
 *       sem nenhuma chamada externa.</li>
 * </ul>
 */
public interface EvolutionConnectionManager {

    /** Nome fixo da instância global da Evolution API. */
    String GLOBAL_INSTANCE_NAME = "mana-paes";

    /**
     * Inicia/retoma a conexão: cria a instância (primeira vez), pede o QR code
     * e entra em {@code CONNECTING}.
     */
    WhatsAppStatus startConnection();

    /** Estado atual (CLOSE | CONNECTING | OPEN) + QR/número conforme o caso. */
    WhatsAppStatus getStatus();

    /** Encerra a conexão e limpa estado, número e QR. */
    void disconnect();

    /** Envia mensagem de teste para o número conectado. */
    TestMessage testConnection();

    /** Resultado do teste de conexão. */
    record TestMessage(boolean success, String message) {
    }
}