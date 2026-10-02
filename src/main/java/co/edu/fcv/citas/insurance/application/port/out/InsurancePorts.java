package co.edu.fcv.citas.insurance.application.port.out;

import co.edu.fcv.citas.insurance.domain.InsuranceViews.AffiliationView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.EpsView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanAvailability;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanView;
import java.util.List;
import java.util.Optional;

/** Puertos de salida de aseguramiento. */
public final class InsurancePorts {
    private InsurancePorts() { }

    public interface EpsPort {
        List<EpsView> findAll(boolean onlyActive);

        Optional<EpsView> findById(long id);

        boolean existsByCodeOrName(String code, String name, Long excludeId);

        /** Lanza {@code InsuranceException.Duplicate} si la base detecta unicidad violada. */
        long save(Long id, String code, String name, boolean active);
    }

    public interface PlanPort {
        List<PlanView> find(Long epsId, boolean onlySelectable);

        Optional<PlanView> findById(long id);

        boolean existsCodeInEps(long epsId, String code);

        long create(long epsId, long regimeId, String code, String name);

        void update(long id, String name, long regimeId, boolean active);

        Optional<PlanAvailability> availability(long planId);
    }

    public interface RegimePort {
        boolean exists(long regimeId);
    }

    /** Afiliaciones del usuario: a lo sumo una vigente; una fila por plan (se reactiva si se vuelve a elegir). */
    public interface AffiliationPort {
        Optional<AffiliationView> current(long userId);

        void lockUser(long userId);

        /** Termina la vigente (is_current=false, valid_to=hoy). */
        void endCurrent(long userId);

        /** Hace vigente la fila del plan para el usuario, creándola o reactivándola. */
        void makeCurrent(long userId, long planId, String membershipNumber);
    }
}
