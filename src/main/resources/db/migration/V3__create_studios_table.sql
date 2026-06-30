CREATE TABLE studios (
    id            VARCHAR(36) PRIMARY KEY,
    studio_name   VARCHAR(255) NOT NULL,
    location      VARCHAR(255) NOT NULL,
    pricing       INTEGER,
    availability  VARCHAR(255) NOT NULL,
    description   TEXT NOT NULL,
    profile_image VARCHAR(255),
    owner_id      INTEGER NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE services (
    id        VARCHAR(36) PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    studio_id VARCHAR(36) NOT NULL REFERENCES studios(id) ON DELETE CASCADE
);

CREATE TABLE studio_ratings (
    id         VARCHAR(36) PRIMARY KEY,
    studio_id  VARCHAR(36) NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    user_id    INTEGER NOT NULL REFERENCES users(id),
    rating     FLOAT NOT NULL,
    review     TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(studio_id, user_id)
);

CREATE INDEX idx_studios_owner ON studios(owner_id);
CREATE INDEX idx_studios_location ON studios(location);
CREATE INDEX idx_ratings_studio ON studio_ratings(studio_id);