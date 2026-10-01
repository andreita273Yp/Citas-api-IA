package co.edu.fcv.citas.insurance.adapter.out.persistence;

import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.EpsPort;
import co.edu.fcv.citas.insurance.domain.InsuranceException;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.EpsView;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class EpsJpaAdapter implements EpsPort {
    private final EpsJpaRepository eps;
    private final JdbcTemplate db;

    EpsJpaAdapter(EpsJpaRepository eps, JdbcTemplate db) {
        this.eps = eps;
        this.db = db;
    }

    @Override
    public List<EpsView> findAll(boolean onlyActive) {
        return db.query("select id,code,name,active from eps where (?=false or active=true) order by name",
                (rs, n) -> new EpsView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)), onlyActive);
    }

    @Override
    public Optional<EpsView> findById(long id) {
        return eps.findById(id).map(e -> new EpsView(e.id, e.code, e.name, e.active));
    }

    @Override
    public boolean existsByCodeOrName(String code, String name, Long excludeId) {
        return eps.existsByCodeOrName(code, name, excludeId);
    }

    @Override
    public long save(Long id, String code, String name, boolean active) {
        EpsEntity e = id == null ? new EpsEntity() : eps.findById(id).orElseThrow();
        e.code = code;
        e.name = name;
        e.active = active;
        try {
            return eps.saveAndFlush(e).id;
        } catch (DataIntegrityViolationException ex) {
            throw new InsuranceException.Duplicate("Ya existe una EPS con ese código o nombre");
        }
    }
}
