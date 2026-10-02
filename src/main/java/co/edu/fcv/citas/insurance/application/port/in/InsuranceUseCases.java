package co.edu.fcv.citas.insurance.application.port.in;

import co.edu.fcv.citas.insurance.domain.InsuranceViews.AffiliationView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.EpsView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanView;
import java.util.List;
import java.util.Optional;

/** Casos de uso de aseguramiento. */
public final class InsuranceUseCases {
    private InsuranceUseCases() { }

    /** HU-012/013 · CRUD lógico ADMIN de EPS y planes (sin borrado físico) y catálogo público de selección. */
    public interface ManageCatalog {
        List<EpsView> listEps();

        EpsView createEps(String code, String name);

        EpsView updateEps(long id, String name, Boolean active);

        List<PlanView> listPlans(Long epsId);

        PlanView createPlan(Long epsId, Long regimeId, String code, String name);

        PlanView updatePlan(long id, String name, Long regimeId, Boolean active);

        /** EPS activas, para registro y afiliación. */
        List<EpsView> selectableEps();

        /** Planes activos de una EPS activa (HU-013 CA-03). */
        List<PlanView> selectablePlans(long epsId);
    }

    /** HU-011 · Afiliación propia, opcional: no condiciona búsqueda ni reserva. */
    public interface ManageAffiliation {
        Optional<AffiliationView> current(long userId);

        AffiliationView affiliate(long userId, Long planId, String membershipNumber);

        void end(long userId);
    }
}
