CREATE TABLE artist_reviews (
    id          VARCHAR(36) PRIMARY KEY,
    reviewer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id  VARCHAR(36) NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    rating      REAL NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_artist_reviews_reviewer_artist UNIQUE (reviewer_id, artist_id)
);

CREATE INDEX idx_artist_reviews_artist ON artist_reviews(artist_id, created_at DESC);
