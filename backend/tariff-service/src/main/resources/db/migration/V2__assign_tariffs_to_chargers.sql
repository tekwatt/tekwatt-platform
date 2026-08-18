CREATE TABLE tariff_assignments (
  id BINARY(16) PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  tariff_id BINARY(16) NOT NULL,
  charger_id BINARY(16) NOT NULL,
  assigned_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_tariff_assignment_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs(id),
  CONSTRAINT uk_tariff_assignment_charger UNIQUE (tenant_id, charger_id)
);
CREATE INDEX idx_tariff_assignments_tariff ON tariff_assignments(tariff_id);
