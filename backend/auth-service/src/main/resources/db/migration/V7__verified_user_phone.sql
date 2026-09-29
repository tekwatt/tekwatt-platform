ALTER TABLE users ADD COLUMN verified_phone VARCHAR(16) NULL;
ALTER TABLE users ADD COLUMN phone_verified_at TIMESTAMP(6) NULL;
CREATE UNIQUE INDEX uk_users_verified_phone ON users(verified_phone);
