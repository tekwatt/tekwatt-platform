ALTER TABLE refresh_tokens ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE refresh_tokens ADD COLUMN last_used_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE refresh_tokens ADD COLUMN revoked_at TIMESTAMP(6) NULL;
ALTER TABLE refresh_tokens ADD COLUMN ip_address VARCHAR(64) NULL;
ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(500) NULL;
CREATE INDEX idx_refresh_tokens_user_created ON refresh_tokens(user_id, created_at);
