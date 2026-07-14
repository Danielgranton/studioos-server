CREATE TABLE search_events (
    id           VARCHAR(36) PRIMARY KEY,
    entity_type  VARCHAR(20) NOT NULL,
    query        VARCHAR(500),
    user_id      INTEGER REFERENCES users(id),
    result_count INTEGER NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_search_events_entity_type ON search_events(entity_type);
CREATE INDEX idx_search_events_query ON search_events(query);
CREATE INDEX idx_search_events_created_at ON search_events(created_at DESC);