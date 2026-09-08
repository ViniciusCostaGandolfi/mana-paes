package vgandolfi.dev.mana_paes.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas da aplicação (prefixo {@code app.}).
 *
 * <p>Segredos (jwt.secret, evolution.apiKey, encryption.masterKey, senha do
 * system-admin) vêm de variáveis de ambiente. O perfil dev tem placeholders
 * genéricos; em produção ({@code prod}) os placeholders são obrigatórios (sem
 * default) — ver application-prod.yaml.</p>
 *
 * <p>{@code notifications.enabled} liga a pipeline assíncrona de notificações
 * (RabbitMQ + consumidor). Em dev/test fica {@code false}: o evento de pedido
 * criado é apenas logado ({@code LoggingOrderEventPublisher}).</p>
 *
 * <p>{@code scheduler.enabled} liga os agendadores (relatório diário e
 * lembretes). Em dev/test fica {@code false} para não disparar durante os
 * testes.</p>
 *
 * <p>{@code system-admin.email/password} define o primeiro {@code SYS_ADMIN}
 * do sistema, criado pelo {@code SystemAdminInitializer} em todos os perfis
 * (idempotente). Em produção é obrigatório (fail-fast); em dev/test há
 * default.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Encryption encryption, Evolution evolution, Backend backend,
                            Frontend frontend, Mail mail, Notifications notifications, Scheduler scheduler,
                            SystemAdmin systemAdmin) {

    public record Jwt(String secret, long expiration, long refreshExpiration) {
    }

    public record Encryption(String masterKey) {
    }

    public record Evolution(String url, String globalApiKey, long mockConnectDelayMs, String instanceName,
                            boolean mockSend) {
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

    public record SystemAdmin(String email, String password) {
    }
}