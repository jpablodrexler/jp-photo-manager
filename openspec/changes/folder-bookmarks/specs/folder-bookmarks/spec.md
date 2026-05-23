# folder-bookmarks

Users can bookmark frequently used folders. Bookmarked folders appear in a pinned section above the main folder tree for instant access.

---

## ADDED Requirements

### Requirement: Users can bookmark folders

`POST /api/folders/bookmarks` SHALL add a bookmark for the authenticated user. The operation SHALL be idempotent — bookmarking an already-bookmarked folder returns the existing bookmark.

#### Scenario: User bookmarks a folder

- **GIVEN** a user with no bookmarks for `/photos/vacation`
- **WHEN** `POST /api/folders/bookmarks` is called with `{ "folderPath": "/photos/vacation" }`
- **THEN** the response is `200 OK` with `{ "id": 1, "folderPath": "/photos/vacation", "createdAt": "..." }` and the bookmark is stored

#### Scenario: Bookmarking an already-bookmarked folder is idempotent

- **GIVEN** a user who has already bookmarked `/photos/vacation`
- **WHEN** `POST /api/folders/bookmarks` is called again with the same path
- **THEN** the response is `200 OK` with the existing bookmark (no duplicate created)

#### Scenario: Exceeding 10 bookmarks is rejected

- **GIVEN** a user who already has 10 bookmarks
- **WHEN** `POST /api/folders/bookmarks` is called with a new path
- **THEN** the response is `400 Bad Request`

### Requirement: Users can list and remove bookmarks

`GET /api/folders/bookmarks` returns the user's bookmarks. `DELETE /api/folders/bookmarks/{id}` removes a single bookmark.

#### Scenario: Bookmarks are listed for the authenticated user

- **GIVEN** a user with 3 bookmarked folders
- **WHEN** `GET /api/folders/bookmarks` is called
- **THEN** the response is `200 OK` with a JSON array of 3 bookmark objects sorted by `createdAt` ascending

#### Scenario: User cannot remove another user's bookmark

- **GIVEN** a bookmark ID belonging to a different user
- **WHEN** `DELETE /api/folders/bookmarks/{id}` is called
- **THEN** the response is `404 Not Found`

### Requirement: FolderNavComponent shows bookmarked folders in a pinned section

The folder navigation tree SHALL display a pinned section above the main tree when the user has at least one bookmark, separated by a `MatDivider`.

#### Scenario: Pinned section appears with bookmarked folders

- **GIVEN** a user with 2 bookmarked folders
- **WHEN** the folder navigation loads
- **THEN** a pinned section with 2 clickable folder entries appears above the tree, separated by a divider

#### Scenario: Pin icon on tree node adds bookmark

- **GIVEN** a tree node for `/photos/vacation` is not bookmarked
- **WHEN** the user clicks the pin icon on that node
- **THEN** `POST /api/folders/bookmarks` is called and the folder appears in the pinned section
