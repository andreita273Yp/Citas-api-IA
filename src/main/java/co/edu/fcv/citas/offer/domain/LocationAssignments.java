package co.edu.fcv.citas.offer.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** HU-017: una o ambas sedes fijas, sin repetidos. La existencia se valida contra el catálogo. */
public record LocationAssignments(Set<Long> locationIds) {
    public static LocationAssignments of(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new OfferException.InvalidData("Asigne al menos una sede");
        Set<Long> unique = new HashSet<>();
        for (Long id : ids) {
            if (id == null) throw new OfferException.InvalidData("Sede inválida");
            if (!unique.add(id)) throw new OfferException.InvalidData("Sede repetida");
        }
        return new LocationAssignments(Set.copyOf(unique));
    }
}
