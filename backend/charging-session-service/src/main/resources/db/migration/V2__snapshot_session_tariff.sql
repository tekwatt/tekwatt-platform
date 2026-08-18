ALTER TABLE charging_sessions ADD COLUMN tariff_id BINARY(16) NULL AFTER connector_id;
ALTER TABLE charging_sessions ADD COLUMN time_price_per_minute DECIMAL(10,4) NOT NULL DEFAULT 0 AFTER price_per_kwh;
ALTER TABLE charging_sessions ADD COLUMN session_fee DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER time_price_per_minute;
ALTER TABLE charging_sessions ADD COLUMN tax_percent DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER session_fee;
