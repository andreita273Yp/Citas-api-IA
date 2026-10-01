package co.edu.fcv.citas.offer.application.port.out;

import co.edu.fcv.citas.offer.domain.Specialty;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistencia del catálogo de especialidades. */
public interface SpecialtyCatalogPort {
    List<Specialty> findAll();

    Optional<Specialty> findById(long id);

    /** Comparación sin distinguir mayúsculas; {@code excludeId} omite la propia especialidad al editar. */
    boolean existsByCodeOrName(String code, String name, Long excludeId);

    Specialty save(Specialty specialty);

    /** Cuántos de los ids existen y están activos. */
    long countActive(Collection<Long> ids);
}
