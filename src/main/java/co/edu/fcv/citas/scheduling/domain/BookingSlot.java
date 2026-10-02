package co.edu.fcv.citas.scheduling.domain;

import java.time.LocalDateTime;

/** Inicio solicitado para una cita: futuro y alineado a 30 min; el fin lo fija la duración de la especialidad. */
public record BookingSlot(LocalDateTime start, int durationMinutes) {
    public BookingSlot {
        if (start == null) throw new SchedulingException.InvalidData("La fecha y hora de inicio son obligatorias");
        if (!SlotGrid.aligned(start)) throw new SchedulingException.InvalidData("La hora debe estar en intervalos de 30 minutos");
        if (durationMinutes != 30 && durationMinutes != 60) throw new SchedulingException.InvalidData("Duración de especialidad inválida");
    }

    public LocalDateTime end() { return start.plusMinutes(durationMinutes); }

    public int slots() { return durationMinutes / SlotGrid.SLOT_MINUTES; }

    /** RN-06: no se permiten citas en el pasado. */
    public void requireFuture(LocalDateTime now) {
        if (!start.isAfter(now)) throw new SchedulingException.InvalidData("No se permiten citas en el pasado");
    }
}
