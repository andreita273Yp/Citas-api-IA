package co.edu.fcv.citas.offer.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

// Entidades JPA de la oferta de atención (modelo de referencia V6); package-private al adaptador.

@Entity
@Table(name = "specialties")
class SpecialtyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true) String code;
    @Column(nullable = false, unique = true) String name;
    @Column(name = "appointment_duration_minutes", nullable = false) int durationMinutes;
    @Column(name = "is_general", nullable = false) boolean general;
    @Column(name = "requires_admin_approval", nullable = false) boolean requiresAdminApproval;
    @Column(nullable = false) boolean active;
}

@Entity
@Table(name = "professionals")
class ProfessionalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false, unique = true) Long userId;
    @Column(name = "professional_code", nullable = false, unique = true) String professionalCode;
    @Column(name = "license_number", nullable = false, unique = true) String licenseNumber;
    @Column(nullable = false) boolean active = true;
}

@Entity
@Table(name = "professional_specialties")
class ProfessionalSpecialtyEntity {
    @EmbeddedId PairId id;
    @Column(name = "is_primary", nullable = false) boolean primary;
    @Column(nullable = false) boolean active;

    protected ProfessionalSpecialtyEntity() { }

    ProfessionalSpecialtyEntity(long professionalId, long specialtyId) {
        this.id = new PairId(professionalId, specialtyId);
    }

    @Embeddable
    static class PairId implements Serializable {
        @Column(name = "professional_id") Long professionalId;
        @Column(name = "specialty_id") Long specialtyId;

        protected PairId() { }

        PairId(Long professionalId, Long specialtyId) {
            this.professionalId = professionalId;
            this.specialtyId = specialtyId;
        }

        @Override public boolean equals(Object o) {
            return o instanceof PairId p && Objects.equals(professionalId, p.professionalId) && Objects.equals(specialtyId, p.specialtyId);
        }

        @Override public int hashCode() { return Objects.hash(professionalId, specialtyId); }
    }
}

@Entity
@Table(name = "professional_locations")
class ProfessionalLocationEntity {
    @EmbeddedId PairId id;
    @Column(nullable = false) boolean active;

    protected ProfessionalLocationEntity() { }

    ProfessionalLocationEntity(long professionalId, long locationId) {
        this.id = new PairId(professionalId, locationId);
    }

    @Embeddable
    static class PairId implements Serializable {
        @Column(name = "professional_id") Long professionalId;
        @Column(name = "location_id") Long locationId;

        protected PairId() { }

        PairId(Long professionalId, Long locationId) {
            this.professionalId = professionalId;
            this.locationId = locationId;
        }

        @Override public boolean equals(Object o) {
            return o instanceof PairId p && Objects.equals(professionalId, p.professionalId) && Objects.equals(locationId, p.locationId);
        }

        @Override public int hashCode() { return Objects.hash(professionalId, locationId); }
    }
}
