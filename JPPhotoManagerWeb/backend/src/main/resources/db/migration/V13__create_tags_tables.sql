CREATE TABLE tags (
    tag_id SERIAL PRIMARY KEY,
    name   VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX ix_tags_name ON tags(name);

CREATE TABLE asset_tags (
    asset_id BIGINT NOT NULL REFERENCES assets(asset_id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tags(tag_id)   ON DELETE CASCADE,
    PRIMARY KEY (asset_id, tag_id)
);
