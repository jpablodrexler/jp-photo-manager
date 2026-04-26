CREATE TABLE folders (
    folder_id BIGSERIAL PRIMARY KEY,
    path      TEXT      NOT NULL
);

CREATE TABLE assets (
    asset_id                     BIGSERIAL   PRIMARY KEY,
    folder_id                    BIGINT      NOT NULL,
    file_name                    TEXT        NOT NULL,
    file_size                    BIGINT      NOT NULL,
    pixel_width                  INTEGER,
    pixel_height                 INTEGER,
    thumbnail_pixel_width        INTEGER,
    thumbnail_pixel_height       INTEGER,
    image_rotation               TEXT,
    thumbnail_creation_date_time TIMESTAMP   NOT NULL,
    hash                         TEXT        NOT NULL,
    file_creation_date_time      TIMESTAMP,
    file_modification_date_time  TIMESTAMP,
    FOREIGN KEY (folder_id) REFERENCES folders (folder_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_assets_folder_id ON assets (folder_id);

CREATE TABLE sync_assets_directories_definitions (
    id                          BIGSERIAL PRIMARY KEY,
    source_directory            TEXT      NOT NULL,
    destination_directory       TEXT      NOT NULL,
    include_sub_folders         BOOLEAN   NOT NULL DEFAULT FALSE,
    delete_assets_not_in_source BOOLEAN   NOT NULL DEFAULT FALSE,
    sort_order                  INTEGER   NOT NULL DEFAULT 0
);

CREATE TABLE convert_assets_directories_definitions (
    id                          BIGSERIAL PRIMARY KEY,
    source_directory            TEXT      NOT NULL,
    destination_directory       TEXT      NOT NULL,
    include_sub_folders         BOOLEAN   NOT NULL DEFAULT FALSE,
    delete_assets_not_in_source BOOLEAN   NOT NULL DEFAULT FALSE,
    sort_order                  INTEGER   NOT NULL DEFAULT 0
);

CREATE TABLE recent_target_paths (
    id   BIGSERIAL PRIMARY KEY,
    path TEXT      NOT NULL
);
