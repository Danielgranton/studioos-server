
-- ─── Wallets ───
CREATE TABLE wallets (
    id                VARCHAR(36) PRIMARY KEY,
    type              VARCHAR(50) NOT NULL,
    studio_id         VARCHAR(36) REFERENCES studios(id) ON DELETE CASCADE,
    available_balance INTEGER NOT NULL DEFAULT 0,
    pending_balance   INTEGER NOT NULL DEFAULT 0,
    withdrawn_balance INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_wallets_studio ON wallets(studio_id) WHERE studio_id IS NOT NULL;
CREATE INDEX idx_wallets_type ON wallets(type);

-- ─── Transactions ───
CREATE TABLE transactions (
    id                    VARCHAR(36) PRIMARY KEY,
    type                  VARCHAR(50) NOT NULL,
    status                VARCHAR(50) NOT NULL,
    amount                INTEGER NOT NULL,
    booking_id            VARCHAR(36) REFERENCES bookings(id),
    studio_id             VARCHAR(36) REFERENCES studios(id),
    user_id               INTEGER REFERENCES users(id),
    mpesa_checkout_request_id VARCHAR(50),
    mpesa_receipt_number  VARCHAR(50),
    mpesa_phone_number    VARCHAR(20),
    description           TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_booking ON transactions(booking_id);
CREATE INDEX idx_transactions_studio ON transactions(studio_id);
CREATE INDEX idx_transactions_user ON transactions(user_id);

-- ─── Escrows ───
CREATE TABLE escrows (
    id             VARCHAR(36) PRIMARY KEY,
    booking_id     VARCHAR(36) NOT NULL REFERENCES bookings(id),
    transaction_id VARCHAR(36) NOT NULL REFERENCES transactions(id),
    status         VARCHAR(50) NOT NULL,
    amount         INTEGER NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_escrows_booking ON escrows(booking_id);
CREATE INDEX idx_escrows_status ON escrows(status);

-- ─── Withdrawals ───
CREATE TABLE withdrawals (
    id                    VARCHAR(36) PRIMARY KEY,
    studio_id             VARCHAR(36) NOT NULL REFERENCES studios(id),
    amount                INTEGER NOT NULL,
    status                VARCHAR(50) NOT NULL,
    transaction_id        VARCHAR(36) REFERENCES transactions(id),
    mpesa_receipt_number  VARCHAR(50),
    mpesa_phone_number    VARCHAR(20),
    rejection_reason      TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_withdrawals_studio ON withdrawals(studio_id);
CREATE INDEX idx_withdrawals_status ON withdrawals(status);

-- ─── Audit Logs ───
CREATE TABLE audit_logs (
    id          VARCHAR(36) PRIMARY KEY,
    event_type  VARCHAR(50) NOT NULL,
    entity_id   VARCHAR(36) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    user_id     INTEGER REFERENCES users(id),
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);