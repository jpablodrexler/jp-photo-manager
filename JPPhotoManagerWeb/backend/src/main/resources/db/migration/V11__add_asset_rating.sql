ALTER TABLE assets ADD COLUMN rating SMALLINT NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5);
CREATE INDEX ix_assets_rating ON assets(rating);
