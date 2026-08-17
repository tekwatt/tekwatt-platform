ALTER TABLE chargers ADD COLUMN station_name VARCHAR(160);
ALTER TABLE chargers ADD COLUMN address VARCHAR(500);
ALTER TABLE chargers ADD COLUMN city VARCHAR(100);
ALTER TABLE chargers ADD COLUMN state VARCHAR(100);
ALTER TABLE chargers ADD COLUMN description TEXT;
ALTER TABLE chargers ADD COLUMN latitude DECIMAL(10,7);
ALTER TABLE chargers ADD COLUMN longitude DECIMAL(10,7);
ALTER TABLE chargers ADD COLUMN station_status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE chargers ADD COLUMN opening_hours VARCHAR(100) DEFAULT '24/7';
ALTER TABLE chargers ADD COLUMN power_kw DECIMAL(10,2);
ALTER TABLE chargers ADD COLUMN price_per_kwh DECIMAL(10,2);
ALTER TABLE chargers ADD COLUMN contact_phone VARCHAR(80);
ALTER TABLE chargers ADD COLUMN contact_email VARCHAR(190);
ALTER TABLE chargers ADD COLUMN firmware_version VARCHAR(80);
ALTER TABLE chargers ADD COLUMN meter_serial_number VARCHAR(100);
ALTER TABLE chargers ADD COLUMN sim_number VARCHAR(80);

UPDATE chargers SET station_name = station_id WHERE station_name IS NULL;
