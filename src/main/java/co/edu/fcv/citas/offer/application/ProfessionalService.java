package co.edu.fcv.citas.offer.application;

import co.edu.fcv.citas.offer.application.port.in.ManageProfessionalsUseCase;
import co.edu.fcv.citas.offer.application.port.out.LocationCatalogPort;
import co.edu.fcv.citas.offer.application.port.out.ProfessionalAccountPort;
import co.edu.fcv.citas.offer.application.port.out.ProfessionalRepositoryPort;
import co.edu.fcv.citas.offer.application.port.out.SpecialtyCatalogPort;
import co.edu.fcv.citas.offer.domain.LocationAssignments;
import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.offer.domain.ProfessionalCredentials;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.SpecialtyAssignments;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.util.List;

public class ProfessionalService implements ManageProfessionalsUseCase {
    private final ProfessionalRepositoryPort professionals;
    private final SpecialtyCatalogPort specialties;
    private final LocationCatalogPort locations;
    private final ProfessionalAccountPort accounts;
    private final TransactionPort tx;

    public ProfessionalService(ProfessionalRepositoryPort professionals, SpecialtyCatalogPort specialties, LocationCatalogPort locations,
                               ProfessionalAccountPort accounts, TransactionPort tx) {
        this.professionals = professionals;
        this.specialties = specialties;
        this.locations = locations;
        this.accounts = accounts;
        this.tx = tx;
    }

    @Override
    public List<ProfessionalView> list() {
        return professionals.findAll();
    }

    @Override
    public ProfessionalView get(long id) {
        return professionals.findById(id).orElseThrow(ProfessionalService::notFound);
    }

    /** Identidad PROFESSIONAL y datos profesionales se crean juntos o no se crea nada (HU-015 CA-03). */
    @Override
    public ProfessionalView create(NewProfessional c) {
        ProfessionalCredentials credentials = new ProfessionalCredentials(c.professionalCode(), c.licenseNumber());
        return tx.inTransaction(() -> {
            if (professionals.existsByCodeOrLicense(credentials))
                throw new OfferException.Duplicate("Código profesional o matrícula ya registrados");
            long userId = accounts.createAccount(c.firstName(), c.lastName(), c.documentType(), c.documentNumber(), c.email(),
                    c.phone(), c.initialPassword());
            return get(professionals.create(userId, credentials));
        });
    }

    @Override
    public ProfessionalView setActive(long id, boolean active) {
        return tx.inTransaction(() -> {
            lock(id);
            professionals.setActive(id, active);
            return get(id);
        });
    }

    @Override
    public ProfessionalView assignSpecialties(long id, SpecialtyAssignments assignments) {
        return tx.inTransaction(() -> {
            lock(id);
            if (specialties.countActive(assignments.specialtyIds()) != assignments.specialtyIds().size())
                throw new OfferException.InvalidData("Especialidad inexistente o inactiva");
            professionals.replaceSpecialties(id, assignments);
            return get(id);
        });
    }

    @Override
    public ProfessionalView assignLocations(long id, List<Long> locationIds) {
        LocationAssignments assignments = LocationAssignments.of(locationIds);
        return tx.inTransaction(() -> {
            lock(id);
            if (!locations.activeIds().containsAll(assignments.locationIds()))
                throw new OfferException.InvalidData("Solo se pueden asignar las sedes fijas HIC e ICV");
            professionals.replaceLocations(id, assignments.locationIds());
            return get(id);
        });
    }

    private void lock(long id) {
        if (!professionals.lock(id)) throw notFound();
    }

    private static OfferException notFound() {
        return new OfferException.NotFound("Profesional no existe");
    }
}
