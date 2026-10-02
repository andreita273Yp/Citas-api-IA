package co.edu.fcv.citas.offer.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** HU-016: una o más especialidades distintas, con exactamente una primaria. */
public record SpecialtyAssignments(List<Assignment> assignments) {
    public SpecialtyAssignments {
        if (assignments == null || assignments.isEmpty()) throw new OfferException.InvalidData("Asigne al menos una especialidad");
        Set<Long> seen = new HashSet<>();
        for (Assignment a : assignments) {
            if (a == null || a.specialtyId() == null) throw new OfferException.InvalidData("Especialidad inválida");
            if (!seen.add(a.specialtyId())) throw new OfferException.InvalidData("Especialidad repetida");
        }
        if (assignments.stream().filter(Assignment::primary).count() != 1)
            throw new OfferException.InvalidData("Debe haber exactamente una especialidad primaria");
        assignments = List.copyOf(assignments);
    }

    public Set<Long> specialtyIds() {
        Set<Long> ids = new HashSet<>();
        assignments.forEach(a -> ids.add(a.specialtyId()));
        return ids;
    }

    public record Assignment(Long specialtyId, boolean primary) { }
}
