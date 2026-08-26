-- V5: order_items
CREATE TABLE order_items (
    id          UUID          NOT NULL PRIMARY KEY,
    order_id    UUID          NOT NULL,
    product_id  UUID          NOT NULL,
    quantity    NUMERIC(19,3) NOT NULL,
    unit_price  NUMERIC(19,2) NOT NULL,
    subtotal    NUMERIC(19,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);