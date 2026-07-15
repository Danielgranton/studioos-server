ALTER TABLE session_deliverables
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'UPLOADING',
    ADD COLUMN bucket VARCHAR(255) NOT NULL DEFAULT 'studioos-files',
    ADD COLUMN object_key VARCHAR(512) NOT NULL DEFAULT '',
    ADD COLUMN content_type VARCHAR(255) NULL,
    ADD COLUMN expires_at TIMESTAMP NULL;

CREATE TABLE session_deliverable_jobs (
    id                  VARCHAR(36) PRIMARY KEY,
    session_deliverable_id VARCHAR(36) NOT NULL REFERENCES session_deliverables(id) ON DELETE CASCADE,
    operation           VARCHAR(100) NOT NULL,
    external_job_id     VARCHAR(255) NOT NULL UNIQUE,
    status              VARCHAR(30) NOT NULL,
    result_reference    VARCHAR(1024) NULL,
    error_message       TEXT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_deliverable_jobs_session_deliverable_id
    ON session_deliverable_jobs(session_deliverable_id);
CREATE INDEX idx_session_deliverable_jobs_status
    ON session_deliverable_jobs(status);
