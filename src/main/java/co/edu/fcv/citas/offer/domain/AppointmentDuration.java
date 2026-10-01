package co.edu.fcv.citas.offer.domain;

/** RF-09: una especialidad dura 30 min (1 slot) o 60 min (2 slots consecutivos). */
public record AppointmentDuration(int minutes) {
    public static final int SLOT_MINUTES = 30;

    public AppointmentDuration {
        if (minutes != 30 && minutes != 60) throw new OfferException.InvalidData("La duración debe ser 30 o 60 minutos");
    }

    public int slots() { return minutes / SLOT_MINUTES; }
}
