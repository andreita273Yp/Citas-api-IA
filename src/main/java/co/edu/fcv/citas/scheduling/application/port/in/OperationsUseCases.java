package co.edu.fcv.citas.scheduling.application.port.in;

import co.edu.fcv.citas.scheduling.domain.AgendaWindow;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-029 a HU-032 · Agenda y cierre del profesional, bandeja ADMIN y auditoría de estados (RF-16 a RF-19). */
public final class OperationsUseCases {
    private OperationsUseCases() { }

    public interface ProfessionalAgenda {
        /** HU-029: solo citas APPROVED propias; del paciente únicamente su nombre. */
        List<AgendaAppointment> list(long professionalUserId, AgendaWindow window, Long locationId);

        /** HU-030: cita propia APPROVED ya terminada → COMPLETED o NO_SHOW, auditada con fuente USER. */
        Closed close(long professionalUserId, long appointmentId, AppointmentStatus status, String reason);
    }

    public interface AdminInbox {
        /** HU-031: solicitudes especializadas REQUESTED y reprogramaciones PENDING; filtros opcionales. */
        List<InboxItem> list(InboxFilter filter);
    }

    public interface StatusHistory {
        /** HU-032: ADMIN lee todo; PROFESSIONAL sus citas asignadas; USER sus citas. Lo ajeno se informa como inexistente. */
        List<HistoryEntry> of(Viewer viewer, long appointmentId);
    }

    public record Viewer(long userId, boolean admin, boolean professional) { }

    public record AgendaAppointment(long id, long locationId, String locationCode, String location, String patient, String specialty,
                                    int durationMinutes, LocalDateTime startsAt, LocalDateTime endsAt, String reason, boolean closable) { }

    public record Closed(long id, AppointmentStatus status) { }

    public enum InboxKind { SPECIALIZED_REQUEST, RESCHEDULE }

    public record InboxFilter(Long locationId, Long professionalId, Long specialtyId, LocalDate date) { }

    /** {@code startsAt}/{@code endsAt} es la franja a decidir; {@code currentStartsAt} es la franja vigente de una reprogramación. */
    public record InboxItem(InboxKind kind, long requestId, long appointmentId, long locationId, String locationCode, String patient,
                            String professional, String specialty, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime currentStartsAt,
                            String reason) { }

    public record HistoryEntry(long id, AppointmentStatus previousStatus, AppointmentStatus newStatus, Long actorId, String source, String reason,
                               LocalDateTime occurredAt) { }
}
