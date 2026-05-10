## Context

`GET /api/assets` currently accepts `folderPath`, `page`, and `sort`. `PhotoManagerFacadeImpl.getAssets` resolves the `Folder` entity via `FolderRepository`, builds a `PageRequest` with one of five `Sort` mappings, and delegates to `AssetRepository.findByFolder(Folder, Pageable)`.

`Asset` has `file_name TEXT NOT NULL` and `file_creation_date_time TIMESTAMP` columns defined in `V1__initial_schema.sql`. Both are already present in every catalog run. An index on `folder_id` exists (`ix_assets_folder_id`); `file_name` and `file_creation_date_time` are unindexed — acceptable for the expected dataset sizes of a personal photo library (thousands to low tens of thousands of assets per folder).

The gallery uses a `Subject<string>` debounce pattern for the search field so that every keystroke does not immediately fire an HTTP request. `MatDatepickerModule` with `MatNativeDateModule` provides date pickers without external library dependencies beyond what Angular Material already provides.

No Flyway migration is needed — all required columns already exist.

## Goals / Non-Goals

**Goals:**

- Filter assets by case-insensitive filename substring.
- Filter assets by creation-date lower bound (inclusive), upper bound (inclusive), or both.
- All filters are optional and compose with each other and with the existing `sort` parameter.
- Existing callers that omit the new parameters receive unchanged behaviour.
- Reset to page 0 when any filter changes; reset all filters when the folder changes.

**Non-Goals:**

- Full-text search over EXIF metadata or file contents.
- Cross-folder search (filters apply within the currently selected folder only).
- Saved or named filter presets.
- Adding a database index on `file_name` (deferred; acceptable for personal-library scale).

## Decisions

### 1. Single JPQL query with optional-predicate pattern — no query branching

**Decision:** `AssetRepository` gains one new method:

```java
@Query("""
    SELECT a FROM Asset a
    WHERE a.folder = :folder
      AND (:search IS NULL OR LOWER(a.fileName) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:dateFrom IS NULL OR a.fileCreationDateTime >= :dateFrom)
      AND (:dateTo   IS NULL OR a.fileCreationDateTime <= :dateTo)
    """)
Page<Asset> findByFolderWithFilters(
    @Param("folder") Folder folder,
    @Param("search") String search,
    @Param("dateFrom") LocalDateTime dateFrom,
    @Param("dateTo") LocalDateTime dateTo,
    Pageable pageable);
```

When all three filter parameters are `null` this query produces the same result set as the existing `findByFolder(Folder, Pageable)`. The facade always calls the new method and passes `null` for absent filters, making the old method unused and available for removal in a later cleanup.

**Rationale:** A single query avoids combinatorial branching across eight possible null/non-null combinations. JPQL's `(:param IS NULL OR ...)` idiom is well-supported by Hibernate and compiles to a single prepared statement that PostgreSQL can cache. The `LOWER(...) LIKE LOWER(...)` pattern gives case-insensitive matching without requiring a PostgreSQL `ILIKE` extension or a `CITEXT` column.

### 2. Date range bounds converted from `LocalDate` to `LocalDateTime` in the facade

**Decision:** `AssetController` accepts `@RequestParam(required = false) LocalDate dateFrom` and `@RequestParam(required = false) LocalDate dateTo` and passes them to `PhotoManagerFacade.getAssets`. The facade converts: `dateFromDt = (dateFrom != null) ? dateFrom.atStartOfDay() : null` and `dateToDt = (dateTo != null) ? dateTo.atTime(LocalTime.MAX) : null`. The repository query uses `LocalDateTime` parameters.

**Rationale:** `LocalDate` in the API is user-friendly (no time zone confusion) and unambiguous for day-based filtering. Converting to `LocalDateTime` in the facade keeps the repository query simple. Using `LocalTime.MAX` (23:59:59.999999999) for the upper bound makes the `dateTo` filter inclusive of the entire final day.

### 3. Facade signature extended with nullable filter parameters — backward-compatible call site

**Decision:** `PhotoManagerFacade.getAssets` signature becomes:

```java
PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria,
                                String search, LocalDate dateFrom, LocalDate dateTo);
```

`AssetController.getAssets` is the only call site; it passes the three new `@RequestParam(required = false)` values directly. No other facade callers exist.

