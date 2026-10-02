package co.edu.fcv.citas.offer.application.port.out;

import co.edu.fcv.citas.offer.domain.ProfessionalCredentials;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.SpecialtyAssignments;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Persistencia de profesionales y sus relaciones N:M con especialidades y sedes. */
public interface ProfessionalRepositoryPort {
    List<ProfessionalView> findAll();

    Optional<ProfessionalView> findById(long id);

    boolean existsByCodeOrLicense(ProfessionalCredentials credentials);

    /** Lanza {@code OfferException.Duplicate} si la base detecta unicidad violada. */
    long create(long userId, ProfessionalCredentials credentials);

    /** Bloquea la fila del profesional para serializar cambios de asignación; {@code false} si no existe. */
    boolean lock(long id);

    void setActive(long id, boolean active);

    /** Las asociaciones que salen quedan inactivas (conservan historial); las demás se activan o crean. */
    void replaceSpecialties(long id, SpecialtyAssignments assignments);

    void replaceLocations(long id, Set<Long> locationIds);
}
