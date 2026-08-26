-- V6: order_status_history
CREATE TABLE order_status_history (
    id               UUID         NOT NULL PRIMARY KEY,
    order_id         UUID         NOT NULL,
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20)  NOT NULL,
    changed_by       UUID         NOT NULL,
    changed_at       TIMESTAMP    NOT NULL,
    reason           VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_osh_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_osh_order_id ON order_status_history (order_id);
CREATE INDEX idx_osh_changed_by ON order_status_history (changed_by);