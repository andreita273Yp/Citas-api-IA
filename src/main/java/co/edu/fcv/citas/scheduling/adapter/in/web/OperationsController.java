package co.edu.fcv.citas.scheduling.adapter.in.web;

import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.AdminInbox;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.AgendaAppointment;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.Closed;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.HistoryEntry;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxFilter;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxItem;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.ProfessionalAgenda;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.StatusHistory;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.Viewer;
import co.edu.fcv.citas.scheduling.domain.AgendaWindow;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-029 a HU-032 · `/professional/**` exige PROFESSIONAL y `/admin/**` exige ADMIN (SecurityConfig).
 * El historial solo se lee: no hay PUT, PATCH ni DELETE (RN-12).
 */
@RestController
@RequestMapping("/api/v1")
class OperationsController {
    private final ProfessionalAgenda agenda;
    private final AdminInbox inbox;
    private final StatusHistory history;

    OperationsController(ProfessionalAgenda agenda, AdminInbox inbox, StatusHistory history) {
        this.agenda = agenda;
        this.inbox = inbox;
        this.history = history;
    }

    /** {@code date}+{@code view} (DAY|WEEK) o {@code from}/{@code to}; sin fechas devuelve toda la agenda aprobada. */
    @GetMapping("/professional/appointments")
    List<AgendaAppointment> agenda(@RequestParam(required = false) LocalDate date, @RequestParam(required = false) String view,
                                   @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) Long locationId) {
        AgendaWindow window = date == null ? new AgendaWindow(from, to) : AgendaWindow.of(date, view(view));
        return agenda.list(CurrentUser.id(), window, locationId);
    }

    @PostMapping("/professional/appointments/{id}/closure")
    Closed close(@PathVariable long id, @RequestBody ClosureRequest body) {
        return agenda.close(CurrentUser.id(), id, closure(body.status()), body.reason());
    }

    @GetMapping("/admin/inbox")
    List<InboxItem> inbox(@RequestParam(required = false) Long locationId, @RequestParam(required = false) Long professionalId,
                          @RequestParam(required = false) Long specialtyId, @RequestParam(required = false) LocalDate date) {
        return inbox.list(new InboxFilter(locationId, professionalId, specialtyId, date));
    }

    @GetMapping("/appointments/{id}/history")
    List<HistoryEntry> history(@PathVariable long id) {
        return history.of(new Viewer(CurrentUser.id(), CurrentUser.has("ADMIN"), CurrentUser.has("PROFESSIONAL")), id);
    }

    private static AgendaWindow.View view(String raw) {
        if (raw == null || raw.isBlank()) return AgendaWindow.View.DAY;
        try {
            return AgendaWindow.View.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SchedulingException.InvalidData("La vista debe ser DAY o WEEK");
        }
    }

    private static AppointmentStatus closure(String raw) {
        if (raw == null) throw new SchedulingException.InvalidData("El cierre debe ser COMPLETED o NO_SHOW");
        try {
            return AppointmentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SchedulingException.InvalidData("El cierre debe ser COMPLETED o NO_SHOW");
        }
    }

    record ClosureRequest(String status, String reason) { }
}
