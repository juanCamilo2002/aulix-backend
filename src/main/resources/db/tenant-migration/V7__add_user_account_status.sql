ALTER TABLE users
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN password_changed_at TIMESTAMP;

UPDATE users
SET password_changed_at = created_at
WHERE password_changed_at IS NULL;
