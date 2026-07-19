## 1. Backend — MediaController video MIME types

- [x] 1.1 Add video MIME type mappings to `MediaController.getStream()`: `.mp4 → video/mp4`, `.mov → video/quicktime`, `.mkv → video/x-matroska`, `.avi → video/x-msvideo`, `.webm → video/webm`

## 2. Frontend — MediaPlayerService (replaces AudioPlayerService)

- [x] 2.1 Rename `core/services/audio-player.service.ts` to `media-player.service.ts`; rename class to `MediaPlayerService`
- [x] 2.2 Add `private videoEl: HTMLVideoElement | null = null`; add `registerVideoElement(el: HTMLVideoElement): void { this.videoEl = el }`
- [x] 2.3 Add `isVideoAsset(asset: Asset): boolean` — return `true` for extensions `.mp4`, `.mov`, `.mkv`, `.avi`, `.webm`
- [x] 2.4 Add `isVideoPlaying = signal(false)` and `isAudioFullscreen = signal(false)` signals
- [x] 2.5 Update `play()`: if `isVideoAsset(currentTrack())`, set `videoEl.src` and call `videoEl.play()`; pause `audio`; set `isVideoPlaying(true)`; else set `audio.src` and call `audio.play()`; pause `videoEl`; set `isVideoPlaying(false)`
- [x] 2.6 Update `stop()` to pause both elements and reset both `src` attributes
- [x] 2.7 Update all consumers of `AudioPlayerService` to inject `MediaPlayerService` instead

## 3. VideoPlayerComponent

- [x] 3.1 Create `features/gallery/video-player/video-player.component.ts` as a standalone component
- [x] 3.2 Template: `<video #videoEl [src]="streamUrl" width="100%" style="aspect-ratio:16/9; object-fit:contain">` (no `controls` attribute — custom controls below); title + metadata row; `<input type="range">` progress slider; five `MatIconButton` controls: skip_previous, stop, play/pause, skip_next, fullscreen
- [x] 3.3 `ngAfterViewInit`: call `mediaPlayerService.registerVideoElement(videoEl.nativeElement)`; wire `timeupdate` event to update `currentTime` signal via service
- [x] 3.4 Fullscreen button click: `videoEl.nativeElement.requestFullscreen()`

## 4. MediaFullscreenOverlayComponent

- [x] 4.1 Create `shared/components/media-fullscreen-overlay/media-fullscreen-overlay.component.ts` as a standalone component
- [x] 4.2 Template: `position: fixed; inset: 0; z-index: 9999; background: #000`; centered album art `<img [src]="audioPlayer.currentTrack()?.thumbnailUrl">`; title and artist in large type; full-width progress `<input type="range">`; five control buttons; close `MatIconButton` at top-right calling `mediaPlayerService.isAudioFullscreen.set(false)`
- [x] 4.3 Add fullscreen button to `AudioPlayerComponent`; clicking calls `mediaPlayerService.isAudioFullscreen.set(true)`

## 5. AppComponent layout update

- [x] 5.1 Add `<mat-sidenav position="end" [opened]="mediaPlayer.isVideoPlaying()">` containing `<app-video-player/>` to `AppComponent` template
- [x] 5.2 Add `@if (mediaPlayer.isAudioFullscreen()) { <app-media-fullscreen-overlay/> }` to `AppComponent` template
- [x] 5.3 Add "Play" button to video asset cards in the gallery; calls `mediaPlayerService.play([asset])` which routes to video panel

## 6. Backend unit tests

- [x] 6.1 Test that `MediaController.getStream()` returns `Content-Type: video/mp4` for a `.mp4` asset
- [x] 6.2 Test that `MediaController.getStream()` returns `Content-Type: video/webm` for a `.webm` asset

## 7. Frontend tests

- [x] 7.1 Add Cypress component test for `VideoPlayerComponent` verifying the `<video>` element is rendered
- [x] 7.2 Add unit test for `MediaPlayerService.isVideoAsset()` covering all supported extensions

## 8. Testing and Commit

- [x] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 8.3 Commit all changes (only after both test suites pass)
