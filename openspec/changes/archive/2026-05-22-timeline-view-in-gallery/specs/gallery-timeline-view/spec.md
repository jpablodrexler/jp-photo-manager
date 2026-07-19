## ADDED Requirements

### Requirement: User can toggle between Grid and Timeline view modes
The gallery toolbar SHALL provide a view-mode toggle that allows the user to switch between the existing flat Grid view and the new Timeline view. The active mode SHALL be visually distinguished. Switching modes SHALL preserve all active filters (search term, date range, min rating, selected preset) and reload assets in the selected mode.

#### Scenario: Toggle to timeline view
- **WHEN** the user clicks the Timeline toggle button in the gallery toolbar
- **THEN** the gallery renders assets grouped by day with date headers, replacing the flat thumbnail grid

#### Scenario: Toggle back to grid view
- **WHEN** the user is in timeline mode and clicks the Grid toggle button
- **THEN** the gallery returns to the flat thumbnail grid with the same filters applied

#### Scenario: Active mode is visually indicated
- **WHEN** a view mode is active
- **THEN** its toggle button appears in a visually distinct state (e.g., filled icon or active color) compared to the inactive mode button

---

### Requirement: Timeline groups assets by day
In timeline mode, the gallery SHALL display assets grouped by their file creation date. Each group SHALL display a date heading (day of month and day of week) preceded by a sticky month-and-year header when the month changes. Groups SHALL be ordered with the most recent date first.

#### Scenario: Assets from multiple days are displayed with day headers
- **WHEN** the timeline view is active and assets from more than one day are loaded
- **THEN** each distinct day appears as a labeled section heading above that day's thumbnails

#### Scenario: Month header appears at the start of each new month
- **WHEN** scrolling through the timeline and the month changes between groups
- **THEN** a month-and-year header (e.g., "May 2025") appears as a sticky label at the top of the viewport until the next month's header takes its place

#### Scenario: Groups are in reverse chronological order
- **WHEN** the timeline is rendered
- **THEN** the most recent day group appears at the top and older groups appear below

---

### Requirement: Timeline supports infinite scroll
The timeline view SHALL load additional day groups as the user scrolls toward the bottom of the page. Loading SHALL be triggered by the same sentinel-element / IntersectionObserver mechanism used by the existing grid view. A loading indicator SHALL be shown while a request is in-flight.

#### Scenario: New groups load on scroll
- **WHEN** the user scrolls to the bottom of the currently loaded timeline content
- **THEN** the next page of day groups is fetched and appended to the timeline without a full-page reload

#### Scenario: Loading indicator shown during fetch
- **WHEN** a timeline page request is in-flight
- **THEN** a progress indicator is visible at the bottom of the timeline

#### Scenario: No more groups available
- **WHEN** all day groups for the current folder and filters have been loaded
- **THEN** the loading sentinel is removed and no further requests are made

---

### Requirement: Timeline respects all existing gallery filters
The timeline view SHALL apply the same filter criteria as the grid view: folder path, text search, date range (dateFrom / dateTo), and minimum star rating. Changing any filter in timeline mode SHALL reset the timeline and reload from page 0.

#### Scenario: Search term filters timeline results
- **WHEN** the user enters a search term while in timeline mode
- **THEN** only assets whose file name matches the search term appear in the timeline groups

#### Scenario: Date range filter restricts visible groups
- **WHEN** the user sets a dateFrom and/or dateTo filter in timeline mode
- **THEN** only day groups within that date range are displayed

#### Scenario: Min rating filter excludes lower-rated assets
- **WHEN** the user sets a minimum rating filter in timeline mode
- **THEN** assets below that rating are excluded; day groups with no remaining assets are omitted entirely

#### Scenario: Filter change resets timeline
- **WHEN** any filter value changes while in timeline mode
- **THEN** the existing timeline groups are cleared and reloaded from the first page

---

### Requirement: Backend timeline endpoint returns day-grouped assets
The system SHALL expose `GET /api/assets/timeline` that accepts the same filter parameters as `GET /api/assets` (except `sort`, which is fixed to `fileCreationDateTime` descending) and returns a paginated list of day groups. Each group SHALL include a `localDate`, a formatted `label`, and the list of assets for that day.

#### Scenario: Request returns day groups for a valid folder
- **WHEN** `GET /api/assets/timeline?folderPath=/photos&page=0` is called
- **THEN** the response contains a list of `TimelineGroupDto` objects, each with a `localDate`, `label`, and `assets` array, sorted newest-day first

#### Scenario: Filter parameters are applied
- **WHEN** `GET /api/assets/timeline?folderPath=/photos&search=beach&minRating=3&page=0` is called
- **THEN** only assets matching the search term and meeting the minimum rating are included in the groups

#### Scenario: Empty folder returns empty list
- **WHEN** `GET /api/assets/timeline` is called for a folder with no matching assets
- **THEN** the response contains an empty `items` list with `totalPages: 0`

#### Scenario: Pagination loads subsequent groups
- **WHEN** `GET /api/assets/timeline?folderPath=/photos&page=1` is called after page 0
- **THEN** the response contains the next set of day groups chronologically following page 0's groups
