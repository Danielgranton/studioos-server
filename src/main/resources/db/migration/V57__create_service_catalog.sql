CREATE TABLE service_catalog (
    id              VARCHAR(36) PRIMARY KEY,
    slug            VARCHAR(140) NOT NULL UNIQUE,
    name            VARCHAR(140) NOT NULL UNIQUE,
    category        VARCHAR(80) NOT NULL,
    description     VARCHAR(500),
    artist_allowed  BOOLEAN NOT NULL DEFAULT TRUE,
    studio_allowed  BOOLEAN NOT NULL DEFAULT TRUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      INTEGER REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE artist_service_offerings ADD COLUMN catalog_service_id VARCHAR(36) REFERENCES service_catalog(id) ON DELETE SET NULL;
ALTER TABLE services ADD COLUMN catalog_service_id VARCHAR(36) REFERENCES service_catalog(id) ON DELETE SET NULL;

CREATE INDEX idx_service_catalog_category ON service_catalog(active, category, name);
CREATE INDEX idx_artist_offerings_catalog ON artist_service_offerings(catalog_service_id, active);
CREATE INDEX idx_studio_services_catalog ON services(catalog_service_id);

INSERT INTO service_catalog (id, slug, name, category, description, artist_allowed, studio_allowed)
VALUES
    (gen_random_uuid()::text, 'recording-session', 'Recording Session', 'Recording & Audio', 'Professional recording for vocals, instruments, and live performances.', false, true),
    (gen_random_uuid()::text, 'vocal-recording', 'Record Vocals', 'Recording & Audio', 'Capture clean, confident vocal performances.', true, true),
    (gen_random_uuid()::text, 'mixing', 'Mixing', 'Recording & Audio', 'Shape a balanced, release-ready mix.', true, true),
    (gen_random_uuid()::text, 'mastering', 'Mastering', 'Recording & Audio', 'Prepare tracks for streaming and release.', true, true),
    (gen_random_uuid()::text, 'beat-production', 'Beat Production', 'Music Production', 'Create original beats and instrumentals.', true, true),
    (gen_random_uuid()::text, 'custom-beat-creation', 'Custom Beat Creation', 'Music Production', 'Build a beat around an artist brief.', true, true),
    (gen_random_uuid()::text, 'songwriting', 'Songwriting', 'Songwriting', 'Develop lyrics, melodies, hooks, and full songs.', true, true),
    (gen_random_uuid()::text, 'ghostwriting', 'Ghostwriting', 'Songwriting', 'Write songs privately for another artist.', true, false),
    (gen_random_uuid()::text, 'session-musician', 'Session Musician', 'Session Musicians', 'Perform instruments or backing vocals for a project.', true, true),
    (gen_random_uuid()::text, 'music-video-production', 'Music Video Production', 'Video Production', 'Produce performance videos, visuals, and music videos.', false, true),
    (gen_random_uuid()::text, 'video-editing', 'Video Editing', 'Video Production', 'Edit music videos, reels, and social content.', true, true),
    (gen_random_uuid()::text, 'album-artwork', 'Album Artwork', 'Branding & Design', 'Design covers and visual assets for releases.', true, false),
    (gen_random_uuid()::text, 'artist-branding', 'Artist Branding', 'Branding & Design', 'Build a consistent visual identity for an artist.', true, false),
    (gen_random_uuid()::text, 'music-marketing', 'Music Marketing', 'Marketing & Promotion', 'Plan campaigns that grow reach and engagement.', true, true),
    (gen_random_uuid()::text, 'playlist-pitching', 'Playlist Pitching', 'Marketing & Promotion', 'Prepare and pitch releases to relevant playlists.', true, false),
    (gen_random_uuid()::text, 'music-distribution', 'Music Distribution', 'Distribution', 'Prepare metadata and distribute releases worldwide.', true, true),
    (gen_random_uuid()::text, 'podcast-editing', 'Podcast Editing', 'Podcast Services', 'Edit, mix, and master podcast episodes.', true, true),
    (gen_random_uuid()::text, 'artist-photography', 'Artist Photography', 'Photography', 'Create press photos and artist imagery.', true, true),
    (gen_random_uuid()::text, 'music-lessons', 'Music Lessons', 'Education', 'Teach vocals, instruments, production, or mixing.', true, true),
    (gen_random_uuid()::text, 'music-business-consultation', 'Music Business Consultation', 'Music Business', 'Get guidance on licensing, royalties, and releases.', true, true),
    (gen_random_uuid()::text, 'live-sound-engineering', 'Live Sound Engineering', 'Live Performance', 'Run sound for concerts, events, and live streams.', false, true)
ON CONFLICT (slug) DO NOTHING;
