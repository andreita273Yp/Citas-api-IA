package co.edu.fcv.citas.catalog.adapter.out.persistence;

import co.edu.fcv.citas.catalog.application.port.out.FixedCatalogReadPort;
import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lee los catálogos fijos de solo lectura del modelo de referencia (V6). */
@Component
public class JdbcFixedCatalogAdapter implements FixedCatalogReadPort {
    private static final Map<String, String> TABLES = Map.of(
            "ROLE", "roles",
            "APPOINTMENT_STATUS", "appointment_statuses",
            "RESCHEDULE_STATUS", "reschedule_request_statuses",
            "REGIME", "insurance_regimes");

    private final JdbcTemplate db;

    public JdbcFixedCatalogAdapter(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public List<CatalogItem> findByType(String type) {
        String table = TABLES.get(type);
        if (table == null) throw new IllegalArgumentException("Catálogo desconocido: " + type);
        return db.query("select id, code, name from " + table + " order by id",
                (rs, n) -> new CatalogItem(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }

    @Override
    public List<Location> findLocations() {
        return db.query("select id, code, name, address, city, department from locations where active = true order by id",
                (rs, n) -> new Location(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)));
    }
}
