CREATE TABLE recording_sessions (
    id               VARCHAR(36) PRIMARY KEY,
    booking_id       VARCHAR(36) NOT NULL UNIQUE,
    studio_id        VARCHAR(36) NOT NULL,
    artist_id        INTEGER     NOT NULL,
    producer_id      INTEGER     NULL,
    engineer_id      INTEGER     NULL,
    scheduled_start  TIMESTAMP   NOT NULL,
    scheduled_end    TIMESTAMP   NOT NULL,
    actual_start     TIMESTAMP   NULL,
    actual_end       TIMESTAMP   NULL,
    status           VARCHAR(30) NOT NULL,
    notes            TEXT        NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recording_sessions_booking_id ON recording_sessions(booking_id);
CREATE INDEX idx_recording_sessions_studio_id ON recording_sessions(studio_id);
CREATE INDEX idx_recording_sessions_artist_id ON recording_sessions(artist_id);
CREATE INDEX idx_recording_sessions_producer_id ON recording_sessions(producer_id);
CREATE INDEX idx_recording_sessions_status ON recording_sessions(status);

CREATE TABLE session_timeline (
    id              VARCHAR(36) PRIMARY KEY,
    session_id      VARCHAR(36) NOT NULL REFERENCES recording_sessions(id) ON DELETE CASCADE,
    action          VARCHAR(50) NOT NULL,
    performed_by    INTEGER     NULL,
    details         TEXT        NULL,
    timestamp       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_timeline_session_id ON session_timeline(session_id);
CREATE INDEX idx_session_timeline_timestamp ON session_timeline(timestamp);

CREATE TABLE session_attendance (
    id              VARCHAR(36) PRIMARY KEY,
    session_id      VARCHAR(36) NOT NULL REFERENCES recording_sessions(id) ON DELETE CASCADE,
    user_id         INTEGER     NOT NULL,
    role            VARCHAR(50) NOT NULL,
    joined_at       TIMESTAMP   NULL,
    left_at         TIMESTAMP   NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_attendance_session_id ON session_attendance(session_id);
CREATE INDEX idx_session_attendance_user_id ON session_attendance(user_id);

CREATE TABLE session_deliverables (
    id                VARCHAR(36) PRIMARY KEY,
    session_id        VARCHAR(36) NOT NULL REFERENCES recording_sessions(id) ON DELETE CASCADE,
    type              VARCHAR(30) NOT NULL,
    original_file_id   VARCHAR(255) NULL,
    preview_file_id    VARCHAR(255) NULL,
    thumbnail_id       VARCHAR(255) NULL,
    duration           INTEGER     NULL,
    uploaded_by        INTEGER     NOT NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_deliverables_session_id ON session_deliverables(session_id);
CREATE INDEX idx_session_deliverables_type ON session_deliverables(type);

CREATE TABLE session_revisions (
    id              VARCHAR(36) PRIMARY KEY,
    session_id      VARCHAR(36) NOT NULL REFERENCES recording_sessions(id) ON DELETE CASCADE,
    deliverable_id  VARCHAR(36) NOT NULL REFERENCES session_deliverables(id) ON DELETE CASCADE,
    requested_by    INTEGER     NOT NULL,
    comments        TEXT        NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_revisions_session_id ON session_revisions(session_id);
CREATE INDEX idx_session_revisions_deliverable_id ON session_revisions(deliverable_id);
