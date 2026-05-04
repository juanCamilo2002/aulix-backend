CREATE TABLE modules
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id  UUID         NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    sort_order INT              DEFAULT 0,
    created_at TIMESTAMP        DEFAULT NOW()
);

CREATE TABLE lessons
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    module_id     UUID         NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    type          VARCHAR(20)  NOT NULL DEFAULT 'VIDEO',
    video_url     TEXT,
    content_md    TEXT,
    duration_secs INT                   DEFAULT 0,
    sort_order    INT                   DEFAULT 0,
    created_at    TIMESTAMP             DEFAULT NOW()
);

CREATE INDEX idx_modules_course     ON modules (course_id);
CREATE INDEX isx_lessons_module     ON lessons (module_id);