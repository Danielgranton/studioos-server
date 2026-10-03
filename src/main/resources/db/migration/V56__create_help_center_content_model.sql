CREATE TABLE help_categories (
    id              VARCHAR(36) PRIMARY KEY,
    slug            VARCHAR(80) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE help_articles (
    id                  VARCHAR(36) PRIMARY KEY,
    slug                VARCHAR(160) NOT NULL UNIQUE,
    title               VARCHAR(180) NOT NULL,
    excerpt             TEXT NOT NULL,
    content             TEXT NOT NULL,
    status              VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    audience            VARCHAR(32) NOT NULL DEFAULT 'ALL',
    featured            BOOLEAN NOT NULL DEFAULT FALSE,
    display_order       INTEGER NOT NULL DEFAULT 0,
    read_time_minutes   INTEGER NOT NULL DEFAULT 1,
    view_count          BIGINT NOT NULL DEFAULT 0,
    helpful_count       BIGINT NOT NULL DEFAULT 0,
    not_helpful_count   BIGINT NOT NULL DEFAULT 0,
    published_at        TIMESTAMP NULL,
    category_id         VARCHAR(36) NOT NULL REFERENCES help_categories(id) ON DELETE RESTRICT,
    created_by          INTEGER REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_help_article_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_help_article_audience CHECK (audience IN ('ALL', 'PRODUCER', 'ARTIST', 'STUDIO_MANAGER', 'BUYER')),
    CONSTRAINT chk_help_article_read_time CHECK (read_time_minutes > 0)
);

CREATE TABLE help_article_tags (
    article_id VARCHAR(36) NOT NULL REFERENCES help_articles(id) ON DELETE CASCADE,
    tag        VARCHAR(60) NOT NULL,
    PRIMARY KEY (article_id, tag)
);

CREATE TABLE help_article_related (
    article_id         VARCHAR(36) NOT NULL REFERENCES help_articles(id) ON DELETE CASCADE,
    related_article_id VARCHAR(36) NOT NULL REFERENCES help_articles(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, related_article_id),
    CONSTRAINT chk_help_article_not_self_related CHECK (article_id <> related_article_id)
);

CREATE INDEX idx_help_categories_active_order ON help_categories(active, display_order);
CREATE INDEX idx_help_articles_published ON help_articles(status, audience, featured, display_order);
CREATE INDEX idx_help_articles_category ON help_articles(category_id, status);
CREATE INDEX idx_help_article_tags_tag ON help_article_tags(tag);
