UPDATE artist_service_offerings offering
SET catalog_service_id = catalog.id
FROM service_catalog catalog
WHERE offering.catalog_service_id IS NULL
  AND LOWER(TRIM(offering.name)) = LOWER(catalog.name);

UPDATE services studio_service
SET catalog_service_id = catalog.id
FROM service_catalog catalog
WHERE studio_service.catalog_service_id IS NULL
  AND LOWER(TRIM(studio_service.name)) = LOWER(catalog.name);
