CREATE TABLE ad_impression_verifications (
    token             VARCHAR(36) PRIMARY KEY,
    advertisement_id  VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    campaign_id       VARCHAR(36) NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    user_id           INTEGER REFERENCES users(id) ON DELETE SET NULL,
    placement         VARCHAR(30) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    expires_at        TIMESTAMP NOT NULL,
    confirmed_at      TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ad_impression_verifications_cleanup
    ON ad_impression_verifications(status, expires_at);
