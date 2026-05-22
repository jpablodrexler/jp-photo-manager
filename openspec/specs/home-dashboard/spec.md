### Requirement: Home stats API returns enriched library data
`GET /api/home/stats` SHALL return all existing fields (`folderCount`, `assetCount`, `lastCatalogCompletedAt`) plus four new fields: `totalFileSize` (sum of all asset file sizes in bytes), `duplicateCount` (number of assets that are duplicates of at least one other asset), `topFolders` (list of up to 5 folders ordered by asset count descending, each with `path` and `assetCount`), and `recentAssets` (list of up to 12 most recently cataloged assets ordered by `thumbnailCreationDateTime` descending, each with `assetId`, `fileName`, `folderPath`, and `thumbnailUrl`).

#### Scenario: Stats include total file size
- **WHEN** `GET /api/home/stats` is called and the library contains assets with known file sizes
- **THEN** the response includes a `totalFileSize` field containing the sum of all `fileSize` values across all cataloged assets

#### Scenario: Duplicate count reflects number of non-unique assets
- **WHEN** `GET /api/home/stats` is called and the library contains 3 assets sharing the same hash and 10 unique assets
- **THEN** `duplicateCount` is 3 (all assets involved in duplicate groups are counted)

#### Scenario: Zero duplicates returns zero count
- **WHEN** all assets in the library have unique hashes
- **THEN** `duplicateCount` is 0

#### Scenario: Top folders are ordered by asset count descending
- **WHEN** `GET /api/home/stats` is called and the library has folders with 500, 200, 900, 50, 120, and 75 assets respectively
- **THEN** `topFolders` contains exactly 5 entries, ordered 900, 500, 200, 120, 75; the 50-asset folder is excluded

#### Scenario: Recent assets are the last 12 cataloged
- **WHEN** `GET /api/home/stats` is called and more than 12 assets exist
- **THEN** `recentAssets` contains exactly 12 entries, ordered by `thumbnailCreationDateTime` descending; each entry includes `assetId`, `fileName`, `folderPath`, and `thumbnailUrl`

#### Scenario: Empty library returns zeroed stats
- **WHEN** `GET /api/home/stats` is called and no assets are cataloged
- **THEN** `totalFileSize` is 0, `duplicateCount` is 0, `topFolders` is an empty list, `recentAssets` is an empty list

---

### Requirement: Home page displays quick action buttons
The `HomeComponent` SHALL display a row of quick-action buttons at the top of the page: **Open Gallery** (navigates to `/gallery`), **Run Catalog** (navigates to `/gallery` and triggers the catalog), **Run Sync** (navigates to `/sync`), and **Find Duplicates** (navigates to `/duplicates`). The **Find Duplicates** button SHALL display a warning badge showing `duplicateCount` when that value is greater than zero.

#### Scenario: Quick action buttons are visible on load
- **WHEN** the home page loads
- **THEN** all four quick-action buttons are rendered and each is clickable

#### Scenario: Duplicates badge shown when duplicates exist
- **WHEN** `duplicateCount` is greater than 0
- **THEN** the Find Duplicates button displays a visible badge with the duplicate count

#### Scenario: Duplicates badge hidden when no duplicates
- **WHEN** `duplicateCount` is 0
- **THEN** no badge is shown on the Find Duplicates button

#### Scenario: Open Gallery navigates to gallery route
- **WHEN** the user clicks the Open Gallery button
- **THEN** the router navigates to `/gallery`

#### Scenario: Find Duplicates navigates to duplicates route
- **WHEN** the user clicks the Find Duplicates button
- **THEN** the router navigates to `/duplicates`

---

### Requirement: Home page displays enriched library stat cards
The `HomeComponent` SHALL display the existing three stat cards (Folders Catalogued, Assets Catalogued, Last Catalog Completed) plus a new **Total Size** card showing `totalFileSize` formatted as a human-readable string (e.g., "24.3 GB") using the existing `FileSizePipe`.

#### Scenario: Total size card appears on the dashboard
- **WHEN** the home page loads and `totalFileSize` is 26,112,000,000 bytes
- **THEN** a stat card labelled "Total Size" displays the value formatted by `FileSizePipe` (e.g., "24.3 GB")

#### Scenario: Total size card shows zero for empty library
- **WHEN** `totalFileSize` is 0
- **THEN** the Total Size card displays "0 B" or equivalent zero representation from `FileSizePipe`

---

### Requirement: Home page displays a recent photos strip
The `HomeComponent` SHALL display a horizontally scrollable row of up to 12 thumbnail cards representing the most recently cataloged assets. Each thumbnail SHALL be rendered using the existing `ThumbnailComponent`. Clicking a thumbnail SHALL navigate to `/gallery` with a `folder` query parameter set to the asset's `folderPath`.

#### Scenario: Recent photos strip is visible when assets exist
- **WHEN** the home page loads and `recentAssets` contains 12 entries
- **THEN** a row of 12 thumbnail cards is displayed

#### Scenario: Recent photos strip is hidden when library is empty
- **WHEN** `recentAssets` is an empty list
- **THEN** no recent photos strip is rendered; an empty-state message is shown instead

#### Scenario: Clicking a thumbnail navigates to the gallery at that folder
- **WHEN** the user clicks a thumbnail whose `folderPath` is `/home/user/Pictures/Vacation`
- **THEN** the router navigates to `/gallery?folder=/home/user/Pictures/Vacation`

---

### Requirement: Home page displays a top folders breakdown
The `HomeComponent` SHALL display a list of up to 5 folders with the most assets. Each row SHALL show the folder path, asset count, and a proportional horizontal bar whose width is relative to the folder with the highest count. The list SHALL be ordered by asset count descending.

#### Scenario: Top folders list shows up to 5 entries
- **WHEN** `topFolders` contains 5 entries
- **THEN** all 5 folders are rendered in descending order of asset count

#### Scenario: The largest folder's bar spans the full width
- **WHEN** the folder with the most assets has `assetCount` equal to the maximum in the list
- **THEN** its bar renders at 100% width; all other bars render proportionally narrower

#### Scenario: Top folders section is hidden when no folders exist
- **WHEN** `topFolders` is an empty list
- **THEN** the top folders section is not rendered

---

### Requirement: Gallery route accepts a folder query parameter for pre-selection
`/gallery?folder=<path>` SHALL pre-select the specified folder in `FolderNavComponent` on load, loading that folder's assets immediately without requiring the user to click the folder in the tree.

#### Scenario: Gallery loads pre-selected folder from query param
- **WHEN** the user navigates to `/gallery?folder=/home/user/Pictures/Vacation`
- **THEN** the gallery loads with `/home/user/Pictures/Vacation` selected and its assets displayed

#### Scenario: Gallery without query param behaves as before
- **WHEN** the user navigates to `/gallery` with no `folder` query parameter
- **THEN** the gallery shows the "Select a folder" placeholder and waits for a folder selection from the tree
