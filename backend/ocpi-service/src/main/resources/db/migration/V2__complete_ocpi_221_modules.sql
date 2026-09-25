ALTER TABLE ocpi_partner_tokens
  ADD COLUMN partner_country_code CHAR(2),
  ADD COLUMN partner_party_id CHAR(3),
  ADD COLUMN versions_url VARCHAR(500),
  ADD COLUMN outbound_token VARCHAR(512),
  ADD COLUMN issued_token VARCHAR(512),
  ADD COLUMN roles_json TEXT,
  ADD COLUMN connection_status VARCHAR(20) NOT NULL DEFAULT 'BOOTSTRAP',
  ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  ADD UNIQUE KEY uk_ocpi_partner_identity (tenant_id, partner_country_code, partner_party_id);

CREATE TABLE ocpi_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id VARCHAR(36) NOT NULL,
  country_code CHAR(2) NOT NULL,
  party_id CHAR(3) NOT NULL,
  token_uid VARCHAR(36) NOT NULL,
  token_type VARCHAR(20) NOT NULL,
  contract_id VARCHAR(36) NOT NULL,
  local_user_id VARCHAR(36),
  valid BOOLEAN NOT NULL,
  whitelist VARCHAR(20) NOT NULL,
  raw_json LONGTEXT NOT NULL,
  last_updated TIMESTAMP(6) NOT NULL,
  UNIQUE KEY uk_ocpi_token (tenant_id, country_code, party_id, token_uid, token_type),
  KEY ix_ocpi_token_uid (tenant_id, token_uid, token_type),
  KEY ix_ocpi_token_user (tenant_id, local_user_id),
  CONSTRAINT fk_ocpi_token_party FOREIGN KEY (tenant_id) REFERENCES ocpi_parties (tenant_id)
);

CREATE TABLE ocpi_charging_preferences (
  tenant_id VARCHAR(36) NOT NULL,
  session_id VARCHAR(36) NOT NULL,
  preferences_json LONGTEXT NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (tenant_id, session_id),
  CONSTRAINT fk_ocpi_preferences_party FOREIGN KEY (tenant_id) REFERENCES ocpi_parties (tenant_id)
);

CREATE TABLE ocpi_cdrs (
  tenant_id VARCHAR(36) NOT NULL,
  cdr_id VARCHAR(39) NOT NULL,
  session_id VARCHAR(36),
  cdr_json LONGTEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  last_updated TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (tenant_id, cdr_id),
  UNIQUE KEY uk_ocpi_cdr_session (tenant_id, session_id),
  CONSTRAINT fk_ocpi_cdr_party FOREIGN KEY (tenant_id) REFERENCES ocpi_parties (tenant_id)
);

CREATE TABLE ocpi_commands (
  id VARCHAR(36) PRIMARY KEY,
  tenant_id VARCHAR(36) NOT NULL,
  command_type VARCHAR(30) NOT NULL,
  response_url VARCHAR(1000) NOT NULL,
  request_json LONGTEXT NOT NULL,
  result VARCHAR(30) NOT NULL,
  result_message VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL,
  completed_at TIMESTAMP(6),
  callback_sent_at TIMESTAMP(6),
  KEY ix_ocpi_command_tenant_created (tenant_id, created_at),
  CONSTRAINT fk_ocpi_command_party FOREIGN KEY (tenant_id) REFERENCES ocpi_parties (tenant_id)
);
