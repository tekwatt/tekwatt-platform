CREATE TABLE chargers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    organization_id UUID,
    station_id VARCHAR(100) NOT NULL UNIQUE,
    serial_number VARCHAR(100) NOT NULL,
    vendor VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    protocol_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_chargers_tenant_id ON chargers (tenant_id);
CREATE INDEX idx_chargers_organization_id ON chargers (organization_id);
