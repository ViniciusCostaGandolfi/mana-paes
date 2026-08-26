package vgandolfi.dev.mana_paes.infrastructure.notification;

/**
 * Caso particular de {@link EvolutionApiException}: a Evolution API não está
 * configurada (URL ou chave ausentes). Em dev/test o envio falha graciosamente
 * e o {@code NotificationLog} é marcado como FAILED/PENDING.
 */
public class EvolutionApiNotConfiguredException extends EvolutionApiException {

    public EvolutionApiNotConfiguredException(String message) {
        super(message);
    }
}