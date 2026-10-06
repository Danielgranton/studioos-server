CREATE TABLE service_bookings (
    id VARCHAR(36) PRIMARY KEY,
    provider_id INTEGER NOT NULL REFERENCES users(id),
    provider_type VARCHAR(16) NOT NULL,
    listing_id VARCHAR(36) NOT NULL,
    studio_id VARCHAR(36) REFERENCES studios(id),
    service_name VARCHAR(120) NOT NULL,
    requester_id INTEGER NOT NULL REFERENCES users(id),
    preferred_date TIMESTAMP NOT NULL,
    request_details VARCHAR(2000) NOT NULL,
    amount INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KES',
    transaction_id VARCHAR(36) REFERENCES transactions(id),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_booking_provider_type CHECK (provider_type IN ('ARTIST', 'PRODUCER')),
    CONSTRAINT chk_service_booking_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'PAYMENT_PENDING', 'PAID', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_service_bookings_requester_created ON service_bookings(requester_id, created_at DESC);
CREATE INDEX idx_service_bookings_provider_created ON service_bookings(provider_id, created_at DESC);
CREATE INDEX idx_service_bookings_transaction ON service_bookings(transaction_id);

ALTER TABLE transactions ADD COLUMN service_booking_id VARCHAR(36) REFERENCES service_bookings(id);
CREATE INDEX idx_transactions_service_booking ON transactions(service_booking_id);
