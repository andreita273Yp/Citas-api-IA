package co.edu.fcv.citas.scheduling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Franja de un bloque de disponibilidad: fecha, inicio y fin alineados a 30 min (HU-018).
 * El bloque se discretiza en slots [inicio, inicio+30).
 */
public record BlockSchedule(LocalDate date, LocalTime start, LocalTime end) {
    public BlockSchedule {
        if (date == null || start == null || end == null) throw new SchedulingException.InvalidData("Fecha, inicio y fin son obligatorios");
        if (!SlotGrid.aligned(start) || !SlotGrid.aligned(end))
            throw new SchedulingException.InvalidData("Inicio y fin deben estar en intervalos de 30 minutos");
        if (!end.isAfter(start)) throw new SchedulingException.InvalidData("El fin debe ser posterior al inicio");
    }

    public LocalDateTime startsAt() { return date.atTime(start); }

    public LocalDateTime endsAt() { return date.atTime(end); }

    /** RN-06: no se publican ni modifican bloques que ya empezaron. */
    public void requireFuture(LocalDateTime now) {
        if (!startsAt().isAfter(now)) throw new SchedulingException.InvalidData("No se permiten bloques en el pasado");
    }

    public boolean overlaps(BlockSchedule other) {
        return date.equals(other.date) && start.isBefore(other.end) && other.start.isBefore(end);
    }

    public List<TimeSlot> slots() {
        List<TimeSlot> slots = new ArrayList<>();
        for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(SlotGrid.SLOT_MINUTES))
            slots.add(new TimeSlot(date.atTime(t), date.atTime(t.plusMinutes(SlotGrid.SLOT_MINUTES))));
        return slots;
    }

    public record TimeSlot(LocalDateTime start, LocalDateTime end) { }
}
