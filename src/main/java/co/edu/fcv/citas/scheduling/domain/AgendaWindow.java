package co.edu.fcv.citas.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/** Rango de fechas de una consulta de agenda (RF-16): día, semana lunes-domingo o rango explícito; ambos extremos opcionales e inclusivos. */
public record AgendaWindow(LocalDate from, LocalDate to) {
    public enum View { DAY, WEEK }

    public AgendaWindow {
        if (from != null && to != null && from.isAfter(to)) throw new SchedulingException.InvalidData("El rango de fechas es inválido");
    }

    public static AgendaWindow of(LocalDate date, View view) {
        return switch (view) {
            case DAY -> new AgendaWindow(date, date);
            case WEEK -> {
                LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new AgendaWindow(monday, monday.plusDays(6));
            }
        };
    }

    public LocalDateTime startInclusive() { return from == null ? null : from.atStartOfDay(); }

    public LocalDateTime endExclusive() { return to == null ? null : to.plusDays(1).atStartOfDay(); }
}
