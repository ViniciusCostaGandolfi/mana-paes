-- V7: daily_reports + daily_report_items
CREATE TABLE daily_reports (
    id            UUID          NOT NULL PRIMARY KEY,
    tenant_id     UUID          NOT NULL,
    report_date   DATE          NOT NULL,
    total_amount  NUMERIC(19,2) NOT NULL,
    total_orders  INTEGER       NOT NULL,
    sent          BOOLEAN       NOT NULL DEFAULT FALSE,
    generated_at  TIMESTAMP     NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_daily_reports_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uk_daily_reports_tenant_date UNIQUE (tenant_id, report_date)
);

CREATE INDEX idx_daily_reports_tenant_id ON daily_reports (tenant_id);

CREATE TABLE daily_report_items (
    id             UUID          NOT NULL PRIMARY KEY,
    report_id      UUID          NOT NULL,
    product_id     UUID          NOT NULL,
    total_quantity NUMERIC(19,3) NOT NULL,
    total_amount   NUMERIC(19,2) NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    CONSTRAINT fk_dri_report FOREIGN KEY (report_id) REFERENCES daily_reports (id),
    CONSTRAINT fk_dri_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_dri_report_id ON daily_report_items (report_id);
CREATE INDEX idx_dri_product_id ON daily_report_items (product_id);