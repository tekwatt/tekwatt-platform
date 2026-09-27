CREATE TABLE sms_provider_credentials (
    id BINARY(16) PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    public_identifier VARCHAR(200) NOT NULL,
    sender VARCHAR(100),
    template_id VARCHAR(200),
    message_variable VARCHAR(100),
    encrypted_secret TEXT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_sms_provider_tenant UNIQUE (tenant_id, provider)
);
