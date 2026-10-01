package co.edu.fcv.citas.offer.application.port.out;

import java.util.Set;

/** Sedes fijas activas del laboratorio (HIC, ICV). */
public interface LocationCatalogPort {
    Set<Long> activeIds();
}
