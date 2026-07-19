## Why

The current search only matches the `file_name` column using a `LIKE '%keyword%'` query. Users cannot search by tags, camera model, or asset description. PostgreSQL's native full-text search (`tsvector` / `tsquery`) supports ranked, multi-field search across filename, tags, EXIF camera model, and description with a `GIN` index for efficiency.

## What Changes

- Add a `search_vector tsvector` generated column to `assets` maintained by a PostgreSQL trigger (Flyway migration V17; requires V16 `description` column first)
- Add a `GIN` index on `search_vector`
- Extend `findByFolderWithFilters` JPQL to add a `search_vector @@ to_tsquery(...)` predicate when a search term is provided
- The existing keyword search input in the frontend is unchanged; the backend improvement is transparent to the UI

## Capabilities

### New Capabilities

- `full-text-search`: Asset search covers filename, tags, EXIF camera model, and description fields using PostgreSQL full-text search with a ranked `ts_rank` ordering option.

### Modified Capabilities

- `search-and-filter`: The existing `findByFolderWithFilters` query gains full-text search support while retaining date-range and rating filters.

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V17__full_text_search.sql` — new Flyway migration: generated column, GIN index, trigger
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/persistence/jpa/JpaAssetRepository.java` — update `findByFolderWithFilters` query to use `to_tsquery`
- `JPPhotoManagerWeb/backend/src/test/` — update search tests for full-text behavior
