CREATE TABLE courses
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(255)        NOT NULL,
    slug          VARCHAR(255) UNIQUE NOT NULL,
    description   TEXT,
    price         DECIMAL(10, 2)   DEFAULT 0.00,
    currency      VARCHAR(3)       DEFAULT 'USD',
    thumbnail_url TEXT,
    published     BOOLEAN          DEFAULT FALSE,
    instructor_id UUID                NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP        DEFAULT NOW(),
    updated_at    TIMESTAMP        DEFAULT NOW()
);

CREATE INDEX idx_courses_slug           ON courses (slug);
CREATE INDEX idx_courses_instructor     ON courses (instructor_id);
CREATE INDEX idx_courses_published      ON courses (published);