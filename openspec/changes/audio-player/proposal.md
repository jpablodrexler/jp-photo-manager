## Why

Audio assets are cataloged by `audio-asset-support` (#59) but there is no way to play them continuously while browsing the gallery. A persistent bottom-bar audio player that survives route navigation lets users listen to music while browsing photos, just as they would in a dedicated music player.

## What Changes

- A persistent `AudioPlayerComponent` is fixed at the bottom of `AppComponent`, visible when a track is loaded
- A singleton `AudioPlayerService` wraps a private `HTMLAudioElement` and exposes Angular signals for playback state
- Three queue modes: single asset, folder (play all audio in folder), playlist (parse `.m3u`/`.m3u8`/`.pls`)
- A new backend endpoint `GET /api/assets/{id}/stream` returns the audio file with `Accept-Ranges` support for seeking
- Playlist parsing via `GET /api/audio/playlist/{assetId}` handled by two `PlaylistParserPort` adapters

## Capabilities

### New Capabilities

- `audio-player`: A persistent bottom-bar audio player allows continuous playback while browsing. Supports single track, folder queue, and playlist files. Playback state (current track, progress) persists across route navigation.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/MediaController.java` — new controller with `/api/assets/{id}/stream` and `/api/audio/playlist/{id}`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/out/playlist/PlaylistParserPort.java` — domain interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/adapter/out/playlist/` — M3U and PLS adapters
- `JPPhotoManagerWeb/backend/src/test/` — tests for playlist parsing and streaming
- `JPPhotoManagerWeb/frontend/src/app/core/services/audio-player.service.ts` — singleton service with signals
- `JPPhotoManagerWeb/frontend/src/app/shared/components/audio-player/audio-player.component.ts` — bottom-bar component
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — host audio player component
