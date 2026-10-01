package co.edu.fcv.citas.support;

import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Utilidades de datos para pruebas de integración sobre MySQL. Limpia los datos transaccionales
 * entre pruebas y conserva los catálogos fijos sembrados por V6.
 */
@Component
public class TestDatabase {
    private static final List<String> TRANSACTIONAL_TABLES = List.of(
            "appointment_status_history", "reschedule_requests", "professional_slots", "appointments",
            "availability_blocks", "professional_locations", "professional_specialties", "professionals",
            "password_reset_tokens", "refresh_tokens", "user_insurance_affiliations", "user_roles", "users",
            "eps_plans", "eps");

    private final JdbcTemplate db;

    public TestDatabase(JdbcTemplate db) {
        this.db = db;
    }

    public void reset() {
        // Una sola conexión: FOREIGN_KEY_CHECKS es una variable de sesión.
        db.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : TRANSACTIONAL_TABLES) statement.execute("TRUNCATE TABLE " + table);
                statement.execute("TRUNCATE TABLE specialties");
                statement.execute("INSERT INTO specialties SELECT * FROM " + TestDatabaseConfig.SPECIALTIES_SNAPSHOT);
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }

    /** Inserta un bloque de disponibilidad y sus slots de 30 min (atajo hasta que exista HU-018). */
    public void publishBlock(long professionalId, long locationId, java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        db.update("insert into availability_blocks(professional_id,location_id,available_date,start_time,end_time,active) values(?,?,?,?,?,true)",
                professionalId, locationId, date, start, end);
        long blockId = db.queryForObject("select last_insert_id()", Long.class);
        for (java.time.LocalTime t = start; t.isBefore(end); t = t.plusMinutes(30))
            db.update("insert into professional_slots(availability_block_id,start_at,end_at) values(?,?,?)",
                    blockId, date.atTime(t), date.atTime(t.plusMinutes(30)));
    }

    public long insertEps(String code, String name) {
        db.update("insert into eps(code,name,active) values(?,?,true)", code, name);
        return db.queryForObject("select id from eps where code=?", Long.class, code);
    }

    public long insertPlan(long epsId, String regimeCode, String code, String name) {
        db.update("insert into eps_plans(eps_id,regime_id,code,name,active) values(?,(select id from insurance_regimes where code=?),?,?,true)",
                epsId, regimeCode, code, name);
        return db.queryForObject("select id from eps_plans where eps_id=? and code=?", Long.class, epsId, code);
    }

    public void grantRole(String email, String roleCode) {
        db.update("insert ignore into user_roles(user_id, role_id) "
                + "select u.id, r.id from users u join roles r on r.code = ? where u.email = ?", roleCode, email);
    }
}
