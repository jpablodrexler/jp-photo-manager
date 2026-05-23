## 1. Dependency

- [ ] 1.1 Add `org.jaudiotagger:jaudiotagger` to `pom.xml`

## 2. Database migration

- [ ] 2.1 Create `V24__create_asset_audio.sql`: `CREATE TABLE asset_audio (id BIGSERIAL PRIMARY KEY, asset_id BIGINT NOT NULL UNIQUE REFERENCES assets(id), title VARCHAR(512) NULL, artist VARCHAR(512) NULL, album VARCHAR(512) NULL, duration_seconds INT NULL, bitrate_kbps INT NULL, sample_rate_hz INT NULL)`

## 3. AudioMetadataService

- [ ] 3.1 Create `infrastructure/service/AudioMetadataService.java` annotated with `@Service`
- [ ] 3.2 Implement `AudioMetadata extract(Path filePath)`: call `AudioFileIO.read(file)`; read `Tag` fields for title, artist, album; call `AudioHeader.getTrackLength()` for duration, `AudioHeader.getBitRateAsNumber()` for bitrate, `AudioHeader.getSampleRate()` for sample rate
- [ ] 3.3 Implement `Optional<byte[]> extractAlbumArt(Path filePath)`: read `ArtworkList`; return first artwork's binary data if present
- [ ] 3.4 Catch `CannotReadException` and log at WARN; return empty/null metadata gracefully

## 4. CatalogAssetsUseCaseImpl — audio routing

- [ ] 4.1 Add audio extensions to the accepted file list: `.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`
- [ ] 4.2 For audio files: call `audioMetadataService.extract(path)`; save `AssetAudio` entity; call `extractAlbumArt()` — if present, save as thumbnail; else attempt FFmpeg waveform if available; else use placeholder thumbnail path

## 5. Asset model update

- [ ] 5.1 Add `fileType` field to `Asset` domain model and `assets` table (`VARCHAR(10)`, values: `IMAGE`, `AUDIO`, `VIDEO`); default `IMAGE` for existing rows (migration update)

## 6. Backend unit tests

- [ ] 6.1 Test that `AudioMetadataService.extract()` correctly parses an MP3 with ID3 tags
- [ ] 6.2 Test that `extractAlbumArt()` returns `Optional.empty()` for a file without album art
- [ ] 6.3 Test that catalog routes `.mp3` files through `AudioMetadataService`

## 7. Frontend — viewer update

- [ ] 7.1 Add `fileType: 'IMAGE' | 'AUDIO' | 'VIDEO'` to the `Asset` interface
- [ ] 7.2 In the viewer component: `@if (asset.fileType === 'AUDIO')` render `<audio controls [src]="'/api/assets/' + asset.assetId + '/stream'">` and a metadata section (title, artist, album, duration, bitrate)
- [ ] 7.3 Add `AudioMetadata` interface to `core/models/audio-metadata.model.ts`

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
