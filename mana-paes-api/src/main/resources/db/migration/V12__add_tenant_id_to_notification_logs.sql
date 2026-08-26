-- V12: notification_logs ganha tenant_id para escopo multi-tenant na consulta de logs
ALTER TABLE notification_logs ADD COLUMN tenant_id UUID;

CREATE INDEX idx_notification_logs_tenant_id ON notification_logs (tenant_id);