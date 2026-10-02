package co.edu.fcv.citas.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;

/** Puertos de salida de la recuperación de contraseña. */
public final class PasswordResetPorts {
    private PasswordResetPorts() { }

    public record ResetToken(long id, long userId) { }

    /** Tokens persistidos solo como hash SHA-256. */
    public interface TokenStorePort {
        /** Marca como usados los tokens vigentes del usuario (una solicitud nueva invalida las anteriores). */
        void invalidateActive(long userId);

        void store(long userId, String tokenHash, Instant expiresAt);

        /** Bloquea y devuelve el token si existe, no se usó y no venció. */
        Optional<ResetToken> lockValid(String tokenHash);

        void markUsed(long tokenId);
    }

    /** Canal de entrega del token; en desarrollo, un buzón local protegido (no hay SMTP obligatorio). */
    public interface RecoveryDeliveryPort {
        void deliver(String email, String token, Instant expiresAt);
    }
}
