package vgandolfi.dev.mana_paes.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

/**
 * Claims extraídos de um access token JWT válido.
 */
public record JwtClaims(UUID userId, String email, String role, UUID tenantId, Instant expiresAt) {
}