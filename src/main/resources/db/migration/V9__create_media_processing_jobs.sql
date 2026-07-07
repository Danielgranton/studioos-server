CREATE TABLE media_processing_jobs (
    id                VARCHAR(36) PRIMARY KEY,
    beat_id           VARCHAR(36) NOT NULL REFERENCES beats(id) ON DELETE CASCADE,
    operation         VARCHAR(50) NOT NULL,
    external_job_id   VARCHAR(100) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    result_reference  VARCHAR(500),
    error_message     TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_media_jobs_beat ON media_processing_jobs(beat_id);
CREATE INDEX idx_media_jobs_status ON media_processing_jobs(status);