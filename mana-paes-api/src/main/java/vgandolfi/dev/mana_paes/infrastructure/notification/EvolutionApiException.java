package vgandolfi.dev.mana_paes.infrastructure.notification;

/**
 * Falha controlada ao chamar a Evolution API (erro de rede, HTTP != 2xx).
 * Capturada pelo {@code NotificationServiceImpl} para registrar FAILED no
 * {@code NotificationLog} — nunca derruba o fluxo de criação de pedido.
 */
public class EvolutionApiException extends RuntimeException {

    public EvolutionApiException(String message) {
        super(message);
    }

    public EvolutionApiException(String message, Throwable cause) {
        super(message, cause);
    }
}