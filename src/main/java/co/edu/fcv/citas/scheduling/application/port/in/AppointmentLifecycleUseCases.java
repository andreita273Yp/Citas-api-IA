package co.edu.fcv.citas.scheduling.application.port.in;

import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-025 a HU-028 · Ciclo de vida de la cita del USER y decisión ADMIN de reprogramaciones (RF-13 a RF-15). */
public final class AppointmentLifecycleUseCases {
    private AppointmentLifecycleUseCases() { }

    public interface MyAppointments {
        /** Citas propias; {@code status}, {@code from} y {@code to} son filtros opcionales. */
        List<AppointmentDetail> list(long userId, AppointmentStatus status, LocalDate from, LocalDate to);

        AppointmentDetail detail(long userId, long appointmentId);

        /** HU-026: cita propia, futura y no terminal → CANCELLED; libera todos sus slots y cancela su reprogramación pendiente. */
        AppointmentDetail cancel(long userId, long appointmentId);

        /** RF-15: tras un rechazo de reprogramación, el USER conserva su cita original. */
        void keepAfterRejection(long userId, long appointmentId, long requestId);
    }

    public interface Reschedule {
        /** HU-027: solo cita propia APPROVED y futura; mismo profesional y especialidad; retiene la nueva franja. */
        RescheduleView request(long userId, long appointmentId, LocalDateTime startAt, Long locationId);

        /** HU-028: aprobar intercambia franjas; rechazar exige motivo, libera la nueva y conserva la original. */
        RescheduleView decide(long adminUserId, long requestId, String decision, String reason);
    }

    public record AppointmentDetail(long id, AppointmentStatus status, long locationId, String locationCode, String location, long professionalId,
                                    String professional, long specialtyId, String specialty, int durationMinutes, LocalDateTime startsAt, LocalDateTime endsAt, String reason,
                                    String decisionReason, RescheduleView reschedule) { }

    public record RescheduleView(long id, long appointmentId, RescheduleStatus status, long locationId, String locationCode, LocalDateTime startAt,
                                 LocalDateTime endAt, String decisionReason, RescheduleStatus.PatientAction patientAction) { }
}
