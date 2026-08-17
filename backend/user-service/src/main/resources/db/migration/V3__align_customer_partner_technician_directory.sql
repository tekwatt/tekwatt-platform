ALTER TABLE user_profiles ADD COLUMN full_name VARCHAR(200) NULL;
ALTER TABLE user_profiles ADD COLUMN city VARCHAR(100) NULL;
ALTER TABLE user_profiles ADD COLUMN zipcode VARCHAR(20) NULL;

UPDATE user_profiles
SET full_name = TRIM(CONCAT(first_name, ' ', last_name))
WHERE full_name IS NULL;

ALTER TABLE partners ADD COLUMN partner_unique_id VARCHAR(80) NOT NULL DEFAULT 'PENDING';
ALTER TABLE partners ADD COLUMN address VARCHAR(500) NULL;
ALTER TABLE partners ADD COLUMN app_login_email VARCHAR(190) NULL;
ALTER TABLE partners ADD COLUMN auth_user_id BINARY(16) NULL;

UPDATE partners
SET partner_unique_id = CONCAT('TW-PART-', UPPER(REPLACE(SUBSTRING(company_name, 1, 12), ' ', '')))
WHERE partner_unique_id = 'PENDING';

ALTER TABLE partners ADD CONSTRAINT uk_partner_auth_user UNIQUE (auth_user_id);

ALTER TABLE technicians ADD COLUMN auth_user_id BINARY(16) NULL;
ALTER TABLE technicians ADD CONSTRAINT uk_technician_auth_user UNIQUE (auth_user_id);
