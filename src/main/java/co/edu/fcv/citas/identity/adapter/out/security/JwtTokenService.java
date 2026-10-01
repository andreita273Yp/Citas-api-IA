package co.edu.fcv.citas.identity.adapter.out.security;

import co.edu.fcv.citas.identity.application.port.out.TokenPort;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT HS256 con secretos, tipos (`typ`) y duraciones distintos para access y refresh. */
@Component
public class JwtTokenService implements TokenPort {
    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(@Value("${app.jwt.access-secret}") String accessSecret, @Value("${app.jwt.refresh-secret}") String refreshSecret,
                           @Value("${app.jwt.access-ttl}") Duration accessTtl, @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        if (accessSecret.equals(refreshSecret)) throw new IllegalStateException("Access y refresh deben usar secretos distintos");
        this.accessKey = key(accessSecret);
        this.refreshKey = key(refreshSecret);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public Tokens issue(UserAccount account, String sessionId) {
        Instant now = Instant.now();
        Instant refreshExpiry = now.plus(refreshTtl);
        String subject = Long.toString(account.id());
        String access = Jwts.builder().subject(subject).claim("roles", account.roles().stream().map(Enum::name).sorted().toList())
                .claim("typ", "access").issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl))).signWith(accessKey).compact();
        String refresh = Jwts.builder().subject(subject).id(sessionId).claim("typ", "refresh")
                .issuedAt(Date.from(now)).expiration(Date.from(refreshExpiry)).signWith(refreshKey).compact();
        return new Tokens(access, accessTtl.toSeconds(), refresh, refreshExpiry);
    }

    @Override
    public RefreshClaims readRefresh(String refreshToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(refreshToken).getPayload();
            if (!"refresh".equals(claims.get("typ", String.class)) || claims.getId() == null) throw new IdentityException.InvalidSession();
            return new RefreshClaims(Long.parseLong(claims.getSubject()), claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IdentityException.InvalidSession();
        }
    }

    /** Usado por el filtro HTTP: devuelve el principal solo para un access válido. */
    public Optional<AccessPrincipal> readAccess(String accessToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(accessToken).getPayload();
            if (!"access".equals(claims.get("typ", String.class))) return Optional.empty();
            List<?> roles = claims.get("roles", List.class);
            return Optional.of(new AccessPrincipal(claims.getSubject(), roles == null ? List.of() : roles.stream().map(String::valueOf).toList()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration refreshTtl() { return refreshTtl; }

    private static SecretKey key(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("JWT secret must be at least 32 bytes");
        return Keys.hmacShaKeyFor(value.getBytes(StandardCharsets.UTF_8));
    }

    public record AccessPrincipal(String userId, List<String> roles) { }
}
