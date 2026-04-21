CREATE TABLE IF NOT EXISTS folders (
    folder_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    path        TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS assets (
    asset_id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id                   INTEGER NOT NULL,
    file_name                   TEXT    NOT NULL,
    file_size                   INTEGER NOT NULL,
    pixel_width                 INTEGER,
    pixel_height                INTEGER,
    thumbnail_pixel_width       INTEGER,
    thumbnail_pixel_height      INTEGER,
    image_rotation              TEXT,
    thumbnail_creation_date_time TEXT   NOT NULL,
    hash                        TEXT    NOT NULL,
    file_creation_date_time     TEXT,
    file_modification_date_time TEXT,
    FOREIGN KEY (folder_id) REFERENCES folders (folder_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_assets_folder_id ON assets (folder_id);

CREATE TABLE IF NOT EXISTS sync_assets_directories_definitions (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    source_directory            TEXT    NOT NULL,
    destination_directory       TEXT    NOT NULL,
    include_sub_folders         INTEGER DEFAULT 0,
    delete_assets_not_in_source INTEGER DEFAULT 0,
    sort_order                  INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS convert_assets_directories_definitions (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    source_directory            TEXT    NOT NULL,
    destination_directory       TEXT    NOT NULL,
    include_sub_folders         INTEGER DEFAULT 0,
    delete_assets_not_in_source INTEGER DEFAULT 0,
    sort_order                  INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS recent_target_paths (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    path    TEXT    NOT NULL
);
