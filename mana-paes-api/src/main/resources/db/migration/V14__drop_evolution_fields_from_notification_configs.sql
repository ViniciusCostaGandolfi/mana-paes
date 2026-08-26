-- V14: remove campos de Evolution por-tenant de notification_configs.
-- A conexao WhatsApp passa a ser GLOBAL (evolution_connections, V13); o token
-- da instancia nunca mais fica em texto puro na config do tenant.
ALTER TABLE notification_configs DROP COLUMN evolution_api_instance_name;
ALTER TABLE notification_configs DROP COLUMN evolution_api_key;