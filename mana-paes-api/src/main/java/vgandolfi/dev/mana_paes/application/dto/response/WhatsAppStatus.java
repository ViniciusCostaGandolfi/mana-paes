package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;

import java.time.Instant;

/**
 * Estado atual da conexão WhatsApp global (instância "mana-paes").
 *
 * <p>{@code state} é um dos valores {@code CLOSE | CONNECTING | OPEN}.
 * {@code qrCodeBase64} só é preenchido quando CONNECTING (data URI para o
 * frontend exibir no {@code <img src>}); {@code connectedNumber} quando OPEN.</p>
 */
public record WhatsAppStatus(String state, String qrCodeBase64, String connectedNumber, Instant updatedAt) {

    public static WhatsAppStatus from(EvolutionConnection connection) {
        return new WhatsAppStatus(
                connection.getConnectionState().name(),
                connection.getQrCodeBase64(),
                connection.getConnectedNumber(),
                connection.getUpdatedAt());
    }

    public static WhatsAppStatus closed(Instant updatedAt) {
        return new WhatsAppStatus("CLOSE", null, null, updatedAt);
    }
}