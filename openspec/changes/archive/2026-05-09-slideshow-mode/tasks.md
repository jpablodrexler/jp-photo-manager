## 1. GalleryComponent — State and lifecycle

- [x] 1.1 Extend the `ViewMode` type: `type ViewMode = 'thumbnails' | 'viewer' | 'slideshow'`
- [x] 1.2 Add slideshow state fields:
  ```typescript
  slideshowInterval = 5;  // seconds
  slideshowPlaying = false;
  private slideshowTimer: ReturnType<typeof setInterval> | null = null;
  slideshowResetTick = false; // toggled on each advance to trigger progress bar DOM reset
  ```
- [x] 1.3 Add `readonly intervalOptions = [3, 5, 10, 15]` for the speed selector
- [x] 1.4 Add `implements OnDestroy` to the class declaration; import `OnDestroy`, `HostListener` from `@angular/core`
- [x] 1.5 Implement `ngOnDestroy()`: call `this.stopSlideshow()`
- [x] 1.6 Add `@HostListener('keydown', ['$event']) onKeyDown(event: KeyboardEvent)`:
  - `'Escape'`: if `viewMode === 'slideshow'`, call `exitSlideshow()`; `event.preventDefault()`
  - `' '` (Space): if `viewMode === 'slideshow'`, call `toggleSlideshowPlay()`; `event.preventDefault()`
  - `'ArrowLeft'`: if `viewMode === 'slideshow'`, call `stepSlideshow(-1)`; if `viewMode === 'viewer'`, call `viewerPrev()`; `event.preventDefault()`
  - `'ArrowRight'`: if `viewMode === 'slideshow'`, call `stepSlideshow(1)`; if `viewMode === 'viewer'`, call `viewerNext()`; `event.preventDefault()`

## 2. GalleryComponent — Slideshow methods

- [x] 2.1 Add `startSlideshow(index: number)`:
  ```typescript
  startSlideshow(index: number): void {
    this.currentViewerIndex = index;
    this.viewerZoom = 1;
    this.viewMode = 'slideshow';
    this.slideshowPlaying = true;
    this.slideshowTimer = setInterval(() => this.advanceSlideshow(), this.slideshowInterval * 1000);
  }
  ```
- [x] 2.2 Add `advanceSlideshow()`:
  ```typescript
  advanceSlideshow(): void {
    if (this.currentViewerIndex < this.assets.length - 1) {
      this.currentViewerIndex++;
      this.viewerZoom = 1;
      this.slideshowResetTick = !this.slideshowResetTick;
    } else {
      this.stopSlideshow();
      this.statusMessage = 'Slideshow complete';
      setTimeout(() => { this.statusMessage = ''; }, 3000);
    }
  }
  ```
- [x] 2.3 Add `pauseSlideshow()`: if `slideshowTimer` is set, call `clearInterval(slideshowTimer)`, set `slideshowTimer = null`, set `slideshowPlaying = false`
- [x] 2.4 Add `resumeSlideshow()`: set `slideshowPlaying = true`; set `slideshowTimer = setInterval(() => this.advanceSlideshow(), this.slideshowInterval * 1000)`
- [x] 2.5 Add `toggleSlideshowPlay()`: if `slideshowPlaying`, call `pauseSlideshow()`; else call `resumeSlideshow()`
- [x] 2.6 Add `stopSlideshow()`: call `pauseSlideshow()` (clears interval); set `slideshowPlaying = false`
- [x] 2.7 Add `exitSlideshow()`: call `stopSlideshow()`; set `viewMode = 'viewer'`
- [x] 2.8 Add `stepSlideshow(direction: -1 | 1)`: call `pauseSlideshow()`; if `direction === -1`, call `viewerPrev()`; else call `viewerNext()`
- [x] 2.9 Add `onIntervalChange()`: if currently playing, call `pauseSlideshow()` then `resumeSlideshow()` to restart the timer with the new interval; also toggle `slideshowResetTick` to reset the progress bar

## 3. GalleryComponent — Template additions

