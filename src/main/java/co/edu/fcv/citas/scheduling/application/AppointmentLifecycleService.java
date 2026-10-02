package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.AppointmentDetail;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.MyAppointments;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.Reschedule;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.RescheduleView;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.LockedSlot;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.OfferPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentQueryPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentWritePort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.HeldSlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.LockedAppointment;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.LockedRequest;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.ReschedulePort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.BookingSlot;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-025 a HU-028 · Mis citas, cancelación y reprogramación. Una reprogramación PENDING retiene su nueva franja
 * asignando esos slots a la misma cita; la cita conserva su franja original hasta que ADMIN decide (RN-10).
 */
public class AppointmentLifecycleService implements MyAppointments, Reschedule {
    private static final int MAX_REASON = 500;
    private final AppointmentQueryPort queries;
    private final AppointmentWritePort appointments;
    private final ReschedulePort reschedules;
    private final SlotPort slots;
    private final HeldSlotPort held;
    private final OfferPort offer;
    private final StatusHistoryPort history;
    private final TransactionPort tx;
    private final Clock clock;

    public AppointmentLifecycleService(AppointmentQueryPort queries, AppointmentWritePort appointments, ReschedulePort reschedules, SlotPort slots,
                                       HeldSlotPort held, OfferPort offer, StatusHistoryPort history, TransactionPort tx, Clock clock) {
        this.queries = queries;
        this.appointments = appointments;
        this.reschedules = reschedules;
        this.slots = slots;
        this.held = held;
        this.offer = offer;
        this.history = history;
        this.tx = tx;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- HU-025 / HU-026

    @Override
    public List<AppointmentDetail> list(long userId, AppointmentStatus status, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new SchedulingException.InvalidData("El rango de fechas es inválido");
        return queries.listForPatient(userId, status, from, to);
    }

    @Override
    public AppointmentDetail detail(long userId, long appointmentId) {
        return queries.detailForPatient(userId, appointmentId).orElseThrow(SchedulingException.NotFound::notFoundAppointment);
    }

    @Override
    public AppointmentDetail cancel(long userId, long appointmentId) {
        tx.inTransaction(() -> {
            LockedAppointment a = own(userId, appointmentId);
            a.status().transitionTo(AppointmentStatus.CANCELLED);
            if (!a.start().isAfter(now())) throw new SchedulingException.Conflict("Solo se puede cancelar una cita futura");
            appointments.changeStatus(a.id(), AppointmentStatus.CANCELLED);
            slots.release(a.id()); // RN-09: franja propia y, si la hubiera, la retenida por la reprogramación.
            reschedules.cancelPending(a.id());
            reschedules.latestRejectedWithoutAction(a.id())
                    .ifPresent(r -> reschedules.setPatientAction(r, RescheduleStatus.PatientAction.CANCEL_APPOINTMENT));
            history.record(a.id(), AppointmentStatus.CANCELLED, userId, "USER", null);
            return null;
        });
        return detail(userId, appointmentId);
    }

    @Override
    public void keepAfterRejection(long userId, long appointmentId, long requestId) {
        tx.inTransaction(() -> {
            own(userId, appointmentId);
            LockedRequest r = reschedules.lock(requestId).filter(x -> x.appointmentId() == appointmentId)
                    .orElseThrow(() -> new SchedulingException.NotFound("Solicitud no existe"));
            if (r.status() != RescheduleStatus.REJECTED || r.patientAction() != null)
                throw new SchedulingException.Conflict("Solo se elige conservar la cita tras un rechazo pendiente de respuesta");
            reschedules.setPatientAction(requestId, RescheduleStatus.PatientAction.KEEP_APPOINTMENT);
            return null;
        });
    }

    // ---------------------------------------------------------------- HU-027 / HU-028

    @Override
    public RescheduleView request(long userId, long appointmentId, LocalDateTime startAt, Long locationId) {
        long requestId = tx.inTransaction(() -> {
            LockedAppointment a = own(userId, appointmentId);
            if (a.status() != AppointmentStatus.APPROVED) throw new SchedulingException.Conflict("Solo se puede reprogramar una cita aprobada");
            if (!a.start().isAfter(now())) throw new SchedulingException.Conflict("Solo se puede reprogramar una cita futura");
            BookingSlot slot = new BookingSlot(startAt, a.durationMinutes());
            slot.requireFuture(now());
            long location = locationId == null ? a.locationId() : locationId;
            if (!offer.offers(a.professionalId(), a.specialtyId(), location))
                throw new SchedulingException.Conflict("El profesional no atiende esa especialidad en la sede elegida");
            if (reschedules.hasPending(a.id())) throw new SchedulingException.Conflict("Ya existe una solicitud de reprogramación pendiente");
            List<LockedSlot> locked = slots.lock(a.professionalId(), location, slot.start(), slot.end());
            if (!SlotCoverage.coversExactly(locked, slot)) throw new SchedulingException.Conflict("La nueva franja ya no está disponible");
            List<Long> ids = locked.stream().map(LockedSlot::id).toList();
            if (slots.assign(a.id(), ids) != ids.size()) throw new SchedulingException.Conflict("La nueva franja ya no está disponible");
            return reschedules.create(a.id(), userId, location, a.start(), a.end(), slot.start(), slot.end());
        });
        return reschedules.view(requestId).orElseThrow();
    }

    @Override
    public RescheduleView decide(long adminUserId, long requestId, String decision, String reason) {
        RescheduleStatus target = switch (decision == null ? "" : decision.trim()) {
            case "APPROVE" -> RescheduleStatus.APPROVED;
            case "REJECT" -> RescheduleStatus.REJECTED;
            default -> throw new SchedulingException.InvalidData("La decisión debe ser APPROVE o REJECT");
        };
        String motive = reason == null || reason.isBlank() ? null : reason.trim();
        if (target == RescheduleStatus.REJECTED && motive == null) throw new SchedulingException.InvalidData("El rechazo exige un motivo");
        if (motive != null && motive.length() > MAX_REASON) throw new SchedulingException.InvalidData("El motivo supera 500 caracteres");

        tx.inTransaction(() -> {
            LockedRequest r = reschedules.lock(requestId).orElseThrow(() -> new SchedulingException.NotFound("Solicitud no existe"));
            if (r.status() != RescheduleStatus.PENDING) throw new SchedulingException.Conflict("La solicitud ya fue decidida");
            LockedAppointment a = appointments.lock(r.appointmentId()).orElseThrow();
            if (target == RescheduleStatus.APPROVED) {
                if (a.status() != AppointmentStatus.APPROVED) throw new SchedulingException.Conflict("La cita ya no está aprobada");
                if (!r.start().isAfter(now())) throw new SchedulingException.Conflict("La nueva franja ya pasó");
                if (held.countHeld(a.id(), r.start(), r.end()) * 30 != a.durationMinutes())
                    throw new SchedulingException.Conflict("La retención de la nueva franja es inconsistente");
                held.releaseOutside(a.id(), r.start(), r.end());
                appointments.changeSchedule(a.id(), r.locationId(), r.start(), r.end());
                history.record(a.id(), AppointmentStatus.APPROVED, adminUserId, "ADMIN", "Reprogramación aprobada");
            } else {
                held.releaseInside(a.id(), r.start(), r.end());
            }
            reschedules.decide(requestId, target, motive, adminUserId);
            return null;
        });
        return reschedules.view(requestId).orElseThrow();
    }

    private LockedAppointment own(long userId, long appointmentId) {
        return appointments.lock(appointmentId).filter(a -> a.patientUserId() == userId)
                .orElseThrow(SchedulingException.NotFound::notFoundAppointment);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
