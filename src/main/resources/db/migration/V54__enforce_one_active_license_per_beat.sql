UPDATE beat_licenses
SET active = false
WHERE active = true
  AND id IN (
      SELECT id
      FROM (
          SELECT id,
                 ROW_NUMBER() OVER (PARTITION BY beat_id ORDER BY id) AS license_number
          FROM beat_licenses
          WHERE active = true
      ) ranked
      WHERE license_number > 1
  );

CREATE UNIQUE INDEX uq_active_license_per_beat
    ON beat_licenses (beat_id)
    WHERE active = true;
