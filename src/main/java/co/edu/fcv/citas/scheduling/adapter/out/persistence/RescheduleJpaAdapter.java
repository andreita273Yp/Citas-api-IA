package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.RescheduleView;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.LockedRequest;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.ReschedulePort;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Solicitudes de reprogramación (tabla `reschedule_requests` del modelo de referencia). */
@Component
class RescheduleJpaAdapter implements ReschedulePort {
    private final RescheduleRequestJpaRepository requests;
    private final StatusCatalog statuses;
    private final JdbcTemplate db;

    RescheduleJpaAdapter(RescheduleRequestJpaRepository requests, StatusCatalog statuses, JdbcTemplate db) {
        this.requests = requests;
        this.statuses = statuses;
        this.db = db;
    }

    @Override
    public boolean hasPending(long appointmentId) {
        return requests.existsByAppointmentIdAndStatusId(appointmentId, statuses.id(RescheduleStatus.PENDING));
    }

    @Override
    public long create(long appointmentId, long requestedByUserId, long locationId, LocalDateTime previousStart, LocalDateTime previousEnd,
                       LocalDateTime start, LocalDateTime end) {
        RescheduleRequestEntity r = new RescheduleRequestEntity();
        r.appointmentId = appointmentId;
        r.requestedByUserId = requestedByUserId;
        r.locationId = locationId;
        r.statusId = statuses.id(RescheduleStatus.PENDING);
        r.previousStart = previousStart;
        r.previousEnd = previousEnd;
        r.start = start;
        r.end = end;
        return requests.saveAndFlush(r).id;
    }

    @Override
    public Optional<LockedRequest> lock(long requestId) {
        return requests.lockById(requestId).map(r -> new LockedRequest(r.id, r.appointmentId, statuses.reschedule(r.statusId), r.locationId, r.start,
                r.end, r.patientAction == null ? null : RescheduleStatus.PatientAction.valueOf(r.patientAction)));
    }

    @Override
    public Optional<RescheduleView> view(long requestId) {
        return db.query(AppointmentReadModel.RESCHEDULE_VIEW + "where r.id=?", (rs, n) -> AppointmentReadModel.reschedule(rs, 1), requestId).stream().findFirst();
    }

    @Override
    public void decide(long requestId, RescheduleStatus status, String reason, long adminUserId) {
        RescheduleRequestEntity r = requests.findById(requestId).orElseThrow();
        r.statusId = statuses.id(status);
        r.decisionReason = reason;
        r.decidedByUserId = adminUserId;
        r.decidedAt = LocalDateTime.now();
        requests.saveAndFlush(r);
    }

    @Override
    public void cancelPending(long appointmentId) {
        requests.findByAppointmentIdAndStatusId(appointmentId, statuses.id(RescheduleStatus.PENDING)).forEach(r -> {
            r.statusId = statuses.id(RescheduleStatus.CANCELLED);
            r.decidedAt = LocalDateTime.now();
        });
        requests.flush();
    }

    @Override
    public Optional<Long> latestRejectedWithoutAction(long appointmentId) {
        return requests.findFirstByAppointmentIdOrderByIdDesc(appointmentId)
                .filter(r -> r.statusId == statuses.id(RescheduleStatus.REJECTED) && r.patientAction == null).map(r -> r.id);
    }

    @Override
    public void setPatientAction(long requestId, RescheduleStatus.PatientAction action) {
        RescheduleRequestEntity r = requests.findById(requestId).orElseThrow();
        r.patientAction = action.name();
        requests.saveAndFlush(r);
    }
}
