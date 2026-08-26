-- V13: evolution_connections (conexao WhatsApp GLOBAL - instancia unica "mana-paes")
-- Singleton sem tenant_id; o token da instancia (instance_api_key) e criptografado.
CREATE TABLE evolution_connections (
    id                UUID         NOT NULL PRIMARY KEY,
    instance_name     VARCHAR(100) NOT NULL,
    instance_api_key  VARCHAR(512) NOT NULL,
    connection_state  VARCHAR(20)  NOT NULL DEFAULT 'CLOSE',
    connected_number  VARCHAR(20),
    qr_code_base64    TEXT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uk_evolution_connections_instance_name UNIQUE (instance_name)
);