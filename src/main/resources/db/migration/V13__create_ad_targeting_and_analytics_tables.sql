CREATE TABLE ad_targeting (
    id          VARCHAR(36) PRIMARY KEY,
    campaign_id VARCHAR(36) NOT NULL UNIQUE REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    countries   TEXT,
    cities      TEXT,
    genres      TEXT,
    age_min     INTEGER,
    age_max     INTEGER,
    gender      VARCHAR(50),
    interests   TEXT,
    devices     TEXT
);

CREATE INDEX idx_ad_targeting_campaign ON ad_targeting(campaign_id);

CREATE TABLE advertisement_impressions (
    id               VARCHAR(36) PRIMARY KEY,
    advertisement_id VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    user_id          INTEGER REFERENCES users(id),
    session_id       VARCHAR(255),
    device           VARCHAR(100),
    ip               VARCHAR(45),
    timestamp        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ad_impressions_ad ON advertisement_impressions(advertisement_id);
CREATE INDEX idx_ad_impressions_user ON advertisement_impressions(user_id);
CREATE INDEX idx_ad_impressions_timestamp ON advertisement_impressions(timestamp DESC);

CREATE TABLE advertisement_clicks (
    id               VARCHAR(36) PRIMARY KEY,
    advertisement_id VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    user_id          INTEGER REFERENCES users(id),
    clicked_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ad_clicks_ad ON advertisement_clicks(advertisement_id);
CREATE INDEX idx_ad_clicks_user ON advertisement_clicks(user_id);
CREATE INDEX idx_ad_clicks_clicked_at ON advertisement_clicks(clicked_at DESC);
