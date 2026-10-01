package co.edu.fcv.citas.offer.adapter.out.persistence;

import co.edu.fcv.citas.offer.domain.ProfessionalCredentials;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.ProfessionalView.AssignedLocation;
import co.edu.fcv.citas.offer.domain.ProfessionalView.AssignedSpecialty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Vista de lectura de profesionales con su usuario y asignaciones vigentes (joins de solo lectura). */
@Component
class ProfessionalReadModel {
    private static final String BASE = "select p.id,p.user_id,u.first_name,u.last_name,u.email,u.phone,p.professional_code,p.license_number,p.active "
            + "from professionals p join users u on u.id=p.user_id ";
    private final JdbcTemplate db;

    ProfessionalReadModel(JdbcTemplate db) {
        this.db = db;
    }

    List<ProfessionalView> findAll() {
        return load(BASE + "order by u.last_name,u.first_name", null);
    }

    Optional<ProfessionalView> findById(long id) {
        return load(BASE + "where p.id=?", id).stream().findFirst();
    }

    private List<ProfessionalView> load(String sql, Long id) {
        record Row(long id, long userId, String first, String last, String email, String phone, String code, String license, boolean active) { }
        List<Row> rows = db.query(sql, (rs, n) -> new Row(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9)), id == null ? new Object[0] : new Object[] {id});
        if (rows.isEmpty()) return List.of();

        Map<Long, List<AssignedSpecialty>> specialties = new HashMap<>();
        db.query("select ps.professional_id,s.id,s.code,s.name,s.appointment_duration_minutes,ps.is_primary from professional_specialties ps "
                        + "join specialties s on s.id=ps.specialty_id where ps.active=true " + (id == null ? "" : "and ps.professional_id=? ")
                        + "order by ps.is_primary desc,s.name",
                rs -> {
                    specialties.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                            .add(new AssignedSpecialty(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getBoolean(6)));
                }, id == null ? new Object[0] : new Object[] {id});

        Map<Long, List<AssignedLocation>> locations = new HashMap<>();
        db.query("select pl.professional_id,l.id,l.code,l.name from professional_locations pl join locations l on l.id=pl.location_id "
                        + "where pl.active=true " + (id == null ? "" : "and pl.professional_id=? ") + "order by l.id",
                rs -> {
                    locations.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                            .add(new AssignedLocation(rs.getLong(2), rs.getString(3), rs.getString(4)));
                }, id == null ? new Object[0] : new Object[] {id});

        return rows.stream().map(r -> new ProfessionalView(r.id(), r.userId(), r.first(), r.last(), r.email(), r.phone(),
                new ProfessionalCredentials(r.code(), r.license()), r.active(),
                List.copyOf(specialties.getOrDefault(r.id(), List.of())), List.copyOf(locations.getOrDefault(r.id(), List.of())))).toList();
    }
}
