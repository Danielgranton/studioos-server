CREATE TABLE artist_service_requests (
    id              VARCHAR(36) PRIMARY KEY,
    service_id      VARCHAR(36) NOT NULL REFERENCES artist_service_offerings(id) ON DELETE CASCADE,
    artist_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requester_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_note    VARCHAR(1000),
    amount          INTEGER NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'KES',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_artist_service_request_amount CHECK (amount >= 0),
    CONSTRAINT ck_artist_service_request_status CHECK (status IN ('PENDING', 'PAID', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_artist_service_requests_artist_status
    ON artist_service_requests(artist_id, status, created_at DESC);
