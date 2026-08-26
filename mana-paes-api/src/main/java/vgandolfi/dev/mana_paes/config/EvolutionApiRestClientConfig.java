package vgandolfi.dev.mana_paes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP para a Evolution API (via Spring {@link RestClient}).
 *
 * <p>URL base vem de {@code app.evolution.url} (vazia em dev/test). Timeouts
 * curtos (5s connect / 10s read) para falhar rápido e não segurar threads do
 * listener. O header {@code apikey} é definido por chamada (chave global ou a
 * chave por-tenant da {@code NotificationConfig}).</p>
 */
@Configuration
public class EvolutionApiRestClientConfig {

    @Bean
    public RestClient evolutionApiRestClient(AppProperties appProperties) {
        String baseUrl = appProperties.evolution().url();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }
}