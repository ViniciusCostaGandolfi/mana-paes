package vgandolfi.dev.mana_paes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas da aplicação (prefixo {@code app.}).
 *
 * <p>Segredos (jwt.secret, evolution.apiKey) vêm de variáveis de ambiente.
 * O perfil dev tem um secret placeholder genérico; em produção ({@code prod})
 * o placeholder é obrigatório (sem default) — ver application-prod.yaml.</p>
 *
 * <p>{@code notifications.enabled} liga a pipeline assíncrona de notificações
 * (RabbitMQ + consumidor). Em dev/test fica {@code false}: o evento de pedido
 * criado é apenas logado ({@code LoggingOrderEventPublisher}).</p>
 *
 * <p>{@code scheduler.enabled} liga o agendador do relatório diário
 * ({@code @Scheduled}). Em dev/test fica {@code false} para não disparar
 * durante os testes.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Evolution evolution, Frontend frontend, Mail mail,
                            Notifications notifications, Scheduler scheduler) {

    public record Jwt(String secret, long expiration, long refreshExpiration) {
    }

    public record Evolution(String url, String globalApiKey) {
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