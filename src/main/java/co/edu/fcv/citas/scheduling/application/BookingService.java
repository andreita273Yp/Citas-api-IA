package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.BookAppointmentUseCase;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.AppointmentPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.LockedSlot;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.NewAppointment;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.OfferPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SpecialtyRules;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.BookingSlot;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-022/023 · Reserva transaccional. Protección contra doble reserva (RN-01) en dos niveles:
 * los slots del rango se bloquean con SELECT ... FOR UPDATE y la asignación solo afecta slots aún libres;
 * si alguna no se aplica, la transacción completa se revierte.
 */
public class BookingService implements BookAppointmentUseCase {
    private static final int MAX_REASON = 500;
    private final OfferPort offer;
    private final SlotPort slots;
    private final AppointmentPort appointments;
    private final StatusHistoryPort history;
    private final TransactionPort tx;
    private final Clock clock;

    public BookingService(OfferPort offer, SlotPort slots, AppointmentPort appointments, StatusHistoryPort history, TransactionPort tx,
                          Clock clock) {
        this.offer = offer;
        this.slots = slots;
        this.appointments = appointments;
        this.history = history;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Booked book(long patientUserId, Command c) {
        SpecialtyRules specialty = offer.findActiveSpecialty(c.specialtyId())
                .orElseThrow(() -> new SchedulingException.NotFound("Especialidad no existe o está inactiva"));
        BookingSlot slot = new BookingSlot(c.startAt(), specialty.durationMinutes());
        slot.requireFuture(LocalDateTime.now(clock));
        String reason = c.reason() == null || c.reason().isBlank() ? null : c.reason().trim();
        if (reason != null && reason.length() > MAX_REASON) throw new SchedulingException.InvalidData("El motivo supera 500 caracteres");

        return tx.inTransaction(() -> {
            if (!offer.offers(c.professionalId(), c.specialtyId(), c.locationId()))
                throw new SchedulingException.Conflict("El profesional no ofrece esa especialidad en esa sede");
            List<LockedSlot> locked = slots.lock(c.professionalId(), c.locationId(), slot.start(), slot.end());
            if (!coversExactly(locked, slot)) throw new SchedulingException.Conflict("El horario ya no está disponible");

            AppointmentStatus status = specialty.general() ? AppointmentStatus.APPROVED : AppointmentStatus.REQUESTED;
            long id = appointments.create(new NewAppointment(patientUserId, c.professionalId(), c.locationId(), c.specialtyId(), status,
                    reason, slot.start(), slot.end()));
            List<Long> ids = locked.stream().map(LockedSlot::id).toList();
            if (slots.assign(id, ids) != ids.size()) throw new SchedulingException.Conflict("El horario ya no está disponible");
            if (specialty.general()) history.record(id, status, null, "SYSTEM", "Aprobación automática de Medicina General");
            else history.record(id, status, patientUserId, "USER", "Solicitud especializada creada");
            return new Booked(id, status, c.professionalId(), c.locationId(), c.specialtyId(), slot.start(), slot.end(), slot.durationMinutes());
        });
    }

    /** Todos los slots libres y consecutivos, cubriendo exactamente la duración (RN-05). */
    private static boolean coversExactly(List<LockedSlot> locked, BookingSlot slot) {
        if (locked.size() != slot.slots()) return false;
        LocalDateTime expected = slot.start();
        for (LockedSlot s : locked) {
            if (!s.free() || !s.start().equals(expected)) return false;
            expected = s.end();
        }
        return expected.equals(slot.end());
    }
}
