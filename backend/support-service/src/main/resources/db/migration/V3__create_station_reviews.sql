CREATE TABLE station_reviews(
  id BINARY(16) PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  station_id BINARY(16) NOT NULL,
  customer_id BINARY(16),
  customer_name VARCHAR(120) NOT NULL,
  rating INT NOT NULL,
  comment TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  INDEX idx_station_reviews_tenant(tenant_id,created_at),
  INDEX idx_station_reviews_station(station_id,status)
);
