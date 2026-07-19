## Why

All components currently use Angular's default (`CheckAlways`) change detection strategy, which re-evaluates every component's template after every browser event. With virtual scrolling rendering up to 50 visible thumbnails, each scroll event triggers change detection on all of them. Switching all components to `ChangeDetectionStrategy.OnPush` reduces change detection to only components with changed inputs or explicit `markForCheck()` calls, eliminating unnecessary DOM diffing.

## What Changes

- Add `changeDetection: ChangeDetectionStrategy.OnPush` to all standalone components
- Replace all in-place mutable state mutations with immutable assignments (new object/array references) where template bindings need to trigger change detection
- Inject `ChangeDetectorRef` in components that update state from SSE callbacks or manual subscriptions outside Angular's zone

## Capabilities

### New Capabilities

_(none — this is a performance refactoring)_

### Modified Capabilities

- `gallery-viewer`: `GalleryComponent` and `ThumbnailComponent` gain `OnPush`, reducing re-render overhead proportional to the number of visible thumbnails
- `app-navigation`: `AppComponent` and all other feature components are updated

## Impact

- All `*.component.ts` files across `app/`, `features/`, and `shared/` — add `changeDetection: ChangeDetectionStrategy.OnPush`
- `GalleryComponent` — replace mutable `Set` operations with immutable `new Set(...)` assignments; inject `ChangeDetectorRef` for SSE callbacks
- `SyncComponent`, `ConvertComponent`, `DuplicatesComponent` — inject `ChangeDetectorRef` for SSE callbacks
- `ThumbnailComponent` — already uses `@Input()` bindings; minimal changes needed
