ALTER TABLE media_processing_jobs
    ALTER COLUMN external_job_id DROP NOT NULL;

ALTER TABLE media_processing_jobs
    ADD COLUMN IF NOT EXISTS asset_reference TEXT,
    ADD COLUMN IF NOT EXISTS parameters_json TEXT,
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;
