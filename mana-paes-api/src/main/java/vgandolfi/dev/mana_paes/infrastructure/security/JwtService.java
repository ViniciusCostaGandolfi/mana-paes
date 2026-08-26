package vgandolfi.dev.mana_paes.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Geração e validação de JWT (HS256).
 *
 * <p>Access token: claims {@code sub} (userId), {@code email}, {@code role},
 * {@code tenantId}, {@code token_type=ACCESS} e expiração
 * {@code app.jwt.expiration}.</p>
 *
 * <p>Refresh token: refresh token stateless — mesmo algoritmo, claim
 * {@code token_type=REFRESH} e expiração {@code app.jwt.refresh-expiration}.
 * Não há persistência em banco.</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret não configurado. Defina a variável JWT_SECRET (mínimo 32 caracteres para HS256).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TENANT_ID, user.getTenant().getId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(appProperties.jwt().expiration())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(appProperties.jwt().refreshExpiration())))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida assinatura + expiração e exige {@code token_type=ACCESS}.
     */
    public JwtClaims parseAccessToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new MalformedJwtException("Token não é do tipo ACCESS");
        }
        return toClaims(claims);
    }

    /**
     * Valida assinatura + expiração e exige {@code token_type=REFRESH}.
     *
     * @return userId (sub)
     */
    public UUID parseRefreshToken(String token) {
        Claims claims = parseSignedClaims(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new MalformedJwtException("Token não é do tipo REFRESH");
        }
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtClaims toClaims(Claims claims) {
        return new JwtClaims(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_ROLE, String.class),
                UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class)),
                claims.getExpiration().toInstant());
    }
}