ALTER TABLE users ADD COLUMN username VARCHAR(30);

CREATE UNIQUE INDEX users_username_unique_idx
    ON users (username)
    WHERE username IS NOT NULL;
