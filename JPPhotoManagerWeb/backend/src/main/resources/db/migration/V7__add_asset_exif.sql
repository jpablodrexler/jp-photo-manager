CREATE TABLE asset_exif (
    asset_id      BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE,
    camera_make   VARCHAR(255),
    camera_model  VARCHAR(255),
    lens_model    VARCHAR(255),
    exposure_time VARCHAR(50),
    f_number      DOUBLE PRECISION,
    iso_speed     INTEGER,
    focal_length  DOUBLE PRECISION,
    date_taken    TIMESTAMP,
    width_pixels  INTEGER,
    height_pixels INTEGER,
    gps_latitude  DOUBLE PRECISION,
    gps_longitude DOUBLE PRECISION
);
