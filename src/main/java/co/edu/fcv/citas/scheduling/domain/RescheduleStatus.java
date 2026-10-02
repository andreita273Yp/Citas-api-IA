package co.edu.fcv.citas.scheduling.domain;

/** Estados de una solicitud de reprogramación (catálogo `reschedule_request_statuses`). Solo PENDING se decide. */
public enum RescheduleStatus {
    PENDING, APPROVED, REJECTED, CANCELLED;

    /** Lo que el USER decide tras un rechazo (RF-15): conservar la cita o cancelarla. */
    public enum PatientAction { KEEP_APPOINTMENT, CANCEL_APPOINTMENT }
}
