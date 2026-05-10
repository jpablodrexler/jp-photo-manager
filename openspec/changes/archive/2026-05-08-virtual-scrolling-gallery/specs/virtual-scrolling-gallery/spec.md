# virtual-scrolling-gallery

Specifies how the gallery thumbnail grid loads and displays images using continuous infinite scroll instead of page-flip pagination.

---

### Requirement: The gallery loads additional pages automatically as the user scrolls

The gallery thumbnail view SHALL NOT show previous/next pagination buttons. Instead, it SHALL continuously append new pages of assets to the grid as the user scrolls toward the bottom. When the end of the loaded items is within the viewport, the next page SHALL be fetched from `GET /api/assets` using the existing `page` offset parameter and its items appended to the existing grid.

#### Scenario: Initial folder load
- **GIVEN** a folder is selected for the first time
- **WHEN** the gallery renders the thumbnail view
- **THEN** page 0 is fetched automatically, thumbnails appear in the grid, and no prev/next buttons are present in the DOM

#### Scenario: Auto-load next page on scroll
- **GIVEN** page 0 has been loaded and `totalPages > 1`
- **WHEN** the user scrolls to the bottom of the visible thumbnails (the sentinel element enters the viewport)
- **THEN** page 1 is fetched and its thumbnails are appended below the existing ones without clearing the grid

#### Scenario: Multiple pages accumulated
- **GIVEN** the user has scrolled through pages 0 and 1 of a 4-page folder
- **WHEN** the user continues scrolling to the bottom
- **THEN** page 2 is fetched and appended; all previously loaded thumbnails remain visible above

#### Scenario: All pages loaded
- **GIVEN** the last page of a folder has been fetched and appended
- **WHEN** the user reaches the bottom of the grid
- **THEN** an "All photos loaded" label is shown and no further `GET /api/assets` requests are made

---

### Requirement: Concurrent and redundant fetches are prevented

The system SHALL NOT issue a second `GET /api/assets` request while a previous request for the same folder is still in flight. Rapid scrolling that causes the sentinel to enter the viewport multiple times SHALL result in at most one in-flight request at a time.

#### Scenario: Rapid scroll triggers sentinel multiple times
- **GIVEN** a page fetch is currently in flight (`isLoading = true`)
- **WHEN** the sentinel enters the viewport again before the response arrives
- **THEN** no additional `GET /api/assets` request is issued; the pending request completes normally

---

### Requirement: A loading indicator is shown while a page fetch is in progress

The gallery SHALL show a `mat-progress-bar` in indeterminate mode below the thumbnail grid while a page request is in flight. The indicator SHALL disappear when the request completes.

#### Scenario: Loading indicator visible during fetch
- **GIVEN** a next-page fetch is in progress
- **WHEN** the user views the bottom of the gallery
- **THEN** a progress bar is visible below the last loaded thumbnail row

#### Scenario: Loading indicator hidden after fetch
- **GIVEN** a next-page fetch has just completed successfully
- **WHEN** the gallery re-renders
- **THEN** the progress bar is no longer present in the DOM

---

### Requirement: The grid resets when the folder or sort order changes

Changing the selected folder or sort criteria SHALL clear all accumulated assets and restart from page 0. Previously loaded thumbnails SHALL be removed from the DOM before the new page 0 results appear.

#### Scenario: Folder change resets the grid
- **GIVEN** the gallery has loaded 3 pages from folder A
- **WHEN** the user selects folder B
- **THEN** the grid is cleared immediately, a fresh fetch for folder B page 0 is issued, and only folder B thumbnails appear

#### Scenario: Sort change resets the grid
- **GIVEN** the gallery has loaded 2 pages sorted by FILE_NAME
- **WHEN** the user changes the sort to FILE_SIZE
- **THEN** the grid is cleared, page 0 with sort=FILE_SIZE is fetched, and only the new results appear

---

### Requirement: The status bar shows the count of loaded and total assets

The status bar SHALL display the number of assets currently loaded out of the total available in the selected folder (e.g. "50 of 230 photos"). This replaces the "X / Y pages (Z items)" text from the former pagination bar.

#### Scenario: Partial load status
- **GIVEN** 100 assets have been loaded from a folder containing 350 total
- **WHEN** the user views the status bar
- **THEN** the status bar reads "100 of 350 photos"

#### Scenario: Full load status
- **GIVEN** all 350 assets of a folder have been loaded
- **WHEN** the user views the status bar
- **THEN** the status bar reads "350 of 350 photos" and the "All photos loaded" end-of-list label is shown
