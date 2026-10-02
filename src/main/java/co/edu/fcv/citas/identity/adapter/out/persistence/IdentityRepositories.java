package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorios Spring Data JPA de identidad; package-private para que solo el adaptador los use.

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmailOrDocumentTypeAndDocumentNumber(String email, String documentType, String documentNumber);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    Optional<UserEntity> findByIdAndActiveTrue(Long id);
}

interface RoleJpaRepository extends JpaRepository<RoleEntity, Integer> {
    Optional<RoleEntity> findByCode(String code);
}

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
    /** SELECT ... FOR UPDATE: serializa renovaciones concurrentes del mismo refresh. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetTokenEntity t where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> lockValid(@Param("hash") String hash, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetTokenEntity t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateActive(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
