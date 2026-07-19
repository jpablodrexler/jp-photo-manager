## Why

The gallery's viewer mode lets users navigate through full-size images with prev/next buttons, but requires manual input for every transition. When reviewing a large set of photos from an event — checking focus, composition, or chronological flow — constant manual clicking is tedious and interrupts the viewing experience. A slideshow mode that auto-advances through the current folder's images at a configurable interval transforms the gallery into a passive presentation tool, matching the behaviour users expect from photo management software.

## What Changes

- Extend `ViewMode` type in `GalleryComponent` from `'thumbnails' | 'viewer'` to `'thumbnails' | 'viewer' | 'slideshow'`.
- Add slideshow state to `GalleryComponent`: `slideshowInterval: number` (seconds, default 5), `slideshowPlaying = false`, `private slideshowTimer: ReturnType<typeof setInterval> | null = null`.
- Add methods `startSlideshow(index: number)`, `toggleSlideshowPlay()`, `stopSlideshow()`, `advanceSlideshow()` (calls `viewerNext()` or stops at last image).
- Add `ngOnDestroy` to `GalleryComponent` to cancel any running timer when the component is destroyed.
- Add `@HostListener('keydown', ['$event'])` to handle Space (play/pause), ArrowLeft/ArrowRight (manual step), and Escape (exit slideshow → viewer mode) while in slideshow mode.
- Add a `mat-icon-button` with icon `slideshow` to the viewer toolbar to enter slideshow mode from the image viewer; add a separate toolbar button in the thumbnails toolbar to enter slideshow starting at index 0.
- Add a slideshow-specific toolbar row in `viewMode === 'slideshow'` with: play/pause toggle, interval speed selector (options: 3 s, 5 s, 10 s, 15 s), and an exit button returning to viewer mode.
- Add a CSS-animated progress bar beneath the image that fills from left to right over `slideshowInterval` seconds and resets on each advance; implemented as a `div.slideshow-progress-bar` whose `animation-duration` is bound to `slideshowInterval + 's'` and whose `animation-play-state` is bound to `slideshowPlaying ? 'running' : 'paused'`.
- When the slideshow reaches the last asset and auto-advances, it stops automatically and shows a brief "Slideshow complete" status message; the user remains on the last image.
- No backend changes are required — the slideshow consumes only the asset image URLs already returned by `GET /api/assets`.

## Capabilities

### New Capabilities

- `slideshow-mode`: Auto-advance through a folder's full-size images at a user-configurable interval (3 s, 5 s, 10 s, 15 s) with play/pause control, keyboard shortcuts, and a progress-bar countdown per image.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`gallery.component.ts`**: `ViewMode` gains `'slideshow'`; new slideshow state fields and methods; `ngOnDestroy` cleans up the timer; `@HostListener('keydown')` handles slideshow keyboard shortcuts; existing `openViewer`, `viewerNext`, `viewerPrev` remain unchanged.
- **`gallery.component.html`**: new `@if (viewMode === 'slideshow')` section in the main toolbar (play/pause, speed, exit); new `@if (viewMode === 'slideshow' && currentViewerAsset)` viewer block with the progress bar; new "Slideshow" button added to the viewer toolbar alongside existing zoom and grid-view buttons; new "Slideshow" button in the thumbnails toolbar when `assets.length > 0`.
- **`gallery.component.scss`**: new `.slideshow-progress-bar` and `.slideshow-controls` rules; CSS keyframe animation `@keyframes progress-fill` for the countdown bar.
- **No new Angular Material modules** — uses `MatButtonModule`, `MatIconModule`, `MatSelectModule` already imported.
- **No backend changes** — purely frontend.
- **No new routes** — slideshow is a mode within the existing `/gallery` route.
