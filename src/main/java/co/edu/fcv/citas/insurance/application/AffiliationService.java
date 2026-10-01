package co.edu.fcv.citas.insurance.application;

import co.edu.fcv.citas.insurance.application.port.in.InsuranceUseCases.ManageAffiliation;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.AffiliationPort;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts.PlanPort;
import co.edu.fcv.citas.insurance.domain.CatalogText;
import co.edu.fcv.citas.insurance.domain.InsuranceException;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.AffiliationView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanAvailability;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.util.Optional;

/**
 * HU-011 · La afiliación referencia solo el plan; EPS y régimen se derivan de él (RF-04, sin duplicar).
 * Repetir el plan vigente no crea filas; cambiarlo deja una única afiliación vigente.
 */
public class AffiliationService implements ManageAffiliation {
    private final AffiliationPort affiliations;
    private final PlanPort plans;
    private final TransactionPort tx;

    public AffiliationService(AffiliationPort affiliations, PlanPort plans, TransactionPort tx) {
        this.affiliations = affiliations;
        this.plans = plans;
        this.tx = tx;
    }

    @Override
    public Optional<AffiliationView> current(long userId) {
        return affiliations.current(userId);
    }

    @Override
    public AffiliationView affiliate(long userId, Long planId, String membershipNumber) {
        if (planId == null) throw new InsuranceException.InvalidData("El plan es obligatorio");
        String membership = CatalogText.membership(membershipNumber);
        return tx.inTransaction(() -> {
            affiliations.lockUser(userId);
            if (!plans.availability(planId).map(PlanAvailability::selectable).orElse(false))
                throw new InsuranceException.InvalidData("El plan no existe, está inactivo o su EPS está inactiva");
            Optional<AffiliationView> current = affiliations.current(userId);
            boolean unchanged = current.map(a -> a.planId() == planId && a.membershipNumber().equals(membership)).orElse(false);
            if (!unchanged) {
                if (current.isPresent() && current.get().planId() != planId) affiliations.endCurrent(userId);
                affiliations.makeCurrent(userId, planId, membership);
            }
            return affiliations.current(userId).orElseThrow();
        });
    }

    @Override
    public void end(long userId) {
        tx.inTransaction(() -> {
            affiliations.lockUser(userId);
            affiliations.endCurrent(userId);
            return null;
        });
    }
}
