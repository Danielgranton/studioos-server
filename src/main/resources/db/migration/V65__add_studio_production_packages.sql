ALTER TABLE studios ADD COLUMN production_package_price INTEGER;

ALTER TABLE services
    ADD COLUMN included_in_production_package BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN service_price INTEGER,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO services (id, name, catalog_service_id, studio_id, included_in_production_package)
SELECT gen_random_uuid()::text, stage.name, catalog.id, studio.id, TRUE
FROM studios studio
CROSS JOIN (VALUES
    ('Beat creation', 'beat-production'),
    ('Recording', 'recording-session'),
    ('Mixing', 'mixing'),
    ('Mastering', 'mastering')
) AS stage(name, slug)
LEFT JOIN service_catalog catalog ON catalog.slug = stage.slug
WHERE NOT EXISTS (
    SELECT 1 FROM services existing
    WHERE existing.studio_id = studio.id AND LOWER(existing.name) = LOWER(stage.name)
);
