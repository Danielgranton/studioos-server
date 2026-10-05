ALTER TABLE bookings
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 1;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_attempt_count CHECK (attempt_count BETWEEN 1 AND 2);
