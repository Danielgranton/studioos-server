ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image_large VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_image_medium VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_image_thumbnail VARCHAR(500);

ALTER TABLE studios
    ADD COLUMN IF NOT EXISTS profile_image_large VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_image_medium VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_image_thumbnail VARCHAR(500);
