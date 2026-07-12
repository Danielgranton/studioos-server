CREATE TABLE ad_upload_sessions (
    id             VARCHAR(36) PRIMARY KEY,
    advertiser_id  INTEGER NOT NULL REFERENCES users(id),
    advertisement_id VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    bucket         VARCHAR(255) NOT NULL,
    object_key     VARCHAR(500) NOT NULL,
    content_type   VARCHAR(100),
    status         VARCHAR(50) NOT NULL,
    expires_at     TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_upload_sessions_ad ON ad_upload_sessions(advertisement_id);

CREATE TABLE ad_media_processing_jobs (
    id                VARCHAR(36) PRIMARY KEY,
    advertisement_id  VARCHAR(36) NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    operation         VARCHAR(50) NOT NULL,
    external_job_id   VARCHAR(100) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    result_reference  VARCHAR(500),
    error_message     TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ad_jobs_advertisement ON ad_media_processing_jobs(advertisement_id);