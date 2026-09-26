ALTER TABLE charging_sessions ADD COLUMN active_connector_id BINARY(16) NULL AFTER connector_id;
UPDATE charging_sessions SET active_connector_id = connector_id WHERE status = 'ACTIVE';
ALTER TABLE charging_sessions ADD CONSTRAINT uk_charging_sessions_active_connector UNIQUE (active_connector_id);
