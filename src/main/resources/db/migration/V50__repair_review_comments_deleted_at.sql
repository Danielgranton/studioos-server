-- V45 was previously applied without this column in some environments.
ALTER TABLE review_comments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_review_comments_review
    ON review_comments(review_type, review_id, created_at)
    WHERE deleted_at IS NULL;
