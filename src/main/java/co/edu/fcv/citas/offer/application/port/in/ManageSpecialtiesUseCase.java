package co.edu.fcv.citas.offer.application.port.in;

import co.edu.fcv.citas.offer.domain.Specialty;
import java.util.List;

/** HU-014 · El ADMIN consulta, crea y edita especialidades; no hay borrado físico (se desactivan). */
public interface ManageSpecialtiesUseCase {
    List<Specialty> list();

    Specialty create(String code, String name, Integer durationMinutes);

    /** Campos nulos se conservan. */
    Specialty update(long id, String name, Integer durationMinutes, Boolean active);
}
