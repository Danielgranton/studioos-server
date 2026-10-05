ALTER TABLE transactions
    ADD COLUMN mpesa_review_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mpesa_review_flagged_at TIMESTAMP,
    ADD COLUMN mpesa_review_reason VARCHAR(500);

CREATE INDEX idx_transactions_mpesa_review_cases
    ON transactions (mpesa_review_flagged_at)
    WHERE mpesa_review_required = TRUE AND status = 'PENDING';
