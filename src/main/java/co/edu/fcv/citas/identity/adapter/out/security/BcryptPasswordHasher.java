package co.edu.fcv.citas.identity.adapter.out.security;

import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;

    BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) { return encoder.encode(rawPassword); }

    @Override
    public boolean matches(String rawPassword, String hash) { return encoder.matches(rawPassword, hash); }
}
