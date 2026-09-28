CREATE TABLE smtp_settings (
  id INT PRIMARY KEY,
  host VARCHAR(255) NOT NULL,
  port INT NOT NULL,
  security_mode VARCHAR(16) NOT NULL,
  username VARCHAR(254) NOT NULL,
  encrypted_password VARCHAR(2048) NOT NULL,
  from_email VARCHAR(254) NOT NULL,
  reply_to VARCHAR(254),
  updated_at TIMESTAMP(6) NOT NULL
);
