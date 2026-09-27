-- Existing sessions are not back-notified. New start/stop transitions enqueue SMS durably.
ALTER TABLE charging_sessions ADD COLUMN started_sms_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charging_sessions ADD COLUMN started_sms_retry_at TIMESTAMP(6) NULL;
ALTER TABLE charging_sessions ADD COLUMN stopped_sms_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charging_sessions ADD COLUMN stopped_sms_retry_at TIMESTAMP(6) NULL;
CREATE INDEX idx_session_started_sms_queue ON charging_sessions (started_sms_pending, started_sms_retry_at);
CREATE INDEX idx_session_stopped_sms_queue ON charging_sessions (stopped_sms_pending, stopped_sms_retry_at);
