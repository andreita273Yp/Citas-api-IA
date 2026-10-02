package co.edu.fcv.citas.offer.application;

import co.edu.fcv.citas.offer.application.port.in.ManageSpecialtiesUseCase;
import co.edu.fcv.citas.offer.application.port.out.SpecialtyCatalogPort;
import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.offer.domain.Specialty;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.util.List;

public class SpecialtyService implements ManageSpecialtiesUseCase {
    private final SpecialtyCatalogPort specialties;
    private final TransactionPort tx;

    public SpecialtyService(SpecialtyCatalogPort specialties, TransactionPort tx) {
        this.specialties = specialties;
        this.tx = tx;
    }

    @Override
    public List<Specialty> list() {
        return specialties.findAll();
    }

    @Override
    public Specialty create(String code, String name, Integer durationMinutes) {
        Specialty draft = Specialty.newSpecialized(code, name, durationMinutes);
        return tx.inTransaction(() -> {
            if (specialties.existsByCodeOrName(draft.code(), draft.name(), null))
                throw new OfferException.Duplicate("Ya existe una especialidad con ese código o nombre");
            return specialties.save(draft);
        });
    }

    @Override
    public Specialty update(long id, String name, Integer durationMinutes, Boolean active) {
        return tx.inTransaction(() -> {
            Specialty current = specialties.findById(id).orElseThrow(() -> new OfferException.NotFound("Especialidad no existe"));
            Specialty updated = current.update(name, durationMinutes, active);
            if (specialties.existsByCodeOrName(updated.code(), updated.name(), id))
                throw new OfferException.Duplicate("Ya existe una especialidad con ese nombre");
            return specialties.save(updated);
        });
    }
}
