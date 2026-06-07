ALTER TABLE albums ADD COLUMN filter_json JSONB NULL;
CREATE INDEX ix_albums_filter_json_not_null ON albums(album_id) WHERE filter_json IS NOT NULL;
