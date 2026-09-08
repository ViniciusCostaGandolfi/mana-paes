-- V15: convites de motoboy + novos papéis (SYS_ADMIN/ADMIN/MOTOBOY)
-- email/password_hash passam a ser opcionais (motoboy pode ter só telefone);
-- o convite guarda token hex (64), expiração e data de aceite.

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users ADD COLUMN invitation_token VARCHAR(64);
ALTER TABLE users ADD COLUMN invitation_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN invitation_accepted_at TIMESTAMP;

-- Papéis antigos -> novos (REQUESTER/PRODUCTION deixam de existir)
UPDATE users SET role = 'ROLE_MOTOBOY' WHERE role = 'ROLE_REQUESTER';
UPDATE users SET role = 'ROLE_ADMIN'   WHERE role = 'ROLE_PRODUCTION';

CREATE INDEX idx_users_invitation_token ON users (invitation_token);