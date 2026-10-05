ALTER TABLE transactions
    ADD COLUMN mpesa_status_checked_at TIMESTAMP;

CREATE INDEX idx_transactions_pending_beat_reconciliation
    ON transactions (created_at, mpesa_status_checked_at)
    WHERE type = 'BEAT_PURCHASE'
      AND status = 'PENDING'
      AND mpesa_checkout_request_id IS NOT NULL;
