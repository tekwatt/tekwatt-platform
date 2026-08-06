CREATE TABLE analytics_events (
  id BINARY(16) PRIMARY KEY,
  external_event_id VARCHAR(100) NOT NULL UNIQUE,
  tenant_id BINARY(16) NOT NULL,
  charger_id BINARY(16) NOT NULL,
  session_id BINARY(16) NOT NULL,
  energy_kwh DECIMAL(14,4) NOT NULL,
  revenue DECIMAL(14,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  duration_seconds BIGINT NOT NULL,
  occurred_at TIMESTAMP(6) NOT NULL,
  recorded_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_analytics_tenant_time ON analytics_events(tenant_id, occurred_at);
CREATE INDEX idx_analytics_charger_time ON analytics_events(charger_id, occurred_at);
