## Context

The gallery viewer already implements `currentViewerIndex`, `viewerNext()`, `viewerPrev()`, and the `currentViewerAsset` getter. Slideshow mode is a thin automation layer on top of these: a `setInterval` that calls `viewerNext()` and a CSS animation that visualises the countdown. No new HTTP calls are needed — image URLs are already loaded via `[src]="currentViewerAsset.imageUrl"` and cached by the browser.

The existing `ViewMode` type is `'thumbnails' | 'viewer'`. Extending it to `'slideshow'` is the minimal, unambiguous approach to track which set of toolbar controls and which content block should be rendered. The template already uses `@if (viewMode === 'thumbnails')` and `@if (viewMode === 'viewer')` guards; adding `@if (viewMode === 'slideshow')` follows the same pattern.

`GalleryComponent` does not currently implement `OnDestroy`. The slideshow requires `ngOnDestroy` to cancel the `setInterval` when the user navigates away, preventing memory leaks and phantom timer callbacks.

## Goals / Non-Goals

**Goals:**

- Auto-advance full-size images at a user-selected interval (3 s, 5 s, 10 s, 15 s).
- Play/pause toggle usable via button and Space key.
- Manual step with ArrowLeft/ArrowRight during slideshow (pauses auto-advance).
- Escape key exits slideshow back to viewer mode.
- CSS progress bar visualising the countdown to the next advance.
- Stop automatically at the last asset; show a completion message.
- Timer cleanup on component destroy.

**Non-Goals:**

- Looping (wrapping from last back to first image).
- Crossfade or CSS transition animations between images.
- Keyboard shortcut customisation.
- Slideshow across multiple pages (only the loaded `assets` array is cycled).
- Saving the preferred interval in `localStorage` or user settings.

## Decisions

### 1. `'slideshow'` as a third `ViewMode` value — not a boolean flag

**Decision:** `ViewMode` becomes `'thumbnails' | 'viewer' | 'slideshow'`. Entering slideshow sets `viewMode = 'slideshow'` and entering viewer sets `viewMode = 'viewer'`. There is no separate `isSlideshowPlaying` flag that coexists with `viewMode === 'viewer'`.

**Rationale:** A third mode keeps toolbar rendering logic simple: each `@if (viewMode === '...')` branch in the template renders a fully distinct set of controls. A boolean flag on top of viewer mode would require `@if (viewMode === 'viewer' && !isSlideshow)` / `@if (viewMode === 'viewer' && isSlideshow)` everywhere, increasing cognitive load and the risk of inconsistent state.

### 2. `setInterval` managed directly in the component — no RxJS `interval`

**Decision:** `startSlideshow()` calls `this.slideshowTimer = setInterval(() => this.advanceSlideshow(), this.slideshowInterval * 1000)`. Pausing calls `clearInterval(this.slideshowTimer)`. Resuming calls `startSlideshow()` again. `ngOnDestroy` unconditionally calls `clearInterval(this.slideshowTimer)`.

**Rationale:** `setInterval` is simpler than an RxJS `interval` subscription when the timer must be cancelled and restarted on every speed change, play/pause toggle, and component destroy. An RxJS-based approach would require managing a `Subscription`, `takeUntil`, and re-subscription on restart — more moving parts for no observable benefit in this case.

### 3. CSS `@keyframes progress-fill` animation with `animation-play-state` binding

**Decision:** A `div.slideshow-progress-bar` element uses a CSS animation:

```css
@keyframes progress-fill {
  from { width: 0; }
  to   { width: 100%; }
}
.slideshow-progress-bar {
  height: 3px;
  background: #4caf50;
  animation-name: progress-fill;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}
```

The template binds:
```html
<div class="slideshow-progress-bar"
     [style.animation-duration]="slideshowInterval + 's'"
     [style.animation-play-state]="slideshowPlaying ? 'running' : 'paused'"
     [style.animation-iteration-count]="1"
     #progressBar>
</div>
```

On each advance, the progress bar element is reset by using Angular's `@ViewChild` to briefly remove and re-add the animation class, or by toggling a boolean that changes the element key with `@if` / `@else`.

**Rationale:** A pure-CSS animation driven by the same `slideshowInterval` value that controls the `setInterval` guarantees visual and functional synchronisation without a separate `requestAnimationFrame` loop. `animation-play-state` is the idiomatic CSS hook for play/pause.

