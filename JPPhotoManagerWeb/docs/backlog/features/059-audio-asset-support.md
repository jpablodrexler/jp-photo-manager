# Feature 59 — audio-asset-support

**As a** photo library owner, **I want** audio files cataloged with their metadata and playable in the viewer, **so that** my music lives next to my photos and videos.

## Acceptance Criteria
- AC1: Given .mp3, .flac, .wav, .aac and .ogg files, when a folder is cataloged, then they are indexed with title, artist, album, duration, bitrate and sample rate.
- AC2: Given a file without tags, when it is cataloged, then it is stored with null metadata.
- AC3: Given embedded album art, when a file is cataloged, then it becomes the thumbnail; with none, a waveform image is generated.
- AC4: Given the viewer, when I open an audio asset, then playback controls and its metadata are shown.

## Technical notes

Catalog audio files (.mp3, .flac, .wav, .aac, .ogg); extract ID3/Vorbis/FLAC metadata (title, artist, album, duration, bitrate, sample rate, embedded album art) via `org.jaudiotagger:jaudiotagger` (Maven); store audio-specific fields in a new `asset_audio` table mirroring `asset_exif` (Flyway migration); use embedded album art as the asset thumbnail if present, or generate a waveform PNG via FFmpeg (`ffmpeg -i input.mp3 -filter_complex "showwavespic=s=200x150" -frames:v 1 waveform.png`) as fallback; the frontend viewer switches on asset type and renders an `<audio controls>` tag for audio assets with title, artist, album, duration, and bitrate displayed alongside playback controls

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
