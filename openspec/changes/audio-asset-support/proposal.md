## Why

The catalog currently ignores audio files. Users store music alongside their photos but cannot browse, search, or play it from the photo manager. Supporting audio files as first-class assets extends the catalog to cover the full range of media a user might have in their photo directories.

## What Changes

- The catalog service accepts audio extensions (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`)
- `org.jaudiotagger:jaudiotagger` extracts ID3/Vorbis/FLAC metadata (title, artist, album, duration, bitrate, sample rate, embedded album art)
- A new `asset_audio` table stores audio-specific fields (Flyway migration)
- The thumbnail is the embedded album art if present; otherwise a waveform PNG generated via FFmpeg, or a generic music-note placeholder icon
- The frontend viewer renders `<audio controls>` with metadata display for audio assets

## Capabilities

### New Capabilities

- `audio-asset-support`: Audio files (.mp3, .flac, .wav, .aac, .ogg) are cataloged as first-class assets. Audio metadata (title, artist, album, duration, bitrate) is extracted and displayed. The thumbnail is the embedded album art or a waveform image.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `org.jaudiotagger:jaudiotagger`
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V24__create_asset_audio.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/AudioMetadataService.java` — jaudiotagger extraction
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CatalogAssetsUseCaseImpl.java` — handle audio extensions
- `JPPhotoManagerWeb/backend/src/test/` — tests for audio metadata extraction
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/` — audio viewer rendering `<audio controls>`
