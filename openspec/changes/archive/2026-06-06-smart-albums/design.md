## Context

The application already has three interdependent features that this change composes:

1. **Virtual albums** (`V8__add_albums.sql`): `albums` table with `album_id, user_id, name, description, created_at`; `album_assets` join table. `AlbumEntity`, `Album` domain model, `AlbumRepository` port, `AlbumRepositoryImpl` adapter, six use-case interfaces and implementations, `AlbumController`.

2. **Search and filter**: `GET /api/assets` accepts `search`, `dateFrom`, `dateTo`, `minRating`, `tags` query parameters. These are assembled into an `AssetFilter` record and passed to `AssetRepository.findFiltered(AssetFilter)`, which delegates to `JpaAssetRepository.findWithFilters(...)` — a JPA Criteria API `Specification` that applies each non-null predicate dynamically. The `folderId` field of `AssetFilter` constrains results to one folder; when `folderId` is null the current implementation includes it as a mandatory predicate (this changes with smart albums — see Decision 2).

3. **Saved search presets** (`V12__add_search_presets.sql`): `search_presets` table stores `filter_json TEXT` — a JSON-serialized `FilterPreset` record with fields `{ search?, dateFrom?, dateTo?, minRating? }`. Jackson is used to serialize/deserialize. The `FilterPreset` record is in `application/dto/` and is the canonical shape for persisting filter criteria as JSON.

The key insight is that a smart album IS a saved search preset promoted to a first-class album: it has a name, is user-scoped, and displays in the `/albums` route with a thumbnail grid — but its contents are determined dynamically by `FilterPreset` criteria rather than the static `album_assets` table. The implementation reuses the existing filter query path rather than building a new one.

## Goals / Non-Goals

**Goals:**

- Extend `albums` with a nullable `filter_json JSONB` column using the same `FilterPreset` shape as `search_presets`.
- When `filter_json` is non-null, serve album assets from `AssetRepository.findFiltered` (across all folders) instead of the `album_assets` join table.
- Allow creating and updating albums in smart mode via `CreateAlbumRequest` / `UpdateAlbumRequest` optional `filterJson` field.
- Surface smart-album status in the frontend: badge on album cards, mode indicator in detail page, hidden add/remove controls for smart albums.
- Allow toggling an album between static and smart mode via `PUT /api/albums/{id}`.

**Non-Goals:**

- Adding new filter fields beyond the existing `FilterPreset` shape (`search`, `dateFrom`, `dateTo`, `minRating`). Tag-based smart album filtering is out of scope.
- Saving a smart album's current results as a new static album ("freeze").
- Combining smart and static: an album is either entirely smart or entirely static — no hybrid membership.
- Real-time push updates when catalog changes add/remove assets that match a smart album's criteria.
- Performance caching of smart album counts or result sets.

## Decisions

### 1. Store `filter_json` as `JSONB` in PostgreSQL — not `TEXT`

**Decision:** The new column is `filter_json JSONB NULL` rather than `TEXT`.

**Rationale:** The `search_presets` table uses `TEXT` for `filter_json`, which was acceptable because preset JSON is never queried server-side (it is only written and read back whole). For albums, having `JSONB` opens the door to future server-side filtering on criteria (e.g. `WHERE filter_json->>'minRating' IS NOT NULL`) without a schema change, and PostgreSQL's JSONB validation rejects malformed JSON at write time rather than failing silently at read time. The Jackson deserialization path in the service layer is unchanged either way.

**Alternative:** Keep `TEXT` for consistency with `search_presets`. Rejected because we gain nothing from the inconsistency and lose JSONB's type safety and future indexing potential.

### 2. Null `folderId` in `AssetFilter` means "all folders" for smart album queries

**Decision:** `AssetRepositoryImpl.findFiltered` is extended so that when `AssetFilter.folderId()` is `null`, the folder predicate in `JpaAssetRepository.findWithFilters` is omitted entirely, returning assets across all folders. The static gallery path always supplies a non-null `folderId`. Smart album queries construct an `AssetFilter` with `folderId = null`.

**Rationale:** `JpaAssetRepository.findWithFilters` already uses a `Specification` (Criteria API) that builds predicates conditionally for search, date range, rating, and tags. Making the folder predicate equally conditional is a minimal change: add `if (folder != null)` around the folder equality predicate. This avoids duplicating the Specification logic for the "all folders" case. The change is backward-compatible because the gallery always passes a non-null folder entity.

**Alternative:** Add a separate `findWithFiltersGlobal` method to `JpaAssetRepository` that omits the folder predicate. Rejected as duplication — the Specification is already parameterised; one conditional branch is cleaner.

### 3. Smart vs static branching lives in `GetAlbumUseCaseImpl` — not in the repository

