CREATE TABLE refresh_tokens (
    token_id  BIGSERIAL PRIMARY KEY,
    user_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token     TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked   BOOLEAN     NOT NULL DEFAULT FALSE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens(user_id);
