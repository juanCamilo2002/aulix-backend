CREATE TABLE refresh_tokens
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id             VARCHAR(64) UNIQUE  NOT NULL,
    token_hash           VARCHAR(64) UNIQUE  NOT NULL,
    user_id              UUID                NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at           TIMESTAMP           NOT NULL,
    revoked_at           TIMESTAMP,
    replaced_by_token_id VARCHAR(64),
    created_at           TIMESTAMP        DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
