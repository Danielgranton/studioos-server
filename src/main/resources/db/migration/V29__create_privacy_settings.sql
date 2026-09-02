CREATE TABLE privacy_settings (
    user_id                         INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    profile_discoverable            BOOLEAN NOT NULL DEFAULT TRUE,
    email_visible                   BOOLEAN NOT NULL DEFAULT FALSE,
    phone_visible                   BOOLEAN NOT NULL DEFAULT FALSE,
    direct_messages_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    personalized_recommendations   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT NOW()
);
