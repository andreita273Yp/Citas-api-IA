package co.edu.fcv.citas.identity.application.port.out;

import java.time.Instant;

/** Sesiones refresh persistidas solo como hash de su identificador. */
public interface RefreshSessionPort {
    void open(long userId, String sessionHash, Instant expiresAt);

    /**
     * Bloquea la sesión y la revoca si estaba vigente. Devuelve {@code false} si no existe, venció o ya estaba
     * revocada; con intentos concurrentes solo uno obtiene {@code true}.
     */
    boolean consume(String sessionHash);

    void revoke(String sessionHash);
}
