package vgandolfi.dev.mana_paes.infrastructure.webhook;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache simples em memória do estado de conexão das instâncias da Evolution API
 * (atualizado por {@code CONNECTION_UPDATE} e {@code QRCODE_UPDATED}).
 *
 * <p>Decisão: estado de conexão é efêmero e o webhook de QRCODE pode disparar
 * várias vezes por segundo durante o scan — persistir em banco seria ruído.
 * Guardar em memória é suficiente para o MVP (monitoramento/diagnóstico).</p>
 */
@Component
public class EvolutionWebhookStateStore {

    private final Map<String, ConnectionState> states = new ConcurrentHashMap<>();

    public void update(String instance, String event, Map<String, Object> data) {
        if (instance == null || instance.isBlank()) {
            return;
        }
        ConnectionState current = states.get(instance);
        String connectionState = current != null ? current.connectionState() : null;
        boolean qrCodePending = current != null && current.qrCodePending();

        if ("CONNECTION_UPDATE".equals(event) && data != null && data.get("state") != null) {
            connectionState = String.valueOf(data.get("state"));
        } else if ("QRCODE_UPDATED".equals(event)) {
            qrCodePending = data != null && data.containsKey("qrcode");
        }
        states.put(instance, new ConnectionState(instance, connectionState, qrCodePending, Instant.now()));
    }

    public Optional<ConnectionState> get(String instance) {
        return Optional.ofNullable(states.get(instance));
    }

    public record ConnectionState(String instance, String connectionState, boolean qrCodePending,
                                  Instant lastUpdateAt) {
    }
}