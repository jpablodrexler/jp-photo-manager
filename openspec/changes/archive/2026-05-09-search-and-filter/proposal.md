## Why

The gallery currently lets users sort the asset list for a folder by five criteria (`FILE_NAME`, `FILE_SIZE`, `FILE_CREATION_DATE_TIME`, `FILE_MODIFICATION_DATE_TIME`, `THUMBNAIL_CREATION_DATE_TIME`), but provides no way to narrow the results. A folder with thousands of catalogued images requires manual page-by-page scrolling to find a specific file or to review photos from a particular trip or date range. Users who remember part of a filename or know approximately when a photo was taken have no way to exploit that knowledge in the UI.

Adding an optional filename search and optional date-range filter to the gallery closes this gap. The implementation extends the existing `GET /api/assets` endpoint with three new optional query parameters, updates the repository with a JPQL predicate query, and adds a small filter toolbar to `GalleryComponent`. No schema changes are needed because `file_name` and `file_creation_date_time` are already indexed columns on the `assets` table. The approach is additive: all existing clients that omit the new parameters receive unchanged behaviour.

## What Changes

- Add three optional `@RequestParam` values to `AssetController.getAssets()`: `search` (String), `dateFrom` (LocalDate), `dateTo` (LocalDate).
- Extend `PhotoManagerFacade.getAssets()` interface signature to accept `String search`, `LocalDate dateFrom`, `LocalDate dateTo` (all nullable).
- Update `PhotoManagerFacadeImpl.getAssets()` to pass the new parameters to the repository and use `fileCreationDateTime` date-range bounds (start-of-day for `dateFrom`, end-of-day for `dateTo`).
- Add `findByFolderWithFilters(Folder folder, String search, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable)` to `AssetRepository` using a JPQL `@Query` with optional-parameter pattern (`(:param IS NULL OR ...)`) so that Spring Data JPA can compile a single query that handles all combinations of null/non-null filter values.
- Add `getAssets(folderPath, page, sort, search, dateFrom, dateTo)` overload to the Angular `AssetService`, appending `search`, `dateFrom`, and `dateTo` to `HttpParams` only when they are non-null.
- Add a filter toolbar row to `GalleryComponent`: a `MatInput` search field (debounced 400 ms via `Subject<string>`), two `MatDatepicker` fields for date from/to, and a clear button that resets all three filters to null and reloads page 0.
- Changing any filter resets `pageIndex` to 0 before calling `loadAssets()`.

## Capabilities

### New Capabilities

- `search-and-filter`: Filter the gallery asset list by a case-insensitive filename substring (`search`), by a creation-date lower bound (`dateFrom`), by a creation-date upper bound (`dateTo`), or any combination of these. Filters are applied server-side and compose with the existing sort and pagination parameters.

### Modified Capabilities

- **`GET /api/assets` (asset-pagination)**: The endpoint now accepts three additional optional query parameters (`search`, `dateFrom`, `dateTo`). When all three are omitted the endpoint behaves identically to its current implementation, preserving backward compatibility.

## Impact

- **`AssetRepository.java`**: new `findByFolderWithFilters` JPQL query method with optional-predicate pattern; the existing `findByFolder(Folder, Pageable)` method is preserved and still used when no filters are provided — or it can be superseded by the new method with all-null filter arguments.
- **`PhotoManagerFacade.java`**: `getAssets` signature gains three nullable parameters: `String search`, `LocalDate dateFrom`, `LocalDate dateTo`.
- **`PhotoManagerFacadeImpl.java`**: updated implementation converts `dateFrom`/`dateTo` to `LocalDateTime` (start and end of day), delegates to the new repository query; the `SORT_MAP` and page-size constants remain unchanged.
- **`AssetController.java`**: `getAssets` handler gains three `@RequestParam(required = false)` parameters and passes them through to the facade.
- **`asset.service.ts`**: `getAssets` gains three optional parameters (`search?: string`, `dateFrom?: string`, `dateTo?: string`) and conditionally appends them to `HttpParams`.
- **`gallery.component.ts`**: new properties `searchTerm = ''`, `dateFrom: Date | null = null`, `dateTo: Date | null = null`, a `Subject<string>` for debouncing, `onSearchChange()`, `onDateChange()`, `clearFilters()`; `loadAssets()` passes filters to the service; `onFolderSelected()` resets filters before loading.
- **`gallery.component.html`**: new filter toolbar row with `<mat-form-field>` search input, two `<mat-datepicker>` date fields, and a clear button; all visible only in thumbnail mode.
- **`gallery.component.scss`**: layout rules for the filter toolbar row (flex, gap, field widths).
- **No DB schema changes** — `file_name` and `file_creation_date_time` are existing columns; no Flyway migration required.
- **No breaking API changes** — all three new query parameters default to absent (null on the server); existing callers are unaffected.
