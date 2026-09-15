-- V46 was previously applied without this auditing column in some environments.
ALTER TABLE review_reports
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
