-- V9: notification_logs
CREATE TABLE notification_logs (
    id            UUID          NOT NULL PRIMARY KEY,
    order_id      UUID,
    channel       VARCHAR(20)   NOT NULL,
    type          VARCHAR(40)   NOT NULL,
    recipient     VARCHAR(150)  NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    content       VARCHAR(4000),
    error_message VARCHAR(1000),
    retry_count   INTEGER       NOT NULL DEFAULT 0,
    sent_at       TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_notification_logs_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_notification_logs_order_id ON notification_logs (order_id);
CREATE INDEX idx_notification_logs_status ON notification_logs (status);
CREATE INDEX idx_notification_logs_created_at ON notification_logs (created_at);