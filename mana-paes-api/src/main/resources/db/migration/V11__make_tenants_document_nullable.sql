-- V11: document (CNPJ) passa a ser opcional no cadastro inicial do tenant.
-- O tenant pode ser criado no registro do primeiro usuário sem CNPJ ainda preenchido.
ALTER TABLE tenants ALTER COLUMN document DROP NOT NULL;