package co.edu.fcv.citas.scheduling.application.port.out;

import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.ProfessionalOption;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.SpecialtyOption;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.AvailabilityCalculator.FreeSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Puertos de salida de búsqueda, reserva y decisión de citas. */
public final class BookingPorts {
    private BookingPorts() { }

    public record SpecialtyRules(long id, int durationMinutes, boolean general) { }

    public interface OfferPort {
        Optional<SpecialtyRules> findActiveSpecialty(long specialtyId);

        /** RN-07/RN-08: profesional activo, especialidad activa asociada y sede asignada. */
        boolean offers(long professionalId, long specialtyId, long locationId);

        List<SpecialtyOption> activeSpecialties(Boolean general);

        List<ProfessionalOption> offeredProfessionals(long specialtyId, long locationId);
    }

    public record FreeSlotRow(FreeSlot slot, String professionalName, String locationCode) { }

    public interface FreeSlotPort {
        /** Slots libres del día que pertenecen a una oferta válida para la especialidad. */
        List<FreeSlotRow> freeSlots(long specialtyId, LocalDate date, Long locationId, Long professionalId);
    }

    public record LockedSlot(long id, LocalDateTime start, LocalDateTime end, boolean free) { }

    public interface SlotPort {
        /** SELECT ... FOR UPDATE de los slots del rango, en orden de inicio (orden fijo para evitar interbloqueos). */
        List<LockedSlot> lock(long professionalId, long locationId, LocalDateTime start, LocalDateTime end);

        /** Asigna solo slots aún libres; devuelve cuántos se asignaron. */
        int assign(long appointmentId, List<Long> slotIds);

        void release(long appointmentId);
    }

    public record NewAppointment(long patientUserId, long professionalId, long locationId, long specialtyId, AppointmentStatus status,
                                 String reason, LocalDateTime start, LocalDateTime end) { }

    public record AppointmentState(long id, AppointmentStatus status, LocalDateTime start) { }

    public interface AppointmentPort {
        long create(NewAppointment appointment);

        Optional<AppointmentState> lock(long appointmentId);

        void changeStatus(long appointmentId, AppointmentStatus status, Long approvedByUserId);
    }

    /** RF-19: auditoría append-only; fuentes SYSTEM, USER o ADMIN. */
    public interface StatusHistoryPort {
        void record(long appointmentId, AppointmentStatus status, Long actorUserId, String source, String reason);
    }
}
