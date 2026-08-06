CREATE TABLE organizations (
  id BINARY(16) PRIMARY KEY, tenant_id BINARY(16) NOT NULL, parent_id BINARY(16), name VARCHAR(150) NOT NULL,
  code VARCHAR(60) NOT NULL, status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_organizations_tenant_code UNIQUE (tenant_id, code),
  CONSTRAINT fk_organizations_parent FOREIGN KEY (parent_id) REFERENCES organizations(id)
);
CREATE INDEX idx_organizations_tenant_id ON organizations(tenant_id);
CREATE INDEX idx_organizations_parent_id ON organizations(parent_id);
