package co.edu.fcv.citas.insurance.domain;

import java.time.LocalDate;

/** Vistas de lectura del aseguramiento. El régimen y la EPS se derivan del plan (RF-04, sin duplicar). */
public final class InsuranceViews {
    private InsuranceViews() { }

    public record EpsView(long id, String code, String name, boolean active) { }

    public record PlanView(long id, long epsId, String epsName, long regimeId, String regimeCode, String regimeName, String code,
                           String name, boolean active) { }

    public record AffiliationView(long planId, String planName, long epsId, String epsName, String regimeCode, String regimeName,
                                  String membershipNumber, LocalDate validFrom) { }

    /** Estado de selección de un plan: debe existir, estar activo y pertenecer a una EPS activa. */
    public record PlanAvailability(long planId, boolean planActive, boolean epsActive) {
        public boolean selectable() { return planActive && epsActive; }
    }
}
