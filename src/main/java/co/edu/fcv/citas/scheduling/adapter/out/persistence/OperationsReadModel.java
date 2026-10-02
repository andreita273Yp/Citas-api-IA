package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.HistoryEntry;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxFilter;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxItem;
import co.edu.fcv.citas.scheduling.application.port.in.OperationsUseCases.InboxKind;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.AgendaQueryPort;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.AgendaRow;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.HistoryQueryPort;
import co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.InboxQueryPort;
import co.edu.fcv.citas.scheduling.domain.AgendaWindow;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Modelos de lectura de agenda profesional (RF-16), bandeja ADMIN (RF-18) e historial de estados (RF-19). */
@Component
class OperationsReadModel implements AgendaQueryPort, InboxQueryPort, HistoryQueryPort {
    private static final String NAMES = "join users u on u.id=a.patient_user_id join professionals p on p.id=a.professional_id "
            + "join users pu on pu.id=p.user_id join specialties s on s.id=a.specialty_id ";
    private static final String SPECIALIZED = "select 'SPECIALIZED_REQUEST',a.id,a.id,a.location_id,l.code,concat(u.first_name,' ',u.last_name),"
            + "concat(pu.first_name,' ',pu.last_name),s.name,a.scheduled_start_at starts_at,a.scheduled_end_at,null,a.reason "
            + "from appointments a join appointment_statuses st on st.id=a.status_id join locations l on l.id=a.location_id " + NAMES
            + "where st.code='REQUESTED' and (? is null or a.location_id=?) and (? is null or a.professional_id=?) and (? is null or a.specialty_id=?) "
            + "and (? is null or date(a.scheduled_start_at)=?)";
    private static final String RESCHEDULES = "select 'RESCHEDULE',r.id,a.id,r.requested_location_id,l.code,concat(u.first_name,' ',u.last_name),"
            + "concat(pu.first_name,' ',pu.last_name),s.name,r.requested_start_at starts_at,r.requested_end_at,a.scheduled_start_at,a.reason "
            + "from reschedule_requests r join reschedule_request_statuses rs on rs.id=r.status_id join appointments a on a.id=r.appointment_id "
            + "join locations l on l.id=r.requested_location_id " + NAMES
            + "where rs.code='PENDING' and (? is null or r.requested_location_id=?) and (? is null or a.professional_id=?) and (? is null or a.specialty_id=?) "
            + "and (? is null or date(r.requested_start_at)=?)";
    private final JdbcTemplate db;

    OperationsReadModel(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public List<AgendaRow> approvedFor(long professionalId, AgendaWindow window, Long locationId) {
        // Del paciente solo se expone el nombre: ni documento, ni correo, ni teléfono (RF-16, privacidad).
        return db.query("select a.id,a.location_id,l.code,l.name,concat(u.first_name,' ',u.last_name),s.name,a.scheduled_start_at,a.scheduled_end_at,a.reason "
                        + "from appointments a join appointment_statuses st on st.id=a.status_id join locations l on l.id=a.location_id "
                        + "join users u on u.id=a.patient_user_id join specialties s on s.id=a.specialty_id "
                        + "where a.professional_id=? and st.code='APPROVED' and (? is null or a.location_id=?) "
                        + "and (? is null or a.scheduled_start_at>=?) and (? is null or a.scheduled_start_at<?) order by a.scheduled_start_at",
                (rs, n) -> new AgendaRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getTimestamp(7).toLocalDateTime(), rs.getTimestamp(8).toLocalDateTime(), rs.getString(9)),
                professionalId, locationId, locationId, window.startInclusive(), window.startInclusive(), window.endExclusive(), window.endExclusive());
    }

    @Override
    public List<InboxItem> pending(InboxFilter f) {
        Object[] params = {f.locationId(), f.locationId(), f.professionalId(), f.professionalId(), f.specialtyId(), f.specialtyId(), f.date(), f.date()};
        Object[] both = new Object[params.length * 2];
        System.arraycopy(params, 0, both, 0, params.length);
        System.arraycopy(params, 0, both, params.length, params.length);
        return db.query(SPECIALIZED + " union all " + RESCHEDULES + " order by starts_at", (rs, n) -> {
            Timestamp current = rs.getTimestamp(11);
            return new InboxItem(InboxKind.valueOf(rs.getString(1)), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getString(5), rs.getString(6),
                    rs.getString(7), rs.getString(8), rs.getTimestamp(9).toLocalDateTime(), rs.getTimestamp(10).toLocalDateTime(),
                    current == null ? null : current.toLocalDateTime(), rs.getString(12));
        }, both);
    }

    @Override
    public boolean isPatient(long appointmentId, long userId) {
        return count("select count(*) from appointments where id=? and patient_user_id=?", appointmentId, userId);
    }

    @Override
    public boolean isAssignedProfessional(long appointmentId, long userId) {
        return count("select count(*) from appointments a join professionals p on p.id=a.professional_id where a.id=? and p.user_id=?", appointmentId, userId);
    }

    @Override
    public List<HistoryEntry> entries(long appointmentId) {
        List<HistoryEntry> raw = db.query("select h.id,s.code,h.changed_by_user_id,h.change_source,h.reason,h.changed_at "
                        + "from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? order by h.changed_at,h.id",
                (rs, n) -> new HistoryEntry(rs.getLong(1), null, AppointmentStatus.valueOf(rs.getString(2)), rs.getObject(3, Long.class), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toLocalDateTime()), appointmentId);
        List<HistoryEntry> result = new ArrayList<>(raw.size());
        AppointmentStatus previous = null;
        for (HistoryEntry e : raw) {
            result.add(new HistoryEntry(e.id(), previous, e.newStatus(), e.actorId(), e.source(), e.reason(), e.occurredAt()));
            previous = e.newStatus();
        }
        return result;
    }

    private boolean count(String sql, Object... args) {
        Integer n = db.queryForObject(sql, Integer.class, args);
        return n != null && n > 0;
    }
}
