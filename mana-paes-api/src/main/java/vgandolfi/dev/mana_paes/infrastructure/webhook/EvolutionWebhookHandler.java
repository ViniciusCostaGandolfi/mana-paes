package vgandolfi.dev.mana_paes.infrastructure.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.EvolutionWebhookPayload;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;

import java.util.Map;

/**
 * Processa os eventos do webhook da Evolution API e PERSISTE o estado da
 * conexão global (instância "mana-paes") na tabela {@code evolution_connections}
 * — a fonte de verdade consultada por {@code GET /api/v1/whatsapp/status}.
 *
 * <p>Eventos tratados:</p>
 * <ul>
 *   <li>{@code QRCODE_UPDATED} — salva o QR code (de {@code data.base64} ou
 *       {@code data.qrcode[.base64]}, normalizado para data URI);</li>
 *   <li>{@code CONNECTION_UPDATE} — {@code state=open} extrai o número de
 *       {@code data.wuid} (formato {@code 5511999999999@s.whatsapp.net} → parte
 *       antes de {@code @}; aceita também {@code data.number}) e salva
 *       OPEN + número; {@code state=close} → CLOSE (limpa número/QR);</li>
 *   <li>{@code MESSAGES_UPSERT} — apenas logado (fora do escopo do MVP).</li>
 * </ul>
 */
@Component
public class EvolutionWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWebhookHandler.class);

    private final EvolutionConnectionRepository connectionRepository;

    public EvolutionWebhookHandler(EvolutionConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Transactional
    public void handle(EvolutionWebhookPayload payload) {
        switch (payload.event()) {
            case "QRCODE_UPDATED" -> handleQrCode(payload);
            case "CONNECTION_UPDATE" -> handleConnectionUpdate(payload);
            case "MESSAGES_UPSERT" ->
                    log.info("evolution_webhook event=MESSAGES_UPSERT instance={} (ignorado no MVP)",
                            payload.instance());
            default ->
                    log.info("evolution_webhook event={} instance={} (evento desconhecido, ignorado)",
                            payload.event(), payload.instance());
        }
    }

    private void handleQrCode(EvolutionWebhookPayload payload) {
        EvolutionConnection connection = findGlobalConnection(payload.instance());
        if (connection == null) {
            return;
        }
        String qrCode = extractQrBase64(payload.data());
        if (qrCode == null) {
            return;
        }
        connection.setQrCodeBase64(qrCode);
        if (connection.getConnectionState() == ConnectionState.CLOSE) {
            connection.setConnectionState(ConnectionState.CONNECTING);
        }
        connectionRepository.save(connection);
        log.info("evolution_webhook event=QRCODE_UPDATED instance={} qrSaved=true",
                payload.instance());
    }

    private void handleConnectionUpdate(EvolutionWebhookPayload payload) {
        EvolutionConnection connection = findGlobalConnection(payload.instance());
        if (connection == null || payload.data() == null) {
            return;
        }
        Map<String, Object> data = payload.data();
        Object rawState = data.get("state");
        String state = rawState != null ? String.valueOf(rawState) : null;
        if ("open".equalsIgnoreCase(state)) {
            connection.setConnectionState(ConnectionState.OPEN);
            connection.setConnectedNumber(extractNumber(data));
            connection.setQrCodeBase64(null);
            connectionRepository.save(connection);
            log.info("evolution_webhook event=CONNECTION_UPDATE instance={} state=open",
                    payload.instance());
        } else if ("close".equalsIgnoreCase(state)) {
            connection.setConnectionState(ConnectionState.CLOSE);
            connection.setConnectedNumber(null);
            connection.setQrCodeBase64(null);
            connectionRepository.save(connection);
            log.info("evolution_webhook event=CONNECTION_UPDATE instance={} state=close",
                    payload.instance());
        } else {
            log.info("evolution_webhook event=CONNECTION_UPDATE instance={} state={} (ignorado)",
                    payload.instance(), state);
        }
    }

    private EvolutionConnection findGlobalConnection(String instanceName) {
        if (instanceName == null || instanceName.isBlank()) {
            return null;
        }
        return connectionRepository.findByInstanceName(instanceName).orElse(null);
    }

    /**
     * Extrai o número do {@code data}: prioriza {@code data.number}; senão usa
     * {@code data.wuid} ({@code 5511999999999@s.whatsapp.net} → {@code 5511999999999}).
     */
    static String extractNumber(Map<String, Object> data) {
        Object number = data.get("number");
        if (number != null && !String.valueOf(number).isBlank()) {
            return String.valueOf(number);
        }
        Object wuid = data.get("wuid");
        return extractNumberFromWuid(wuid != null ? String.valueOf(wuid) : null);
    }

    /**
     * Normaliza um wuid da Evolution ({@code "5511999999999@s.whatsapp.net"})
     * para o número puro (parte antes de {@code @}).
     */
    public static String extractNumberFromWuid(String wuid) {
        if (wuid == null || wuid.isBlank()) {
            return null;
        }
        int at = wuid.indexOf('@');
        String number = at > 0 ? wuid.substring(0, at) : wuid;
        return number.isBlank() ? null : number;
    }

    /**
     * Extrai o QR base64 do {@code data} do QRCODE_UPDATED: prioriza
     * {@code data.base64}, depois {@code data.qrcode[.base64]}; normaliza para
     * data URI ({@code data:image/png;base64,...}) quando vier cru.
     */
    static String extractQrBase64(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object topLevel = data.get("base64");
        String raw = topLevel != null ? String.valueOf(topLevel) : null;
        if (raw == null || raw.isBlank()) {
            Object qrcode = data.get("qrcode");
            if (qrcode instanceof Map<?, ?> qrMap) {
                Object nested = qrMap.get("base64");
                raw = nested != null ? String.valueOf(nested) : null;
            } else if (qrcode != null) {
                raw = String.valueOf(qrcode);
            }
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.startsWith("data:") ? raw : "data:image/png;base64," + raw;
    }
}