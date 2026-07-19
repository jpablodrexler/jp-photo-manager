# slideshow-mode

Specifies how the gallery auto-advances through full-size images at a configurable interval, with play/pause control, keyboard shortcuts, and a CSS progress-bar countdown.

---

### Requirement: Slideshow mode auto-advances through the loaded assets

When slideshow mode is active and playing, the gallery SHALL display the current full-size image and automatically advance to the next asset after `slideshowInterval` seconds. When the last asset is reached, the slideshow SHALL stop automatically and display a "Slideshow complete" status message. The slideshow SHALL operate on the currently loaded `assets` array; it SHALL NOT trigger additional `GET /api/assets` page requests during playback.

#### Scenario: Slideshow advances to the next image

- **GIVEN** the gallery has loaded 10 assets and slideshow mode is playing at 5-second interval with `currentViewerIndex = 2`
- **WHEN** 5 seconds elapse
- **THEN** `currentViewerIndex` becomes 3; the full-size image for asset 4 is displayed

#### Scenario: Slideshow stops at the last asset

- **GIVEN** slideshow mode is playing and `currentViewerIndex` points to the last asset
- **WHEN** the interval timer fires
- **THEN** `slideshowPlaying` becomes `false`; the timer is cancelled; the status bar shows "Slideshow complete" for 3 seconds; the last asset remains displayed

#### Scenario: Slideshow does not load additional pages

- **GIVEN** the folder has 3 pages of assets; only page 0 (100 assets) is loaded
- **WHEN** the slideshow reaches asset 100 (the last loaded)
- **THEN** the slideshow stops; no `GET /api/assets?page=1` request is issued

---

### Requirement: Slideshow can be entered from both the viewer toolbar and the thumbnail grid toolbar

A "Slideshow" button (`slideshow` icon) SHALL appear in the viewer toolbar (alongside the existing zoom and grid-view buttons) and in the thumbnail grid toolbar (visible when at least one asset is loaded). Clicking the viewer toolbar button SHALL start slideshow from the currently viewed image. Clicking the thumbnail toolbar button SHALL start slideshow from the first asset (index 0).

#### Scenario: Enter slideshow from viewer toolbar

- **GIVEN** the user is in viewer mode viewing asset at index 4
- **WHEN** the user clicks the "Slideshow" button in the viewer toolbar
- **THEN** `viewMode` becomes `'slideshow'`; `currentViewerIndex` remains 4; playback starts immediately

#### Scenario: Enter slideshow from thumbnail toolbar

- **GIVEN** the user is in thumbnails mode with 10 assets loaded
- **WHEN** the user clicks the "Slideshow" button in the thumbnail toolbar
- **THEN** `viewMode` becomes `'slideshow'`; `currentViewerIndex` is 0; playback starts from the first asset

#### Scenario: Slideshow button hidden when no assets are loaded

- **GIVEN** no folder is selected and `assets` is empty
- **WHEN** the user views the thumbnail toolbar
- **THEN** the slideshow entry button is not present in the DOM

---

### Requirement: Slideshow has a play/pause toggle

The slideshow toolbar SHALL show a play/pause button. Clicking it or pressing the Space key SHALL toggle playback. When paused, the timer is cancelled and the progress bar animation freezes. When resumed, the timer is restarted from the beginning of the current image's interval.

#### Scenario: User pauses the slideshow

- **GIVEN** slideshow is playing
- **WHEN** the user clicks the pause button (or presses Space)
- **THEN** `slideshowPlaying` becomes `false`; the `setInterval` is cancelled; the progress bar animation pauses; the current image remains displayed indefinitely

#### Scenario: User resumes the slideshow

- **GIVEN** slideshow is paused
- **WHEN** the user clicks the play button (or presses Space)
- **THEN** `slideshowPlaying` becomes `true`; a new `setInterval` is started with the current `slideshowInterval`; the progress bar animation restarts from 0

---

### Requirement: The slideshow interval is configurable during playback

