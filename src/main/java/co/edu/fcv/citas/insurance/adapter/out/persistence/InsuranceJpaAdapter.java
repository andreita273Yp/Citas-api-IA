package co.edu.fcv.citas.insurance.adapter.out.persistence;

import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.AffiliationPort;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.PlanPort;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.RegimePort;
import co.edu.fcv.citas.insurance.domain.InsuranceException;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.AffiliationView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanAvailability;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Planes, regímenes y afiliaciones: escrituras con Spring Data JPA; vistas con EPS, régimen y plan por SQL. */
@Component
class InsuranceJpaAdapter implements PlanPort, RegimePort, AffiliationPort {
    private static final String PLAN_VIEW = "select p.id,p.eps_id,e.name,p.regime_id,r.code,r.name,p.code,p.name,p.active "
            + "from eps_plans p join eps e on e.id=p.eps_id join insurance_regimes r on r.id=p.regime_id ";
    private final EpsPlanJpaRepository plans;
    private final AffiliationJpaRepository affiliations;
    private final JdbcTemplate db;

    InsuranceJpaAdapter(EpsPlanJpaRepository plans, AffiliationJpaRepository affiliations, JdbcTemplate db) {
        this.plans = plans;
        this.affiliations = affiliations;
        this.db = db;
    }

    // ---------------------------------------------------------------- planes

    @Override
    public List<PlanView> find(Long epsId, boolean onlySelectable) {
        return db.query(PLAN_VIEW + "where (? is null or p.eps_id=?) and (?=false or (p.active=true and e.active=true)) order by e.name,p.name",
                (rs, n) -> plan(rs), epsId, epsId, onlySelectable);
    }

    @Override
    public Optional<PlanView> findById(long id) {
        return db.query(PLAN_VIEW + "where p.id=?", (rs, n) -> plan(rs), id).stream().findFirst();
    }

    @Override
    public boolean existsCodeInEps(long epsId, String code) {
        return plans.existsByEpsIdAndCodeIgnoreCase(epsId, code);
    }

    @Override
    public long create(long epsId, long regimeId, String code, String name) {
        EpsPlanEntity p = new EpsPlanEntity();
        p.epsId = epsId;
        p.regimeId = regimeId;
        p.code = code;
        p.name = name;
        try {
            return plans.saveAndFlush(p).id;
        } catch (DataIntegrityViolationException ex) {
            throw new InsuranceException.Duplicate("Ya existe un plan con ese código en la EPS");
        }
    }

    @Override
    public void update(long id, String name, long regimeId, boolean active) {
        EpsPlanEntity p = plans.findById(id).orElseThrow();
        p.name = name;
        p.regimeId = regimeId;
        p.active = active;
        plans.saveAndFlush(p);
    }

    @Override
    public Optional<PlanAvailability> availability(long planId) {
        return db.query("select p.id,p.active,e.active from eps_plans p join eps e on e.id=p.eps_id where p.id=?",
                (rs, n) -> new PlanAvailability(rs.getLong(1), rs.getBoolean(2), rs.getBoolean(3)), planId).stream().findFirst();
    }

    // ---------------------------------------------------------------- regímenes

    @Override
    public boolean exists(long regimeId) {
        return db.queryForObject("select count(*) from insurance_regimes where id=?", Integer.class, regimeId) > 0;
    }

    // ---------------------------------------------------------------- afiliación

    @Override
    public Optional<AffiliationView> current(long userId) {
        return db.query("select a.plan_id,p.name,e.id,e.name,r.code,r.name,a.membership_number,a.valid_from "
                        + "from user_insurance_affiliations a join eps_plans p on p.id=a.plan_id join eps e on e.id=p.eps_id "
                        + "join insurance_regimes r on r.id=p.regime_id where a.user_id=? and a.is_current=true order by a.id desc",
                (rs, n) -> new AffiliationView(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getDate(8) == null ? null : rs.getDate(8).toLocalDate()), userId).stream().findFirst();
    }

    @Override
    public void lockUser(long userId) {
        db.queryForList("select id from users where id=? for update", Long.class, userId);
    }

    @Override
    public void endCurrent(long userId) {
        affiliations.findByUserIdAndCurrentTrue(userId).forEach(a -> {
            a.current = false;
            a.validTo = LocalDate.now();
        });
        affiliations.flush();
    }

    @Override
    public void makeCurrent(long userId, long planId, String membershipNumber) {
        AffiliationEntity a = affiliations.findFirstByUserIdAndPlanIdOrderByIdDesc(userId, planId).orElseGet(AffiliationEntity::new);
        a.userId = userId;
        a.planId = planId;
        a.membershipNumber = membershipNumber;
        a.current = true;
        a.validFrom = LocalDate.now();
        a.validTo = null;
        affiliations.saveAndFlush(a);
    }

    private static PlanView plan(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PlanView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getString(5), rs.getString(6), rs.getString(7),
                rs.getString(8), rs.getBoolean(9));
    }
}
