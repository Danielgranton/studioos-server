CREATE TABLE studio_media (
    id              VARCHAR(36) PRIMARY KEY,
    studio_id       VARCHAR(36) NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    media_type      VARCHAR(20) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    original_url    VARCHAR(500),
    large_url       VARCHAR(500),
    medium_url      VARCHAR(500),
    thumbnail_url   VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_studio_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

CREATE INDEX idx_studio_media_studio ON studio_media(studio_id, display_order, created_at);