**Rationale:** Adding parameters to the facade interface rather than creating an overload avoids ambiguity and keeps the interface surface minimal. Changing the existing method signature is safe because there is exactly one call site in the production code.

### 4. 400 ms debounce on the search input via `Subject<string>` + `debounceTime`

**Decision:** `GalleryComponent` holds a `private readonly searchSubject = new Subject<string>()`. In `ngOnInit`, it subscribes with `.pipe(debounceTime(400), distinctUntilChanged()).subscribe(() => { this.pageIndex = 0; this.loadAssets(); })`. The template binds `(input)="searchSubject.next($event.target.value)"` on the search `<input>`. The subscription is stored and unsubscribed in `ngOnDestroy`.

**Rationale:** Without debouncing, every character typed fires an HTTP request. 400 ms is the standard UX threshold — fast enough to feel responsive, long enough to avoid a request per keystroke. `distinctUntilChanged` prevents a redundant reload when the user types and immediately deletes the same character.

### 5. `MatNativeDateModule` for date pickers — no Moment.js dependency

**Decision:** `GalleryComponent` imports `MatDatepickerModule` and `MatNativeDateModule`. Date values are bound as `Date | null` properties (`dateFrom`, `dateTo`). On submit to the service the component formats them as `YYYY-MM-DD` strings using `date.toISOString().substring(0, 10)`.

**Rationale:** `MatNativeDateModule` uses the browser's built-in `Date` object and requires no extra npm dependency. The `YYYY-MM-DD` string format is unambiguous and matches the `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` annotation used on the Spring `LocalDate` parameters.

## Data Flow

```
User types in search field
  → Subject<string>.next(value) [400 ms debounce]
    → pageIndex = 0
    → loadAssets()
      → assetService.getAssets(folderPath, 0, sort, search, dateFrom, dateTo)
        → GET /api/assets?folderPath=…&page=0&sort=…&search=…&dateFrom=…&dateTo=…
          → AssetController.getAssets(folderPath, page, sort, search, dateFrom, dateTo)
            → facade.getAssets(folderPath, 0, sort, search, dateFrom, dateTo)
              → folderRepository.findByPath(folderPath) → Folder
              → convert dateFrom/dateTo to LocalDateTime bounds
              → assetRepository.findByFolderWithFilters(folder, search, dateFromDt, dateToDt, pageRequest)
              → return PaginatedData<Asset>
            → map to PaginatedData<AssetDto>
          → 200 OK
        → Observable<PaginatedData<Asset>>
      → assets = data.items; totalPages = data.totalPages; …
      → grid re-renders with filtered thumbnails

User changes folder
  → onFolderSelected(path) → clearFilters() → loadAssets()
```

## File Change List

**New files:**

- `backend/.../test/.../AssetControllerSearchTest.java` — `@WebMvcTest` for the extended `GET /api/assets`

**Modified files:**

- `backend/.../domain/repository/AssetRepository.java` — add `findByFolderWithFilters` JPQL query method
- `backend/.../application/PhotoManagerFacade.java` — extend `getAssets` signature with `search`, `dateFrom`, `dateTo`
- `backend/.../application/PhotoManagerFacadeImpl.java` — update `getAssets` implementation to convert dates and call new repository method
- `backend/.../api/AssetController.java` — add three `@RequestParam(required = false)` parameters to `getAssets` handler; add `@DateTimeFormat(iso = DATE)` on `LocalDate` params
- `frontend/src/app/core/services/asset.service.ts` — extend `getAssets` with optional `search?`, `dateFrom?`, `dateTo?` parameters; conditionally append to `HttpParams`
- `frontend/src/app/features/gallery/gallery.component.ts` — add `searchTerm`, `dateFrom`, `dateTo`, `searchSubject`; implement `onSearchChange`, `onDateChange`, `clearFilters`; subscribe/unsubscribe debounce; pass filters to `loadAssets`; import `Subject`, `debounceTime`, `distinctUntilChanged`, `MatDatepickerModule`, `MatNativeDateModule`, `MatInputModule`
- `frontend/src/app/features/gallery/gallery.component.html` — add filter toolbar row below main toolbar, inside `@if (viewMode === 'thumbnails')`
- `frontend/src/app/features/gallery/gallery.component.scss` — add `.filter-toolbar` layout styles
