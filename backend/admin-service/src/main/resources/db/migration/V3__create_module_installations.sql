CREATE TABLE module_installations(
 id BINARY(16) PRIMARY KEY,
 tenant_id BINARY(16) NOT NULL,
 module_key VARCHAR(80) NOT NULL,
 installed_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_module_tenant_key UNIQUE(tenant_id,module_key)
);
