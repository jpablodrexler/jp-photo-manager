ALTER TABLE assets ADD COLUMN deleted_at TIMESTAMPTZ NULL;
CREATE INDEX ix_assets_deleted_at ON assets(deleted_at);
