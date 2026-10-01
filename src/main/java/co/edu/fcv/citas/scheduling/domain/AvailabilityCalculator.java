package co.edu.fcv.citas.scheduling.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU-021 · Calcula los inicios reservables: una especialidad de 30 min usa 1 slot libre, una de 60 min exige
 * 2 slots libres consecutivos del mismo profesional y sede (RN-05). Nunca ofrece inicios pasados (RN-06).
 */
public final class AvailabilityCalculator {
    private AvailabilityCalculator() { }

    public static List<Option> options(List<FreeSlot> freeSlots, int durationMinutes, LocalDateTime now) {
        int needed = durationMinutes / SlotGrid.SLOT_MINUTES;
        Map<String, FreeSlot> byStart = new HashMap<>();
        for (FreeSlot s : freeSlots) byStart.put(key(s.professionalId(), s.locationId(), s.start()), s);

        List<Option> options = new ArrayList<>();
        for (FreeSlot first : freeSlots) {
            if (!first.start().isAfter(now)) continue;
            FreeSlot last = first;
            int found = 1;
            while (found < needed) {
                FreeSlot next = byStart.get(key(first.professionalId(), first.locationId(), last.end()));
                if (next == null) break;
                last = next;
                found++;
            }
            if (found == needed) options.add(new Option(first.professionalId(), first.locationId(), first.start(), last.end(), durationMinutes));
        }
        options.sort(Comparator.comparing(Option::start).thenComparing(Option::professionalId).thenComparing(Option::locationId));
        return options;
    }

    private static String key(long professional, long location, LocalDateTime start) {
        return professional + "|" + location + "|" + start;
    }

    public record FreeSlot(long professionalId, long locationId, LocalDateTime start, LocalDateTime end) { }

    public record Option(long professionalId, long locationId, LocalDateTime start, LocalDateTime end, int durationMinutes) { }
}
