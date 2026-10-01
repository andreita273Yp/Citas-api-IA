package co.edu.fcv.citas.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.fcv.citas.scheduling.domain.AvailabilityCalculator.FreeSlot;
import co.edu.fcv.citas.scheduling.domain.AvailabilityCalculator.Option;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reglas puras de agenda: slots de 30/60 min (RN-05), pasado (RN-06), solapamiento y transiciones (RN-11). */
class SchedulingDomainTest {
    private static final LocalDate DAY = LocalDate.of(2030, 1, 15);
    private static final LocalDateTime NOW = DAY.minusDays(1).atStartOfDay();

    private static FreeSlot slot(long professional, String start) {
        LocalDateTime at = DAY.atTime(LocalTime.parse(start));
        return new FreeSlot(professional, 1, at, at.plusMinutes(30));
    }

    @Test
    void thirtyMinuteSpecialtyUsesEveryFreeSlot() {
        List<Option> options = AvailabilityCalculator.options(List.of(slot(1, "08:00"), slot(1, "08:30"), slot(1, "10:00")), 30, NOW);
        assertThat(options).extracting(o -> o.start().toLocalTime().toString()).containsExactly("08:00", "08:30", "10:00");
        assertThat(options).allSatisfy(o -> assertThat(o.end()).isEqualTo(o.start().plusMinutes(30)));
    }

    @Test
    void sixtyMinuteSpecialtyNeedsTwoConsecutiveFreeSlotsOfTheSameProfessionalAndLocation() {
        List<FreeSlot> free = List.of(slot(1, "08:00"), slot(1, "08:30"), slot(1, "09:00"), slot(1, "10:00"), slot(2, "10:30"),
                new FreeSlot(1, 2, DAY.atTime(10, 30), DAY.atTime(11, 0)));
        List<Option> options = AvailabilityCalculator.options(free, 60, NOW);
        assertThat(options).extracting(o -> o.start().toLocalTime().toString()).containsExactly("08:00", "08:30");
        assertThat(options.getFirst().end()).isEqualTo(DAY.atTime(9, 0));
        assertThat(options).allSatisfy(o -> assertThat(o.durationMinutes()).isEqualTo(60));
    }

    @Test
    void pastStartsAreNeverOffered() {
        List<Option> options = AvailabilityCalculator.options(List.of(slot(1, "08:00"), slot(1, "08:30")), 30, DAY.atTime(8, 0));
        assertThat(options).extracting(o -> o.start().toLocalTime().toString()).containsExactly("08:30");
    }

    @Test
    void blockScheduleIsAlignedPositiveAndDiscretizedInThirtyMinuteSlots() {
        BlockSchedule block = new BlockSchedule(DAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
        assertThat(block.slots()).hasSize(4);
        assertThat(block.slots().getLast().end()).isEqualTo(DAY.atTime(10, 0));
        assertThatThrownBy(() -> new BlockSchedule(DAY, LocalTime.of(8, 15), LocalTime.of(9, 0))).isInstanceOf(SchedulingException.InvalidData.class);
        assertThatThrownBy(() -> new BlockSchedule(DAY, LocalTime.of(9, 0), LocalTime.of(9, 0))).isInstanceOf(SchedulingException.InvalidData.class);
        assertThatThrownBy(() -> block.requireFuture(DAY.atTime(8, 0))).isInstanceOf(SchedulingException.InvalidData.class);
    }

    @Test
    void overlapIsDetectedOnlyForIntersectingRangesOfTheSameDay() {
        BlockSchedule morning = new BlockSchedule(DAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertThat(morning.overlaps(new BlockSchedule(DAY, LocalTime.of(11, 30), LocalTime.of(13, 0)))).isTrue();
        assertThat(morning.overlaps(new BlockSchedule(DAY, LocalTime.of(12, 0), LocalTime.of(13, 0)))).isFalse();
        assertThat(morning.overlaps(new BlockSchedule(DAY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(12, 0)))).isFalse();
    }

    @Test
    void appointmentTransitionsAreExplicitAndTerminalStatesAreFinal() {
        assertThat(AppointmentStatus.REQUESTED.transitionTo(AppointmentStatus.APPROVED)).isEqualTo(AppointmentStatus.APPROVED);
        assertThat(AppointmentStatus.REQUESTED.transitionTo(AppointmentStatus.REJECTED)).isEqualTo(AppointmentStatus.REJECTED);
        assertThatThrownBy(() -> AppointmentStatus.APPROVED.transitionTo(AppointmentStatus.REJECTED)).isInstanceOf(SchedulingException.Conflict.class);
        assertThatThrownBy(() -> AppointmentStatus.CANCELLED.transitionTo(AppointmentStatus.APPROVED)).isInstanceOf(SchedulingException.Conflict.class);
        assertThat(AppointmentStatus.REJECTED.terminal()).isTrue();
        assertThat(AppointmentStatus.APPROVED.terminal()).isFalse();
    }

    @Test
    void bookingSlotMustBeAlignedAndFuture() {
        BookingSlot sixty = new BookingSlot(DAY.atTime(9, 0), 60);
        assertThat(sixty.end()).isEqualTo(DAY.atTime(10, 0));
        assertThat(sixty.slots()).isEqualTo(2);
        assertThatThrownBy(() -> new BookingSlot(DAY.atTime(9, 10), 30)).isInstanceOf(SchedulingException.InvalidData.class);
        assertThatThrownBy(() -> sixty.requireFuture(DAY.atTime(9, 0))).isInstanceOf(SchedulingException.InvalidData.class);
    }
}
