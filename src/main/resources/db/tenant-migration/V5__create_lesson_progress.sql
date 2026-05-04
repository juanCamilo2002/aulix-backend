CREATE TABLE lesson_progress
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    lesson_id     UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    completed     BOOLEAN          DEFAULT FALSE,
    last_position INT              DEFAULT 0,
    updated_at    TIMESTAMP        DEFAULT NOW(),
    UNIQUE (enrollment_id, lesson_id)
);

CREATE INDEX idx_progress_enrollment ON lesson_progress (enrollment_id);
CREATE INDEX idx_progress_lesson ON lesson_progress (lesson_id);