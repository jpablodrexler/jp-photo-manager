## Why

Video assets are cataloged by `video-file-support` (#21) but clicking a video in the gallery opens the same image viewer, which cannot play video. A dedicated right-side panel video player with fullscreen support and integration with the audio player's queue system provides a complete media playback experience within the gallery.

## What Changes

- A `VideoPlayerComponent` occupies the existing `MatSidenav` right-side drawer, opening when a video asset is played
- `AudioPlayerService` is superseded by a unified `MediaPlayerService` that handles both audio and video
- `GET /api/assets/{id}/stream` (already defined in `audio-player` #65) serves video files; MIME types added for `.mp4`, `.mov`, `.mkv`, `.avi`, `.webm`
- Fullscreen: `HTMLVideoElement.requestFullscreen()` for video; a custom `MediaFullscreenOverlayComponent` for audio
- `AudioController` from #65 is renamed `MediaController` (endpoint path unchanged: `/api/assets/{id}/stream`)

## Capabilities

### New Capabilities

- `video-player`: Video assets play in a right-side `MatSidenav` panel with title, metadata, progress bar, and five control buttons. Fullscreen is supported via browser-native `requestFullscreen()`. Audio fullscreen uses a custom overlay.

### Modified Capabilities

- **`audio-player`**: `AudioPlayerService` is replaced by the unified `MediaPlayerService` which handles both audio and video routing. All existing audio player functionality is preserved.

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/MediaController.java` — add video MIME types to `GET /api/assets/{id}/stream`
- `JPPhotoManagerWeb/frontend/src/app/core/services/media-player.service.ts` — replaces `AudioPlayerService`; adds `registerVideoElement()` and `isVideoAsset()` methods
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/video-player/video-player.component.ts` — new standalone component
- `JPPhotoManagerWeb/frontend/src/app/shared/components/media-fullscreen-overlay/media-fullscreen-overlay.component.ts` — audio fullscreen overlay
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — host video player in right sidenav; update audio player to use `MediaPlayerService`