**Decision:** `GetAlbumUseCaseImpl.executeAssets` inspects `album.getFilterJson()`. If non-null, it deserializes to `FilterPreset` using Jackson's `ObjectMapper`, constructs an `AssetFilter` with `folderId = null`, and calls `assetRepository.findFiltered(filter)`. If null, it calls `albumRepository.findAssetsByAlbumId(albumId, page, PAGE_SIZE)` as before. The same branching applies in `executeSummary` for the `assetCount` field.

**Rationale:** The use case is the correct layer for orchestration decisions. The repository layer should not need to know about the album/smart distinction — it should expose generic query methods. The `ObjectMapper` is already available as a Spring bean (used by `SearchPresetServiceImpl`); the use case can declare it as a constructor dependency with `@RequiredArgsConstructor`.

**Alternative:** Add a `findAlbumAssets(Long albumId, String filterJson, int page, int pageSize)` method to `AlbumRepository` that does the branching internally. Rejected because it mixes concerns: the repository would need to parse JSON and call `AssetRepository` — a cross-repository dependency that violates the single-responsibility principle.

### 4. `filterJson` travels as a JSON object (not a string) in the HTTP API

**Decision:** In `CreateAlbumRequest`, `UpdateAlbumRequest`, `AlbumSummaryDto`, and `AlbumDto`, the `filterJson` field is typed as a nested object with fields `search`, `dateFrom`, `dateTo`, `minRating` — not as a raw JSON string. The backend serializes this to a `String` for storage using `ObjectMapper.writeValueAsString` (same pattern as `SearchPresetServiceImpl`).

**Rationale:** Returning a raw JSON string embedded inside a JSON response body is error-prone (double-escaping). The frontend TypeScript model mirrors the same structure as `SearchPreset` and can apply the preset values directly without parsing. The `AlbumFilterJson` TypeScript interface is identical to the filter fields on `SearchPreset`.

**Alternative:** Return `filterJson` as the raw string stored in the DB. Rejected as described above.

### 5. Existing `album_assets` rows are preserved when an album is converted to smart mode

**Decision:** When `PUT /api/albums/{id}` sets a non-null `filterJson`, the album's existing static membership rows in `album_assets` are NOT deleted. If the album is later reverted to static mode (by passing `filterJson: null`), the previously added static assets become visible again.

**Rationale:** Destructive conversion (deleting static memberships on smart-mode switch) would be irreversible and surprising. Users who experiment with smart mode and then revert should not lose their curation work. The `album_assets` rows are simply ignored while the album is in smart mode.

**Alternative:** Clear `album_assets` on conversion to smart mode to avoid confusion. Rejected because data loss on toggle is worse than having temporarily invisible rows.

### 6. "Add to album" dialog in `GalleryComponent` disables smart albums as targets

**Decision:** The `AddToAlbumDialogComponent` filters `userAlbums` to show only static albums (those where `filterJson` is null) as selectable targets. Smart albums appear in the list but are rendered as disabled options with a tooltip "Smart album — managed automatically".

**Rationale:** Adding an asset to a smart album via the join table is meaningless: the album's contents are always recomputed from the filter. Silently allowing the add operation would confuse users who see the asset disappear from the album on the next page load (because the join table is ignored for smart albums). Showing smart albums as disabled makes the distinction visible without hiding them entirely.

## Risks / Trade-offs

- **Performance of cross-folder queries:** Smart album queries run against all folders rather than a single folder directory. For catalogs with tens of thousands of assets the `findWithFilters` Criteria query currently has a `folder` predicate that limits the scan. Removing that predicate increases the query scan range. The existing indexes on `file_name`, `file_creation_date_time`, and `rating` columns mitigate this, but very large catalogs (hundreds of thousands of assets) may see slower smart album page loads compared to static albums. A future `LIMIT/OFFSET` is already applied via `Pageable`, so full table scans are bounded to the count query. Accepted as a known trade-off; caching is a non-goal for this iteration.

- **Count query for smart albums runs a separate `COUNT(*)` via `Specification`:** The existing `countAssets(Long albumId)` query on `JpaAlbumRepository` uses a JPQL count over `album_assets`. For smart albums we must issue a separate `count` query through `JpaSpecificationExecutor`. Spring Data JPA generates these automatically when `findAll(Specification, Pageable)` is called — the `Page` object returned by `findAll` includes `getTotalElements()` without a separate call. So `countAssets` for smart albums is obtained from the `Page.getTotalElements()` of the first-page query rather than a standalone count, which means the summary count and the asset page are fetched in a single round-trip. Accepted.

- **Toggling mode does not clear `album_assets`:** As documented in Decision 5, static membership rows persist invisibly while an album is in smart mode. This is intentional but adds a small amount of orphan-like data. A future cleanup migration or admin endpoint can address this if it becomes significant.
