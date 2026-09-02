ALTER TABLE audit_logs ADD COLUMN ip_address VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(500);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
