-- ─── Base CPM rates ───
CREATE TABLE base_cpm_rates (
    id         VARCHAR(36) PRIMARY KEY,
    media_type VARCHAR(20) NOT NULL UNIQUE,
    base_cpm   INTEGER NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT true
);

-- ─── Pricing rules (configurable multipliers) ───
CREATE TABLE pricing_rules (
    id          VARCHAR(36) PRIMARY KEY,
    type        VARCHAR(30) NOT NULL,
    rule_key    VARCHAR(50),
    min_value   INTEGER,
    max_value   INTEGER,
    multiplier  DOUBLE PRECISION NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    description TEXT
);
CREATE INDEX idx_pricing_rules_type ON pricing_rules(type);

-- ─── Historical pricing record per advertisement ───
CREATE TABLE advertisement_pricing (
    id                     VARCHAR(36) PRIMARY KEY,
    campaign_id            VARCHAR(36) NOT NULL,
    advertisement_id       VARCHAR(36) NOT NULL,
    base_cpm               INTEGER NOT NULL,
    placement_multiplier   DOUBLE PRECISION NOT NULL,
    placement_rule_found   BOOLEAN NOT NULL,
    targeting_multiplier   DOUBLE PRECISION NOT NULL,
    targeting_rule_found   BOOLEAN NOT NULL,
    duration_multiplier    DOUBLE PRECISION NOT NULL,
    duration_rule_found    BOOLEAN NOT NULL,
    budget_multiplier      DOUBLE PRECISION NOT NULL,
    budget_rule_found      BOOLEAN NOT NULL,
    promotion_multiplier   DOUBLE PRECISION NOT NULL,
    promotion_rule_found   BOOLEAN NOT NULL,
    final_cpm              DOUBLE PRECISION NOT NULL,
    estimated_impressions  BIGINT NOT NULL,
    currency               VARCHAR(3) NOT NULL DEFAULT 'KES',
    calculated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_pricing_campaign ON advertisement_pricing(campaign_id);

-- ─── Campaigns ───
CREATE TABLE ad_campaigns (
    id             VARCHAR(36) PRIMARY KEY,
    advertiser_id  INTEGER NOT NULL REFERENCES users(id),
    studio_id      VARCHAR(36) REFERENCES studios(id),
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    placement      VARCHAR(30) NOT NULL,
    status         VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    start_date     TIMESTAMP NOT NULL,
    end_date       TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_campaigns_advertiser ON ad_campaigns(advertiser_id);
CREATE INDEX idx_ad_campaigns_status ON ad_campaigns(status);

-- ─── Advertisements (creatives) ───
CREATE TABLE advertisements (
    id            VARCHAR(36) PRIMARY KEY,
    campaign_id   VARCHAR(36) NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    type          VARCHAR(20) NOT NULL,
    headline      VARCHAR(255) NOT NULL,
    description   TEXT,
    cta_text      VARCHAR(100),
    cta_url       VARCHAR(500),
    media_url     VARCHAR(500),
    thumbnail_url VARCHAR(500),
    duration      INTEGER,
    status        VARCHAR(30) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_advertisements_campaign ON advertisements(campaign_id);

-- ─── Budgets ───
CREATE TABLE ad_budgets (
    id                VARCHAR(36) PRIMARY KEY,
    campaign_id       VARCHAR(36) NOT NULL UNIQUE REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    total_budget      INTEGER NOT NULL,
    daily_budget      INTEGER,
    remaining_budget  INTEGER NOT NULL,
    spent_budget      INTEGER NOT NULL DEFAULT 0,
    currency          VARCHAR(3) NOT NULL DEFAULT 'KES'
);

-- ─── Seed data ───
-- NOTE: these are illustrative starting values matching the worked examples discussed,
-- not final business decisions. An admin should review and adjust before real advertisers
-- start pricing off of them.

INSERT INTO base_cpm_rates (id, media_type, base_cpm, active) VALUES
    (gen_random_uuid()::text, 'IMAGE', 80, true),
    (gen_random_uuid()::text, 'AUDIO', 120, true),
    (gen_random_uuid()::text, 'VIDEO', 180, true);

INSERT INTO pricing_rules (id, type, rule_key, multiplier, active, description) VALUES
    (gen_random_uuid()::text, 'PLACEMENT', 'HOME_BANNER', 1.5, true, 'Highest-visibility placement'),
    (gen_random_uuid()::text, 'PLACEMENT', 'BEAT_PLAYER', 1.3, true, NULL),
    (gen_random_uuid()::text, 'PLACEMENT', 'SEARCH_RESULTS', 1.1, true, NULL),
    (gen_random_uuid()::text, 'PLACEMENT', 'STUDIO_PROFILE', 1.0, true, NULL),
    (gen_random_uuid()::text, 'PLACEMENT', 'EXPLORE_PAGE', 0.9, true, NULL);

INSERT INTO pricing_rules (id, type, min_value, max_value, multiplier, active, description) VALUES
    (gen_random_uuid()::text, 'DURATION_TIER', 1, 6, 1.0, true, '1-6 days'),
    (gen_random_uuid()::text, 'DURATION_TIER', 7, 29, 0.97, true, '1-4 weeks'),
    (gen_random_uuid()::text, 'DURATION_TIER', 30, NULL, 0.90, true, '30+ days'),
    (gen_random_uuid()::text, 'BUDGET_TIER', 0, 19999, 1.0, true, 'Under 20k'),
    (gen_random_uuid()::text, 'BUDGET_TIER', 20000, 99999, 0.95, true, '20k-100k'),
    (gen_random_uuid()::text, 'BUDGET_TIER', 100000, NULL, 0.90, true, '100k+');