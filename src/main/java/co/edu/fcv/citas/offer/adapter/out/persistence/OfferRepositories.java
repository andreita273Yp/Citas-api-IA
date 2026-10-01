package co.edu.fcv.citas.offer.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorios Spring Data JPA de la oferta; package-private al adaptador.

interface SpecialtyJpaRepository extends JpaRepository<SpecialtyEntity, Long> {
    List<SpecialtyEntity> findAllByOrderByNameAsc();

    @Query("select count(s) > 0 from SpecialtyEntity s where (lower(s.code) = lower(:code) or lower(s.name) = lower(:name))"
            + " and (:excludeId is null or s.id <> :excludeId)")
    boolean existsByCodeOrName(@Param("code") String code, @Param("name") String name, @Param("excludeId") Long excludeId);

    long countByIdInAndActiveTrue(Collection<Long> ids);
}

interface ProfessionalJpaRepository extends JpaRepository<ProfessionalEntity, Long> {
    boolean existsByProfessionalCodeOrLicenseNumber(String professionalCode, String licenseNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProfessionalEntity p where p.id = :id")
    Optional<ProfessionalEntity> lockById(@Param("id") Long id);
}

interface ProfessionalSpecialtyJpaRepository extends JpaRepository<ProfessionalSpecialtyEntity, ProfessionalSpecialtyEntity.PairId> {
    List<ProfessionalSpecialtyEntity> findByIdProfessionalId(Long professionalId);
}

interface ProfessionalLocationJpaRepository extends JpaRepository<ProfessionalLocationEntity, ProfessionalLocationEntity.PairId> {
    List<ProfessionalLocationEntity> findByIdProfessionalId(Long professionalId);
}
