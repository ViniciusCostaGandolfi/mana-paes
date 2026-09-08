package vgandolfi.dev.mana_paes.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.EvolutionWebhookPayload;
import vgandolfi.dev.mana_paes.infrastructure.webhook.EvolutionWebhookHandler;

/**
 * Recebe os eventos da Evolution API. Endpoint {@code permitAll} (SecurityConfig
 * já libera {@code /api/v1/webhooks/**}).
 *
 * <p>Responde 200 sempre que o payload é parseável — inclusive para eventos
 * desconhecidos ou sem {@code event} (apenas loga) — para evitar retries da
 * Evolution. Payload malformado (JSON inválido) ainda gera 400 pelo
 * {@code GlobalExceptionHandler}; aceitamos esse trade-off e documentamos.</p>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final EvolutionWebhookHandler handler;

    public WebhookController(EvolutionWebhookHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/evolution-api")
    public ResponseEntity<Void> handleEvolutionWebhook(
            @RequestBody(required = false) EvolutionWebhookPayload payload) {
        if (payload == null || payload.event() == null || payload.event().isBlank()) {
            log.warn("evolution_webhook_invalid_payload payload={}", payload);
            return ResponseEntity.ok().build();
        }
        handler.handle(payload);
        return ResponseEntity.ok().build();
    }
}