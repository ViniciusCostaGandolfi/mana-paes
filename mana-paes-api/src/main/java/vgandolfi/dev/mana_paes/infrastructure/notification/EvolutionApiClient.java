package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.infrastructure.evolution.EvolutionClient;

/**
 * Wrapper fino sobre o {@link EvolutionClient} com a API usada pela aplicação
 * (os 5 métodos do MVP). Mantém a compatibilidade com os chamadores existentes
 * ({@code EvolutionConnectionServiceImpl}, {@code NotificationServiceImpl}) e
 * os testes; o {@link EvolutionClient} concentra TODOS os endpoints da
 * Evolution API v2.3.7.
 */
@Component
public class EvolutionApiClient {

    private final EvolutionClient evolutionClient;

    public EvolutionApiClient(@Qualifier("evolutionApiRestClient") RestClient restClient,
                              AppProperties appProperties) {
        this.evolutionClient = new EvolutionClient(restClient, appProperties);
    }

    /** Cria a instância (chave global) e retorna o token da instância. */
    public String createInstance(String instanceName, String webhookUrl) {
        return evolutionClient.createInstance(instanceName, webhookUrl).token();
    }

    /** Conecta a instância e retorna o QR code (data URI). */
    public String connectInstance(String instanceName, String apiKey) {
        return evolutionClient.connectInstance(instanceName, apiKey);
    }

    /** Estado da conexão (state/wuid). */
    public EvolutionClient.ConnectionStateInfo getConnectionState(String instanceName, String apiKey) {
        return evolutionClient.getConnectionState(instanceName, apiKey);
    }

    /** Encerra a sessão da instância. */
    public void logoutInstance(String instanceName, String apiKey) {
        evolutionClient.logoutInstance(instanceName, apiKey);
    }

    /** Envia mensagem de texto. */
    public void sendText(String instanceName, String apiKey, String number, String text) {
        evolutionClient.sendText(instanceName, apiKey, number, text);
    }
}