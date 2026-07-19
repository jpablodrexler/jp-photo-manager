# video-player

Video assets play in a right-side `MatSidenav` panel with title, metadata, progress bar, and five control buttons. Fullscreen is supported via browser-native `requestFullscreen()`. Audio fullscreen uses a custom overlay.

---

## ADDED Requirements

### Requirement: Video assets play in a right-side panel

When a video asset is played from the gallery, a right-side `MatSidenav` panel SHALL open containing `VideoPlayerComponent` with the video element and controls.

#### Scenario: Clicking play on a video asset opens the video panel

- **GIVEN** the gallery displays a video asset
- **WHEN** the user clicks "Play" on that asset
- **THEN** the right `MatSidenav` opens with `VideoPlayerComponent`; the `<video>` element begins streaming from `GET /api/assets/{id}/stream`

#### Scenario: Playing a video stops active audio playback

- **GIVEN** an audio track is currently playing via the bottom bar
- **WHEN** the user plays a video asset
- **THEN** audio playback stops and the video begins; the audio player bar no longer shows the current track

### Requirement: Video player supports fullscreen via browser-native API

The fullscreen button in `VideoPlayerComponent` SHALL invoke `HTMLVideoElement.requestFullscreen()`.

#### Scenario: Fullscreen button activates native video fullscreen

- **GIVEN** the video player is open with a video playing
- **WHEN** the user clicks the fullscreen button
- **THEN** the video fills the entire screen with the browser's native video controls overlay

### Requirement: Audio fullscreen uses a custom overlay

When the audio player's fullscreen button is clicked, a `MediaFullscreenOverlayComponent` SHALL cover the viewport with album art, metadata, and playback controls.

#### Scenario: Audio fullscreen overlay is shown

- **GIVEN** an audio track is playing in the bottom bar
- **WHEN** the user clicks the fullscreen button in the audio player
- **THEN** the `MediaFullscreenOverlayComponent` covers the viewport showing the album art, track title, artist, progress bar, and control buttons

#### Scenario: Audio fullscreen overlay is dismissed

- **GIVEN** the audio fullscreen overlay is visible
- **WHEN** the user clicks the close button
- **THEN** the overlay is dismissed and the gallery is visible behind it

### Requirement: MediaPlayerService routes play() to audio or video

`MediaPlayerService.play()` SHALL route playback to the `HTMLAudioElement` or `HTMLVideoElement` based on the asset's file extension.

#### Scenario: play() routes video assets to the video element

- **GIVEN** a `.mp4` asset is in the queue
- **WHEN** `mediaPlayerService.play()` is called
- **THEN** the `<video>` element's `src` is set and `video.play()` is called; the `<audio>` element is paused

#### Scenario: play() routes audio assets to the audio element

- **GIVEN** a `.mp3` asset is in the queue
- **WHEN** `mediaPlayerService.play()` is called
- **THEN** the `<audio>` element's `src` is set and `audio.play()` is called; the `<video>` element is paused
