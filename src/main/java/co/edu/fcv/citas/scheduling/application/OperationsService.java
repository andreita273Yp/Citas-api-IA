package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.AdminInbox;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.AgendaAppointment;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.Closed;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.HistoryEntry;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxFilter;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxItem;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.ProfessionalAgenda;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.StatusHistory;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.Viewer;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.Owner;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.ProfessionalPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentWritePort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.HeldSlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.LockedAppointment;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.ReschedulePort;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.AgendaQueryPort;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.HistoryQueryPort;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.InboxQueryPort;
import co.edu.fcv.citas.scheduling.domain.AgendaWindow;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** HU-029 a HU-032 · Agenda y cierre del profesional, bandeja ADMIN y lectura protegida del historial. */
public class OperationsService implements ProfessionalAgenda, AdminInbox, StatusHistory {
    private final ProfessionalPort professionals;
    private final AgendaQueryPort agenda;
    private final AppointmentWritePort appointments;
    private final ReschedulePort reschedules;
    private final HeldSlotPort held;
    private final InboxQueryPort inbox;
    private final HistoryQueryPort historyQueries;
    private final StatusHistoryPort history;
    private final TransactionPort tx;
    private final Clock clock;

    public OperationsService(ProfessionalPort professionals, AgendaQueryPort agenda, AppointmentWritePort appointments, ReschedulePort reschedules,
                             HeldSlotPort held, InboxQueryPort inbox, HistoryQueryPort historyQueries, StatusHistoryPort history, TransactionPort tx,
                             Clock clock) {
        this.professionals = professionals;
        this.agenda = agenda;
        this.appointments = appointments;
        this.reschedules = reschedules;
        this.held = held;
        this.inbox = inbox;
        this.historyQueries = historyQueries;
        this.history = history;
        this.tx = tx;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- HU-029 / HU-030

    @Override
    public List<AgendaAppointment> list(long professionalUserId, AgendaWindow window, Long locationId) {
        long professionalId = professional(professionalUserId);
        LocalDateTime now = now();
        return agenda.approvedFor(professionalId, window, locationId).stream()
                .map(r -> new AgendaAppointment(r.id(), r.locationId(), r.locationCode(), r.location(), r.patient(), r.specialty(),
                        (int) Duration.between(r.startsAt(), r.endsAt()).toMinutes(), r.startsAt(), r.endsAt(), r.reason(), !r.endsAt().isAfter(now)))
                .toList();
    }

    @Override
    public Closed close(long professionalUserId, long appointmentId, AppointmentStatus status, String reason) {
        if (status != AppointmentStatus.COMPLETED && status != AppointmentStatus.NO_SHOW)
            throw new SchedulingException.InvalidData("El cierre debe ser COMPLETED o NO_SHOW");
        return tx.inTransaction(() -> {
            long professionalId = professional(professionalUserId);
            LockedAppointment a = appointments.lock(appointmentId).filter(x -> x.professionalId() == professionalId)
                    .orElseThrow(SchedulingException.NotFound::notFoundAppointment);
            if (a.status() != AppointmentStatus.APPROVED) throw new SchedulingException.Conflict("Solo se puede cerrar una cita aprobada");
            if (a.end().isAfter(now())) throw new SchedulingException.Conflict("La cita aún no ha terminado");
            a.status().transitionTo(status);
            // Una reprogramación pendiente de una cita ya atendida pierde sentido: se cancela y libera su franja retenida.
            reschedules.cancelPending(appointmentId);
            held.releaseOutside(appointmentId, a.start(), a.end());
            appointments.changeStatus(appointmentId, status);
            history.record(appointmentId, status, professionalUserId, "USER", blank(reason));
            return new Closed(appointmentId, status);
        });
    }

    // ---------------------------------------------------------------- HU-031

    @Override
    public List<InboxItem> list(InboxFilter filter) {
        return inbox.pending(filter);
    }

    // ---------------------------------------------------------------- HU-032

    @Override
    public List<HistoryEntry> of(Viewer viewer, long appointmentId) {
        boolean allowed = viewer.admin()
                || (viewer.professional() && historyQueries.isAssignedProfessional(appointmentId, viewer.userId()))
                || historyQueries.isPatient(appointmentId, viewer.userId());
        if (!allowed) throw SchedulingException.NotFound.notFoundAppointment();
        List<HistoryEntry> entries = historyQueries.entries(appointmentId);
        if (entries.isEmpty()) throw SchedulingException.NotFound.notFoundAppointment();
        return entries;
    }

    /** El JWT identifica al usuario; la agenda pertenece a su registro activo en professionals. */
    private long professional(long userId) {
        Owner owner = professionals.findByUserId(userId).orElseThrow(() -> new SchedulingException.Forbidden("Profesional no registrado"));
        if (!owner.active()) throw new SchedulingException.Forbidden("Profesional inactivo");
        return owner.professionalId();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
