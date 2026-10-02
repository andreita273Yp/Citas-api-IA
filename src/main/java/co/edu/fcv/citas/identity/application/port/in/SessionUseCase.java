package co.edu.fcv.citas.identity.application.port.in;

import java.time.Instant;

/** HU-006/HU-007 · Iniciar, renovar y cerrar sesión con access y refresh JWT separados. */
public interface SessionUseCase {
    IssuedSession login(String email, String password);

    /** Rota ambos tokens; el refresh recibido queda revocado aunque haya intentos concurrentes. */
    IssuedSession refresh(String refreshToken);

    /** Revoca la sesión del refresh indicado; un token inválido se ignora. */
    void logout(String refreshToken);

    record IssuedSession(String accessToken, long accessExpiresInSeconds, String refreshToken, Instant refreshExpiresAt) { }
}
