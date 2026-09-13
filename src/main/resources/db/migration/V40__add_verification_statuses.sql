ALTER TABLE users
    ADD COLUMN verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED';

UPDATE users
SET verification_status = CASE
    WHEN account_verified = TRUE THEN 'VERIFIED'
    ELSE 'UNVERIFIED'
END;

ALTER TABLE studios
    ADD COLUMN verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED';

UPDATE studios
SET verification_status = CASE
    WHEN verified = TRUE THEN 'VERIFIED'
    ELSE 'UNVERIFIED'
END;

ALTER TABLE users
    ADD CONSTRAINT ck_users_verification_status
    CHECK (verification_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED'));

ALTER TABLE studios
    ADD CONSTRAINT ck_studios_verification_status
    CHECK (verification_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED'));
