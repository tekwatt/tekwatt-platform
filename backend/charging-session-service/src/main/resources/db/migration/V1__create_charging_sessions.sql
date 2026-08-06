CREATE TABLE charging_sessions (
  id BINARY(16) PRIMARY KEY, tenant_id BINARY(16) NOT NULL, user_id BINARY(16) NOT NULL, charger_id BINARY(16) NOT NULL,
  connector_id BINARY(16) NOT NULL, transaction_id VARCHAR(100) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL,
  meter_start_wh DECIMAL(14,3) NOT NULL, meter_stop_wh DECIMAL(14,3), energy_kwh DECIMAL(12,3) NOT NULL,
  price_per_kwh DECIMAL(10,4) NOT NULL, total_cost DECIMAL(12,2) NOT NULL, currency VARCHAR(3) NOT NULL,
  started_at TIMESTAMP(6) NOT NULL, stopped_at TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_sessions_tenant_started ON charging_sessions (tenant_id, started_at);
CREATE INDEX idx_sessions_charger ON charging_sessions (charger_id);
CREATE TABLE meter_readings (
  id BINARY(16) PRIMARY KEY, session_id BINARY(16) NOT NULL, meter_wh DECIMAL(14,3) NOT NULL,
  recorded_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_meter_readings_session FOREIGN KEY (session_id) REFERENCES charging_sessions(id)
);
CREATE INDEX idx_meter_readings_session_time ON meter_readings (session_id, recorded_at);
