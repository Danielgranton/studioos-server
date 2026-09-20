CREATE TABLE studio_likes (
    id        VARCHAR(36) PRIMARY KEY,
    user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id VARCHAR(36) NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_studio_likes_user_studio UNIQUE (user_id, studio_id)
);

CREATE INDEX idx_studio_likes_studio ON studio_likes(studio_id);
