package co.edu.fcv.citas.scheduling.application.port.in;

import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.time.LocalDateTime;

/** HU-022/023 · El USER reserva: Medicina General queda APPROVED; las especializadas REQUESTED con slots retenidos. */
public interface BookAppointmentUseCase {
    Booked book(long patientUserId, Command command);

    record Command(long professionalId, long locationId, long specialtyId, LocalDateTime startAt, String reason) { }

    record Booked(long id, AppointmentStatus status, long professionalId, long locationId, long specialtyId, LocalDateTime startAt,
                  LocalDateTime endAt, int durationMinutes) { }
}
