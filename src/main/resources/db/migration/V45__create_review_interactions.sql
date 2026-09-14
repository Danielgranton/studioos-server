CREATE TABLE review_reactions (
    id          VARCHAR(36) PRIMARY KEY,
    review_type VARCHAR(20) NOT NULL CHECK (review_type IN ('ARTIST', 'PRODUCER', 'STUDIO')),
    review_id   VARCHAR(36) NOT NULL,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction    VARCHAR(10) NOT NULL CHECK (reaction IN ('LIKE', 'DISLIKE')),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_reaction_user UNIQUE (review_type, review_id, user_id)
);

CREATE INDEX idx_review_reactions_review
    ON review_reactions(review_type, review_id, reaction);

CREATE TABLE review_comments (
    id          VARCHAR(36) PRIMARY KEY,
    review_type VARCHAR(20) NOT NULL CHECK (review_type IN ('ARTIST', 'PRODUCER', 'STUDIO')),
    review_id   VARCHAR(36) NOT NULL,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body        TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP NULL
);

CREATE INDEX idx_review_comments_review
    ON review_comments(review_type, review_id, created_at)
    WHERE deleted_at IS NULL;
