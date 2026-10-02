package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.StatusHistoryPort;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Auditoría append-only de estados de cita (RF-19, RN-12): solo inserta; V7 rechaza UPDATE y DELETE en la base.
 * Las fuentes válidas son las del modelo: SYSTEM, USER y ADMIN (un PROFESSIONAL registra como USER; el actor queda en
 * changed_by_user_id). La lectura vive en {@link OperationsReadModel}.
 */
@Component
class AppointmentStatusHistory implements StatusHistoryPort {
    private final JdbcTemplate db;

    AppointmentStatusHistory(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public void record(long appointmentId, AppointmentStatus status, Long actorUserId, String source, String reason) {
        db.update("insert into appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason) "
                + "values(?,(select id from appointment_statuses where code=?),?,?,?)", appointmentId, status.name(), actorUserId, source, reason);
    }
}
