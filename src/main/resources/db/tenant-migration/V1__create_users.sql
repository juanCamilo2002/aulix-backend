CREATE TABLE users
(
    id         UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    full_name  VARCHAR(255),
    role       VARCHAR(50)         NOT NULL DEFAULT 'STUDENT',
    avatar_url TEXT,
    last_login TIMESTAMP,
    created_at TIMESTAMP                    DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);