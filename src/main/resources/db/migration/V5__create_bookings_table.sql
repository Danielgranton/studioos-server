CREATE TABLE bookings (
    id            VARCHAR(36) PRIMARY KEY,
    studio_id     VARCHAR(36) NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    artist_id     INTEGER NOT NULL REFERENCES users(id),
    session_date  TIMESTAMP NOT NULL,
    duration_hours INTEGER NOT NULL,
    total_price   INTEGER,
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_studio ON bookings(studio_id);
CREATE INDEX idx_bookings_artist ON bookings(artist_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_session ON bookings(session_date);