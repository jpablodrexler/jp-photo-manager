## Why

The application's current layout assumes a wide desktop viewport. The top navigation bar renders all route links inline — on screens narrower than ~700 px they overflow or wrap chaotically. The folder tree sidebar is a fixed 280 px panel that occupies roughly a third of the screen on a phone, leaving too little space for the thumbnail grid. The thumbnail grid's `minmax(200px, 1fr)` floor means only one card fits per row on a narrow screen, which works but was not intentionally designed. Touch targets (icon buttons, tree nodes) are too small for reliable finger interaction. As photographers increasingly review work from tablets and phones, a fully desktop-only layout creates friction.

Making the application responsive does not require changes to the REST API or business logic — it is a purely frontend concern. The existing Angular Material components (toolbar, sidenav, button) already support responsive patterns; the work is wiring them up correctly.

## What Changes

- Replace the flat inline nav links in `app.component.html` with a hamburger menu (`MatMenuModule`) that appears when the viewport is narrower than 768 px, and keep the inline links for wider viewports. `AppComponent` injects `BreakpointObserver` from `@angular/cdk/layout` to expose an `isMobile` boolean that the template uses with `@if`.
- Wrap the folder-nav sidebar in a `MatSidenav` inside `GalleryComponent`. On desktop (≥ 768 px) the sidenav is in `side` mode and always open. On mobile (< 768 px) it switches to `over` mode and is closed by default; a hamburger icon button in the gallery toolbar toggles it. `GalleryComponent` injects `BreakpointObserver` for the same breakpoint.
- Add responsive CSS media queries to `styles.scss` and component SCSS files:
  - `.folder-tree` width reduces to 240 px at tablet and hides at mobile (sidenav takes over).
  - `.thumbnail-grid` column minimum drops from 200 px to 140 px at mobile so two columns fit on a 320 px screen.
  - `.gallery-toolbar .current-folder` `max-width` reduces to 120 px on mobile to prevent overflow.
  - `.app-toolbar` hides text labels next to icons when `isMobile`, showing icons only.
- No backend changes are needed. No existing API contracts are altered. No routes or auth guards change.

## Capabilities

### New Capabilities

- `mobile-responsive-layout`: The application adapts its layout to the viewport width. On mobile devices (< 768 px) the navigation collapses into a hamburger menu and the folder tree becomes an overlay drawer, making the core photo-browsing workflow usable on phones and tablets.

### Modified Capabilities

- **Gallery layout**: The folder tree panel becomes a `MatSidenav` with mode that changes based on the active breakpoint. The toolbar gains a toggle button on mobile.
- **App navigation**: Inline nav links collapse into a `mat-menu` dropdown on narrow viewports.

## Impact

- **`app.component.ts`**: inject `BreakpointObserver`; expose `isMobile` signal updated via `subscribe`.
- **`app.component.html`**: add hamburger `<button mat-icon-button [matMenuTriggerFor]="navMenu">` visible only when `isMobile`; wrap nav links in `<mat-menu #navMenu>`; hide inline links when `isMobile`.
- **`app.component.scss`**: no structural changes; icon-only layout handled via template `@if`.
- **`gallery.component.ts`**: inject `BreakpointObserver`; expose `isMobile` and `sidenavOpen` state; add `toggleSidenav()` method; close sidenav on folder selection when in overlay mode.
- **`gallery.component.html`**: wrap root `div.gallery-layout` with `<mat-sidenav-container>`; move `<div class="folder-tree">` into `<mat-sidenav>`; add toggle button to toolbar.
- **`gallery.component.scss`**: remove fixed `.folder-tree` reference (now managed by `MatSidenav`).
- **`styles.scss`**: add `@media (max-width: 767px)` block reducing `.thumbnail-grid` column min to 140 px.
- **No new routes, services, or backend endpoints.**
