ALTER TABLE beat_purchases ADD COLUMN is_exclusive BOOLEAN NOT NULL DEFAULT false;

CREATE UNIQUE INDEX idx_beat_purchases_exclusive_lock
ON beat_purchases (license_id)
WHERE is_exclusive = true AND status IN ('PENDING', 'PAID');