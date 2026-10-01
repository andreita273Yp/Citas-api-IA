package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorios Spring Data JPA de agenda y citas; package-private al adaptador.

interface AvailabilityBlockJpaRepository extends JpaRepository<AvailabilityBlockEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AvailabilityBlockEntity b where b.id = :id and b.professionalId = :professionalId and b.active = true")
    Optional<AvailabilityBlockEntity> lockOwn(@Param("professionalId") Long professionalId, @Param("id") Long id);

    @Query("select count(b) > 0 from AvailabilityBlockEntity b where b.professionalId = :professionalId and b.active = true"
            + " and b.date = :date and b.startTime < :end and b.endTime > :start and (:excludeId is null or b.id <> :excludeId)")
    boolean overlaps(@Param("professionalId") Long professionalId, @Param("date") LocalDate date, @Param("start") LocalTime start,
                     @Param("end") LocalTime end, @Param("excludeId") Long excludeId);
}

interface ProfessionalSlotJpaRepository extends JpaRepository<ProfessionalSlotEntity, Long> {
    /** RN-01: bloquea los slots del rango en orden de inicio; la transacción concurrente espera y luego los ve ocupados. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProfessionalSlotEntity s where s.blockId in (select b.id from AvailabilityBlockEntity b"
            + " where b.professionalId = :professionalId and b.locationId = :locationId and b.active = true)"
            + " and s.startAt >= :start and s.endAt <= :end order by s.startAt")
    List<ProfessionalSlotEntity> lockRange(@Param("professionalId") Long professionalId, @Param("locationId") Long locationId,
                                           @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProfessionalSlotEntity> findByBlockIdOrderByStartAt(Long blockId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProfessionalSlotEntity s set s.appointmentId = :appointmentId where s.id in :ids and s.appointmentId is null")
    int assignFree(@Param("appointmentId") Long appointmentId, @Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProfessionalSlotEntity s set s.appointmentId = null where s.appointmentId = :appointmentId")
    int release(@Param("appointmentId") Long appointmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProfessionalSlotEntity s where s.blockId = :blockId")
    int deleteByBlock(@Param("blockId") Long blockId);
}

interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AppointmentEntity a where a.id = :id")
    Optional<AppointmentEntity> lockById(@Param("id") Long id);
}
