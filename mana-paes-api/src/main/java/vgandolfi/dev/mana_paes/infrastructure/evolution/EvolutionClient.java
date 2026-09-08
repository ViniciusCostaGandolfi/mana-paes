package vgandolfi.dev.mana_paes.infrastructure.evolution;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiException;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiNotConfiguredException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente completo da Evolution API (v2.3.7) — TODOS os endpoints relevantes
 * de instância e mensagem, usando o mesmo padrão do cliente anterior
 * ({@link RestClient} {@code evolutionApiRestClient}, header {@code apikey},
 * baseUrl de {@code app.evolution.url}).
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>apenas {@code createInstance} usa a CHAVE GLOBAL
 *       ({@code app.evolution.globalApiKey}); todos os demais métodos recebem
 *       {@code instanceName} + {@code apiKey} do chamador (token da instância);</li>
 *   <li>URL/chave em branco → {@link EvolutionApiNotConfiguredException};</li>
 *   <li>respostas não-2xx → {@link EvolutionApiException} (o chamador decide o
 *       fallback).</li>
 * </ul>
 */
@Component
public class EvolutionClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionClient.class);

    private final RestClient restClient;
    private final AppProperties appProperties;

    public EvolutionClient(RestClient evolutionApiRestClient, AppProperties appProperties) {
        this.restClient = evolutionApiRestClient;
        this.appProperties = appProperties;
    }

    // ------------------------------------------------------------------
    // Instance
    // ------------------------------------------------------------------

    /**
     * Cria a instância na Evolution API (header apikey = chave GLOBAL) e
     * devolve o resultado com o token da instância (instance_api_key).
     *
     * @param webhookUrl URL do webhook do backend; {@code null}/vazio desabilita
     *                   o webhook na instância
     */
    public CreateInstanceResult createInstance(String instanceName, String webhookUrl) {
        baseUrlOrThrow();
        String globalKey = appProperties.evolution().globalApiKey();
        if (isBlank(globalKey)) {
            throw new EvolutionApiNotConfiguredException(
                    "Chave global da Evolution não configurada (app.evolution.global-api-key)");
        }

        Map<String, Object> body = new java.util.HashMap<>();
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
            CreateInstanceResult result = parseCreateInstance(response, instanceName);
            log.info("evolution_instance_created instance={}", instanceName);
            return result;
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

    /**
     * Lista todas as instâncias da conta ({@code GET /instance/fetchInstances},
     * header apikey = chave GLOBAL).
     */
    public List<InstanceInfo> fetchInstances() {
        baseUrlOrThrow();
        String globalKey = appProperties.evolution().globalApiKey();
        requireApiKey(globalKey);
        try {
            JsonNode response = restClient.get()
                    .uri("/instance/fetchInstances")
                    .header("apikey", globalKey)
                    .retrieve()
                    .body(JsonNode.class);
            return parseInstances(response);
        } catch (RestClientException ex) {
            log.error("evolution_fetch_instances_failed", ex);
            throw new EvolutionApiException(
                    "Falha ao listar instâncias na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /** Reinicia a instância ({@code POST /instance/restart/{instance}}). */
    public void restartInstance(String instanceName, String apiKey) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            restClient.post()
                    .uri("/instance/restart/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_instance_restarted instance={}", instanceName);
        } catch (RestClientException ex) {
            log.error("evolution_instance_restart_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao reiniciar instância na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /** Encerra a sessão da instância ({@code DELETE /instance/logout/{instance}}). */
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

    /** Exclui a instância ({@code DELETE /instance/delete/{instance}}). */
    public void deleteInstance(String instanceName, String apiKey) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            restClient.delete()
                    .uri("/instance/delete/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_instance_deleted instance={}", instanceName);
        } catch (RestClientException ex) {
            log.error("evolution_instance_delete_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao excluir instância na Evolution API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Define a presença da instância ({@code POST /instance/setPresence/{instance}}).
     *
     * @param presence available | unavailable | composing | recording | paused
     */
    public void setPresence(String instanceName, String apiKey, String presence) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            restClient.post()
                    .uri("/instance/setPresence/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("presence", presence))
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_presence_set instance={} presence={}", instanceName, presence);
        } catch (RestClientException ex) {
            log.error("evolution_set_presence_failed instance={}", instanceName, ex);
            throw new EvolutionApiException(
                    "Falha ao definir presença na Evolution API: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------
    // Message
    // ------------------------------------------------------------------

    /**
     * Envia uma mensagem de texto ({@code POST /message/sendText/{instance}}).
     * Body {@code {number, text}} (formato do curl/Postman real do usuário).
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

    /**
     * Envia mídia ({@code POST /message/sendMedia/{instance}}) com body
     * {@code {number, mediatype, media, caption?}}.
     */
    public void sendMedia(String instanceName, String apiKey, String number, String mediaType,
                          String media, String caption) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("number", number);
            body.put("mediatype", mediaType);
            body.put("media", media);
            if (!isBlank(caption)) {
                body.put("caption", caption);
            }
            restClient.post()
                    .uri("/message/sendMedia/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_media_sent instance={} recipientDigits={}", instanceName, number);
        } catch (RestClientException ex) {
            log.error("evolution_send_media_failed instance={} recipientDigits={}", instanceName, number, ex);
            throw new EvolutionApiException(
                    "Falha ao enviar mídia via Evolution API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Envia botões interativos ({@code POST /message/sendButtons/{instance}}) com
     * body {@code {number, title, description, footer?, buttons:[]}}.
     */
    public void sendButtons(String instanceName, String apiKey, String number, String title,
                            String description, String footer, List<Map<String, Object>> buttons) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("number", number);
            body.put("title", title);
            body.put("description", description);
            if (!isBlank(footer)) {
                body.put("footer", footer);
            }
            body.put("buttons", buttons == null ? List.of() : buttons);
            restClient.post()
                    .uri("/message/sendButtons/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_buttons_sent instance={} recipientDigits={}", instanceName, number);
        } catch (RestClientException ex) {
            log.error("evolution_send_buttons_failed instance={} recipientDigits={}", instanceName, number, ex);
            throw new EvolutionApiException(
                    "Falha ao enviar botões via Evolution API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Envia lista interativa ({@code POST /message/sendList/{instance}}) com
     * body {@code {number, title, description, buttonText?, sections:[]}}.
     */
    public void sendList(String instanceName, String apiKey, String number, String title,
                         String description, String buttonText, List<Map<String, Object>> sections) {
        baseUrlOrThrow();
        requireApiKey(apiKey);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("number", number);
            body.put("title", title);
            body.put("description", description);
            if (!isBlank(buttonText)) {
                body.put("buttonText", buttonText);
            }
            body.put("sections", sections == null ? List.of() : sections);
            restClient.post()
                    .uri("/message/sendList/{instance}", instanceName)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evolution_list_sent instance={} recipientDigits={}", instanceName, number);
        } catch (RestClientException ex) {
            log.error("evolution_send_list_failed instance={} recipientDigits={}", instanceName, number, ex);
            throw new EvolutionApiException(
                    "Falha ao enviar lista via Evolution API: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------
    // Records
    // ------------------------------------------------------------------

    /** Resultado do {@code createInstance}. */
    public record CreateInstanceResult(String instanceName, String instanceId, String status, String token) {
    }

    /** Estado da conexão ({@code state} open/close + {@code wuid}). */
    public record ConnectionStateInfo(String state, String wuid) {
    }

    /** Instância listada por {@code fetchInstances}. */
    public record InstanceInfo(String instanceName, String status, String connectionStatus) {
    }

    // ------------------------------------------------------------------
    // Helpers de parse (defensivos: toleram formatos diferentes da API)
    // ------------------------------------------------------------------

    private CreateInstanceResult parseCreateInstance(JsonNode response, String requestedName) {
        if (response == null || !response.isObject()) {
            throw new EvolutionApiException("Resposta inválida ao criar instância (sem corpo JSON)");
        }
        String instanceName = requestedName;
        String instanceId = null;
        String status = null;
        JsonNode instance = response.get("instance");
        if (instance != null && instance.isObject()) {
            instanceName = textOrNull(instance, "instanceName") != null
                    ? textOrNull(instance, "instanceName") : instanceName;
            instanceId = textOrNull(instance, "id") != null ? textOrNull(instance, "id")
                    : textOrNull(instance, "instanceId");
            status = textOrNull(instance, "status");
        }
        String token = extractToken(response, instance);
        if (token == null) {
            throw new EvolutionApiException(
                    "Não foi possível extrair o token da instância (apikey) da resposta do createInstance");
        }
        return new CreateInstanceResult(instanceName, instanceId, status, token);
    }

    private String extractToken(JsonNode response, JsonNode instance) {
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
        if (instance != null && instance.isObject()) {
            JsonNode apikey = instance.has("apikey") ? instance.get("apikey") : instance.get("apiKey");
            if (apikey != null && apikey.isTextual() && !apikey.asText().isBlank()) {
                return apikey.asText();
            }
        }
        return null;
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

    private List<InstanceInfo> parseInstances(JsonNode response) {
        List<InstanceInfo> result = new ArrayList<>();
        if (response == null || !response.isArray()) {
            return result;
        }
        for (JsonNode node : response) {
            if (!node.isObject()) {
                continue;
            }
            result.add(new InstanceInfo(
                    textOrNull(node, "instanceName"),
                    textOrNull(node, "status"),
                    textOrNull(node, "connectionStatus")));
        }
        return result;
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

    private void baseUrlOrThrow() {
        String baseUrl = appProperties.evolution().url();
        if (isBlank(baseUrl)) {
            throw new EvolutionApiNotConfiguredException(
                    "Evolution API não configurada (app.evolution.url vazio)");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}