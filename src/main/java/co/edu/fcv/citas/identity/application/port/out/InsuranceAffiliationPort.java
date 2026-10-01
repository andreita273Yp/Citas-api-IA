package co.edu.fcv.citas.identity.application.port.out;

/** Afiliación inicial opcional a un plan EPS activo al registrarse. */
public interface InsuranceAffiliationPort {
    boolean isActivePlan(long planId);

    void affiliate(long userId, long planId);
}