- [x] 3.1 In the `<mat-toolbar>` block, add a new branch for slideshow toolbar controls:
  ```html
  @if (viewMode === 'slideshow') {
    <button mat-icon-button (click)="toggleSlideshowPlay()" [title]="slideshowPlaying ? 'Pause' : 'Play'">
      <mat-icon>{{ slideshowPlaying ? 'pause' : 'play_arrow' }}</mat-icon>
    </button>
    <mat-select [(ngModel)]="slideshowInterval" (ngModelChange)="onIntervalChange()" style="width: 80px;" title="Interval">
      @for (opt of intervalOptions; track opt) {
        <mat-option [value]="opt">{{ opt }}s</mat-option>
      }
    </mat-select>
    <button mat-icon-button (click)="exitSlideshow()" title="Exit slideshow">
      <mat-icon>close</mat-icon>
    </button>
  }
  ```
- [x] 3.2 In the viewer toolbar block (`@if (viewMode === 'viewer')`), add a slideshow entry button before the existing close button:
  ```html
  <button mat-icon-button (click)="startSlideshow(currentViewerIndex)" title="Slideshow">
    <mat-icon>slideshow</mat-icon>
  </button>
  ```
- [x] 3.3 In the thumbnails toolbar block, add a slideshow entry button (visible when `assets.length > 0`):
  ```html
  @if (assets.length > 0) {
    <button mat-icon-button (click)="startSlideshow(0)" title="Start slideshow">
      <mat-icon>slideshow</mat-icon>
    </button>
  }
  ```
- [x] 3.4 Add the slideshow viewer content block after the existing viewer block:
  ```html
  @if (viewMode === 'slideshow' && currentViewerAsset) {
    <div class="image-viewer flex-1">
      <button mat-icon-button class="viewer-nav viewer-nav--prev" (click)="stepSlideshow(-1)" [disabled]="currentViewerIndex === 0">
        <mat-icon>chevron_left</mat-icon>
      </button>
      <img
        [src]="currentViewerAsset.imageUrl"
        [alt]="currentViewerAsset.fileName"
      />
      <button mat-icon-button class="viewer-nav viewer-nav--next" (click)="stepSlideshow(1)" [disabled]="currentViewerIndex >= assets.length - 1">
        <mat-icon>chevron_right</mat-icon>
      </button>
    </div>
    @if (slideshowResetTick) {
      <div class="slideshow-progress-bar"
           [style.animation-duration]="slideshowInterval + 's'"
           [style.animation-play-state]="slideshowPlaying ? 'running' : 'paused'">
      </div>
    } @else {
      <div class="slideshow-progress-bar"
           [style.animation-duration]="slideshowInterval + 's'"
           [style.animation-play-state]="slideshowPlaying ? 'running' : 'paused'">
      </div>
    }
  }
  ```
  Note: the `@if` / `@else` on `slideshowResetTick` renders the same element in both branches so Angular replaces the DOM node on each tick, restarting the CSS animation

## 4. GalleryComponent — Styles

- [x] 4.1 Add to `gallery.component.scss`:
  ```scss
  @keyframes progress-fill {
    from { width: 0; }
    to   { width: 100%; }
  }

  .slideshow-progress-bar {
    height: 3px;
    background: #4caf50;
    width: 0;
    animation-name: progress-fill;
    animation-timing-function: linear;
    animation-fill-mode: forwards;
    animation-iteration-count: 1;
  }
  ```

## 5. Frontend — Tests

- [x] 5.1 In `gallery.component.cy.ts`, add test: mount `GalleryComponent` with stubbed `AssetService` returning 3 assets; click the "Slideshow" button in the thumbnails toolbar; assert `viewMode === 'slideshow'`; assert the play/pause button is visible; assert the progress bar element exists
- [x] 5.2 Add test: while in slideshow mode, use `cy.clock()`/`cy.tick(5000)` to simulate one interval expiry; assert `currentViewerIndex` advanced to 1
- [x] 5.3 Add test: click the pause button while playing; assert timer is not advancing (tick 5000 ms; assert index unchanged)
- [x] 5.4 Add test: press Escape key while in slideshow mode; assert `viewMode === 'viewer'`
- [x] 5.5 Add test: press Space key while in slideshow mode; assert play/pause toggles
- [x] 5.6 Add test: when slideshow reaches the last asset and advances, assert `slideshowPlaying === false` and `statusMessage === 'Slideshow complete'`
- [x] 5.7 Add test: click the "Slideshow" button in the viewer toolbar; assert `viewMode === 'slideshow'` and `currentViewerIndex` is the same as before
- [x] 5.8 Run `npm test` and confirm all tests pass
