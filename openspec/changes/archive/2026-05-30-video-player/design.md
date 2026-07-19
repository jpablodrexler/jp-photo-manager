## Context

The `AppComponent` template uses `mat-sidenav-container` with a left drawer for folder navigation. A second `<mat-sidenav position="end">` can hold `VideoPlayerComponent`. `MediaPlayerService` extends the existing `AudioPlayerService` design: it holds a private `HTMLAudioElement` for audio and receives a reference to `VideoPlayerComponent`'s `HTMLVideoElement` via `registerVideoElement()`. The `isVideoAsset(asset)` method checks the file extension to route `play()` calls.

## Goals / Non-Goals

**Goals:**
- `MediaPlayerService` replaces `AudioPlayerService`: adds `registerVideoElement(el: HTMLVideoElement)`, `isVideoAsset(asset: Asset): boolean` (checks `.mp4`, `.mov`, `.mkv`, `.avi`, `.webm`); `play()` routes to audio or video element; stopping one stops the other
- `VideoPlayerComponent`: fills right `MatSidenav`; `<video [src] width="100%" style="aspect-ratio:16/9">` with `object-fit: contain`; title + metadata row below; progress bar (`<input type="range">`); five buttons: skip_previous, stop, play/pause, skip_next, fullscreen; `ngAfterViewInit` calls `mediaPlayerService.registerVideoElement(videoEl.nativeElement)`; fullscreen button calls `videoEl.nativeElement.requestFullscreen()`
- `MediaFullscreenOverlayComponent`: `position: fixed; inset: 0; z-index: 9999; background: #000`; shows album art centered, track title/artist in large type, progress bar, controls, close button at top-right; toggled by `isAudioFullscreen` signal on `MediaPlayerService`
- `AppComponent`: add `<mat-sidenav position="end">` containing `VideoPlayerComponent`; open/close by `mediaPlayerService.isVideoPlaying()` signal
- Backend `MediaController`: add video MIME types: `.mp4 → video/mp4`, `.mov → video/quicktime`, `.mkv → video/x-matroska`, `.avi → video/x-msvideo`, `.webm → video/webm`

**Non-Goals:**
- Picture-in-picture (PiP) mode
- Subtitle/caption support
- Playback speed control (a future enhancement)

## Decisions

### 1. Unified `MediaPlayerService` replaces `AudioPlayerService`

**Decision:** Rename `AudioPlayerService` to `MediaPlayerService` and extend it. Existing audio player consumers (`AudioPlayerComponent`) are updated to inject `MediaPlayerService` instead — no interface change.

**Rationale:** All queue state, playback signals, and control methods are shared between audio and video. Duplicating them in a separate service would require synchronisation logic.

### 2. Browser-native fullscreen for video

**Decision:** `HTMLVideoElement.requestFullscreen()` — the browser renders the video fullscreen with its native controls overlay.

**Rationale:** Browser-native fullscreen for video is universally supported and requires no custom overlay. The native controls are familiar to users.

### 3. Custom overlay for audio fullscreen

**Decision:** `MediaFullscreenOverlayComponent` is a fixed-position Angular component overlaid in `AppComponent`, toggled by an `isAudioFullscreen` signal.

**Rationale:** Audio files have no video element to fullscreen. A custom overlay shows album art, metadata, and controls in a cinema-mode style, consistent with standalone music players.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Right sidenav conflicts with existing layout | Low | The existing left sidenav is unaffected; the right sidenav can be sized independently |
| Video streaming without `Accept-Ranges` causes no seeking | Medium | Already addressed in #65 with `ResourceRegion` streaming |
