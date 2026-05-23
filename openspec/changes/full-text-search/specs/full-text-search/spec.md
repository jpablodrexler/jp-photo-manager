# full-text-search

Asset search covers filename, tags, EXIF camera model, and description fields using PostgreSQL full-text search (`tsvector` / `tsquery`) with a `GIN` index. The existing search input in the frontend is unchanged; the backend improvement is transparent to the UI.

---

## ADDED Requirements

### Requirement: search_vector column is maintained by a trigger

The `assets` table SHALL contain a `search_vector TSVECTOR` column updated by a PostgreSQL trigger after every `INSERT` or `UPDATE` on `assets` and after changes to `asset_tags`. A `GIN` index on `search_vector` SHALL be created by the V17 migration.

#### Scenario: V17 migration creates search_vector and GIN index

- **WHEN** the V17 Flyway migration runs
- **THEN** the `assets` table has a `search_vector` column, a `GIN` index on it, and a trigger that maintains it; existing rows have `search_vector` backfilled

#### Scenario: Cataloging a new asset populates search_vector

- **GIVEN** an asset is cataloged with `file_name = "vacation.jpg"` and camera model "Canon EOS R5"
- **WHEN** the asset is inserted into the database
- **THEN** the trigger fires and `search_vector` contains lexemes from both `file_name` and `camera_model`

### Requirement: findByFolderWithFilters uses full-text search when search term is provided

When the `search` parameter has 3 or more characters, the `findByFolderWithFilters` query SHALL use `search_vector @@ websearch_to_tsquery('english', :search)` instead of a `LIKE` predicate. For search terms fewer than 3 characters, the existing `LIKE` behavior is retained.

#### Scenario: Multi-word search matches across filename and tags

- **GIVEN** an asset with `file_name = "sunset.jpg"` tagged "travel" and "beach"
- **WHEN** a search for "sunset beach" is submitted
- **THEN** the asset is returned in the results

#### Scenario: Camera model search returns matching assets

- **GIVEN** an asset cataloged with EXIF camera model "Nikon Z6"
- **WHEN** a search for "nikon" is submitted
- **THEN** the asset is returned in the results

#### Scenario: Short search terms use LIKE fallback

- **GIVEN** a search term of "jp" (2 characters)
- **WHEN** `findByFolderWithFilters` is called
- **THEN** the query uses `LOWER(file_name) LIKE '%jp%'` rather than `websearch_to_tsquery`

### Requirement: search_vector covers filename, description, camera model, and tags

The trigger-maintained `search_vector` SHALL combine: `setweight(to_tsvector('english', file_name), 'A')` + `setweight(to_tsvector('english', COALESCE(description, '')), 'B')` + camera model from `asset_exif` + tag names from `asset_tags`.

#### Scenario: Description search returns matching assets

- **GIVEN** an asset with `description = "mountain hiking trip"` (after `asset-description` #37 is implemented)
- **WHEN** a search for "hiking" is submitted
- **THEN** the asset is returned in the results
