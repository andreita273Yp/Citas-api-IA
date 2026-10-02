package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.DecideSpecializedRequestUseCase;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.AppointmentPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.AppointmentState;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.LocalDateTime;

/** HU-024 · Decisión ADMIN: REQUESTED → APPROVED (conserva slots) o REQUESTED → REJECTED con motivo (libera slots, RN-09). */
public class DecisionService implements DecideSpecializedRequestUseCase {
    private static final int MAX_REASON = 500;
    private final AppointmentPort appointments;
    private final SlotPort slots;
    private final StatusHistoryPort history;
    private final TransactionPort tx;
    private final Clock clock;

    public DecisionService(AppointmentPort appointments, SlotPort slots, StatusHistoryPort history, TransactionPort tx, Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.history = history;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Decided decide(long adminUserId, long appointmentId, String decision, String reason) {
        AppointmentStatus target = switch (decision == null ? "" : decision.trim()) {
            case "APPROVE" -> AppointmentStatus.APPROVED;
            case "REJECT" -> AppointmentStatus.REJECTED;
            default -> throw new SchedulingException.InvalidData("La decisión debe ser APPROVE o REJECT");
        };
        String motive = reason == null || reason.isBlank() ? null : reason.trim();
        if (target == AppointmentStatus.REJECTED && motive == null) throw new SchedulingException.InvalidData("El rechazo exige un motivo");
        if (motive != null && motive.length() > MAX_REASON) throw new SchedulingException.InvalidData("El motivo supera 500 caracteres");

        return tx.inTransaction(() -> {
            AppointmentState current = appointments.lock(appointmentId).orElseThrow(() -> new SchedulingException.NotFound("Cita no existe"));
            if (current.status() != AppointmentStatus.REQUESTED)
                throw new SchedulingException.Conflict("Solo se decide una solicitud especializada en estado REQUESTED");
            current.status().transitionTo(target);
            if (target == AppointmentStatus.APPROVED) {
                if (!current.start().isAfter(LocalDateTime.now(clock))) throw new SchedulingException.Conflict("La cita ya pasó; no se puede aprobar");
                appointments.changeStatus(appointmentId, target, adminUserId);
            } else {
                appointments.changeStatus(appointmentId, target, null);
                slots.release(appointmentId);
            }
            history.record(appointmentId, target, adminUserId, "ADMIN", motive);
            return new Decided(appointmentId, target);
        });
    }
}
