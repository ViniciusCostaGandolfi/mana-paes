package vgandolfi.dev.mana_paes.application.dto.request;

import java.util.Map;

/**
 * Payload do webhook da Evolution API. Campos opcionais para aceitar qualquer
 * formato — o {@code WebhookController} loga e responde 200 mesmo para eventos
 * desconhecidos.
 *
 * @param event    tipo do evento (CONNECTION_UPDATE, QRCODE_UPDATED, MESSAGES_UPSERT...)
 * @param instance nome da instância Evolution
 * @param data     dados específicos do evento
 */
public record EvolutionWebhookPayload(String event, String instance, Map<String, Object> data) {
}