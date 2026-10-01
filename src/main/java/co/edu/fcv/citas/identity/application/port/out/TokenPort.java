package co.edu.fcv.citas.identity.application.port.out;

import co.edu.fcv.citas.identity.domain.UserAccount;
import java.time.Instant;

/** Emisión y lectura de JWT con secretos y tipos distintos para access y refresh. */
public interface TokenPort {
    Tokens issue(UserAccount account, String sessionId);

    /** Lanza {@code IdentityException.InvalidSession} si el token no es un refresh válido y vigente. */
    RefreshClaims readRefresh(String refreshToken);

    record Tokens(String accessToken, long accessExpiresInSeconds, String refreshToken, Instant refreshExpiresAt) { }

    record RefreshClaims(long userId, String sessionId) { }
}
