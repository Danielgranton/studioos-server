CREATE TABLE notification_preferences (
    id                VARCHAR(36) PRIMARY KEY,
    user_id           INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(80)  NOT NULL,
    in_app_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    email_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    sms_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_notification_preferences_user_type UNIQUE (user_id, notification_type)
);

CREATE INDEX idx_notification_preferences_user_id
    ON notification_preferences(user_id);
