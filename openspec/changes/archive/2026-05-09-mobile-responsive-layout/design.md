## Context

The gallery layout is currently a flex row: `div.folder-tree` (fixed `width: 280px; min-width: 280px`) beside `div.flex-1.flex-col`. The top navigation is a flat `mat-toolbar` with inline `<a mat-button>` elements for every route. Neither adapts to narrow viewports.

Angular CDK ships `BreakpointObserver` which wraps `window.matchMedia` and emits synchronously on breakpoint crossings. Angular Material ships `MatSidenav` which supports `mode="side"` (persistent, pushes content) and `mode="over"` (overlay, floats above content). Both are already declared as Angular Material peer dependencies and require no new packages.

The global SCSS file (`styles.scss`) defines `.folder-tree` (280 px), `.thumbnail-grid` (`minmax(200px, 1fr)`), and `.thumbnail-card`. These are the primary targets for media-query overrides.

## Goals / Non-Goals

**Goals:**

- Navigation is usable on viewports as narrow as 320 px without horizontal scrolling.
- The folder tree is accessible on mobile via a toggleable overlay drawer.
- The thumbnail grid shows at least two columns on a 375 px wide screen.
- Touch targets meet the 44 × 44 px minimum for all interactive controls.
- No backend changes of any kind.

**Non-Goals:**

- Separate mobile components or routes (one component, one template, adapts via `@if`).
- Full PWA / offline support.
- Responsive adjustments for sync, convert, or duplicates pages (their layouts are simpler forms that already reflow acceptably).
- Custom breakpoints beyond the single 768 px threshold.

## Decisions

### 1. Single 768 px breakpoint — not a multi-tier scale

**Decision:** Use one breakpoint: `768px`. Below it, the layout is "mobile". At or above it, the layout is "desktop". Both the Angular CDK observer and the CSS media queries use this single value.

**Rationale:** The app has two meaningfully different layouts: a two-column sidebar+grid (desktop) and a single-column full-width grid with an overlay drawer (mobile). A three-tier system (mobile / tablet / desktop) adds complexity for marginal benefit since the content is a photo grid that naturally fills whatever horizontal space is available. Keeping one breakpoint means one `isMobile` boolean in each component, one CSS `@media` block, and straightforward testing.

### 2. `BreakpointObserver` for TypeScript state — not `ViewChild` / element resize

**Decision:** `AppComponent` and `GalleryComponent` both inject `BreakpointObserver` from `@angular/cdk/layout` and subscribe to `Breakpoints.Handset` (which covers both portrait and landscape phone viewports, approximately ≤ 767 px) in `ngOnInit`. The subscription updates a `isMobile = false` boolean field. The subscription is stored and unsubscribed in `ngOnDestroy`.

**Rationale:** `BreakpointObserver` reuses the CDK package that is already a dependency (it ships with Angular Material). It fires synchronously on the first emission so `isMobile` is correct before the first change-detection cycle. The alternative — querying `window.innerWidth` or using `ResizeObserver` on a container element — requires more boilerplate and does not participate in Angular's change detection as cleanly. `ViewChild` on a sidenav container to measure width is even more fragile.

### 3. `MatSidenav` in `GalleryComponent` for the folder tree — not a CSS transform trick

**Decision:** The folder-nav sidebar is moved inside a `<mat-sidenav>` within a `<mat-sidenav-container>` that wraps the entire gallery layout. On desktop (`!isMobile`) the sidenav uses `mode="side"` and `[opened]="true"`. On mobile (`isMobile`) it uses `mode="over"` and `[opened]="sidenavOpen"` which starts as `false`. A toggle button (hamburger icon) in the gallery toolbar calls `toggleSidenav()`. When the user selects a folder on mobile, the component automatically closes the sidenav.

**Rationale:** A pure CSS approach (hiding the sidebar and using a translated `position: fixed` panel) works but requires managing focus, keyboard traps, and backdrop separately. `MatSidenav` handles all of this correctly: it renders a backdrop overlay, traps focus, responds to `Escape`, and emits `(closedStart)` / `(openedStart)` events for side-effects. The API is already available with no extra npm packages.

