CREATE TABLE albums (
    album_id   BIGSERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE album_assets (
    album_id  BIGINT NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE,
    asset_id  BIGINT NOT NULL REFERENCES assets(asset_id) ON DELETE CASCADE,
    added_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_album_assets PRIMARY KEY (album_id, asset_id)
);

CREATE INDEX ix_albums_user_id ON albums(user_id);
