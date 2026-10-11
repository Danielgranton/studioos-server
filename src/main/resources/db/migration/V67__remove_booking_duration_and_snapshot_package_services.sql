ALTER TABLE bookings
    ADD COLUMN package_services JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE bookings b
SET package_services = COALESCE((
    SELECT jsonb_agg(s.name ORDER BY
        CASE LOWER(TRIM(s.name))
            WHEN 'beat creation' THEN 1
            WHEN 'recording' THEN 2
            WHEN 'mixing' THEN 3
            WHEN 'mastering' THEN 4
            ELSE 5
        END,
        LOWER(TRIM(s.name))
    )
    FROM services s
    WHERE s.studio_id = b.studio_id
      AND s.active = TRUE
      AND s.included_in_production_package = TRUE
), '[]'::jsonb);

ALTER TABLE bookings DROP COLUMN duration_hours;