### 4. Hamburger `mat-menu` in `AppComponent` for navigation — not a separate mobile nav component

**Decision:** `app.component.html` uses `@if (isMobile)` to switch between two rendering modes: (a) a hamburger `<button mat-icon-button [matMenuTriggerFor]="navMenu">` paired with a `<mat-menu #navMenu>` containing all nav links as `<button mat-menu-item routerLink="...">`, and (b) the existing inline `<a mat-button>` links. Both modes are in the same template. No separate component is created.

**Rationale:** Extracting a separate `MobileNavComponent` would add a file and an import with no real reuse benefit — there is only one navigation bar. Keeping both modes in the same template means the route-active state and logout handler are shared without prop-passing. The template duplication (nav links appear twice) is minimal (7 links) and kept synchronized by proximity.

### 5. CSS media queries for grid and toolbar — not Angular CDK layout directives

**Decision:** `styles.scss` adds a single `@media (max-width: 767px)` block that overrides `.thumbnail-grid` to use `minmax(140px, 1fr)` and limits `.gallery-toolbar .current-folder` `max-width` to `120px`. These are purely presentational and require no TypeScript.

**Rationale:** The thumbnail grid and current-folder label are presentational concerns that do not drive any component logic. CSS media queries are simpler and more performant than Angular layout directives for purely visual adjustments. The CDK `BreakpointObserver` subscription is reserved for layout decisions that require TypeScript state (sidenav mode, nav menu visibility), where component logic must branch.

### 6. Folder auto-close on selection in overlay mode

**Decision:** In `GalleryComponent`, the `onFolderSelected(path)` method checks `if (this.isMobile) { this.sidenavOpen = false; }` after setting `currentFolder` and calling `loadAssets()`.

**Rationale:** On a phone, the overlay sidenav covers the entire thumbnail grid. If selecting a folder left the drawer open the user would see nothing change — they would need a second tap to close it. Auto-closing mirrors the behavior of most mobile navigation drawers (Material Design guidelines recommend closing overlays after navigation). The user can reopen it anytime with the toolbar toggle button.

## Data Flow

```
Viewport width changes (or initial load)
  → BreakpointObserver emits { matches: true/false }
    → AppComponent.isMobile = true/false
      → @if (isMobile) switches nav to hamburger / inline links
    → GalleryComponent.isMobile = true/false
      → MatSidenav [mode] and [opened] bindings update
      → if newly mobile and sidenav was open → close it

User taps hamburger button (mobile)
  → mat-menu opens with nav links as mat-menu-items
  → user taps a route link
  → Angular Router navigates
  → mat-menu closes automatically

User taps folder toggle (mobile gallery)
  → GalleryComponent.toggleSidenav() → sidenavOpen = !sidenavOpen
  → MatSidenav opens as overlay
  → user selects a folder
  → GalleryComponent.onFolderSelected(path):
      currentFolder = path; loadAssets(); if (isMobile) sidenavOpen = false
  → MatSidenav closes; thumbnail grid is now visible

User resizes from mobile to desktop
  → BreakpointObserver emits { matches: false }
  → GalleryComponent.isMobile = false
  → MatSidenav mode="side", opened=true → folder tree always visible
```

## File Change List

**Modified files:**

- `frontend/src/app/app.component.ts` — inject `BreakpointObserver`; `isMobile` field; subscribe/unsubscribe
- `frontend/src/app/app.component.html` — hamburger menu for mobile; conditional inline links for desktop
- `frontend/src/app/features/gallery/gallery.component.ts` — inject `BreakpointObserver`; `isMobile`, `sidenavOpen` fields; `toggleSidenav()`; update `onFolderSelected`; unsubscribe
- `frontend/src/app/features/gallery/gallery.component.html` — wrap in `mat-sidenav-container`; move folder-nav into `mat-sidenav`; add toggle button
- `frontend/src/app/features/gallery/gallery.component.scss` — remove `.folder-tree` width (now set by sidenav)
- `frontend/src/styles.scss` — add `@media (max-width: 767px)` block

**No new files. No backend changes.**
