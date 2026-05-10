# mobile-responsive-layout

Specifies how the application layout adapts to narrow viewports so that core photo-browsing workflows are usable on phones and tablets without horizontal scrolling.

---

### Requirement: The navigation bar collapses into a hamburger menu on mobile

When the viewport width is less than 768 px, the inline navigation links (`Home`, `Gallery`, `Sync`, `Convert`, `Duplicates`, `Users`, `Logout`) SHALL be hidden and replaced by a single hamburger icon button. Tapping the button SHALL open a `mat-menu` dropdown containing all navigation items. On viewports ≥ 768 px the inline links SHALL be shown and the hamburger button SHALL be absent.

#### Scenario: Mobile viewport shows hamburger menu

- **GIVEN** the user is authenticated and the viewport width is 375 px
- **WHEN** the application shell renders
- **THEN** the hamburger button with `aria-label="Open navigation"` is visible; the inline `<a mat-button>` nav links are not rendered

#### Scenario: Desktop viewport shows inline links

- **GIVEN** the user is authenticated and the viewport width is 1280 px
- **WHEN** the application shell renders
- **THEN** the hamburger button is not rendered; all inline nav links are visible in the toolbar

#### Scenario: Hamburger menu contains all routes

- **GIVEN** the user is authenticated and the viewport is mobile
- **WHEN** the user taps the hamburger button
- **THEN** a dropdown menu appears containing items for Home, Gallery, Sync, Convert, Duplicates, Users, and Logout

---

### Requirement: The folder tree becomes a toggleable overlay drawer on mobile

On viewports ≥ 768 px the `mat-sidenav` SHALL use `mode="side"` and SHALL be open by default, rendering the folder tree as a persistent 280 px panel. On viewports < 768 px the sidenav SHALL switch to `mode="over"`, be closed by default, and float above the content when opened. A toggle button in the gallery toolbar SHALL be visible only on mobile and SHALL toggle the sidenav open and closed.

#### Scenario: Desktop shows persistent sidebar

- **GIVEN** the gallery page is open and the viewport is 1280 px wide
- **WHEN** the page loads
- **THEN** `mat-sidenav` has `mode="side"`; the folder tree panel is visible alongside the thumbnail grid with no toggle button in the toolbar

#### Scenario: Mobile shows closed drawer by default

- **GIVEN** the gallery page is open and the viewport is 375 px wide
- **WHEN** the page loads
- **THEN** `mat-sidenav` has `mode="over"`; the folder tree panel is not visible; the thumbnail grid occupies the full width; a toggle button with `aria-label="Toggle folder panel"` is present in the gallery toolbar

#### Scenario: Toggle button opens the drawer

- **GIVEN** the gallery is in mobile mode with the sidenav closed
- **WHEN** the user taps the toggle button
- **THEN** the sidenav opens as an overlay above the thumbnail grid; a backdrop is rendered behind it

#### Scenario: Selecting a folder closes the drawer

- **GIVEN** the sidenav is open in mobile overlay mode
- **WHEN** the user selects a folder in the folder tree
- **THEN** the sidenav closes automatically; the thumbnail grid becomes fully visible; the selected folder's assets are loaded

---

### Requirement: The thumbnail grid adapts to narrow viewports

On viewports < 768 px the thumbnail grid column minimum width SHALL be 140 px (instead of 200 px) so that at least two columns render on a 320 px screen. The gap between cards SHALL reduce to 4 px and padding SHALL reduce to 8 px. Thumbnail card image height SHALL reduce to 110 px.

#### Scenario: Two columns on a 320 px screen

- **GIVEN** a folder with 10 assets is loaded and the viewport is 320 px wide
- **WHEN** the thumbnail grid renders
- **THEN** at least two thumbnail columns are visible; no horizontal scrollbar appears

#### Scenario: Desktop retains original grid dimensions

- **GIVEN** a folder with 10 assets is loaded and the viewport is 1280 px wide
- **WHEN** the thumbnail grid renders
- **THEN** the grid column minimum is 200 px; thumbnails are 150 px tall; gap is 8 px; padding is 16 px

---

### Requirement: Touch targets meet the 44 × 44 px minimum

All interactive controls (icon buttons in the toolbar, tree nodes in the folder tree, thumbnail cards, navigation items) SHALL have a minimum tap target of 44 × 44 px on mobile viewports. Angular Material's default icon button size (48 × 48 dp) already satisfies this; tree nodes that are shorter than 44 px SHALL have sufficient vertical padding added.

#### Scenario: Toolbar icon buttons are touch-friendly

- **GIVEN** the gallery is open in mobile mode
- **WHEN** the toolbar is rendered
- **THEN** each `mat-icon-button` in the toolbar has a rendered height of at least 44 px

---

### Requirement: The current folder path truncates gracefully on mobile

The `current-folder` span in the gallery toolbar SHALL have a `max-width` of 120 px on mobile (reduced from 400 px on desktop) with `text-overflow: ellipsis` so that the folder path does not push action buttons off-screen.

#### Scenario: Long folder path is truncated on mobile

- **GIVEN** the selected folder path is `/photos/2024/summer/vacation/europe`
- **WHEN** the gallery toolbar renders on a 375 px viewport
- **THEN** the folder path text is truncated with an ellipsis; all toolbar buttons remain visible without horizontal scrolling

---

### Requirement: Resizing the viewport dynamically switches between layouts

If the user resizes the browser window from desktop to mobile (or vice versa) while on the gallery page, the layout SHALL switch reactively: the sidenav mode SHALL update, the toggle button SHALL appear or disappear, and the nav bar SHALL switch between inline links and hamburger menu without requiring a page reload.

#### Scenario: Resize from desktop to mobile

- **GIVEN** the gallery is open in desktop mode (sidenav `mode="side"`, no toggle button)
- **WHEN** the viewport width is resized below 768 px
- **THEN** `mat-sidenav` switches to `mode="over"`; the sidenav closes; the toggle button appears in the toolbar; the hamburger icon appears in the app toolbar

#### Scenario: Resize from mobile to desktop

- **GIVEN** the gallery is open in mobile mode (sidenav `mode="over"`, toggle button visible)
- **WHEN** the viewport width is resized to 1280 px
- **THEN** `mat-sidenav` switches to `mode="side"` and opens; the toggle button disappears; the inline nav links reappear

---

### Requirement: No backend changes are required

The mobile-responsive-layout feature SHALL be implemented entirely in the Angular frontend. No new REST endpoints SHALL be added. No existing API contracts SHALL be modified. No backend services, entities, or configuration files SHALL change.

#### Scenario: API calls are unchanged on mobile

- **GIVEN** the application is running on a mobile viewport
- **WHEN** the user browses the gallery and loads a folder
- **THEN** `GET /api/assets?folderPath=...&page=0` is called with the same parameters as on desktop; the response shape is identical
