package co.edu.fcv.citas.offer.adapter.out.persistence;

import co.edu.fcv.citas.offer.application.port.out.ProfessionalRepositoryPort;
import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.offer.domain.ProfessionalCredentials;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.SpecialtyAssignments;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Escrituras con Spring Data JPA; las lecturas con joins las resuelve {@link ProfessionalReadModel}. */
@Component
class ProfessionalJpaAdapter implements ProfessionalRepositoryPort {
    private final ProfessionalJpaRepository professionals;
    private final ProfessionalSpecialtyJpaRepository specialties;
    private final ProfessionalLocationJpaRepository locations;
    private final ProfessionalReadModel reads;

    ProfessionalJpaAdapter(ProfessionalJpaRepository professionals, ProfessionalSpecialtyJpaRepository specialties,
                           ProfessionalLocationJpaRepository locations, ProfessionalReadModel reads) {
        this.professionals = professionals;
        this.specialties = specialties;
        this.locations = locations;
        this.reads = reads;
    }

    @Override
    public List<ProfessionalView> findAll() {
        return reads.findAll();
    }

    @Override
    public Optional<ProfessionalView> findById(long id) {
        return reads.findById(id);
    }

    @Override
    public boolean existsByCodeOrLicense(ProfessionalCredentials c) {
        return professionals.existsByProfessionalCodeOrLicenseNumber(c.professionalCode(), c.licenseNumber());
    }

    @Override
    public long create(long userId, ProfessionalCredentials c) {
        ProfessionalEntity e = new ProfessionalEntity();
        e.userId = userId;
        e.professionalCode = c.professionalCode();
        e.licenseNumber = c.licenseNumber();
        try {
            return professionals.saveAndFlush(e).id;
        } catch (DataIntegrityViolationException ex) {
            throw new OfferException.Duplicate("Código profesional o matrícula ya registrados");
        }
    }

    @Override
    public boolean lock(long id) {
        return professionals.lockById(id).isPresent();
    }

    @Override
    public void setActive(long id, boolean active) {
        professionals.findById(id).ifPresent(p -> {
            p.active = active;
            professionals.flush();
        });
    }

    @Override
    public void replaceSpecialties(long id, SpecialtyAssignments assignments) {
        Map<Long, ProfessionalSpecialtyEntity> existing = specialties.findByIdProfessionalId(id).stream()
                .collect(Collectors.toMap(e -> e.id.specialtyId, Function.identity()));
        existing.values().forEach(e -> {
            e.active = false;
            e.primary = false;
        });
        for (SpecialtyAssignments.Assignment a : assignments.assignments()) {
            ProfessionalSpecialtyEntity e = existing.computeIfAbsent(a.specialtyId(), sid -> new ProfessionalSpecialtyEntity(id, sid));
            e.active = true;
            e.primary = a.primary();
        }
        specialties.saveAllAndFlush(existing.values());
    }

    @Override
    public void replaceLocations(long id, Set<Long> locationIds) {
        Map<Long, ProfessionalLocationEntity> existing = locations.findByIdProfessionalId(id).stream()
                .collect(Collectors.toMap(e -> e.id.locationId, Function.identity()));
        existing.values().forEach(e -> e.active = false);
        for (Long locationId : locationIds) existing.computeIfAbsent(locationId, lid -> new ProfessionalLocationEntity(id, lid)).active = true;
        locations.saveAllAndFlush(existing.values());
    }
}
