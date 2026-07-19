## Context

The gallery component already handles `keydown` events for slideshow control via a `@HostListener("keydown", ["$event"])` method on `GalleryComponent`. This works for the slideshow because the component is mounted and focused when slideshow mode is active, but it does not extend to other routes or to shortcuts that must fire regardless of which element has DOM focus.

There is no shared keyboard abstraction. If additional shortcut-heavy features were added in the same ad-hoc way, every feature component would independently register conflicting `@HostListener` or `document.addEventListener` calls, making it impossible to detect conflicts or list all active bindings in one place.

The routes in scope are `/gallery`, `/albums`, `/duplicates`, `/home`, `/sync`, `/convert`, and `/admin/users`. Navigation shortcuts must work from all of them. Rating and delete shortcuts are meaningful only in the gallery. The search-focus shortcut is meaningful only in the gallery where a visible search `<input>` exists.

The help overlay must present bindings in a discoverable, readable format. It should not require the user to open any menu; pressing `?` from anywhere should reveal the full reference.

## Goals / Non-Goals

**Goals:**

- A single `KeyboardService` (`providedIn: 'root'`) that owns all `document`-level `keydown` subscriptions.
- Suppression of shortcuts when focus is inside `<input>`, `<textarea>`, or `[contenteditable]`.
- Navigation shortcuts: `G` → `/gallery`, `A` → `/albums`, `D` → `/duplicates`.
- Rating shortcuts: `0`–`5` rate (or clear) the currently selected or viewed asset while on the gallery route.
- Delete shortcut: `Del` soft-deletes selected assets while on the gallery route.
- Search-focus shortcut: `/` focuses the gallery search input and suppresses the browser find-in-page shortcut.
- Help overlay: `?` opens a full-screen Material dialog listing all bindings.
- `GalleryComponent` subscribes to service subjects to execute gallery-scoped actions; the service itself does not import `GalleryComponent`.
- All existing slideshow shortcuts (Space, ←, →, Escape) remain in `GalleryComponent` and are unaffected.

**Non-Goals:**

- Custom user-configurable keybinding remapping.
- Shortcuts for the `/sync`, `/convert`, or `/admin/users` routes in this change.
- Shortcut handling in the login page (`/login`) — the guard prevents service initialisation until authenticated.
- Mobile / touch device shortcut handling.
- Backend changes of any kind.

## Decisions

### 1. Central `KeyboardService` using `fromEvent` on `document`

**Decision:** Create a singleton `KeyboardService` in `core/services/`. On `init(router: Router)`, subscribe to `fromEvent<KeyboardEvent>(document, 'keydown').pipe(takeUntil(this.destroy$))`. Route navigation is performed by calling `router.navigateByUrl(path)`. Gallery-scoped actions are emitted via `Subject` properties (`rateSelected$: Subject<number>`, `deleteSelected$: Subject<void>`, `focusSearch$: Subject<void>`) that `GalleryComponent` subscribes to in `ngOnInit`.

**Rationale:** A single `document`-level subscription avoids duplicate event listeners and provides a central place to check guard conditions (focus inside input, authentication). Emitting subjects instead of calling component methods keeps the service decoupled from any component. The `Router` is injected by the caller (`AppComponent`) so the service remains constructable without routing infrastructure in tests.

**Alternative considered:** Adding more `@HostListener` bindings directly to each component — rejected because it scatters the shortcut registry across the codebase, makes conflict detection impossible, and cannot support a centralised help overlay.

### 2. Input-field suppression via `event.target` inspection

**Decision:** At the top of the keydown handler, check:

```typescript
const target = event.target as HTMLElement;
if (
  target instanceof HTMLInputElement ||
  target instanceof HTMLTextAreaElement ||
  target.isContentEditable
) {
  return;
}
```

This guard runs before any shortcut is evaluated.

**Rationale:** This is the standard browser pattern for "application shortcuts that must not interfere with typing". Checking `instanceof` is reliable and does not require querying the DOM tree. `isContentEditable` covers `[contenteditable]` elements used by rich-text widgets such as the Angular Material chip autocomplete.

**Alternative considered:** Checking `document.activeElement.closest('mat-form-field')` — too fragile; it would suppress shortcuts whenever focus is anywhere inside a Material form field, including clickable checkboxes and select dropdowns that do not accept text input.

### 3. Gallery-scoped subjects instead of route guards in the service

