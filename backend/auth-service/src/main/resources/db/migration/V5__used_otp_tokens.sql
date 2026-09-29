CREATE TABLE used_otp_tokens (
  token_hash VARCHAR(64) PRIMARY KEY,
  used_at TIMESTAMP(6) NOT NULL
);
