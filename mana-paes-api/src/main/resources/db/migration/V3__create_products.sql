-- V3: products
CREATE TABLE products (
    id           UUID          NOT NULL PRIMARY KEY,
    tenant_id    UUID          NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    unit_price   NUMERIC(19,2) NOT NULL,
    unit_measure VARCHAR(10)   NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL,
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_products_tenant_active ON products (tenant_id, active);