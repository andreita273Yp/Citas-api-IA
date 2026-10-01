package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
}
