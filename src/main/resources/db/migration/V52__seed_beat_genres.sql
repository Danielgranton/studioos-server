INSERT INTO beat_genres (id, name)
VALUES
    (gen_random_uuid()::varchar, 'Afrobeats'),
    (gen_random_uuid()::varchar, 'Hip Hop'),
    (gen_random_uuid()::varchar, 'R&B'),
    (gen_random_uuid()::varchar, 'Dancehall'),
    (gen_random_uuid()::varchar, 'Amapiano'),
    (gen_random_uuid()::varchar, 'Gospel'),
    (gen_random_uuid()::varchar, 'Pop'),
    (gen_random_uuid()::varchar, 'Reggae'),
    (gen_random_uuid()::varchar, 'Trap'),
    (gen_random_uuid()::varchar, 'Drill')
ON CONFLICT (name) DO NOTHING;
