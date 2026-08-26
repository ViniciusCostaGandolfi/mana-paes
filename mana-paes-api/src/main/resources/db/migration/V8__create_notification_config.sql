-- V8: notification_configs (1:1 com tenants)
CREATE TABLE notification_configs (
    id                          UUID         NOT NULL PRIMARY KEY,
    tenant_id                   UUID         NOT NULL,
    admin_whatsapp_number       VARCHAR(20),
    admin_email                 VARCHAR(150),
    daily_report_time           TIME         NOT NULL,
    whatsapp_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    email_enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    evolution_api_instance_name VARCHAR(100),
    evolution_api_key           VARCHAR(255),
    created_at                  TIMESTAMP    NOT NULL,
    updated_at                  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_notification_configs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uk_notification_configs_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_notification_configs_tenant_id ON notification_configs (tenant_id);