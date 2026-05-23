## Why

The application is fully navigable by mouse but offers no keyboard-first workflow. Power users — photographers reviewing hundreds of assets per session — must reach for the mouse to switch routes, rate the current image, soft-delete selections, or search. The gallery's viewer mode already handles Space/←/→ for slideshow control, but those bindings are local to one component and undiscoverable. Providing a documented, consistent set of global shortcuts reduces context-switching overhead and aligns the web application with the behaviour users expect from native photo-management tools.

## What Changes

- **Frontend — `KeyboardService`:** a new `core/services/keyboard.service.ts` singleton (`providedIn: 'root'`) that subscribes to `fromEvent<KeyboardEvent>(document, 'keydown')` on initialisation. All shortcut logic lives here, keeping components thin. The service suppresses shortcuts when focus is inside `<input>`, `<textarea>`, or any element with a `[contenteditable]` attribute so that typing in filter fields, dialog boxes, and rename inputs is unaffected.
- **Frontend — navigation shortcuts:** `G` navigates to `/gallery`; `A` navigates to `/albums`; `D` navigates to `/duplicates`. These work from any authenticated page.
- **Frontend — rating shortcut:** while the gallery viewer or thumbnails mode is active, pressing `1`–`5` calls `GalleryComponent.rateCurrentAsset()` (viewer) or rates the single selected asset (thumbnails mode). `0` clears the rating.
- **Frontend — soft-delete shortcut:** `Del` (Delete key) triggers the same soft-delete flow as the existing "Remove from catalog" toolbar action for whichever assets are currently selected in the gallery. The shortcut is a no-op when no assets are selected.
- **Frontend — search-focus shortcut:** `/` focuses the search `<input>` inside `GalleryComponent` and calls `event.preventDefault()` to suppress the browser's native find-in-page shortcut. The service emits on a dedicated `focusSearch$` subject; `GalleryComponent` subscribes and calls `searchInput.nativeElement.focus()`.
- **Frontend — help overlay:** `?` opens a full-screen Angular Material dialog (`KeyboardShortcutsHelpComponent`) listing all registered bindings in a two-column table. The overlay can be dismissed with `Escape` or by clicking outside it.
- **Frontend — `AppComponent` integration:** `AppComponent` injects `KeyboardService` and calls `service.init(router)` in `ngOnInit()`, passing the `Router` instance so the service can issue navigation commands. The service is torn down via `takeUntilDestroyed`.

## Capabilities

### New Capabilities

- `global-keyboard-shortcuts`: Authenticated users can navigate between main routes, open the search field, rate the current asset, soft-delete selected assets, and view all shortcuts — all without leaving the keyboard.
- `keyboard-shortcuts-help-overlay`: Pressing `?` opens a modal listing every available shortcut, its scope (global vs. gallery), and a brief description.

### Modified Capabilities

- `gallery-viewer`: Rating and soft-delete actions that were previously mouse-only are now also triggerable by keyboard shortcuts when the gallery is the active route. The viewer's existing Space/←/→/Escape slideshow shortcuts are unchanged but are now listed in the help overlay.

## Impact

- `JPPhotoManagerWeb/frontend/src/app/core/services/keyboard.service.ts` — new file (the central shortcut dispatcher).
- `JPPhotoManagerWeb/frontend/src/app/core/services/keyboard.service.cy.ts` — new Cypress component tests.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/keyboard-shortcuts-help.component.ts` — new standalone Angular Material dialog component.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/keyboard-shortcuts-help.component.html` — template for the help overlay.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/keyboard-shortcuts-help.component.scss` — styles for the help overlay.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/keyboard-shortcuts-help/keyboard-shortcuts-help.component.cy.ts` — Cypress component tests.
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — inject `KeyboardService`, call `service.init(router)` in `ngOnInit()`.
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.ts` — subscribe to `KeyboardService.rateSelected$`, `KeyboardService.deleteSelected$`, and `KeyboardService.focusSearch$`; expose `@ViewChild('searchInput')` so the focus subscription can call `focus()`.
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.html` — add `#searchInput` template reference on the search `<input>`.
- No backend changes. No database migrations.
