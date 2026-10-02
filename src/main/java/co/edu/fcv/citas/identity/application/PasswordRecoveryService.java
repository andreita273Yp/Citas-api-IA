package co.edu.fcv.citas.identity.application;

import co.edu.fcv.citas.identity.application.port.in.PasswordRecoveryUseCase;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.RecoveryDeliveryPort;
import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.ResetToken;
import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.TokenStorePort;
import co.edu.fcv.citas.identity.application.port.out.RefreshSessionPort;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.PasswordPolicy;
import co.edu.fcv.citas.identity.domain.UserAccount;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * HU-008/009 · Token aleatorio de 256 bits, válido 30 minutos y de un solo uso. Solo se persiste su hash;
 * el texto plano sale únicamente por el canal de entrega. Una solicitud nueva invalida las anteriores.
 */
public class PasswordRecoveryService implements PasswordRecoveryUseCase {
    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountPort users;
    private final TokenStorePort tokens;
    private final RecoveryDeliveryPort delivery;
    private final RefreshSessionPort sessions;
    private final PasswordHasher passwords;
    private final TransactionPort tx;
    private final Clock clock;

    public PasswordRecoveryService(UserAccountPort users, TokenStorePort tokens, RecoveryDeliveryPort delivery, RefreshSessionPort sessions,
                                   PasswordHasher passwords, TransactionPort tx, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.delivery = delivery;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void request(String email) {
        Optional<UserAccount> account;
        try {
            account = users.findActiveByEmail(EmailAddress.of(email));
        } catch (IdentityException.InvalidData e) {
            return; // Respuesta genérica: un email inválido no se distingue de uno inexistente.
        }
        account.ifPresent(user -> {
            String token = newToken();
            Instant expiresAt = clock.instant().plus(TOKEN_TTL);
            tx.inTransaction(() -> {
                tokens.invalidateActive(user.id());
                tokens.store(user.id(), SessionService.hash(token), expiresAt);
                return null;
            });
            delivery.deliver(user.personal().email().value(), token, expiresAt);
        });
    }

    @Override
    public void reset(String token, String password, String confirmation) {
        if (token == null || token.isBlank()) throw new IdentityException.InvalidResetToken();
        PasswordPolicy.check(password);
        if (!password.equals(confirmation)) throw new IdentityException.InvalidData("La confirmación no coincide con la contraseña");
        tx.inTransaction(() -> {
            ResetToken valid = tokens.lockValid(SessionService.hash(token.trim())).orElseThrow(IdentityException.InvalidResetToken::new);
            users.changePassword(valid.userId(), passwords.hash(password));
            tokens.markUsed(valid.id());
            sessions.revokeAll(valid.userId());
            return null;
        });
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
