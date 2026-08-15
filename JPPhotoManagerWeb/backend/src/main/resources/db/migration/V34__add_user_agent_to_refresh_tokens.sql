ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(512) NULL;
ALTER TABLE refresh_tokens ADD COLUMN last_used_at TIMESTAMPTZ NULL;
