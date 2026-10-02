package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Entidades JPA de agenda y citas (modelo de referencia V6); package-private al adaptador.

@Entity
@Table(name = "availability_blocks")
class AvailabilityBlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "professional_id", nullable = false) Long professionalId;
    @Column(name = "location_id", nullable = false) Long locationId;
    @Column(name = "available_date", nullable = false) LocalDate date;
    @Column(name = "start_time", nullable = false) LocalTime startTime;
    @Column(name = "end_time", nullable = false) LocalTime endTime;
    @Column(nullable = false) boolean active = true;
}

@Entity
@Table(name = "professional_slots")
class ProfessionalSlotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "availability_block_id", nullable = false) Long blockId;
    @Column(name = "start_at", nullable = false) LocalDateTime startAt;
    @Column(name = "end_at", nullable = false) LocalDateTime endAt;
    @Column(name = "appointment_id") Long appointmentId;

    protected ProfessionalSlotEntity() { }

    ProfessionalSlotEntity(Long blockId, LocalDateTime startAt, LocalDateTime endAt) {
        this.blockId = blockId;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}

@Entity
@Table(name = "reschedule_requests")
class RescheduleRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "appointment_id", nullable = false) Long appointmentId;
    @Column(name = "requested_by_user_id", nullable = false) Long requestedByUserId;
    @Column(name = "requested_location_id", nullable = false) Long locationId;
    @Column(name = "status_id", nullable = false) Long statusId;
    @Column(name = "previous_start_at", nullable = false) LocalDateTime previousStart;
    @Column(name = "previous_end_at", nullable = false) LocalDateTime previousEnd;
    @Column(name = "requested_start_at", nullable = false) LocalDateTime start;
    @Column(name = "requested_end_at", nullable = false) LocalDateTime end;
    @Column(name = "decision_reason") String decisionReason;
    @Column(name = "decided_by_user_id") Long decidedByUserId;
    @Column(name = "decided_at") LocalDateTime decidedAt;
    @Column(name = "patient_action_after_rejection") String patientAction;
}

@Entity
@Table(name = "appointments")
class AppointmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "patient_user_id", nullable = false) Long patientUserId;
    @Column(name = "professional_id", nullable = false) Long professionalId;
    @Column(name = "location_id", nullable = false) Long locationId;
    @Column(name = "specialty_id", nullable = false) Long specialtyId;
    @Column(name = "insurance_affiliation_id") Long insuranceAffiliationId;
    @Column(name = "status_id", nullable = false) Long statusId;
    String reason;
    @Column(name = "scheduled_start_at", nullable = false) LocalDateTime start;
    @Column(name = "scheduled_end_at", nullable = false) LocalDateTime end;
    @Column(name = "created_by_user_id", nullable = false) Long createdByUserId;
    @Column(name = "approved_by_user_id") Long approvedByUserId;
    @Column(name = "approved_at") LocalDateTime approvedAt;
}
