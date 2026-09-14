ALTER TABLE artist_reviews
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS moderated_by INTEGER REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

ALTER TABLE producer_reviews
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS moderated_by INTEGER REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

ALTER TABLE studio_ratings
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS moderated_by INTEGER REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_artist_reviews_active ON artist_reviews(artist_id, moderation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_producer_reviews_active ON producer_reviews(producer_id, moderation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_studio_ratings_active ON studio_ratings(studio_id, moderation_status, created_at DESC);

CREATE TABLE IF NOT EXISTS review_reports (
    id            VARCHAR(36) PRIMARY KEY,
    review_type   VARCHAR(20) NOT NULL CHECK (review_type IN ('ARTIST', 'PRODUCER', 'STUDIO')),
    review_id     VARCHAR(36) NOT NULL,
    reporter_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason        VARCHAR(40) NOT NULL,
    details       TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by   INTEGER REFERENCES users(id),
    reviewed_at   TIMESTAMP,
    resolution    TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_report_reporter UNIQUE (review_type, review_id, reporter_id)
);

CREATE INDEX IF NOT EXISTS idx_review_reports_queue ON review_reports(status, created_at DESC);
