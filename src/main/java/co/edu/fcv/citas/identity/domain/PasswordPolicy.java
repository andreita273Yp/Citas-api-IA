package co.edu.fcv.citas.identity.domain;

import java.nio.charset.StandardCharsets;

/** Contraseña de 8 caracteres a 72 bytes UTF-8 (límite de BCrypt). Sus espacios no se alteran. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() { }

    public static void check(String password) {
        if (password == null || password.length() < MIN_LENGTH)
            throw new IdentityException.InvalidData("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IdentityException.InvalidData("La contraseña supera el límite permitido");
    }
}
