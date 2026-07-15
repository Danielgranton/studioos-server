CREATE TABLE refresh_sessions (
    id            VARCHAR(36) PRIMARY KEY,
    user_id       INTEGER      NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    token_version INTEGER      NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMP    NULL,
    device_id     VARCHAR(255) NULL,
    device_name   VARCHAR(255) NULL,
    user_agent    VARCHAR(512) NULL,
    ip_address    VARCHAR(64)  NULL
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions(user_id);
CREATE INDEX idx_refresh_sessions_revoked_at ON refresh_sessions(revoked_at);
