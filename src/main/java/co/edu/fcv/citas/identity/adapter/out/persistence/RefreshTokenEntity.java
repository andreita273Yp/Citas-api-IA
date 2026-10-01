package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Tabla `refresh_tokens`: guarda solo el hash del identificador de sesión. */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "token_hash", nullable = false, unique = true) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "revoked_at") LocalDateTime revokedAt;

    protected RefreshTokenEntity() { }

    RefreshTokenEntity(Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    boolean activeAt(LocalDateTime now) { return revokedAt == null && expiresAt.isAfter(now); }

    void revoke(LocalDateTime now) { revokedAt = now; }
}
