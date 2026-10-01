package co.edu.fcv.citas.identity.adapter.out.persistence;

import co.edu.fcv.citas.identity.application.port.out.InsuranceAffiliationPort;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Afiliación inicial al registrarse; el módulo de EPS/planes (HU-011 a HU-013) se completa en la Fase 4. */
@Component
class InsuranceAffiliationJdbcAdapter implements InsuranceAffiliationPort {
    private final JdbcTemplate db;

    InsuranceAffiliationJdbcAdapter(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public boolean isActivePlan(long planId) {
        return db.queryForObject("select count(*) from eps_plans p join eps e on e.id=p.eps_id where p.id=? and p.active=true and e.active=true",
                Integer.class, planId) > 0;
    }

    @Override
    public void affiliate(long userId, long planId) {
        db.update("insert into user_insurance_affiliations(user_id,plan_id,membership_number,is_current,valid_from) values(?,?,?,true,curdate())",
                userId, planId, "AF-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
