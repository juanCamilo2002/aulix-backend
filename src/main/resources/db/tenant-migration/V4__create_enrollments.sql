CREATE TABLE enrollments
(
    id                UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id),
    course_id         UUID        NOT NULL REFERENCES courses (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    stripe_payment_id VARCHAR(255),
    amount_paid       DECIMAL(10, 2)       DEFAULT 0.00,
    enrolled_at       TIMESTAMP            DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

CREATE INDEX idx_enrollments_user ON enrollments (user_id);
CREATE INDEX idx_enrollments_course ON enrollments (course_id);
CREATE INDEX idx_enrollments_status ON enrollments (status);