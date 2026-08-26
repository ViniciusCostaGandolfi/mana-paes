package vgandolfi.dev.mana_paes.infrastructure.security;

import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;

import java.util.UUID;

/**
 * Principal autenticado, criado pelo {@link JwtAuthFilter} a partir dos claims
 * do access token (autenticação stateless, sem consulta ao banco por requisição).
 */
public record AuthenticatedUser(UUID userId, String email, UUID tenantId, UserRole role) {
}