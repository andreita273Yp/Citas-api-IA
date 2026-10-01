CREATE TABLE specialties (
 id VARCHAR(36) PRIMARY KEY, name VARCHAR(120) NOT NULL, duration_minutes INT NOT NULL, active BOOLEAN NOT NULL,
 CONSTRAINT uk_specialty_name UNIQUE(name), CONSTRAINT chk_specialty_duration CHECK (duration_minutes IN (30,60))
);
CREATE TABLE professionals (
 user_id VARCHAR(36) PRIMARY KEY, professional_code VARCHAR(40) NOT NULL UNIQUE, license_number VARCHAR(60) NOT NULL UNIQUE, active BOOLEAN NOT NULL,
 CONSTRAINT fk_professional_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE professional_specialties (
 professional_id VARCHAR(36) NOT NULL, specialty_id VARCHAR(36) NOT NULL, primary_specialty BOOLEAN NOT NULL,
 PRIMARY KEY(professional_id,specialty_id), CONSTRAINT fk_ps_prof FOREIGN KEY(professional_id) REFERENCES professionals(user_id), CONSTRAINT fk_ps_specialty FOREIGN KEY(specialty_id) REFERENCES specialties(id)
);
CREATE TABLE professional_locations (
 professional_id VARCHAR(36) NOT NULL, location_id VARCHAR(20) NOT NULL, PRIMARY KEY(professional_id,location_id),
 CONSTRAINT fk_pl_prof FOREIGN KEY(professional_id) REFERENCES professionals(user_id), CONSTRAINT fk_pl_location FOREIGN KEY(location_id) REFERENCES locations(code)
);
CREATE TABLE availability_blocks (
 id VARCHAR(36) PRIMARY KEY, professional_id VARCHAR(36) NOT NULL, location_id VARCHAR(20) NOT NULL, starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_block_prof FOREIGN KEY(professional_id) REFERENCES professionals(user_id), CONSTRAINT fk_block_location FOREIGN KEY(location_id) REFERENCES locations(code), CONSTRAINT chk_block_range CHECK(ends_at > starts_at)
);
CREATE TABLE appointments (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, professional_id VARCHAR(36) NOT NULL, location_id VARCHAR(20) NOT NULL, specialty_id VARCHAR(36) NOT NULL, starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL, status VARCHAR(30) NOT NULL, reason VARCHAR(500), decision_reason VARCHAR(500), created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_appointment_user FOREIGN KEY(user_id) REFERENCES users(id), CONSTRAINT fk_appointment_prof FOREIGN KEY(professional_id) REFERENCES professionals(user_id), CONSTRAINT fk_appointment_location FOREIGN KEY(location_id) REFERENCES locations(code), CONSTRAINT fk_appointment_specialty FOREIGN KEY(specialty_id) REFERENCES specialties(id)
);
CREATE TABLE availability_slots (
 id VARCHAR(36) PRIMARY KEY, block_id VARCHAR(36) NOT NULL, professional_id VARCHAR(36) NOT NULL, location_id VARCHAR(20) NOT NULL, starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL, appointment_id VARCHAR(36),
 CONSTRAINT uk_slot UNIQUE(professional_id,location_id,starts_at), CONSTRAINT fk_slot_block FOREIGN KEY(block_id) REFERENCES availability_blocks(id), CONSTRAINT fk_slot_appointment FOREIGN KEY(appointment_id) REFERENCES appointments(id)
);
CREATE TABLE appointment_history (
 id VARCHAR(36) PRIMARY KEY, appointment_id VARCHAR(36) NOT NULL, previous_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, actor_id VARCHAR(36), source VARCHAR(30) NOT NULL, reason VARCHAR(500), occurred_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_history_appointment FOREIGN KEY(appointment_id) REFERENCES appointments(id)
);
CREATE INDEX idx_slots_search ON availability_slots(location_id, professional_id, starts_at, appointment_id);
