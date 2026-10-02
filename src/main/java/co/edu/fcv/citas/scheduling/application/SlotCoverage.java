package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.LockedSlot;
import co.edu.fcv.citas.scheduling.domain.BookingSlot;
import java.time.LocalDateTime;
import java.util.List;

/** RN-01/RN-05: los slots bloqueados están todos libres, son consecutivos y cubren exactamente la franja. */
final class SlotCoverage {
    private SlotCoverage() { }

    static boolean coversExactly(List<LockedSlot> locked, BookingSlot slot) {
        if (locked.size() != slot.slots()) return false;
        LocalDateTime expected = slot.start();
        for (LockedSlot s : locked) {
            if (!s.free() || !s.start().equals(expected)) return false;
            expected = s.end();
        }
        return expected.equals(slot.end());
    }
}
