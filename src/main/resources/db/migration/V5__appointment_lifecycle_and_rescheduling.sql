CREATE TABLE reschedule_requests (
 id VARCHAR(36) PRIMARY KEY,
 appointment_id VARCHAR(36) NOT NULL,
 proposed_location_id VARCHAR(20) NOT NULL,
 proposed_start_at DATETIME(6) NOT NULL,
 proposed_end_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL,
 reason VARCHAR(500),
 decision_reason VARCHAR(500),
 active_marker TINYINT,
 created_at DATETIME(6) NOT NULL,
 decided_at DATETIME(6),
 CONSTRAINT fk_reschedule_appointment FOREIGN KEY(appointment_id) REFERENCES appointments(id),
 CONSTRAINT fk_reschedule_location FOREIGN KEY(proposed_location_id) REFERENCES locations(code),
 CONSTRAINT uk_reschedule_pending UNIQUE(appointment_id, active_marker),
 CONSTRAINT chk_reschedule_status CHECK(status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE TABLE reschedule_slot_holds (
 request_id VARCHAR(36) NOT NULL,
 slot_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(request_id, slot_id),
 CONSTRAINT uk_reschedule_hold_slot UNIQUE(slot_id),
 CONSTRAINT fk_reschedule_hold_request FOREIGN KEY(request_id) REFERENCES reschedule_requests(id),
 CONSTRAINT fk_reschedule_hold_slot FOREIGN KEY(slot_id) REFERENCES availability_slots(id)
);

CREATE INDEX idx_appointments_user_schedule ON appointments(user_id, starts_at, status);
CREATE INDEX idx_reschedule_requests_appointment ON reschedule_requests(appointment_id, status);
