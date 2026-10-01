CREATE TABLE eps (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL,
  CONSTRAINT uk_eps_name UNIQUE(name)
);
CREATE TABLE eps_plans (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  eps_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL,
  CONSTRAINT uk_eps_plan_name UNIQUE(eps_id, name),
  CONSTRAINT fk_eps_plan_eps FOREIGN KEY(eps_id) REFERENCES eps(id)
);
CREATE TABLE affiliations (
  user_id VARCHAR(36) NOT NULL PRIMARY KEY,
  insurance_plan_id BIGINT NOT NULL,
  regime_code VARCHAR(40) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_affiliation_user FOREIGN KEY(user_id) REFERENCES users(id),
  CONSTRAINT fk_affiliation_plan FOREIGN KEY(insurance_plan_id) REFERENCES eps_plans(id)
);
CREATE TABLE password_reset_tokens (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_password_reset_hash UNIQUE(token_hash),
  CONSTRAINT fk_password_reset_user FOREIGN KEY(user_id) REFERENCES users(id),
  INDEX idx_password_reset_user_active(user_id, consumed_at, expires_at)
);
