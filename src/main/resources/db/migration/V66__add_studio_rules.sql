ALTER TABLE studios
    ADD COLUMN rules JSONB NOT NULL DEFAULT '[]'::jsonb;
