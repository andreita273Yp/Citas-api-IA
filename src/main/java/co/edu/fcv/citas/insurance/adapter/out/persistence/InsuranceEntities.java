package co.edu.fcv.citas.insurance.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

// Entidades JPA de aseguramiento (modelo de referencia V6); package-private al adaptador.

@Entity
@Table(name = "eps")
class EpsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true) String code;
    @Column(nullable = false) String name;
    @Column(nullable = false) boolean active = true;
}

@Entity
@Table(name = "eps_plans")
class EpsPlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "eps_id", nullable = false) Long epsId;
    @Column(name = "regime_id", nullable = false) Long regimeId;
    @Column(nullable = false) String code;
    @Column(nullable = false) String name;
    @Column(nullable = false) boolean active = true;
}

@Entity
@Table(name = "user_insurance_affiliations")
class AffiliationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false) Long userId;
    @Column(name = "plan_id", nullable = false) Long planId;
    @Column(name = "membership_number", nullable = false) String membershipNumber;
    @Column(name = "is_current", nullable = false) boolean current;
    @Column(name = "valid_from") LocalDate validFrom;
    @Column(name = "valid_to") LocalDate validTo;
}
