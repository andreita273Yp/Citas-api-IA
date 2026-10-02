package co.edu.fcv.citas.scheduling.application.port.in;

import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;

/** HU-024 · ADMIN aprueba (conserva slots) o rechaza con motivo (libera slots) una cita REQUESTED. */
public interface DecideSpecializedRequestUseCase {
    Decided decide(long adminUserId, long appointmentId, String decision, String reason);

    record Decided(long id, AppointmentStatus status) { }
}
