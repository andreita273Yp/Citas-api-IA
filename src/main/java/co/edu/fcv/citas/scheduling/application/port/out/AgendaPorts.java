package co.edu.fcv.citas.scheduling.application.port.out;

import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase.BlockView;
import co.edu.fcv.citas.scheduling.domain.BlockSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Puertos de salida de la agenda del profesional. */
public final class AgendaPorts {
    private AgendaPorts() { }

    /** Profesional dueño de una agenda, con sus sedes asignadas activas. */
    public record Owner(long professionalId, boolean active, Set<Long> locationIds) { }

    public interface ProfessionalPort {
        Optional<Owner> findByUserId(long userId);

        /** SELECT ... FOR UPDATE sobre el profesional: serializa altas/ediciones de sus bloques. */
        void lock(long professionalId);
    }

    public interface BlockPort {
        List<BlockView> findOwn(long professionalId, LocalDate from, LocalDate to, Long locationId);

        /** Bloquea el bloque si pertenece al profesional. */
        Optional<BlockView> lockOwn(long professionalId, long blockId);

        boolean overlaps(long professionalId, BlockSchedule schedule, Long excludeBlockId);

        /** Crea el bloque y sus slots de 30 minutos. */
        long create(long professionalId, long locationId, BlockSchedule schedule);

        /** Bloquea los slots del bloque y cuenta los comprometidos (ocupados o retenidos). */
        int lockCommittedSlots(long blockId);

        /** Reemplaza franja/sede y regenera los slots (solo bloques sin compromisos). */
        void reschedule(long blockId, long locationId, BlockSchedule schedule);

        void delete(long blockId);
    }
}
