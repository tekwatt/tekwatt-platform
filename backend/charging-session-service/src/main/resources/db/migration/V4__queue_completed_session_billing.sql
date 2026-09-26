-- Existing sessions are not automatically back-billed. Only new stop events enqueue work.
ALTER TABLE charging_sessions ADD COLUMN billing_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charging_sessions ADD COLUMN billing_retry_at TIMESTAMP(6) NULL;
CREATE INDEX idx_session_billing_queue ON charging_sessions (billing_pending, billing_retry_at);
