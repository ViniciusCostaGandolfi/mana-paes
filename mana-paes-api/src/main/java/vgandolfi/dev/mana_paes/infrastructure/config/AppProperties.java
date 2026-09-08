package vgandolfi.dev.mana_paes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas da aplicação (prefixo {@code app.}).
 *
 * <p>Segredos (jwt.secret, evolution.apiKey, encryption.masterKey) vêm de
 * variáveis de ambiente. O perfil dev tem placeholders genéricos; em produção
 * ({@code prod}) os placeholders são obrigatórios (sem default) — ver
 * application-prod.yaml.</p>
 *
 * <p>{@code notifications.enabled} liga a pipeline assíncrona de notificações
 * (RabbitMQ + consumidor). Em dev/test fica {@code false}: o evento de pedido
 * criado é apenas logado ({@code LoggingOrderEventPublisher}).</p>
 *
 * <p>{@code scheduler.enabled} liga o agendador do relatório diário
 * ({@code @Scheduled}). Em dev/test fica {@code false} para não disparar
 * durante os testes.</p>
 *
 * <p>{@code evolution.url} em branco (dev/test) ativa a implementação MOCK da
 * conexão WhatsApp; preenchida (prod) ativa a implementação real via Evolution
 * API. {@code evolution.mockConnectDelayMs} controla a transição automática
 * CONNECTING → OPEN do mock. {@code backend.url} é usado para montar a URL do
 * webhook da Evolution API registrada na instância.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Encryption encryption, Evolution evolution, Backend backend,
                            Frontend frontend, Mail mail, Notifications notifications, Scheduler scheduler) {

    public record Jwt(String secret, long expiration, long refreshExpiration) {
    }

    public record Encryption(String masterKey) {
    }

    public record Evolution(String url, String globalApiKey, long mockConnectDelayMs) {
    }

    public record Backend(String url) {
    }

    public record Frontend(String url) {
    }

    public record Mail(boolean enabled) {
    }

    public record Notifications(boolean enabled, int maxRetries) {
    }

    public record Scheduler(boolean enabled) {
    }
}