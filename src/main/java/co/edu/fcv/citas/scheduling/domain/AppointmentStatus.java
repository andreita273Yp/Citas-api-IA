package co.edu.fcv.citas.scheduling.domain;

import java.util.Set;

/** Estados de cita y sus transiciones explícitas (RN-11). Los terminales no admiten salida. */
public enum AppointmentStatus {
    REQUESTED, APPROVED, REJECTED, CANCELLED, COMPLETED, NO_SHOW;

    public Set<AppointmentStatus> next() {
        return switch (this) {
            case REQUESTED -> Set.of(APPROVED, REJECTED, CANCELLED);
            case APPROVED -> Set.of(CANCELLED, COMPLETED, NO_SHOW);
            case REJECTED, CANCELLED, COMPLETED, NO_SHOW -> Set.of();
        };
    }

    public boolean terminal() { return next().isEmpty(); }

    /** Lanza {@code Conflict} si la transición no está permitida. */
    public AppointmentStatus transitionTo(AppointmentStatus target) {
        if (!next().contains(target)) throw new SchedulingException.Conflict("Transición no permitida: " + this + " → " + target);
        return target;
    }
}
