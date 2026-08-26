package vgandolfi.dev.mana_paes.infrastructure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vgandolfi.dev.mana_paes.config.AppProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente da Evolution API (WhatsApp).
 *
 * <p>Endpoints usados:</p>
 * <ul>
 *   <li>{@code POST /instance/create} — cria a instância global (integração
 *       WHATSAPP-BAILEYS) e devolve o token da instância (apikey);</li>
 *   <li>{@code GET /instance/connect/{instance}} — inicia a conexão e devolve
 *       o QR code (base64) para o scan;</li>
 *   <li>{@code GET /instance/connectionState/{instance}} — estado atual
 *       (open/close) e wuid do número conectado;</li>
 *   <li>{@code DELETE /instance/logout/{instance}} — encerra a conexão;</li>
 *   <li>{@code POST /message/sendText/{instance}} — envio de mensagens.</li>
 * </ul>
 *
 * <p>O header {@code apikey} é definido por chamada: a CHAVE GLOBAL
 * ({@code app.evolution.global-api-key}) para operações de gestão
 * (createInstance) e o token da instância para as demais. A instância usada é
 * sempre a GLOBAL ("mana-paes"), resolvida pelo chamador (serviço de conexão /
 * notificações).</p>
 *
 * <p>Em dev/test, sem {@code app.evolution.url}, lança
 * {@link EvolutionApiNotConfiguredException} — capturada pelos serviços, que
 * registram falha graciosa (NotificationLog FAILED, teste sem sucesso).</p>
 */
