package co.edu.fcv.citas.identity.application.port.in;

/** HU-008/009 · Recuperación de contraseña con token temporal de un solo uso (RF-03). */
public interface PasswordRecoveryUseCase {
    /** Siempre responde igual, exista o no la cuenta; si existe, emite un token y lo entrega por el canal configurado. */
    void request(String email);

    /** Cambia la contraseña, consume el token y revoca todas las sesiones refresh del usuario. */
    void reset(String token, String password, String confirmation);
}
