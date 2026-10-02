package co.edu.fcv.citas.insurance.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorios Spring Data JPA de aseguramiento; package-private al adaptador.

interface EpsJpaRepository extends JpaRepository<EpsEntity, Long> {
    @Query("select count(e) > 0 from EpsEntity e where (lower(e.code) = lower(:code) or lower(e.name) = lower(:name))"
            + " and (:excludeId is null or e.id <> :excludeId)")
    boolean existsByCodeOrName(@Param("code") String code, @Param("name") String name, @Param("excludeId") Long excludeId);
}

interface EpsPlanJpaRepository extends JpaRepository<EpsPlanEntity, Long> {
    boolean existsByEpsIdAndCodeIgnoreCase(Long epsId, String code);
}

interface AffiliationJpaRepository extends JpaRepository<AffiliationEntity, Long> {
    List<AffiliationEntity> findByUserIdAndCurrentTrue(Long userId);

    Optional<AffiliationEntity> findFirstByUserIdAndPlanIdOrderByIdDesc(Long userId, Long planId);
}
