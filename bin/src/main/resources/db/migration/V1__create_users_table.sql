CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    phone         VARCHAR(20) UNIQUE,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    location      VARCHAR(255),
    bio           TEXT,
    genre         VARCHAR(255),
    profile_image VARCHAR(255),
    experience    VARCHAR(255),
    instagram     VARCHAR(255),
    youtube       VARCHAR(255),
    link          VARCHAR(255),
    settings      JSONB,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);