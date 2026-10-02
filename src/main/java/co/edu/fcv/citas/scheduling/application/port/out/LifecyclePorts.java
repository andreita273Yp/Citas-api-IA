package co.edu.fcv.citas.scheduling.application.port.out;

import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.AppointmentDetail;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.RescheduleView;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Puertos de salida del ciclo de vida de la cita y de la reprogramación. */
public final class LifecyclePorts {
    private LifecyclePorts() { }

    /** Modelo de lectura de citas propias con su última reprogramación. */
    public interface AppointmentQueryPort {
        List<AppointmentDetail> listForPatient(long userId, AppointmentStatus status, LocalDate from, LocalDate to);

        Optional<AppointmentDetail> detailForPatient(long userId, long appointmentId);
    }

    /** Cita bloqueada para modificar su estado o su franja. */
    public record LockedAppointment(long id, long patientUserId, long professionalId, long locationId, long specialtyId, AppointmentStatus status,
                                    LocalDateTime start, LocalDateTime end) {
        public int durationMinutes() { return (int) java.time.Duration.between(start, end).toMinutes(); }
    }

    public interface AppointmentWritePort {
        Optional<LockedAppointment> lock(long appointmentId);

        void changeStatus(long appointmentId, AppointmentStatus status);

        void changeSchedule(long appointmentId, long locationId, LocalDateTime start, LocalDateTime end);
    }

    public interface HeldSlotPort {
        int countHeld(long appointmentId, LocalDateTime start, LocalDateTime end);

        /** Libera los slots de la cita dentro del rango (franja retenida que se descarta). */
        void releaseInside(long appointmentId, LocalDateTime start, LocalDateTime end);

        /** Libera los slots de la cita fuera del rango (franja anterior tras aprobar). */
        void releaseOutside(long appointmentId, LocalDateTime start, LocalDateTime end);
    }

    public record LockedRequest(long id, long appointmentId, RescheduleStatus status, long locationId, LocalDateTime start, LocalDateTime end,
                                RescheduleStatus.PatientAction patientAction) { }

    public interface ReschedulePort {
        boolean hasPending(long appointmentId);

        long create(long appointmentId, long requestedByUserId, long locationId, LocalDateTime previousStart, LocalDateTime previousEnd,
                    LocalDateTime start, LocalDateTime end);

        Optional<LockedRequest> lock(long requestId);

        Optional<RescheduleView> view(long requestId);

        void decide(long requestId, RescheduleStatus status, String reason, long adminUserId);

        /** Marca como CANCELLED la solicitud pendiente de la cita, si existe. */
        void cancelPending(long appointmentId);

        /** Última solicitud de la cita, si fue rechazada y el USER aún no eligió qué hacer. */
        Optional<Long> latestRejectedWithoutAction(long appointmentId);

        void setPatientAction(long requestId, RescheduleStatus.PatientAction action);
    }
}
