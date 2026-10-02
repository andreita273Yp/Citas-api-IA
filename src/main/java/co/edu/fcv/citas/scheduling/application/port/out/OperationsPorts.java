package co.edu.fcv.citas.scheduling.application.port.out;

import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.HistoryEntry;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxFilter;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxItem;
import co.edu.fcv.citas.scheduling.domain.AgendaWindow;
import java.time.LocalDateTime;
import java.util.List;

/** Puertos de salida de agenda profesional, bandeja ADMIN y auditoría. */
public final class OperationsPorts {
    private OperationsPorts() { }

    public record AgendaRow(long id, long locationId, String locationCode, String location, String patient, String specialty,
                            LocalDateTime startsAt, LocalDateTime endsAt, String reason) { }

    public interface AgendaQueryPort {
        List<AgendaRow> approvedFor(long professionalId, AgendaWindow window, Long locationId);
    }

    public interface InboxQueryPort {
        List<InboxItem> pending(InboxFilter filter);
    }

    public interface HistoryQueryPort {
        boolean isPatient(long appointmentId, long userId);

        boolean isAssignedProfessional(long appointmentId, long userId);

        /** Entradas en orden cronológico; el estado anterior se deriva de la entrada previa. */
        List<HistoryEntry> entries(long appointmentId);
    }
}
