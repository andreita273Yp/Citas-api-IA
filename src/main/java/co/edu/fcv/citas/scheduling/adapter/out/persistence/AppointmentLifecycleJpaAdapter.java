package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentWritePort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.HeldSlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.LockedAppointment;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Cambios de estado y de franja de una cita, y gestión de sus slots retenidos (Spring Data JPA). */
@Component
class AppointmentLifecycleJpaAdapter implements AppointmentWritePort, HeldSlotPort {
    private final AppointmentJpaRepository appointments;
    private final ProfessionalSlotJpaRepository slots;
    private final StatusCatalog statuses;

    AppointmentLifecycleJpaAdapter(AppointmentJpaRepository appointments, ProfessionalSlotJpaRepository slots, StatusCatalog statuses) {
        this.appointments = appointments;
        this.slots = slots;
        this.statuses = statuses;
    }

    @Override
    public Optional<LockedAppointment> lock(long appointmentId) {
        return appointments.lockById(appointmentId).map(a -> new LockedAppointment(a.id, a.patientUserId, a.professionalId, a.locationId,
                a.specialtyId, statuses.appointment(a.statusId), a.start, a.end));
    }

    @Override
    public void changeStatus(long appointmentId, AppointmentStatus status) {
        AppointmentEntity a = appointments.findById(appointmentId).orElseThrow();
        a.statusId = statuses.id(status);
        appointments.saveAndFlush(a);
    }

    @Override
    public void changeSchedule(long appointmentId, long locationId, LocalDateTime start, LocalDateTime end) {
        AppointmentEntity a = appointments.findById(appointmentId).orElseThrow();
        a.locationId = locationId;
        a.start = start;
        a.end = end;
        appointments.saveAndFlush(a);
    }

    @Override
    public int countHeld(long appointmentId, LocalDateTime start, LocalDateTime end) {
        return (int) slots.countHeld(appointmentId, start, end);
    }

    @Override
    public void releaseInside(long appointmentId, LocalDateTime start, LocalDateTime end) {
        slots.releaseInside(appointmentId, start, end);
    }

    @Override
    public void releaseOutside(long appointmentId, LocalDateTime start, LocalDateTime end) {
        slots.releaseOutside(appointmentId, start, end);
    }
}
