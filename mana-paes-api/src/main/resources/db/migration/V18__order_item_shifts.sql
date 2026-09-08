-- V18: pedidos por turno (manhã/tarde/noite) + opções escolhidas no item.
-- A coluna legacy order_items.quantity fica nullable (não mapeada pela entidade).

ALTER TABLE order_items ADD COLUMN quantidade_manha NUMERIC(19,3) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN quantidade_tarde NUMERIC(19,3) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN quantidade_noite NUMERIC(19,3) NOT NULL DEFAULT 0;
ALTER TABLE order_items ALTER COLUMN quantity DROP NOT NULL;

CREATE TABLE order_item_options (
    id                UUID           NOT NULL PRIMARY KEY,
    order_item_id     UUID           NOT NULL,
    option_name       VARCHAR(100)   NOT NULL,
    price_adjustment  NUMERIC(19,2)  NOT NULL DEFAULT 0,
    option_id         UUID,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL,
    CONSTRAINT fk_order_item_options_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_item_options_order_item_id ON order_item_options (order_item_id);

-- Notificações: modelo WhatsApp-first (email apenas recuperação de senha)
UPDATE notification_configs SET whatsapp_enabled = TRUE WHERE whatsapp_enabled = FALSE AND email_enabled = TRUE;
UPDATE notification_configs SET email_enabled = FALSE WHERE email_enabled = TRUE;