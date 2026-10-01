package co.edu.fcv.citas.identity.application;

import co.edu.fcv.citas.identity.application.port.in.SessionUseCase;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.RefreshSessionPort;
import co.edu.fcv.citas.identity.application.port.out.TokenPort;
import co.edu.fcv.citas.shared.application.TransactionPort;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** HU-006/HU-007 · Sesión con access de corta duración y refresh rotado en cada uso. */
public class SessionService implements SessionUseCase {
    private final UserAccountPort users;
    private final RefreshSessionPort sessions;
    private final PasswordHasher passwords;
    private final TokenPort tokens;
    private final TransactionPort tx;

    public SessionService(UserAccountPort users, RefreshSessionPort sessions, PasswordHasher passwords, TokenPort tokens,
                          TransactionPort tx) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokens = tokens;
        this.tx = tx;
    }

    @Override
    public IssuedSession login(String email, String password) {
        EmailAddress address;
        try {
            address = EmailAddress.of(email);
        } catch (IdentityException.InvalidData e) {
            throw new IdentityException.InvalidCredentials();
        }
        UserAccount account = users.findActiveByEmail(address)
                .filter(found -> password != null && passwords.matches(password, found.passwordHash()))
                .orElseThrow(IdentityException.InvalidCredentials::new);
        return tx.inTransaction(() -> open(account));
    }

    @Override
    public IssuedSession refresh(String refreshToken) {
        TokenPort.RefreshClaims claims = tokens.readRefresh(refreshToken);
        return tx.inTransaction(() -> {
            if (!sessions.consume(hash(claims.sessionId()))) throw new IdentityException.InvalidSession();
            UserAccount account = users.findActiveById(claims.userId()).orElseThrow(IdentityException.InvalidSession::new);
            return open(account);
        });
    }

    @Override
    public void logout(String refreshToken) {
        try {
            TokenPort.RefreshClaims claims = tokens.readRefresh(refreshToken);
            tx.inTransaction(() -> {
                sessions.revoke(hash(claims.sessionId()));
                return null;
            });
        } catch (IdentityException.InvalidSession ignored) {
            // Cerrar una sesión inexistente o inválida no revela información ni falla.
        }
    }

    private IssuedSession open(UserAccount account) {
        String sessionId = UUID.randomUUID().toString();
        TokenPort.Tokens issued = tokens.issue(account, sessionId);
        sessions.open(account.id(), hash(sessionId), issued.refreshExpiresAt());
        return new IssuedSession(issued.accessToken(), issued.accessExpiresInSeconds(), issued.refreshToken(), issued.refreshExpiresAt());
    }

    /** Solo el hash del identificador de sesión se persiste. */
    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
