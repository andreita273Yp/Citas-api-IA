package co.edu.fcv.citas.offer.adapter.out.persistence;

import co.edu.fcv.citas.offer.application.port.out.LocationCatalogPort;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class LocationCatalogJdbcAdapter implements LocationCatalogPort {
    private final JdbcTemplate db;

    LocationCatalogJdbcAdapter(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public Set<Long> activeIds() {
        return new HashSet<>(db.queryForList("select id from locations where active=true", Long.class));
    }
}
