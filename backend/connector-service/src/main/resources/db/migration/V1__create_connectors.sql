CREATE TABLE connectors (
  id BINARY(16) PRIMARY KEY, tenant_id BINARY(16) NOT NULL, charger_id BINARY(16) NOT NULL,
  connector_number INTEGER NOT NULL CHECK (connector_number > 0), type VARCHAR(20) NOT NULL,
  max_power_kw DECIMAL(10,2) NOT NULL CHECK (max_power_kw > 0), max_voltage INTEGER NOT NULL CHECK (max_voltage > 0),
  max_current INTEGER NOT NULL CHECK (max_current > 0), status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT uk_connector_charger_number UNIQUE (charger_id, connector_number)
);
CREATE INDEX idx_connectors_charger_id ON connectors (charger_id);
CREATE INDEX idx_connectors_tenant_id ON connectors (tenant_id);
