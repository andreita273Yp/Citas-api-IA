package co.edu.fcv.citas.offer.adapter.out.persistence;

import co.edu.fcv.citas.offer.application.port.out.SpecialtyCatalogPort;
import co.edu.fcv.citas.offer.domain.AppointmentDuration;
import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.offer.domain.Specialty;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class SpecialtyCatalogJpaAdapter implements SpecialtyCatalogPort {
    private final SpecialtyJpaRepository specialties;

    SpecialtyCatalogJpaAdapter(SpecialtyJpaRepository specialties) {
        this.specialties = specialties;
    }

    @Override
    public List<Specialty> findAll() {
        return specialties.findAllByOrderByNameAsc().stream().map(SpecialtyCatalogJpaAdapter::toDomain).toList();
    }

    @Override
    public Optional<Specialty> findById(long id) {
        return specialties.findById(id).map(SpecialtyCatalogJpaAdapter::toDomain);
    }

    @Override
    public boolean existsByCodeOrName(String code, String name, Long excludeId) {
        return specialties.existsByCodeOrName(code, name, excludeId);
    }

    @Override
    public Specialty save(Specialty s) {
        SpecialtyEntity e = s.id() == null ? new SpecialtyEntity() : specialties.findById(s.id()).orElseThrow();
        e.code = s.code();
        e.name = s.name();
        e.durationMinutes = s.duration().minutes();
        e.general = s.general();
        e.requiresAdminApproval = s.requiresAdminApproval();
        e.active = s.active();
        try {
            return toDomain(specialties.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw new OfferException.Duplicate("Ya existe una especialidad con ese código o nombre");
        }
    }

    @Override
    public long countActive(Collection<Long> ids) {
        return specialties.countByIdInAndActiveTrue(ids);
    }

    private static Specialty toDomain(SpecialtyEntity e) {
        return new Specialty(e.id, e.code, e.name, new AppointmentDuration(e.durationMinutes), e.general, e.active);
    }
}
