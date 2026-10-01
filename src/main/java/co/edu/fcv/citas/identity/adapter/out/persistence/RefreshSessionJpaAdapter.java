package co.edu.fcv.citas.identity.adapter.out.persistence;

import co.edu.fcv.citas.identity.application.port.out.RefreshSessionPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
class RefreshSessionJpaAdapter implements RefreshSessionPort {
    private final RefreshTokenJpaRepository tokens;

    RefreshSessionJpaAdapter(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public void open(long userId, String sessionHash, Instant expiresAt) {
        tokens.save(new RefreshTokenEntity(userId, sessionHash, LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())));
    }

    @Override
    public boolean consume(String sessionHash) {
        LocalDateTime now = LocalDateTime.now();
        return tokens.findByTokenHash(sessionHash).filter(t -> t.activeAt(now)).map(t -> {
            t.revoke(now);
            return true;
        }).orElse(false);
    }

    @Override
    public void revoke(String sessionHash) {
        LocalDateTime now = LocalDateTime.now();
        tokens.findByTokenHash(sessionHash).filter(t -> t.activeAt(now)).ifPresent(t -> t.revoke(now));
    }
}
