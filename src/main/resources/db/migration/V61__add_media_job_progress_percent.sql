ALTER TABLE media_processing_jobs
    ADD COLUMN IF NOT EXISTS progress_percent INTEGER NOT NULL DEFAULT 0;

ALTER TABLE media_processing_jobs
    ADD CONSTRAINT media_processing_jobs_progress_percent_check
    CHECK (progress_percent BETWEEN 0 AND 100);
