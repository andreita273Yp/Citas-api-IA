package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.RescheduleStatus;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Traduce los catálogos fijos de estados (V6) entre código de dominio e id de base; se cargan una vez. */
@Component
class StatusCatalog {
    private final JdbcTemplate db;
    private volatile Map<AppointmentStatus, Long> appointment;
    private volatile Map<RescheduleStatus, Long> reschedule;

    StatusCatalog(JdbcTemplate db) {
        this.db = db;
    }

    long id(AppointmentStatus status) {
        if (appointment == null) appointment = load("appointment_statuses", AppointmentStatus.class);
        return appointment.get(status);
    }

    long id(RescheduleStatus status) {
        if (reschedule == null) reschedule = load("reschedule_request_statuses", RescheduleStatus.class);
        return reschedule.get(status);
    }

    AppointmentStatus appointment(long id) {
        id(AppointmentStatus.REQUESTED);
        return appointment.entrySet().stream().filter(e -> e.getValue() == id).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    RescheduleStatus reschedule(long id) {
        id(RescheduleStatus.PENDING);
        return reschedule.entrySet().stream().filter(e -> e.getValue() == id).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private <E extends Enum<E>> Map<E, Long> load(String table, Class<E> type) {
        Map<E, Long> map = new EnumMap<>(type);
        db.query("select code,id from " + table, rs -> { map.put(Enum.valueOf(type, rs.getString(1)), rs.getLong(2)); });
        return map;
    }
}
