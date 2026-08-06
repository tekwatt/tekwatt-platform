CREATE TABLE charging_sessions (
    id UUID PRIMARY KEY, tenant_id UUID NOT NULL, user_id UUID NOT NULL, charger_id UUID NOT NULL, connector_id UUID NOT NULL,
    transaction_id VARCHAR(100) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL,
    meter_start_wh NUMERIC(14,3) NOT NULL, meter_stop_wh NUMERIC(14,3), energy_kwh NUMERIC(12,3) NOT NULL,
    price_per_kwh NUMERIC(10,4) NOT NULL, total_cost NUMERIC(12,2) NOT NULL, currency VARCHAR(3) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL, stopped_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_sessions_tenant_started ON charging_sessions (tenant_id, started_at);
CREATE INDEX idx_sessions_charger ON charging_sessions (charger_id);
CREATE TABLE meter_readings (
    id UUID PRIMARY KEY, session_id UUID NOT NULL REFERENCES charging_sessions(id), meter_wh NUMERIC(14,3) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_meter_readings_session_time ON meter_readings (session_id, recorded_at);
