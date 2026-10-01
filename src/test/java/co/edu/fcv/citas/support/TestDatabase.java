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
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }

    public void grantRole(String email, String roleCode) {
        db.update("insert ignore into user_roles(user_id, role_id) "
                + "select u.id, r.id from users u join roles r on r.code = ? where u.email = ?", roleCode, email);
    }
}
