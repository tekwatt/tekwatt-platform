CREATE TABLE chargers (
  id BINARY(16) PRIMARY KEY, tenant_id BINARY(16) NOT NULL, organization_id BINARY(16),
  station_id VARCHAR(100) NOT NULL UNIQUE, serial_number VARCHAR(100) NOT NULL, vendor VARCHAR(100) NOT NULL,
  model VARCHAR(100) NOT NULL, protocol_version VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
  last_heartbeat TIMESTAMP(6), created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_chargers_tenant_id ON chargers (tenant_id);
CREATE INDEX idx_chargers_organization_id ON chargers (organization_id);
