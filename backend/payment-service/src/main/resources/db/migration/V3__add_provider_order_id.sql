ALTER TABLE payments ADD COLUMN provider_order_id VARCHAR(150);
CREATE INDEX idx_payments_provider_order ON payments(provider_order_id);
