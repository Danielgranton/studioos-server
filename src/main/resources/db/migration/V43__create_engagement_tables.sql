CREATE TABLE engagement_edges (
    id          VARCHAR(36) PRIMARY KEY,
    actor_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id   VARCHAR(64) NOT NULL,
    action      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_engagement_target_type CHECK (target_type IN ('USER', 'STUDIO')),
    CONSTRAINT ck_engagement_action CHECK (action IN ('FOLLOW', 'FAVORITE')),
    CONSTRAINT uq_engagement_edge UNIQUE (actor_id, target_type, target_id, action)
);

CREATE INDEX idx_engagement_edges_target ON engagement_edges(target_type, target_id, action);

CREATE TABLE engagement_views (
    id          VARCHAR(36) PRIMARY KEY,
    viewer_id   INTEGER REFERENCES users(id) ON DELETE SET NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   VARCHAR(64) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_engagement_view_target CHECK (target_type IN ('USER', 'STUDIO'))
);

CREATE INDEX idx_engagement_views_target ON engagement_views(target_type, target_id, created_at DESC);
