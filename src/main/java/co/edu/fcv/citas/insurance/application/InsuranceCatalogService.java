package co.edu.fcv.citas.insurance.application;

import co.edu.fcv.citas.insurance.application.port.in.InsuranceUseCases.ManageCatalog;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.EpsPort;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.PlanPort;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.RegimePort;
import co.edu.fcv.citas.insurance.domain.CatalogText;
import co.edu.fcv.citas.insurance.domain.InsuranceException;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.EpsView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanView;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.util.List;

/** HU-012/013 · EPS y planes: altas, ediciones y activación; nunca borrado físico (RF-06). */
public class InsuranceCatalogService implements ManageCatalog {
    private final EpsPort eps;
    private final PlanPort plans;
    private final RegimePort regimes;
    private final TransactionPort tx;

    public InsuranceCatalogService(EpsPort eps, PlanPort plans, RegimePort regimes, TransactionPort tx) {
        this.eps = eps;
        this.plans = plans;
        this.regimes = regimes;
        this.tx = tx;
    }

    @Override
    public List<EpsView> listEps() {
        return eps.findAll(false);
    }

    @Override
    public EpsView createEps(String code, String name) {
        String c = CatalogText.code(code, 30);
        String n = CatalogText.name(name, 150);
        return tx.inTransaction(() -> {
            if (eps.existsByCodeOrName(c, n, null)) throw new InsuranceException.Duplicate("Ya existe una EPS con ese código o nombre");
            return eps.findById(eps.save(null, c, n, true)).orElseThrow();
        });
    }

    @Override
    public EpsView updateEps(long id, String name, Boolean active) {
        return tx.inTransaction(() -> {
            EpsView current = eps.findById(id).orElseThrow(() -> new InsuranceException.NotFound("EPS no existe"));
            String n = name == null ? current.name() : CatalogText.name(name, 150);
            if (eps.existsByCodeOrName(current.code(), n, id)) throw new InsuranceException.Duplicate("Ya existe una EPS con ese nombre");
            eps.save(id, current.code(), n, active == null ? current.active() : active);
            return eps.findById(id).orElseThrow();
        });
    }

    @Override
    public List<PlanView> listPlans(Long epsId) {
        return plans.find(epsId, false);
    }

    @Override
    public PlanView createPlan(Long epsId, Long regimeId, String code, String name) {
        if (epsId == null || regimeId == null) throw new InsuranceException.InvalidData("La EPS y el régimen son obligatorios");
        String c = CatalogText.code(code, 50);
        String n = CatalogText.name(name, 150);
        return tx.inTransaction(() -> {
            if (eps.findById(epsId).isEmpty()) throw new InsuranceException.InvalidData("La EPS no existe");
            if (!regimes.exists(regimeId)) throw new InsuranceException.InvalidData("El régimen no existe");
            if (plans.existsCodeInEps(epsId, c)) throw new InsuranceException.Duplicate("Ya existe un plan con ese código en la EPS");
            return plans.findById(plans.create(epsId, regimeId, c, n)).orElseThrow();
        });
    }

    @Override
    public PlanView updatePlan(long id, String name, Long regimeId, Boolean active) {
        return tx.inTransaction(() -> {
            PlanView current = plans.findById(id).orElseThrow(() -> new InsuranceException.NotFound("Plan no existe"));
            long regime = regimeId == null ? current.regimeId() : regimeId;
            if (!regimes.exists(regime)) throw new InsuranceException.InvalidData("El régimen no existe");
            plans.update(id, name == null ? current.name() : CatalogText.name(name, 150), regime, active == null ? current.active() : active);
            return plans.findById(id).orElseThrow();
        });
    }

    @Override
    public List<EpsView> selectableEps() {
        return eps.findAll(true);
    }

    @Override
    public List<PlanView> selectablePlans(long epsId) {
        return plans.find(epsId, true);
    }
}
