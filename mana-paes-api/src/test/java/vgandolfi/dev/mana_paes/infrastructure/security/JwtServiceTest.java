package vgandolfi.dev.mana_paes.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-1234";

    private AppProperties appProperties(long expirationMs) {
        return new AppProperties(
                new AppProperties.Jwt(SECRET, expirationMs, 86400000L),
                new AppProperties.Evolution("", ""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    private User user(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        User user = new User();
        user.setId(id);
        user.setEmail("joao@example.com");
        user.setRole(UserRole.ROLE_ADMIN);
        user.setTenant(tenant);
        return user;
    }

    @Test
    void accessTokenRoundTrip() {
        JwtService jwtService = new JwtService(appProperties(3600000L));
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        String token = jwtService.generateAccessToken(user);
        JwtClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo("joao@example.com");
        assertThat(claims.role()).isEqualTo("ROLE_ADMIN");
        assertThat(claims.tenantId()).isEqualTo(user.getTenant().getId());
    }

    @Test
    void refreshTokenReturnsUserId() {
        JwtService jwtService = new JwtService(appProperties(3600000L));
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(user(userId));

        assertThat(jwtService.parseRefreshToken(token)).isEqualTo(userId);
    }

    @Test
    void refreshTokenRejectedAsAccessToken() {
        JwtService jwtService = new JwtService(appProperties(3600000L));
        String refresh = jwtService.generateRefreshToken(user(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.parseAccessToken(refresh))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void accessTokenRejectedAsRefreshToken() {
        JwtService jwtService = new JwtService(appProperties(3600000L));
        String access = jwtService.generateAccessToken(user(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.parseRefreshToken(access))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void expiredTokenRejected() {
        JwtService jwtService = new JwtService(appProperties(-1000L));
        String token = jwtService.generateAccessToken(user(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenRejected() {
        JwtService jwtService = new JwtService(appProperties(3600000L));
        String token = jwtService.generateAccessToken(user(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.parseAccessToken(token + "x"))
                .isInstanceOfAny(MalformedJwtException.class, io.jsonwebtoken.JwtException.class);
    }
}