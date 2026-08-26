-- V1: tenants
CREATE TABLE tenants (
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    document    VARCHAR(18)  NOT NULL,
    phone       VARCHAR(20),
    address     VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_tenants_document ON tenants (document);
CREATE INDEX idx_tenants_active ON tenants (active);