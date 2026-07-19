# video-file-support

Specifies how the application catalogs video files, generates video thumbnails via FFmpeg, persists the video asset type, and presents video assets in the gallery with a play-overlay icon and a native `<video controls>` viewer.

---

## ADDED Requirements

### Requirement: Video files are included in the catalog pass

The catalog process SHALL include files with extensions `.mp4`, `.mov`, and `.mkv` (case-insensitive) when listing files in a cataloged directory. These files SHALL be persisted as `Asset` rows with `is_video = TRUE`. Files with image extensions SHALL continue to be persisted with `is_video = FALSE`.

#### Scenario: Video file in a cataloged folder is indexed

- **GIVEN** a folder contains a `.mp4` file alongside several `.jpg` files
- **WHEN** the catalog process runs against that folder
- **THEN** an `Asset` row is created for the `.mp4` file with `is_video = TRUE` and `Asset` rows are also created for the `.jpg` files with `is_video = FALSE`

#### Scenario: Unsupported file extension is not cataloged

- **GIVEN** a folder contains a `.avi` file and a `.mp4` file
- **WHEN** the catalog process runs against that folder
- **THEN** an `Asset` row is created for the `.mp4` file but no `Asset` row is created for the `.avi` file

#### Scenario: Video file extension matching is case-insensitive

- **GIVEN** a folder contains files named `clip.MP4`, `video.Mov`, and `recording.MKV`
- **WHEN** the catalog process runs against that folder
- **THEN** `Asset` rows are created for all three files with `is_video = TRUE`

---

### Requirement: Video thumbnails are generated via FFmpeg

For each video file cataloged, the system SHALL invoke `ffmpeg` via `ProcessBuilder` to extract a single still frame at the one-second mark, scaled to fit within 200×150 pixels preserving the original aspect ratio, and save it as a JPEG thumbnail. The thumbnail SHALL be stored under the same path and naming convention as image thumbnails. If FFmpeg is not available or returns a non-zero exit code, the catalog SHALL log a warning and skip the file rather than aborting the catalog run.

#### Scenario: Video thumbnail is generated successfully

- **GIVEN** a `.mp4` file exists in a cataloged folder and FFmpeg is installed
- **WHEN** the catalog process creates an `Asset` row for the file
- **THEN** a JPEG thumbnail is stored for the asset and retrievable via `GET /api/assets/{id}/thumbnail`

#### Scenario: FFmpeg fails for a specific video file

- **GIVEN** a `.mkv` file with a corrupt video stream exists in a cataloged folder and FFmpeg is installed
- **WHEN** the catalog process attempts to generate a thumbnail for the file
- **THEN** a warning is logged, no thumbnail is stored, and the catalog continues processing other files in the folder

#### Scenario: FFmpeg is not installed

- **GIVEN** a `.mp4` file exists in a cataloged folder and FFmpeg is not installed on the host
- **WHEN** the catalog process attempts to generate a thumbnail for the file
- **THEN** a warning is logged, the file is skipped, and image files in the same folder are cataloged normally

---

### Requirement: Video files do not trigger EXIF parsing or image rotation detection

When generating a thumbnail or computing the rotation for a video file, the system SHALL return `ROTATE_0` from `getImageRotation()` and an all-null `ExifMetadata` record from `getExifMetadata()` without attempting to parse EXIF data from the video container.

#### Scenario: Image rotation lookup for a video file

- **GIVEN** a `.mov` file is being cataloged
- **WHEN** `StoragePort.getImageRotation()` is called for that file path
- **THEN** the method returns `ROTATE_0` without reading the file's binary content for EXIF tags

#### Scenario: EXIF metadata lookup for a video file

- **GIVEN** a `.mp4` file is being cataloged
- **WHEN** `StoragePort.getExifMetadata()` is called for that file path
- **THEN** the method returns an `ExifMetadata` record with all fields set to `null`

---

### Requirement: `is_video` is persisted in the `assets` table and exposed via the API

The `assets` table SHALL have a non-null `BOOLEAN` column `is_video` (default `FALSE`). Every `AssetDto` returned by `GET /api/assets` SHALL include an `isVideo` field reflecting the persisted value. Existing assets cataloged before this migration have `is_video = FALSE` by default.

#### Scenario: `isVideo` field is present in the asset list response

- **GIVEN** a cataloged folder contains both image and video assets
- **WHEN** an authenticated user calls `GET /api/assets?folderPath=<folder>`
- **THEN** the response contains `"isVideo": false` for image assets and `"isVideo": true` for video assets

#### Scenario: Re-cataloging a video file after migration

- **GIVEN** an existing `Asset` row for a `.mp4` file has `is_video = FALSE` (cataloged before this migration)
- **WHEN** the catalog process re-runs and processes the same file
- **THEN** the `Asset` row is updated so that `is_video = TRUE`

---

### Requirement: Video thumbnails in the gallery list display a play-overlay icon

In the gallery list (thumbnails mode), any asset whose `isVideo` is `true` SHALL display a play-circle icon overlaid at the center of the thumbnail image. Image assets SHALL display no overlay icon.

#### Scenario: Play-overlay icon is shown for a video asset

- **GIVEN** the gallery is in thumbnails mode and the current folder contains a video asset
- **WHEN** the thumbnail row for the video asset is rendered
- **THEN** a play-circle icon is visible over the thumbnail image

#### Scenario: No overlay icon for an image asset

- **GIVEN** the gallery is in thumbnails mode and the current folder contains an image asset
- **WHEN** the thumbnail row for the image asset is rendered
- **THEN** no play-circle icon is shown over the thumbnail image

---

### Requirement: The gallery viewer renders `<video controls>` for video assets

When the user opens the viewer for a video asset, the gallery SHALL render a `<video controls>` HTML element whose `src` attribute is set to the asset's `imageUrl`. For image assets, the viewer SHALL continue to render an `<img>` element. The switch between the two elements SHALL be determined by the asset's `isVideo` field.

#### Scenario: Viewer opens a video asset

- **GIVEN** the user double-clicks a video asset row in the gallery list
- **WHEN** the viewer opens
- **THEN** a `<video controls>` element is present in the DOM and an `<img>` element is not

#### Scenario: Viewer opens an image asset

- **GIVEN** the user double-clicks an image asset row in the gallery list
- **WHEN** the viewer opens
- **THEN** an `<img>` element is present in the DOM and a `<video controls>` element is not

#### Scenario: Navigating from an image to a video asset in the viewer

- **GIVEN** the viewer is open on an image asset
- **WHEN** the user clicks the next-arrow button and the next asset is a video
- **THEN** the `<img>` element is replaced by a `<video controls>` element

---

### Requirement: FFmpeg is available in the backend Docker runtime image

The backend `Dockerfile` SHALL install FFmpeg in the runtime stage. The installation SHALL use the Alpine package manager (`apk add --no-cache ffmpeg`) so that `ProcessBuilder` invocations of `ffmpeg` succeed when the application runs inside the container.

#### Scenario: FFmpeg is executable inside the container

- **GIVEN** the backend Docker image has been built from the updated `Dockerfile`
- **WHEN** the container runs and the catalog process encounters a `.mp4` file
- **THEN** the `ProcessBuilder` invocation of `ffmpeg` exits with code `0` and a JPEG thumbnail is produced
