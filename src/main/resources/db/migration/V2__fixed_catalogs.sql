CREATE TABLE fixed_catalog_entries (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  catalog_type VARCHAR(40) NOT NULL,
  code VARCHAR(40) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  CONSTRAINT uk_fixed_catalog_type_code UNIQUE (catalog_type, code)
);

CREATE TABLE locations (
  code VARCHAR(20) NOT NULL PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  address VARCHAR(255) NOT NULL
);

INSERT INTO fixed_catalog_entries (catalog_type, code, display_name) VALUES
  ('APPOINTMENT_STATUS', 'REQUESTED', 'Solicitada'),
  ('APPOINTMENT_STATUS', 'APPROVED', 'Aprobada'),
  ('APPOINTMENT_STATUS', 'REJECTED', 'Rechazada'),
  ('APPOINTMENT_STATUS', 'CANCELLED', 'Cancelada'),
  ('APPOINTMENT_STATUS', 'COMPLETED', 'Completada'),
  ('APPOINTMENT_STATUS', 'NO_SHOW', 'No asistió'),
  ('RESCHEDULE_STATUS', 'PENDING', 'Pendiente'),
  ('RESCHEDULE_STATUS', 'APPROVED', 'Aprobada'),
  ('RESCHEDULE_STATUS', 'REJECTED', 'Rechazada'),
  ('REGIME', 'CONTRIBUTIVE', 'Contributivo'),
  ('REGIME', 'SUBSIDIZED', 'Subsidiado'),
  ('REGIME', 'SPECIAL', 'Especial');

INSERT INTO locations (code, name, address) VALUES
  ('HIC', 'Hospital Internacional de Colombia (HIC)', 'Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander'),
  ('ICV', 'Fundación Cardiovascular de Colombia / Instituto Cardiovascular (ICV)', 'Calle 155A No. 23-58, Urbanización El Bosque, Floridablanca, Santander');
