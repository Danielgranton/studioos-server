-- ─── Beat Genres (lookup) ───
CREATE TABLE beat_genres (
    id   VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- ─── Beats ───
CREATE TABLE beats (
    id                 VARCHAR(36) PRIMARY KEY,
    producer_id        INTEGER NOT NULL REFERENCES users(id),
    studio_id          VARCHAR(36) REFERENCES studios(id),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    genre_id           VARCHAR(36) REFERENCES beat_genres(id),
    bpm                INTEGER,
    key_signature      VARCHAR(10),
    mood               VARCHAR(50),
    cover_url          VARCHAR(500),
    audio_url          VARCHAR(500),
    preview_url        VARCHAR(500),
    waveform_url       VARCHAR(500),
    duration           INTEGER,
    status             VARCHAR(50) NOT NULL,
    visibility         VARCHAR(50) NOT NULL,
    exclusive_sold     BOOLEAN NOT NULL DEFAULT false,
    play_count         INTEGER NOT NULL DEFAULT 0,
    download_count     INTEGER NOT NULL DEFAULT 0,
    like_count         INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_beats_producer ON beats(producer_id);
CREATE INDEX idx_beats_studio ON beats(studio_id);
CREATE INDEX idx_beats_status ON beats(status);
CREATE INDEX idx_beats_visibility ON beats(visibility);
CREATE INDEX idx_beats_genre ON beats(genre_id);

-- ─── Beat Licenses ───
CREATE TABLE beat_licenses (
    id                  VARCHAR(36) PRIMARY KEY,
    beat_id             VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    type                VARCHAR(50) NOT NULL,
    price               INTEGER NOT NULL,
    commercial_use      BOOLEAN NOT NULL DEFAULT false,
    max_streams         INTEGER,
    allow_music_video   BOOLEAN NOT NULL DEFAULT false,
    allow_radio         BOOLEAN NOT NULL DEFAULT false,
    allow_tv            BOOLEAN NOT NULL DEFAULT false,
    allow_modification  BOOLEAN NOT NULL DEFAULT false,
    exclusive           BOOLEAN NOT NULL DEFAULT false,
    active              BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX idx_beat_licenses_beat ON beat_licenses(beat_id);

-- ─── Beat Purchases ───
CREATE TABLE beat_purchases (
    id               VARCHAR(36) PRIMARY KEY,
    beat_id          VARCHAR(36) NOT NULL REFERENCES beats(id),
    buyer_id         INTEGER NOT NULL REFERENCES users(id),
    license_id       VARCHAR(36) NOT NULL REFERENCES beat_licenses(id),
    transaction_id   VARCHAR(36) REFERENCES transactions(id),
    amount           INTEGER NOT NULL,
    status            VARCHAR(50) NOT NULL,
    download_count   INTEGER NOT NULL DEFAULT 0,
    purchased_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_beat_purchases_beat ON beat_purchases(beat_id);
CREATE INDEX idx_beat_purchases_buyer ON beat_purchases(buyer_id);
CREATE INDEX idx_beat_purchases_status ON beat_purchases(status);

-- ─── Upload Sessions ───
CREATE TABLE upload_sessions (
    id              VARCHAR(36) PRIMARY KEY,
    producer_id     INTEGER NOT NULL REFERENCES users(id),
    beat_id         VARCHAR(36) REFERENCES beats(id),
    bucket          VARCHAR(255) NOT NULL,
    object_key      VARCHAR(500) NOT NULL,
    file_type       VARCHAR(20) NOT NULL,
    content_type    VARCHAR(100),
    checksum        VARCHAR(64),
    size_bytes      BIGINT,
    status           VARCHAR(50) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_upload_sessions_producer ON upload_sessions(producer_id);
CREATE INDEX idx_upload_sessions_beat ON upload_sessions(beat_id);

-- ─── Beat Likes ───
CREATE TABLE beat_likes (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id),
    beat_id     VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, beat_id)
);
CREATE INDEX idx_beat_likes_beat ON beat_likes(beat_id);

-- ─── Beat Reviews ───
CREATE TABLE beat_reviews (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id),
    beat_id     VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, beat_id)
);
CREATE INDEX idx_beat_reviews_beat ON beat_reviews(beat_id);

-- ─── Beat Play History ───
CREATE TABLE beat_play_history (
    id                VARCHAR(36) PRIMARY KEY,
    user_id           INTEGER REFERENCES users(id),
    beat_id           VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    duration_played   INTEGER,
    played_at         TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_beat_play_history_beat ON beat_play_history(beat_id);
CREATE INDEX idx_beat_play_history_user ON beat_play_history(user_id);

-- ─── Beat Downloads ───
CREATE TABLE beat_downloads (
    id             VARCHAR(36) PRIMARY KEY,
    purchase_id    VARCHAR(36) NOT NULL REFERENCES beat_purchases(id) ON DELETE CASCADE,
    downloaded_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address     VARCHAR(45)
);
CREATE INDEX idx_beat_downloads_purchase ON beat_downloads(purchase_id);

-- ─── Beat Tags ───
CREATE TABLE beat_tags (
    id    VARCHAR(36) PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE beat_tag_map (
    beat_id  VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    tag_id   VARCHAR(36) NOT NULL REFERENCES beat_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (beat_id, tag_id)
);

-- ─── Beat Collections ───
CREATE TABLE beat_collections (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id),
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_beat_collections_user ON beat_collections(user_id);

CREATE TABLE beat_collection_items (
    collection_id  VARCHAR(36) NOT NULL REFERENCES beat_collections(id) ON DELETE CASCADE,
    beat_id        VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    added_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (collection_id, beat_id)
);