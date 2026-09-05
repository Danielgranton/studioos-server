ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS device_type VARCHAR(32);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS browser VARCHAR(128);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS operating_system VARCHAR(128);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;
UPDATE refresh_sessions SET last_active_at = created_at WHERE last_active_at IS NULL;
