-- V17: vendedores (sem login), opções de produto, remoção do convite de motoboy.
-- - orders.requester_id passa a ser nullable (pedido pode vir do link público via vendor)
-- - nova tabela vendors (telefone identifica o link público)
-- - usuários MOTOBOY (legado) são migrados para vendors e removidos de users
-- - colunas de convite removidas de users
-- - opções de produto (grupos -> opções com preço adicional)
-- - scheduled_reminders ganha delivery_day_offset (data de entrega do link)

-- Pedidos órfãos de motoboys ficam sem requester (pedido passará a ser via vendor)
ALTER TABLE orders ALTER COLUMN requester_id DROP NOT NULL;
ALTER TABLE order_status_history ALTER COLUMN changed_by DROP NOT NULL;

CREATE TABLE vendors (
    id               UUID         NOT NULL PRIMARY KEY,
    tenant_id        UUID         NOT NULL,
    name             VARCHAR(150) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    whatsapp_number  VARCHAR(20),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT fk_vendors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uk_vendors_tenant_phone UNIQUE (tenant_id, phone)
);

CREATE INDEX idx_vendors_tenant_id ON vendors (tenant_id);
CREATE INDEX idx_vendors_phone ON vendors (phone);

ALTER TABLE orders ADD COLUMN vendor_id UUID REFERENCES vendors (id);
CREATE INDEX idx_orders_vendor_id ON orders (vendor_id);

-- Migra motoboys (legado) para vendedores e remove o papel inexistente
INSERT INTO vendors (id, tenant_id, name, phone, whatsapp_number, active, created_at, updated_at)
SELECT gen_random_uuid(), tenant_id, name, COALESCE(whatsapp_number, phone), whatsapp_number, active, created_at, updated_at
FROM users
WHERE role = 'ROLE_MOTOBOY';

DELETE FROM users WHERE role = 'ROLE_MOTOBOY';

ALTER TABLE users DROP COLUMN invitation_token;
ALTER TABLE users DROP COLUMN invitation_expires_at;
ALTER TABLE users DROP COLUMN invitation_accepted_at;

-- Opções de produto: grupos -> opções (price_adjustment soma ao unit_price base)
CREATE TABLE product_option_groups (
    id          UUID          NOT NULL PRIMARY KEY,
    product_id  UUID          NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    required    BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order  INT           NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_product_option_groups_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_option_groups_product_id ON product_option_groups (product_id);

CREATE TABLE product_options (
    id                UUID           NOT NULL PRIMARY KEY,
    option_group_id   UUID           NOT NULL,
    name              VARCHAR(100)   NOT NULL,
    price_adjustment  NUMERIC(19,2)  NOT NULL DEFAULT 0,
    sort_order        INT            NOT NULL DEFAULT 0,
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL,
    CONSTRAINT fk_product_options_group FOREIGN KEY (option_group_id) REFERENCES product_option_groups (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_options_group_id ON product_options (option_group_id);

-- Lembretes: deslocamento da data de entrega do link (default 1 = amanhã)
ALTER TABLE scheduled_reminders ADD COLUMN delivery_day_offset INT NOT NULL DEFAULT 1;