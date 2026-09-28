CREATE TABLE password_reset_codes (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL UNIQUE,
  code_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  requested_at TIMESTAMP(6) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);
