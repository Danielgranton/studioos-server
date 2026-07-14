ALTER TABLE ad_campaigns ADD COLUMN transaction_id VARCHAR(36);

CREATE TABLE ad_impressions (
    id                VARCHAR(36) PRIMARY KEY,
    advertisement_id  VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    campaign_id       VARCHAR(36) NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    user_id           INTEGER REFERENCES users(id),
    placement         VARCHAR(30) NOT NULL,
    occurred_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_impressions_advertisement_id ON ad_impressions(advertisement_id);
CREATE INDEX idx_ad_impressions_campaign ON ad_impressions(campaign_id);

CREATE TABLE ad_clicks (
    id                VARCHAR(36) PRIMARY KEY,
    advertisement_id  VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    campaign_id       VARCHAR(36) NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    user_id           INTEGER REFERENCES users(id),
    clicked_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_clicks_advertisement_id ON ad_clicks(advertisement_id);