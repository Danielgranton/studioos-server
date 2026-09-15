ALTER TABLE notification_outbox
    ADD COLUMN provider_message_id VARCHAR(128),
    ADD COLUMN last_attempt_at TIMESTAMP,
    ADD COLUMN failed_at TIMESTAMP;
