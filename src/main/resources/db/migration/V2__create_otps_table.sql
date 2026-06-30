CREATE TABLE otps (
    id         BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    code       VARCHAR(10)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otps_identifier ON otps(identifier);