CREATE TABLE artist_service_offerings (
    id              VARCHAR(36) PRIMARY KEY,
    artist_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500),
    price           INTEGER NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'KES',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_artist_service_price CHECK (price >= 0),
    CONSTRAINT ck_artist_service_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_artist_services_artist_active
    ON artist_service_offerings(artist_id, active, created_at DESC);
