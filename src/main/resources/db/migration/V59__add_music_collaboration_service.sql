INSERT INTO service_catalog (
    id,
    slug,
    name,
    category,
    description,
    artist_allowed,
    studio_allowed
)
VALUES (
    gen_random_uuid()::text,
    'music-collaboration',
    'Music Collaboration',
    'Music Production',
    'Collaborate with artists and producers on writing, vocals, production, and new musical ideas.',
    TRUE,
    TRUE
)
ON CONFLICT (slug) DO NOTHING;

UPDATE artist_service_offerings offering
SET catalog_service_id = catalog.id
FROM service_catalog catalog
WHERE offering.catalog_service_id IS NULL
  AND catalog.slug = 'songwriting'
  AND LOWER(TRIM(offering.name)) IN (
      'songwriting', 'song writing', 'song writer', 'songwriter',
      'lyric writing', 'lyrics writing', 'song composition'
  );

UPDATE artist_service_offerings offering
SET catalog_service_id = catalog.id
FROM service_catalog catalog
WHERE offering.catalog_service_id IS NULL
  AND catalog.slug = 'music-collaboration'
  AND LOWER(TRIM(offering.name)) IN (
      'music collaboration', 'collaboration', 'collaborate on music',
      'musical collaboration', 'music collab', 'collab'
  );
