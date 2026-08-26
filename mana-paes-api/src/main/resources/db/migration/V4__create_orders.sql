-- V4: orders
CREATE TABLE orders (
    id            UUID          NOT NULL PRIMARY KEY,
    tenant_id     UUID          NOT NULL,
    requester_id  UUID          NOT NULL,
    delivery_date DATE          NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    total_amount  NUMERIC(19,2) NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_orders_requester FOREIGN KEY (requester_id) REFERENCES users (id)
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);
CREATE INDEX idx_orders_requester_id ON orders (requester_id);
CREATE INDEX idx_orders_delivery_date ON orders (delivery_date);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_tenant_delivery_date ON orders (tenant_id, delivery_date);
CREATE INDEX idx_orders_tenant_status_delivery ON orders (tenant_id, status, delivery_date);