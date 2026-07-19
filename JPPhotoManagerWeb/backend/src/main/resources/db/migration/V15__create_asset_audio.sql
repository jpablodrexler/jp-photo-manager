CREATE TABLE asset_audio (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL UNIQUE REFERENCES assets(asset_id),
    title VARCHAR(512) NULL,
    artist VARCHAR(512) NULL,
    album VARCHAR(512) NULL,
    duration_seconds INT NULL,
    bitrate_kbps INT NULL,
    sample_rate_hz INT NULL
);
