## Context

The `CatalogAssetsUseCaseImpl` dispatches on file extension to determine how to process each file. Adding audio extensions routes the file to `AudioMetadataService` instead of `StorageService.readImageExif()`. Jaudiotagger handles MP3 (ID3v1/v2), FLAC (Vorbis comments), and OGG; WAV and AAC have limited metadata support. The embedded album art is extracted as a `byte[]` and saved as the thumbnail JPEG.

## Goals / Non-Goals

**Goals:**
- Flyway V24: `CREATE TABLE asset_audio (id BIGSERIAL PK, asset_id BIGINT FK UNIQUE, title VARCHAR(512), artist VARCHAR(512), album VARCHAR(512), duration_seconds INT, bitrate_kbps INT, sample_rate_hz INT)`
- `AudioMetadataService` uses jaudiotagger's `AudioFileIO.read()` to extract fields and return `AudioMetadata`
- Thumbnail priority: (1) embedded album art → save as JPEG thumbnail; (2) FFmpeg waveform (`ffmpeg -i input -filter_complex "showwavespic=s=200x150" -frames:v 1 waveform.png`) if FFmpeg available; (3) generic placeholder
- `CatalogAssetsUseCaseImpl` routes audio extensions to `AudioMetadataService`; saves `asset_audio` row
- Frontend: `GalleryComponent` checks `asset.type === 'AUDIO'`; the viewer renders `<audio controls [src]="audioStreamUrl">` with title, artist, album, duration below

**Non-Goals:**
- Editing ID3 tags via the photo manager
- Lyrics display
- BPM / key detection

## Decisions

### 1. `asset_audio` mirrors `asset_exif` pattern

**Decision:** A separate `asset_audio` table with a 1:1 FK to `assets`, same as `asset_exif`.

**Rationale:** Keeps the `assets` table lean; allows null-safe audio metadata retrieval without NULLable columns on `assets`.

### 2. Embedded album art as JPEG thumbnail

**Decision:** Extract `Artwork.getBinaryData()` from jaudiotagger; write it via `ThumbnailStorageService.saveThumbnail()` (same path as image thumbnails).

**Rationale:** Reuses the existing thumbnail infrastructure. The frontend thumbnail display code is unchanged.

### 3. Waveform fallback requires FFmpeg

**Decision:** The waveform PNG is generated only when FFmpeg is available (`video-file-support` #21). Without FFmpeg, a static placeholder icon is used.

**Rationale:** FFmpeg is optional for audio asset support. The placeholder ensures audio files are cataloged correctly even without it.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Jaudiotagger cannot parse some AAC/WAV variants | Low | Catch `CannotReadException` per file; log and store `null` metadata (asset is still cataloged) |
| Large embedded album art slows catalog | Low | Cap thumbnail at 200×150 px (same as image thumbnails) via `ImageIO` resize |
