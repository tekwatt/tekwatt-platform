CREATE TABLE reports (
  id BINARY(16) PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  report_type VARCHAR(30) NOT NULL,
  report_format VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL,
  period_from TIMESTAMP(6) NOT NULL,
  period_to TIMESTAMP(6) NOT NULL,
  file_name VARCHAR(255),
  content_type VARCHAR(100),
  file_content LONGBLOB,
  error_message VARCHAR(1000),
  created_at TIMESTAMP(6) NOT NULL,
  completed_at TIMESTAMP(6)
);
CREATE INDEX idx_reports_tenant_created ON reports(tenant_id, created_at);
