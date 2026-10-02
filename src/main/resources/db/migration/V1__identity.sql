CREATE TABLE users (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  document_type VARCHAR(20) NOT NULL,
  document_number VARCHAR(50) NOT NULL,
  email VARCHAR(254) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT uk_users_document UNIQUE (document_type, document_number)
);
CREATE TABLE roles (name VARCHAR(30) NOT NULL PRIMARY KEY);
CREATE TABLE user_roles (
  user_id VARCHAR(36) NOT NULL,
  role_name VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role_name),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_name) REFERENCES roles(name)
);
CREATE TABLE refresh_sessions (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  jti_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_refresh_jti_hash UNIQUE (jti_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_refresh_user_revoked (user_id, revoked_at)
);
INSERT INTO roles(name) VALUES ('USER'), ('PROFESSIONAL'), ('ADMIN');
