# Feature 66 — video-player

**As a** photo library owner, **I want** a video player pane and fullscreen for video and audio, **so that** I watch or listen to media without leaving the library.

## Acceptance Criteria
- AC1: Given a video asset, when I press play, then the video panel opens at the right; playing a video stops active audio.
- AC2: Given a video, when I press fullscreen, then native fullscreen activates.
- AC3: Given audio, when I press fullscreen, then a fullscreen overlay shows and can be dismissed.
- AC4: Given the media player service, when play is called, then video assets are routed to the video element and audio assets to the audio element by extension.
- AC5: Given streaming, when media is requested, then the unified stream endpoint serves both types.

## Technical notes

Right-side video player pane and fullscreen support for both video and audio; `VideoPlayerComponent` occupies a `MatSidenav` with `position="end"`; unified `MediaPlayerService` (`providedIn: 'root'`) replaces `AudioPlayerService` and manages both media types via an internal `HTMLAudioElement` for audio and a reference to `VideoPlayerComponent`'s `HTMLVideoElement` registered via `registerVideoElement()`; `isVideoAsset()` routes `play()` calls by file extension; video fullscreen uses `HTMLVideoElement.requestFullscreen()`; audio fullscreen uses a custom `MediaFullscreenOverlayComponent`; unified streaming endpoint `GET /api/assets/{id}/stream` replaces the audio-only `/audio` endpoint; `AudioController` renamed `MediaController`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
