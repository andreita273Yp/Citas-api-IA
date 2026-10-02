package co.edu.fcv.citas.scheduling.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Cuadrícula de 30 minutos en la que se discretiza toda la agenda (RF-08). */
public final class SlotGrid {
    public static final int SLOT_MINUTES = 30;

    private SlotGrid() { }

    public static boolean aligned(LocalTime time) {
        return time.getMinute() % SLOT_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    public static boolean aligned(LocalDateTime time) {
        return aligned(time.toLocalTime());
    }
}