The slideshow toolbar SHALL include a speed selector with options 3 s, 5 s, 10 s, and 15 s (default 5 s). Changing the interval SHALL immediately cancel the current timer and restart it with the new interval so the change takes effect for the current image without waiting for the previous timer to expire.

#### Scenario: User changes the interval while playing

- **GIVEN** slideshow is playing at 10-second interval and 3 seconds have elapsed on the current image
- **WHEN** the user selects "3s" from the speed selector
- **THEN** the current timer is cancelled; a new timer starts with a 3-second interval; the progress bar resets and animates over 3 seconds; the next advance occurs 3 seconds after the change

---

### Requirement: A progress bar shows the countdown to the next advance

A thin horizontal progress bar SHALL be displayed below the current image, filling from left to right over `slideshowInterval` seconds using a CSS linear animation. The bar SHALL pause when `slideshowPlaying` is `false` and SHALL restart from 0 on each advance or when the interval is changed.

#### Scenario: Progress bar fills during playback

- **GIVEN** slideshow is playing at a 5-second interval
- **WHEN** 2.5 seconds have elapsed since the last advance
- **THEN** the progress bar is approximately 50% filled

#### Scenario: Progress bar pauses when slideshow is paused

- **GIVEN** the progress bar is 30% filled
- **WHEN** the user pauses the slideshow
- **THEN** the bar stops animating and remains at 30% until playback is resumed

#### Scenario: Progress bar resets on each advance

- **GIVEN** the slideshow advances to the next image
- **WHEN** the new image is displayed
- **THEN** the progress bar resets to 0% and begins filling again from the start

---

### Requirement: Manual navigation during slideshow pauses auto-advance

Clicking the prev/next arrow buttons or pressing ArrowLeft/ArrowRight while in slideshow mode SHALL navigate to the adjacent image and immediately pause the slideshow. The user must explicitly press play to resume auto-advance.

#### Scenario: Manual step pauses auto-advance

- **GIVEN** slideshow is playing at 5-second interval with `currentViewerIndex = 3`
- **WHEN** the user presses ArrowLeft
- **THEN** `currentViewerIndex` becomes 2; `slideshowPlaying` becomes `false`; the timer is cancelled; no further auto-advance occurs until the user resumes

#### Scenario: Manual step via next button

- **GIVEN** slideshow is playing
- **WHEN** the user clicks the right arrow button
- **THEN** `currentViewerIndex` increments; playback is paused

---

### Requirement: Keyboard shortcuts control slideshow playback

While `viewMode === 'slideshow'`, the following keyboard shortcuts SHALL be active:

| Key | Action |
|---|---|
| `Space` | Toggle play/pause |
| `ArrowLeft` | Step to previous image and pause |
| `ArrowRight` | Step to next image and pause |
| `Escape` | Exit slideshow; return to viewer mode |

#### Scenario: Escape exits slideshow

- **GIVEN** slideshow mode is active (playing or paused)
- **WHEN** the user presses Escape
- **THEN** `viewMode` becomes `'viewer'`; the timer is cancelled; `slideshowPlaying` becomes `false`; the user sees the viewer toolbar with zoom and grid-view buttons

#### Scenario: Space toggles play/pause

- **GIVEN** slideshow mode is playing
- **WHEN** the user presses Space
- **THEN** playback pauses; pressing Space again resumes playback

---

### Requirement: The slideshow timer is cancelled when the component is destroyed

`GalleryComponent.ngOnDestroy()` SHALL cancel any active slideshow `setInterval`. Navigating away from `/gallery` while a slideshow is running SHALL not leave an orphaned timer in the browser.

#### Scenario: Timer cancelled on navigation away

- **GIVEN** a slideshow is running
- **WHEN** the user navigates to a different route (e.g., `/home`)
- **THEN** `ngOnDestroy` is called; `clearInterval` is called with the timer reference; no further `advanceSlideshow` calls occur
