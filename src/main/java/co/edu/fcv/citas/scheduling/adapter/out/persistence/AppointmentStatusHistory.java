package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Auditoría append-only de estados de cita (RF-19, RN-12): solo inserta y lee; no expone
 * actualización ni borrado. Las fuentes válidas son las del modelo: SYSTEM, USER y ADMIN
 * (un PROFESSIONAL registra como USER; el actor queda en changed_by_user_id).
 */
@Component
public class AppointmentStatusHistory implements StatusHistoryPort {
  private final JdbcTemplate db;

  public AppointmentStatusHistory(JdbcTemplate db) { this.db = db; }

  @Override
  public void record(long appointmentId, AppointmentStatus status, Long actorUserId, String source, String reason) {
    record(appointmentId, status.name(), actorUserId, source, reason);
  }

  public void record(long appointmentId, String statusCode, Long actorUserId, String source, String reason) {
    db.update("insert into appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason) "
        + "values(?,(select id from appointment_statuses where code=?),?,?,?)",
        appointmentId, statusCode, actorUserId, source, reason);
  }

  /** Historial en orden cronológico; el estado anterior se deriva de la entrada previa. */
  public List<Entry> of(long appointmentId) {
    List<Entry> raw = db.query("select h.id,s.code,h.changed_by_user_id,h.change_source,h.reason,h.changed_at "
        + "from appointment_status_history h join appointment_statuses s on s.id=h.status_id "
        + "where h.appointment_id=? order by h.changed_at,h.id",
        (rs, n) -> new Entry(rs.getString(1), null, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
            rs.getTimestamp(6).toLocalDateTime()), appointmentId);
    List<Entry> result = new ArrayList<>(raw.size());
    String previous = null;
    for (Entry e : raw) {
      result.add(new Entry(e.id(), previous, e.newStatus(), e.actorId(), e.source(), e.reason(), e.occurredAt()));
      previous = e.newStatus();
    }
    return result;
  }

  public record Entry(String id, String previousStatus, String newStatus, String actorId, String source, String reason,
      LocalDateTime occurredAt) {}
}