**Decision:** The service emits on `rateSelected$`, `deleteSelected$`, and `focusSearch$` whenever the matching key is pressed (and the input-suppression guard passes). It does not check the current route. `GalleryComponent` subscribes to these subjects in `ngOnInit` and unsubscribes in `ngOnDestroy` via `takeUntil(this.destroy$)`. When the user navigates away from the gallery the subscriptions are automatically torn down, so key presses on other routes emit into subjects with no active observers — effectively a no-op.

**Rationale:** Letting the component manage its own subscription lifetime is idiomatic Angular. It avoids coupling the service to the router URL for action dispatch, keeps the service responsibilities narrow (emit events), and means components remain testable in isolation by providing a stub `KeyboardService`.

**Alternative considered:** Having the service call `router.url` inside the handler and dispatch actions itself — rejected because it requires the service to import component types and breaks the dependency direction.

### 4. `?` key opens `KeyboardShortcutsHelpComponent` via `MatDialog`

**Decision:** `KeyboardService` injects `MatDialog` and calls `dialog.open(KeyboardShortcutsHelpComponent, { width: '640px', maxHeight: '90vh' })` when `?` is pressed. `KeyboardShortcutsHelpComponent` is a standalone Angular Material dialog component living in `shared/components/keyboard-shortcuts-help/`. It displays a static table of bindings grouped by scope (Global, Gallery).

**Rationale:** `MatDialog` is already a project dependency used throughout the codebase. Opening the dialog from `KeyboardService` means the `?` shortcut works on every route without each component needing to open it. A static table is sufficient for the initial implementation; a dynamic registry can be layered on later if needed.

**Alternative considered:** A slide-in side drawer overlay — rejected because a dialog is simpler to implement, consistent with the project's existing dialog usage, and naturally dismissable with Escape.

### 5. `AppComponent` owns service initialisation

**Decision:** `AppComponent.ngOnInit()` calls `this.keyboardService.init(this.router)`. The service uses `takeUntilDestroyed(this.destroyRef)` (or a `Subject` + `takeUntil`) internally to clean up when the app component is destroyed (i.e. on full page unload).

**Rationale:** `AppComponent` is the root shell and is always mounted when the user is authenticated. It already holds a reference to `Router`, making it the natural place to pass the router reference into the service. This also avoids the service itself injecting `Router` in the constructor — keeping the service's constructor dependencies minimal and its unit tests simpler.

**Alternative considered:** Calling `service.init()` from an `APP_INITIALIZER` token — adds complexity and makes it harder to pass the `Router` reference without circular injection.

### 6. Existing slideshow shortcuts remain in `GalleryComponent`

**Decision:** The Space, ←, →, and Escape shortcuts handled by `GalleryComponent.onKeyDown()` are not migrated to `KeyboardService`. They remain unchanged.

**Rationale:** Those shortcuts depend on component-local state (`viewMode`, `slideshowPlaying`) and are only relevant when the gallery is rendered, so they do not benefit from centralisation. Migrating them would increase scope and risk regression in the already-working slideshow feature. Their bindings are added to the help overlay's "Gallery" section as documentation only.

## Risks / Trade-offs

**`/` suppresses browser find-in-page on Firefox/Chrome**
→ Calling `event.preventDefault()` on `key === '/'` prevents the native browser search shortcut while any gallery page is loaded. This is intentional and matches the behaviour of established web applications (GitHub, Gmail). The `?` overlay makes the trade-off discoverable.

**Rating shortcut fires while the gallery is mounted but viewer is closed**
→ When the user is in thumbnails mode with a single asset selected, pressing `1`–`5` rates that asset. If no asset is selected the `rateSelected$` emission is handled by `GalleryComponent` which guards with `if (this.selectedAssets.size === 1)` — so the keypress is silently ignored. This is documented in the spec.

**`MatDialog` injected into `KeyboardService`**
→ Injecting a UI service into a core service is a minor layering compromise. The alternative — emitting an `openHelp$` subject and having `AppComponent` open the dialog — adds boilerplate. Given that `KeyboardService` lives in `core/services/` and `MatDialog` is a pure Angular Material service with no direct UI rendering responsibility, this is acceptable.

**Shift key required for `?`**
→ On standard QWERTY layouts `?` is `Shift+/`. The handler checks `event.key === '?'` which correctly resolves the shifted key. Non-QWERTY layouts may require different key combos to produce `?`; this is an acceptable limitation for the current scope.
