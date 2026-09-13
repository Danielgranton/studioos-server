ALTER TABLE users
    ADD COLUMN verification_reason VARCHAR(500),
    ADD COLUMN verification_reviewed_at TIMESTAMP,
    ADD COLUMN verification_reviewed_by INTEGER REFERENCES users(id);

ALTER TABLE studios
    ADD COLUMN verification_reason VARCHAR(500),
    ADD COLUMN verification_reviewed_at TIMESTAMP,
    ADD COLUMN verification_reviewed_by INTEGER REFERENCES users(id);
