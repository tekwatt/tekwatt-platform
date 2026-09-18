CREATE TABLE ocpi_parties (
  tenant_id VARCHAR(36) PRIMARY KEY,
  country_code CHAR(2) NOT NULL,
  party_id CHAR(3) NOT NULL,
  business_name VARCHAR(100) NOT NULL,
  country CHAR(3) NOT NULL,
  time_zone VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_ocpi_party (country_code, party_id)
);

CREATE TABLE ocpi_partner_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id VARCHAR(36) NOT NULL,
  partner_name VARCHAR(100) NOT NULL,
  token_sha256 CHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_ocpi_partner_token (token_sha256),
  KEY ix_ocpi_partner_tenant (tenant_id),
  CONSTRAINT fk_ocpi_partner_party FOREIGN KEY (tenant_id) REFERENCES ocpi_parties (tenant_id)
);
