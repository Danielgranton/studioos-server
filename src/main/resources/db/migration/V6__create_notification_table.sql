CREATE TABLE notifications (
    id                 VARCHAR(36) PRIMARY KEY,
    user_id            INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type               VARCHAR(50) NOT NULL,
    title              VARCHAR(255) NOT NULL,
    message            TEXT NOT NULL,
    related_entity_id  VARCHAR(36),
    is_read            BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);