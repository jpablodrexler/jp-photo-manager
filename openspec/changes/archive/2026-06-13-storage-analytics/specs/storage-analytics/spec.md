# storage-analytics

Specifies how the application exposes a dedicated analytics page at `/analytics` with four interactive charts — storage per folder, file-format distribution, photos per month, and rating distribution — built from aggregate queries over the catalogued asset data and rendered using `ngx-charts`.

---

## ADDED Requirements

### Requirement: The backend exposes a `GET /api/analytics` endpoint returning four aggregate datasets

The system SHALL expose `GET /api/analytics` requiring authentication. The response SHALL be `200 OK` with a JSON body containing four top-level keys: `folderStorage` (array of `{ folderPath, bytes }` entries), `formatDistribution` (array of `{ extension, count }` entries), `photosPerMonth` (array of `{ month, count }` entries where `month` is formatted `YYYY-MM`), and `ratingDistribution` (array of `{ rating, count }` entries for integer ratings 0–5). Only non-deleted assets SHALL be included in all aggregations. Unauthenticated requests SHALL receive `401 Unauthorized`.

#### Scenario: Analytics data is returned for a populated library

- **GIVEN** the catalog contains 500 non-deleted assets spread across 3 folders, 4 file extensions, 24 months, and ratings 0 through 5
- **WHEN** an authenticated user calls `GET /api/analytics`
- **THEN** the response is `200 OK`; `folderStorage` contains 3 entries each with a non-zero `bytes` value; `formatDistribution` contains 4 entries; `photosPerMonth` contains up to 24 entries; `ratingDistribution` contains entries for each rating value that has at least one asset

#### Scenario: Analytics data is empty for an empty catalog

- **GIVEN** the catalog contains no non-deleted assets
- **WHEN** an authenticated user calls `GET /api/analytics`
- **THEN** the response is `200 OK`; all four arrays are empty

#### Scenario: Deleted assets are excluded from analytics

- **GIVEN** the catalog contains 10 non-deleted assets and 5 soft-deleted assets (with non-null `deletedAt`)
- **WHEN** an authenticated user calls `GET /api/analytics`
- **THEN** the aggregate counts and sizes reflect only the 10 non-deleted assets; the 5 deleted assets do not appear in any dataset

#### Scenario: Unauthenticated request is rejected

- **GIVEN** the request has no valid JWT cookie
- **WHEN** `GET /api/analytics` is called
- **THEN** the response is `401 Unauthorized`

---

### Requirement: `folderStorage` aggregates total bytes per catalogued folder

The `folderStorage` array SHALL contain one entry per catalogued folder that has at least one non-deleted asset. Each entry SHALL include the full folder path and the sum of `fileSize` values for all non-deleted assets in that folder. The array SHALL be sorted descending by bytes. The implementation SHALL cap results to the top 20 folders by storage; if more than 20 folders exist, the remaining storage SHALL be aggregated into a single entry with `folderPath` equal to `"other"`.

#### Scenario: Folder storage is summed correctly

- **GIVEN** folder `/photos/vacation` contains 3 non-deleted assets with file sizes 1 000 000, 2 000 000, and 3 000 000 bytes
- **WHEN** `GET /api/analytics` is called
- **THEN** the `folderStorage` entry for `/photos/vacation` has `bytes` equal to 6 000 000

#### Scenario: Folder storage is sorted descending

- **GIVEN** folder A has 5 000 000 bytes and folder B has 10 000 000 bytes
- **WHEN** `GET /api/analytics` is called
- **THEN** the `folderStorage` array lists folder B first and folder A second

#### Scenario: More than 20 folders are collapsed into an "other" entry

- **GIVEN** the catalog contains 25 distinct folders with non-deleted assets
- **WHEN** `GET /api/analytics` is called
- **THEN** `folderStorage` contains exactly 21 entries: the 20 largest folders by storage and one entry with `folderPath = "other"` whose `bytes` is the sum of the remaining 5 folders

---

### Requirement: `formatDistribution` aggregates asset count by file extension

The `formatDistribution` array SHALL contain one entry per distinct lowercase file extension found among non-deleted assets. The extension SHALL be derived from the asset's `fileName` by taking the substring after the last `.`. Assets without a `.` in their filename SHALL be counted under the extension value `"unknown"`. The array SHALL be sorted descending by count.

#### Scenario: Extensions are counted and lowercased

- **GIVEN** the catalog contains 200 `.jpg` assets, 50 `.JPG` assets (uppercase), 30 `.png` assets, and 10 `.gif` assets
- **WHEN** `GET /api/analytics` is called
- **THEN** `formatDistribution` contains an entry `{ "extension": "jpg", "count": 250 }`, `{ "extension": "png", "count": 30 }`, and `{ "extension": "gif", "count": 10 }`

#### Scenario: Assets without an extension are grouped as "unknown"

