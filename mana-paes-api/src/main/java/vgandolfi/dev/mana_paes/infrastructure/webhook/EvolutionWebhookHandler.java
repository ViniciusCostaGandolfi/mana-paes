package vgandolfi.dev.mana_paes.infrastructure.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.dto.request.EvolutionWebhookPayload;

/**
 * Processa os eventos do webhook da Evolution API.
 *
 * <p>Eventos tratados no MVP:</p>
 * <ul>
 *   <li>{@code CONNECTION_UPDATE} — estado da conexão (open/close)</li>
 *   <li>{@code QRCODE_UPDATED} — QR pendente para pareamento</li>
 *   <li>{@code MESSAGES_UPSERT} — mensagens de entrada/saída: apenas logado
 *       (fora do escopo do MVP)</li>
 * </ul>
 */
@Component
public class EvolutionWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWebhookHandler.class);

    private final EvolutionWebhookStateStore stateStore;

    public EvolutionWebhookHandler(EvolutionWebhookStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void handle(EvolutionWebhookPayload payload) {
        switch (payload.event()) {
            case "CONNECTION_UPDATE", "QRCODE_UPDATED" -> {
                stateStore.update(payload.instance(), payload.event(), payload.data());
                log.info("evolution_webhook event={} instance={} dataKeys={}",
                        payload.event(), payload.instance(),
                        payload.data() != null ? payload.data().keySet() : null);
            }
            case "MESSAGES_UPSERT" ->
                    log.info("evolution_webhook event=MESSAGES_UPSERT instance={} (ignorado no MVP)",
                            payload.instance());
            default ->
                    log.info("evolution_webhook event={} instance={} (evento desconhecido, ignorado)",
                            payload.event(), payload.instance());
        }
    }
}