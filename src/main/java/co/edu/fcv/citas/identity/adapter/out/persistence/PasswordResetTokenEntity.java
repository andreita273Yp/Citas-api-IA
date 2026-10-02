package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Tabla `password_reset_tokens`: solo el hash del token, su vencimiento y su uso. */
@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false) Long userId;
    @Column(name = "token_hash", nullable = false, unique = true) String tokenHash;
    @Column(name = "expires_at", nullable = false) LocalDateTime expiresAt;
    @Column(name = "used_at") LocalDateTime usedAt;
}
