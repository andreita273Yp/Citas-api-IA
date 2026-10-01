package co.edu.fcv.citas.identity.adapter.out.persistence;

import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.ResetToken;
import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.TokenStorePort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class PasswordResetTokenJpaAdapter implements TokenStorePort {
    private final PasswordResetTokenJpaRepository tokens;

    PasswordResetTokenJpaAdapter(PasswordResetTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public void invalidateActive(long userId) {
        tokens.invalidateActive(userId, LocalDateTime.now());
    }

    @Override
    public void store(long userId, String tokenHash, Instant expiresAt) {
        PasswordResetTokenEntity e = new PasswordResetTokenEntity();
        e.userId = userId;
        e.tokenHash = tokenHash;
        e.expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
        tokens.saveAndFlush(e);
    }

    @Override
    public Optional<ResetToken> lockValid(String tokenHash) {
        return tokens.lockValid(tokenHash, LocalDateTime.now()).map(t -> new ResetToken(t.id, t.userId));
    }

    @Override
    public void markUsed(long tokenId) {
        tokens.findById(tokenId).ifPresent(t -> {
            t.usedAt = LocalDateTime.now();
            tokens.flush();
        });
    }
}