- **GIVEN** the catalog contains 5 assets whose `fileName` contains no `.` character
- **WHEN** `GET /api/analytics` is called
- **THEN** `formatDistribution` contains an entry `{ "extension": "unknown", "count": 5 }`

---

### Requirement: `photosPerMonth` aggregates asset count by creation month

The `photosPerMonth` array SHALL contain one entry per distinct calendar month (formatted `YYYY-MM`) in which at least one non-deleted asset has a `fileCreationDateTime`. The array SHALL be sorted ascending by month (oldest first).

#### Scenario: Assets are grouped by creation month

- **GIVEN** the catalog contains 10 assets created in January 2024 and 25 assets created in February 2024
- **WHEN** `GET /api/analytics` is called
- **THEN** `photosPerMonth` contains `{ "month": "2024-01", "count": 10 }` and `{ "month": "2024-02", "count": 25 }` in that order

#### Scenario: Months with no assets are omitted

- **GIVEN** assets exist in January 2024 and March 2024 but not February 2024
- **WHEN** `GET /api/analytics` is called
- **THEN** `photosPerMonth` contains entries for `"2024-01"` and `"2024-03"` only; no entry for `"2024-02"` is included

#### Scenario: Assets with null creation date are excluded

- **GIVEN** 5 assets have a null `fileCreationDateTime`
- **WHEN** `GET /api/analytics` is called
- **THEN** those 5 assets do not appear in any `photosPerMonth` entry

---

### Requirement: `ratingDistribution` aggregates asset count per star rating

The `ratingDistribution` array SHALL contain one entry per integer rating value (0 through 5) that has at least one non-deleted asset. Rating 0 represents unrated assets. The array SHALL be sorted ascending by rating value.

#### Scenario: Rating distribution covers all represented ratings

- **GIVEN** the catalog contains 100 assets with rating 0, 50 with rating 3, and 20 with rating 5
- **WHEN** `GET /api/analytics` is called
- **THEN** `ratingDistribution` contains `{ "rating": 0, "count": 100 }`, `{ "rating": 3, "count": 50 }`, and `{ "rating": 5, "count": 20 }` in ascending order; ratings 1, 2, and 4 are omitted because no assets have those ratings

#### Scenario: Rating 0 represents unrated assets

- **GIVEN** 75 assets have `rating = 0` (unrated)
- **WHEN** `GET /api/analytics` is called
- **THEN** `ratingDistribution` includes `{ "rating": 0, "count": 75 }`

---

### Requirement: The frontend provides a `/analytics` route with four `ngx-charts` charts

The frontend SHALL add a `/analytics` route loading a lazy `AnalyticsComponent` protected by `authGuard`. `AnalyticsComponent` SHALL call `GET /api/analytics` on initialisation via `AnalyticsService` and render four charts inside Angular Material cards: (1) a treemap showing storage per folder (`folderStorage`), (2) a pie chart showing file-format distribution (`formatDistribution`), (3) a vertical bar chart showing photos per month (`photosPerMonth`), (4) a vertical bar chart showing rating distribution (`ratingDistribution`). While the data is loading, a spinner SHALL be displayed. If the API call fails, an error message SHALL be shown and no charts SHALL be rendered.

#### Scenario: Analytics page renders four charts on successful API response

- **GIVEN** `GET /api/analytics` returns non-empty datasets for all four keys
- **WHEN** the user navigates to `/analytics`
- **THEN** four chart containers are visible on the page: a treemap, a pie chart, and two bar charts; no loading spinner is shown

#### Scenario: Loading spinner is shown while the API call is in progress

- **GIVEN** the API call has not yet resolved
- **WHEN** the user navigates to `/analytics`
- **THEN** a loading spinner is displayed and no chart containers are rendered

#### Scenario: Error message is shown when the API call fails

- **GIVEN** `GET /api/analytics` returns an HTTP error
- **WHEN** the user navigates to `/analytics`
- **THEN** an error message is displayed; no chart containers are rendered and no spinner is shown

#### Scenario: Unauthenticated users are redirected to login

- **GIVEN** the user is not authenticated
- **WHEN** the user navigates to `/analytics`
- **THEN** `authGuard` redirects the user to `/login`; the analytics page is not loaded

---

### Requirement: The navigation bar includes an "Analytics" link

The `AppComponent` top navigation bar SHALL display an "Analytics" link to `/analytics`. The link SHALL be visible only when the user is authenticated, consistent with other navigation links. It SHALL be included in both the desktop toolbar and the mobile hamburger menu.

#### Scenario: Analytics link is visible when authenticated

- **GIVEN** the user is logged in
- **WHEN** any page is loaded
- **THEN** an "Analytics" navigation link pointing to `/analytics` is visible in the toolbar

#### Scenario: Analytics link is absent when not authenticated

- **GIVEN** the user is not authenticated
- **WHEN** the login page is displayed
- **THEN** no "Analytics" link is visible in the toolbar
