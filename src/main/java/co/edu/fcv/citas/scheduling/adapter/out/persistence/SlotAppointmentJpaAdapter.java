package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.AppointmentPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.AppointmentState;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.LockedSlot;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.NewAppointment;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SlotPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Slots y citas con Spring Data JPA y bloqueo pesimista. */
@Component
class SlotAppointmentJpaAdapter implements SlotPort, AppointmentPort {
    private final ProfessionalSlotJpaRepository slots;
    private final AppointmentJpaRepository appointments;
    private final JdbcTemplate db;
    private final Map<AppointmentStatus, Long> statusIds = new ConcurrentHashMap<>();

    SlotAppointmentJpaAdapter(ProfessionalSlotJpaRepository slots, AppointmentJpaRepository appointments, JdbcTemplate db) {
        this.slots = slots;
        this.appointments = appointments;
        this.db = db;
    }

    @Override
    public List<LockedSlot> lock(long professionalId, long locationId, LocalDateTime start, LocalDateTime end) {
        return slots.lockRange(professionalId, locationId, start, end).stream()
                .map(s -> new LockedSlot(s.id, s.startAt, s.endAt, s.appointmentId == null)).toList();
    }

    @Override
    public int assign(long appointmentId, List<Long> slotIds) {
        return slots.assignFree(appointmentId, slotIds);
    }

    @Override
    public void release(long appointmentId) {
        slots.release(appointmentId);
    }

    @Override
    public long create(NewAppointment a) {
        AppointmentEntity e = new AppointmentEntity();
        e.patientUserId = a.patientUserId();
        e.professionalId = a.professionalId();
        e.locationId = a.locationId();
        e.specialtyId = a.specialtyId();
        e.statusId = statusId(a.status());
        e.reason = a.reason();
        e.start = a.start();
        e.end = a.end();
        e.createdByUserId = a.patientUserId();
        e.insuranceAffiliationId = currentAffiliation(a.patientUserId());
        if (a.status() == AppointmentStatus.APPROVED) e.approvedAt = LocalDateTime.now();
        return appointments.saveAndFlush(e).id;
    }

    @Override
    public Optional<AppointmentState> lock(long appointmentId) {
        return appointments.lockById(appointmentId).map(e -> new AppointmentState(e.id, statusOf(e.statusId), e.start));
    }

    @Override
    public void changeStatus(long appointmentId, AppointmentStatus status, Long approvedByUserId) {
        AppointmentEntity e = appointments.findById(appointmentId).orElseThrow();
        e.statusId = statusId(status);
        if (status == AppointmentStatus.APPROVED) {
            e.approvedByUserId = approvedByUserId;
            e.approvedAt = LocalDateTime.now();
        }
        appointments.saveAndFlush(e);
    }

    /** Afiliación vigente opcional: no condiciona la reserva. */
    private Long currentAffiliation(long userId) {
        List<Long> ids = db.queryForList("select id from user_insurance_affiliations where user_id=? and is_current=true", Long.class, userId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private long statusId(AppointmentStatus status) {
        if (statusIds.isEmpty()) loadStatuses();
        return statusIds.get(status);
    }

    private AppointmentStatus statusOf(long id) {
        if (statusIds.isEmpty()) loadStatuses();
        return statusIds.entrySet().stream().filter(e -> e.getValue() == id).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private void loadStatuses() {
        statusIds.putAll(db.query("select code,id from appointment_statuses", (rs, n) -> Map.entry(AppointmentStatus.valueOf(rs.getString(1)), rs.getLong(2)))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
