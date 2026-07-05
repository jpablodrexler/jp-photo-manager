ALTER TABLE assets ALTER COLUMN hash DROP NOT NULL;
ALTER TABLE assets ALTER COLUMN thumbnail_creation_date_time DROP NOT NULL;
ALTER TABLE assets ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE assets ADD COLUMN hash_completed_at TIMESTAMP;
ALTER TABLE assets ADD COLUMN exif_completed_at TIMESTAMP;
ALTER TABLE assets ADD COLUMN thumbnail_completed_at TIMESTAMP;

CREATE INDEX ix_assets_processing_status ON assets (processing_status);
