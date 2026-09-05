ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

UPDATE users
SET status = CASE
    WHEN deleted_at IS NOT NULL THEN 'DELETED'
    WHEN account_verified = TRUE THEN 'ACTIVE'
    ELSE 'PENDING'
END;

UPDATE users
SET email_verified_at = COALESCE(email_verified_at, updated_at)
WHERE email_verified = TRUE;

CREATE INDEX IF NOT EXISTS idx_users_status_created_at ON users(status, created_at);
