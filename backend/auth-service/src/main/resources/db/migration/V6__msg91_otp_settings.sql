CREATE TABLE msg91_otp_settings (
  id INT PRIMARY KEY,
  widget_id VARCHAR(128) NOT NULL,
  encrypted_widget_token VARCHAR(8192) NOT NULL,
  encrypted_server_auth_key VARCHAR(4096) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL
);