@Component
public class EvolutionApiClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiClient.class);

    private final RestClient restClient;
    private final AppProperties appProperties;

    public EvolutionApiClient(@Qualifier("evolutionApiRestClient") RestClient restClient,
                              AppProperties appProperties) {
        this.restClient = restClient;
        this.appProperties = appProperties;
    }

    /**
     * Cria a instância na Evolution API e retorna o token da instância
     * (instance_api_key). O token pode vir em {@code hash} (string) ou
     * {@code hash.apikey} (parse defensivo).
     *
     * @param webhookUrl URL do webhook do backend; {@code null}/vazio desabilita
     *                   o webhook na instância
     */
    public String createInstance(String instanceName, String webhookUrl) {
        baseUrlOrThrow();
        String globalKey = appProperties.evolution().globalApiKey();
        if (isBlank(globalKey)) {
            throw new EvolutionApiNotConfiguredException(
                    "Chave global da Evolution não configurada (app.evolution.global-api-key)");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("instanceName", instanceName);
        body.put("qrcode", false);
        body.put("integration", "WHATSAPP-BAILEYS");
        body.put("webhook", isBlank(webhookUrl)
                ? Map.of("enabled", false)
                : Map.of("enabled", true, "url", webhookUrl,
                "events", List.of("CONNECTION_UPDATE", "QRCODE_UPDATED")));

        try {
            JsonNode response = restClient.post()
                    .uri("/instance/create")
                    .header("apikey", globalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String token = extractInstanceApiKey(response);
            log.info("evolution_instance_created instance={}", instanceName);
            return token;
        } catch (RestClientException ex) {
            log.error("evolution_instance_create_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao criar instância na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Inicia a conexão e retorna o QR code como data URI
     * ({@code data:image/png;base64,...}) pronto para exibição.
     */
    public String connectInstance(String instanceName, String apiKey) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            JsonNode response = restClient.get()
                    .uri("/instance/connect/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            return extractQrCode(response);
        } catch (RestClientException ex) {
            log.error("evolution_connect_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao conectar instância na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /** Estado da conexão retornado por {@code /instance/connectionState}. */
    public record ConnectionStateInfo(String state, String wuid) {
    }

    public ConnectionStateInfo getConnectionState(String instanceName, String apiKey) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            JsonNode response = restClient.get()
                    .uri("/instance/connectionState/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            return parseConnectionState(response);
        } catch (RestClientException ex) {
            log.error("evolution_connection_state_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao consultar estado da instância na Evolution API: " + ex.getMessage(), ex);
        }
    }

    public void logoutInstance(String instanceName, String apiKey) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            restClient.delete()
                    .uri("/instance/logout/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_logout_success instance={}", instanceName);
        } catch (RestClientException ex) {
            log.error("evolution_logout_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao encerrar sessão na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Envia uma mensagem de texto. A instância e a chave vêm do chamador — para
     * a aplicação, SEMPRE a conexão GLOBAL ("mana-paes", token da instância
     * descriptografado). Lança {@link EvolutionApiException} em caso de falha
     * (não configurada, rede, HTTP != 2xx).
     */
    public void sendText(String instanceName, String apiKey, String number, String text) {
        baseUrlOrThrow();
        if (isBlank(instanceName)) {
            throw new EvolutionApiNotConfiguredException(
                    "Instância da Evolution API não configurada (instanceName)");
        }
        requireApiKey(apiKey);

        try {
            restClient.post()
                    .uri("/message/sendText/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("number", number, "text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_send_success instance={} recipientDigits={}", instanceName, number);
        } catch (RestClientException ex) {
            log.error("evolution_send_failed instance={} recipientDigits={}", instanceName, number, ex);
            throw new EvolutionApiException(
                    "Falha ao enviar mensagem via Evolution API: " + ex.getMessage(), ex);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers de parse (defensivos: toleram formatos diferentes da API)
    // ---------------------------------------------------------------------

    private String extractInstanceApiKey(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw new EvolutionApiException("Resposta inválida ao criar instância (sem corpo JSON)");
        }
        JsonNode hash = response.get("hash");
        if (hash != null) {
            if (hash.isTextual() && !hash.asText().isBlank()) {
                return hash.asText();
            }
            JsonNode apikey = hash.get("apikey");
            if (apikey != null && apikey.isTextual() && !apikey.asText().isBlank()) {
                return apikey.asText();
            }
        }
        JsonNode instance = response.get("instance");
        if (instance != null && instance.isObject()) {
            JsonNode apikey = instance.has("apikey") ? instance.get("apikey") : instance.get("apiKey");
            if (apikey != null && apikey.isTextual() && !apikey.asText().isBlank()) {
                return apikey.asText();
            }
        }
        throw new EvolutionApiException(
                "Não foi possível extrair o token da instância (apikey) da resposta do createInstance");
    }

    private String extractQrCode(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw new EvolutionApiException("Resposta inválida ao conectar instância (sem corpo JSON)");
        }
        String base64 = null;
        JsonNode qrcode = response.get("qrcode");
        if (qrcode != null && qrcode.isObject()) {
            JsonNode nested = qrcode.get("base64");
            if (nested != null && nested.isTextual() && !nested.asText().isBlank()) {
                base64 = nested.asText();
            } else if (qrcode.get("code") != null && qrcode.get("code").isTextual()) {
                base64 = qrcode.get("code").asText();
            }
        } else if (qrcode != null && qrcode.isTextual() && !qrcode.asText().isBlank()) {
            base64 = qrcode.asText();
        }
        if (base64 == null) {
            JsonNode topLevel = response.get("base64");
            if (topLevel != null && topLevel.isTextual() && !topLevel.asText().isBlank()) {
                base64 = topLevel.asText();
            }
        }
        if (base64 == null) {
            throw new EvolutionApiException(
                    "QR code não encontrado na resposta do connectInstance (esperava qrcode.base64 ou base64)");
        }
        return base64.startsWith("data:") ? base64 : "data:image/png;base64," + base64;
    }

    private ConnectionStateInfo parseConnectionState(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw new EvolutionApiException("Resposta inválida ao consultar connectionState (sem corpo JSON)");
        }
        String state = textOrNull(response, "state");
        String wuid = textOrNull(response, "wuid");
        JsonNode instance = response.get("instance");
        if (instance != null && instance.isObject()) {
            if (state == null) {
                state = textOrNull(instance, "state");
            }
            if (wuid == null) {
                wuid = textOrNull(instance, "wuid");
            }
        }
        return new ConnectionStateInfo(state, wuid);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            return value.asText();
        }
        return null;
    }

    private void requireApiKey(String apiKey) {
        if (isBlank(apiKey)) {
            throw new EvolutionApiNotConfiguredException("API key da Evolution não configurada");
        }
    }

    private String baseUrlOrThrow() {
        String baseUrl = appProperties.evolution().url();
        if (isBlank(baseUrl)) {
            throw new EvolutionApiNotConfiguredException(
                    "Evolution API não configurada (app.evolution.url vazio)");
        }
        return baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}