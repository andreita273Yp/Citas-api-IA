package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.AppointmentDetail;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.RescheduleView;
import co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentQueryPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Modelo de lectura de "Mis citas" (RF-13): sede, profesional, especialidad, franja, estado, motivo de rechazo y última reprogramación. */
@Component
class AppointmentReadModel implements AppointmentQueryPort {
    static final String RESCHEDULE_VIEW = "select r.id,r.appointment_id,rs.code,r.requested_location_id,rl.code,r.requested_start_at,r.requested_end_at,"
            + "r.decision_reason,r.patient_action_after_rejection from reschedule_requests r "
            + "join reschedule_request_statuses rs on rs.id=r.status_id join locations rl on rl.id=r.requested_location_id ";
    private static final String VIEW = "select a.id,st.code,a.location_id,l.code,l.name,concat(pu.first_name,' ',pu.last_name),s.name,"
            + "timestampdiff(minute,a.scheduled_start_at,a.scheduled_end_at),a.scheduled_start_at,a.scheduled_end_at,a.reason,"
            + "case when st.code='REJECTED' then (select h.reason from appointment_status_history h where h.appointment_id=a.id and h.status_id=a.status_id "
            + "order by h.changed_at desc,h.id desc limit 1) end,"
            + "(select max(r.id) from reschedule_requests r where r.appointment_id=a.id),a.professional_id,a.specialty_id "
            + "from appointments a join appointment_statuses st on st.id=a.status_id join locations l on l.id=a.location_id "
            + "join professionals p on p.id=a.professional_id join users pu on pu.id=p.user_id join specialties s on s.id=a.specialty_id ";
    private final JdbcTemplate db;

    AppointmentReadModel(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public List<AppointmentDetail> listForPatient(long userId, AppointmentStatus status, LocalDate from, LocalDate to) {
        String code = status == null ? null : status.name();
        return db.query(VIEW + "where a.patient_user_id=? and (? is null or st.code=?) and (? is null or a.scheduled_start_at>=?) "
                        + "and (? is null or a.scheduled_start_at<?) order by a.scheduled_start_at desc",
                (rs, n) -> detail(rs), userId, code, code, from, from == null ? null : from.atStartOfDay(), to, to == null ? null : to.plusDays(1).atStartOfDay());
    }

    @Override
    public Optional<AppointmentDetail> detailForPatient(long userId, long appointmentId) {
        return db.query(VIEW + "where a.id=? and a.patient_user_id=?", (rs, n) -> detail(rs), appointmentId, userId).stream().findFirst();
    }

    private AppointmentDetail detail(ResultSet rs) throws SQLException {
        long rescheduleId = rs.getLong(13);
        RescheduleView reschedule = rs.wasNull() ? null
                : db.query(RESCHEDULE_VIEW + "where r.id=?", (r, n) -> reschedule(r, 1), rescheduleId).stream().findFirst().orElse(null);
        return new AppointmentDetail(rs.getLong(1), AppointmentStatus.valueOf(rs.getString(2)), rs.getLong(3), rs.getString(4), rs.getString(5),
                rs.getLong(14), rs.getString(6), rs.getLong(15), rs.getString(7), rs.getInt(8), rs.getTimestamp(9).toLocalDateTime(), rs.getTimestamp(10).toLocalDateTime(),
                rs.getString(11), rs.getString(12), reschedule);
    }

    static RescheduleView reschedule(ResultSet rs, int from) throws SQLException {
        String action = rs.getString(from + 8);
        return new RescheduleView(rs.getLong(from), rs.getLong(from + 1), RescheduleStatus.valueOf(rs.getString(from + 2)), rs.getLong(from + 3),
                rs.getString(from + 4), rs.getTimestamp(from + 5).toLocalDateTime(), rs.getTimestamp(from + 6).toLocalDateTime(),
                rs.getString(from + 7), action == null ? null : RescheduleStatus.PatientAction.valueOf(action));
    }
}
