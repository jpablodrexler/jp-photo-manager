# folder-stats-in-tree

Each folder node in the sidebar tree displays its asset count and total file size, loaded on demand as the tree is expanded.

---

## ADDED Requirements

### Requirement: Folder stats are available via a dedicated endpoint

`GET /api/folders/stats?path=<folderPath>` SHALL return the count of non-deleted assets and their total file size for the specified folder path (direct children only, non-recursive).

#### Scenario: Stats returned for a folder with assets

- **GIVEN** a folder `/photos/vacation` contains 50 non-deleted assets with a combined size of 250 MB
- **WHEN** `GET /api/folders/stats?path=/photos/vacation` is called
- **THEN** the response is `200 OK` with body `{ "assetCount": 50, "totalSizeBytes": 262144000 }`

#### Scenario: Stats returned for an empty folder

- **GIVEN** a folder `/photos/empty` contains no assets
- **WHEN** `GET /api/folders/stats?path=/photos/empty` is called
- **THEN** the response is `200 OK` with body `{ "assetCount": 0, "totalSizeBytes": 0 }`

#### Scenario: Stats exclude soft-deleted assets

- **GIVEN** a folder `/photos/archive` contains 10 assets, 3 of which are soft-deleted
- **WHEN** `GET /api/folders/stats?path=/photos/archive` is called
- **THEN** the response returns `{ "assetCount": 7, ... }` (only 7 non-deleted assets counted)

### Requirement: FolderNavComponent displays stats inline

The folder tree SHALL show asset count and human-readable total size alongside each folder name after stats are loaded.

#### Scenario: Stats displayed after folder node is expanded

- **GIVEN** a folder node `/photos/vacation` is visible in the tree
- **WHEN** the user expands the node
- **THEN** the node label updates to show `Vacation (50 · 238.4 MB)` within 1 second
