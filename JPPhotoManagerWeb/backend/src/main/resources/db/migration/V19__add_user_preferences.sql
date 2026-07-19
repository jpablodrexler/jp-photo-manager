CREATE TABLE user_preferences (
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    theme_mode VARCHAR(10)  NOT NULL DEFAULT 'dark',
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id)
);
