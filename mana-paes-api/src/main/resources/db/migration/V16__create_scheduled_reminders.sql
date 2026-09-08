-- V16: lembretes agendados para motoboys (disparo diário recorrente).
-- Idempotência por last_sent_date (máx. 1 envio/dia por lembrete).

CREATE TABLE scheduled_reminders (
    id              UUID         NOT NULL PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    reminder_time   TIME         NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    email_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    last_sent_date  DATE,
    created_by      UUID,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_scheduled_reminders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_scheduled_reminders_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_scheduled_reminders_tenant_id ON scheduled_reminders (tenant_id);
CREATE INDEX idx_scheduled_reminders_enabled_time ON scheduled_reminders (enabled, reminder_time);