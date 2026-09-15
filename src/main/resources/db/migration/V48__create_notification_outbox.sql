CREATE TABLE notification_outbox (
    id                 VARCHAR(36) PRIMARY KEY,
    notification_id    VARCHAR(36),
    user_id            INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel            VARCHAR(20) NOT NULL,
    recipient          VARCHAR(320) NOT NULL,
    subject            VARCHAR(255),
    body               TEXT NOT NULL,
    status             VARCHAR(20) NOT NULL,
    attempts           INTEGER NOT NULL DEFAULT 0,
    next_attempt_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_error         TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at            TIMESTAMP
);

CREATE INDEX idx_notification_outbox_ready
    ON notification_outbox(status, next_attempt_at, created_at);
