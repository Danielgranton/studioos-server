ALTER TABLE studio_ratings
    ADD COLUMN IF NOT EXISTS booking_id VARCHAR(36);

ALTER TABLE beat_reviews
    ADD COLUMN IF NOT EXISTS purchase_id VARCHAR(36);

CREATE TABLE IF NOT EXISTS producer_reviews (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id),
    producer_id INTEGER NOT NULL REFERENCES users(id),
    booking_id  VARCHAR(36) NOT NULL REFERENCES bookings(id),
    rating      FLOAT NOT NULL,
    review      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, producer_id)
);

CREATE INDEX IF NOT EXISTS idx_producer_reviews_producer ON producer_reviews(producer_id);
CREATE INDEX IF NOT EXISTS idx_producer_reviews_booking ON producer_reviews(booking_id);
