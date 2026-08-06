CREATE TABLE user_profiles (
  id BINARY(16) PRIMARY KEY, auth_user_id BINARY(16) NOT NULL UNIQUE, tenant_id BINARY(16) NOT NULL,
  first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
  phone VARCHAR(32), status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_user_profiles_tenant_id ON user_profiles(tenant_id);
