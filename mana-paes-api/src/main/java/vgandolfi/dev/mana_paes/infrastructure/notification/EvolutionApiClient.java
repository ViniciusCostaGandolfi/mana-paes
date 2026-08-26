package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vgandolfi.dev.mana_paes.config.AppProperties;

import java.util.Map;

/**
 * Cliente da Evolution API (envio de mensagens WhatsApp).
 *
 * <p>Endpoint: {@code POST {baseUrl}/message/sendText/{instance}} com header
 * {@code apikey} e corpo {@code {number, text}}. A instância e a chave podem
 * vir da {@code NotificationConfig} do tenant (override) ou dos valores globais
 * {@code app.evolution.*}.</p>
 *
 * <p>Em dev/test, sem {@code app.evolution.url}, lança
 * {@link EvolutionApiNotConfiguredException} — capturada pelo serviço, que
 * registra o {@code NotificationLog} como FAILED sem derrubar o fluxo.</p>
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
     * Envia uma mensagem de texto. Lança {@link EvolutionApiException} em caso
     * de falha (não configurada, rede, HTTP != 2xx).
     */
    public void sendText(String instanceName, String tenantApiKey, String number, String text) {
        String baseUrl = appProperties.evolution().url();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new EvolutionApiNotConfiguredException(
                    "Evolution API não configurada (app.evolution.url vazio)");
        }
        if (instanceName == null || instanceName.isBlank()) {
            throw new EvolutionApiNotConfiguredException(
                    "Instância da Evolution API não configurada (evolutionApiInstanceName)");
        }
        String effectiveApiKey = (tenantApiKey != null && !tenantApiKey.isBlank())
                ? tenantApiKey
                : appProperties.evolution().globalApiKey();
        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            throw new EvolutionApiNotConfiguredException("API key da Evolution não configurada");
        }

        try {
            restClient.post()
                    .uri("/message/sendText/{instance}", instanceName)
                    .header("apikey", effectiveApiKey)
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
}