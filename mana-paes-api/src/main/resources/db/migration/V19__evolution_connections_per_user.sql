-- V19: conexão Evolution POR ADMIN (1 instância por usuário ADMIN).
-- user_id nullable para suportar a conexão global legada/estado sem dono;
-- UNIQUE em user_id garante 1 instância por ADMIN quando presente.

ALTER TABLE evolution_connections ADD COLUMN user_id UUID;

ALTER TABLE evolution_connections
    ADD CONSTRAINT fk_evolution_connections_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uk_evolution_connections_user_id ON evolution_connections (user_id);