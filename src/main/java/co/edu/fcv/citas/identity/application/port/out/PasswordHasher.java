package co.edu.fcv.citas.identity.application.port.out;

/** Hash adaptativo de contraseñas (BCrypt en el adaptador). */
public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