**Progress bar reset strategy:** Wrapping the progress bar in `@if (slideshowPlaying)` ensures the element is destroyed and re-created on each play/pause cycle, which resets the animation. On each `advanceSlideshow()` call, a `resetProgress` flag is toggled: `@if (resetProgress)` / `@else` renders the same bar, replacing the DOM node and restarting the animation.

### 4. Manual step during slideshow pauses auto-advance

**Decision:** `viewerPrev()` and `viewerNext()` calls during slideshow (via button or ArrowLeft/ArrowRight) call `pauseSlideshow()` first. The user must press play or the play button to resume. An explicit `stepSlideshow(direction: -1 | 1)` method handles this to avoid duplicating pause logic across key handler and button click handler.

**Rationale:** Restarting the interval after a manual step would make it hard for the user to control the flow — the image would advance again after `slideshowInterval` seconds regardless of what they just did. Pausing on manual step is the least-surprising behaviour.

### 5. Stop at last image — no loop

**Decision:** `advanceSlideshow()` checks `currentViewerIndex >= assets.length - 1`. If true, it calls `stopSlideshow()` and sets `statusMessage = 'Slideshow complete'` (cleared after 3 s). The user remains on the last image in slideshow mode (paused).

**Rationale:** Looping could cause images to cycle indefinitely without user attention, wasting resources. Stopping at the end is the safer default and is consistent with most photo viewer slideshows. The user can manually navigate back or re-enter slideshow from the first image.

### 6. Keyboard shortcuts handled by `@HostListener('keydown')` — guarded by `viewMode`

**Decision:** A `@HostListener('keydown', ['$event']) onKeyDown(event: KeyboardEvent)` method is added to `GalleryComponent`. Inside, a `switch` on `event.key` handles:
- `' '` (Space): `toggleSlideshowPlay()`; `event.preventDefault()` to prevent page scroll
- `'ArrowLeft'`: `stepSlideshow(-1)`
- `'ArrowRight'`: `stepSlideshow(1)` / `advanceSlideshow()`
- `'Escape'`: `exitSlideshow()` (returns to viewer mode, stops timer)

Each case is guarded: Space and Escape only fire when `viewMode === 'slideshow'`; ArrowLeft/ArrowRight fire in both `'viewer'` and `'slideshow'` modes (consistent with existing viewer navigation expectations).

**Rationale:** `@HostListener` on the component catches keyboard events while the gallery is the active view without requiring a global `document` listener that must be manually removed. Guarding by `viewMode` prevents slideshow keys from accidentally firing during thumbnail browsing.

## Data Flow

```
User clicks "Slideshow" from viewer toolbar
  → startSlideshow(currentViewerIndex)
    → viewMode = 'slideshow'
    → slideshowPlaying = true
    → slideshowTimer = setInterval(advanceSlideshow, interval * 1000)
    → progress bar animation starts

Timer fires → advanceSlideshow()
  → if currentViewerIndex < assets.length - 1:
      currentViewerIndex++; viewerZoom = 1; resetProgress toggle
  → else:
      stopSlideshow(); statusMessage = 'Slideshow complete'

User presses Space
  → toggleSlideshowPlay()
    → if playing: clearInterval → slideshowPlaying = false
    → if paused:  setInterval  → slideshowPlaying = true

User presses ArrowRight
  → stepSlideshow(1)
    → pauseSlideshow()
    → viewerNext()

User presses Escape
  → exitSlideshow()
    → stopSlideshow()
    → viewMode = 'viewer'

Component destroyed
  → ngOnDestroy() → clearInterval(slideshowTimer)
```

## File Change List

**New files:**

- `frontend/src/app/features/gallery/gallery.component.cy.ts` *(additions to existing file)* — new slideshow test cases

**Modified files:**

- `frontend/src/app/features/gallery/gallery.component.ts` — extend `ViewMode`; add slideshow state and methods; `@HostListener`; `ngOnDestroy`
- `frontend/src/app/features/gallery/gallery.component.html` — slideshow toolbar block; slideshow viewer block with progress bar; "Slideshow" entry button in viewer and thumbnails toolbars
- `frontend/src/app/features/gallery/gallery.component.scss` — `.slideshow-progress-bar`, `.slideshow-controls`, `@keyframes progress-fill`
