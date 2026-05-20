# Planned Improvements

This document records all planned improvements to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them.

---

## Improvement List

| #   | Change name                 | Brief description                                                                                                                                                                       | Artifacts  | Implementation |
| --- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- |
| 1   | `exif-metadata-panel`       | Store full EXIF metadata during cataloging; display a collapsible panel in the viewer showing camera, exposure, GPS, and date-taken fields                                              | ✅ Created | ✅ Implemented |
| 2   | `virtual-scrolling-gallery` | Replace the CSS grid with a fixed-height list layout; render only visible items in the DOM via `CdkVirtualScrollViewport` with `*cdkVirtualFor`; remove the unused `ScrollingModule` import and the IntersectionObserver sentinel; each list row shows the thumbnail on the left and filename, size, and date metadata on the right | ⬜ Pending | ⬜ Pending |
| 3   | `drag-and-drop-upload`      | Upload image files directly from the browser by dropping them onto the gallery grid or clicking an Upload button; files are saved to the selected folder and indexed immediately        | ✅ Created | ✅ Implemented |
| 4   | `virtual-albums`            | User-scoped named collections; assets can belong to multiple albums; albums have their own paginated asset view and CRUD API                                                            | ✅ Created | ✅ Implemented |
| 5   | `refresh-token`             | Long-lived HttpOnly refresh-token cookie with rotate-on-use; Angular service proactively refreshes before expiry; interceptor retries 401s transparently                                | ✅ Created | ✅ Implemented |
| 6   | `bulk-download-zip`         | Download a selection of assets as a ZIP archive via a streaming `ZipOutputStream`; configurable per-request size cap                                                                    | ✅ Created | ✅ Implemented |
| 7   | `search-and-filter`         | Filter gallery assets by filename keyword, date-from, and date-to via a `findByFolderWithFilters` JPQL query; 400 ms debounced search input                                             | ✅ Created | ✅ Implemented |
| 8   | `slideshow-mode`            | Third `ViewMode` in the gallery that auto-advances through assets on a configurable interval; animated progress bar; keyboard shortcuts; pause on manual navigation                     | ✅ Created | ✅ Implemented |
| 9   | `soft-delete-recycle-bin`   | "Remove from catalog" sets `deleted_at` instead of deleting; recycle-bin page lists, restores, and purges soft-deleted assets; scheduled auto-purge after configurable retention period | ✅ Created | ✅ Implemented |
| 10  | `mobile-responsive-layout`  | Navigation collapses to a hamburger menu below 768 px; folder tree becomes a `MatSidenav` overlay drawer on mobile; thumbnail grid column minimum reduces to 140 px                     | ✅ Created | ✅ Implemented |
| 11  | `star-ratings`              | 0–5 star rating per asset stored in the database; rating widget on thumbnails and in the viewer; filter by minimum rating; sort by rating descending                                    | ✅ Created | ✅ Implemented |
| 12  | `saved-search-presets`      | Save the current filter state (search, date range, minimum rating) as a named preset scoped to the authenticated user; restore with one click from a dropdown in the filter toolbar     | ✅ Created | ✅ Implemented |
| 13  | `gps-map-view`              | Add a Leaflet.js map panel to the EXIF viewer and a `/map` route showing clustered photo pins for the current folder or album; clicking a pin navigates to that asset in the gallery   | ⬜ Pending | ⬜ Pending      |
| 14  | `smart-albums`              | Extend albums with an optional `filter_json` column (same shape as saved search presets) so albums can be dynamically populated at query time; UI toggle between static and dynamic mode | ⬜ Pending | ⬜ Pending      |
| 15  | `dark-mode`                 | Angular Material dark palette toggle stored in `localStorage` with `prefers-color-scheme` fallback; preference persisted per user on the backend; toolbar toggle button                 | ⬜ Pending | ⬜ Pending      |
| 16  | `keyboard-shortcuts`        | Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings               | ⬜ Pending | ⬜ Pending      |
| 17  | `batch-rename`              | Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint             | ⬜ Pending | ⬜ Pending      |
| 18  | `timeline-view`             | Third gallery view mode grouping assets by year then month with sticky date headers; `GET /api/assets/timeline-groups` returns counts per bucket; buckets load lazily via Intersection Observer | ⬜ Pending | ⬜ Pending |
| 19  | `shareable-album-links`     | `POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication | ⬜ Pending | ⬜ Pending |
| 20  | `storage-analytics`         | `/analytics` route with `ngx-charts` visualizations: storage-per-folder treemap, file-format pie, photos-per-month histogram, rating distribution bar; backed by aggregate JPQL queries  | ⬜ Pending | ⬜ Pending      |
| 21  | `video-file-support`        | Thumbnail generation via FFmpeg (`ProcessBuilder`) for `.mp4`/`.mov`/`.mkv`; catalog service accepts video MIME types; frontend shows play-overlay icon and `<video>` tag in the viewer  | ⬜ Pending | ⬜ Pending      |
| 22  | `duplicate-auto-resolve`    | "Clean up automatically" dialog on the duplicates page with policies (keep oldest, keep newest, keep highest resolution, keep preferred folder); delegates to existing soft-delete path   | ⬜ Pending | ⬜ Pending      |
| 23  | `progressive-web-app`       | `ng add @angular/pwa` with a cache-first strategy for thumbnails; background sync queue for offline rating/tag edits replayed on reconnect; HttpOnly cookie auth remains intact          | ⬜ Pending | ⬜ Pending      |
| 24  | `wallpaper-suggestion`      | Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button | ⬜ Pending | ⬜ Pending |
| 25  | `on-push-change-detection`  | Apply `ChangeDetectionStrategy.OnPush` to all 18 components; replace mutable state mutations with immutable assignments so Angular's OnPush check can detect changes; prioritise `ThumbnailComponent` (one instance per visible image) and `GalleryComponent` (the most complex); inject `ChangeDetectorRef` where manual `markForCheck()` calls are needed (e.g. after SSE events or async callbacks outside the Angular zone) | ⬜ Pending | ⬜ Pending |
| 26  | `thumbnail-http-cache`      | Add `Cache-Control: public, max-age=31536000, immutable` to the `GET /api/assets/{id}/thumbnail` response; thumbnails are content-addressed by asset ID and never mutated once written, making them safe for permanent browser and CDN caching; currently every gallery load re-fetches every thumbnail from disk with no cache headers set | ⬜ Pending | ⬜ Pending |
| 27  | `image-etag-cache`          | Add `ETag` (derived from the SHA-256 `hash` already stored on the `Asset` entity) and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response; enables conditional `If-None-Match` requests so the browser receives a `304 Not Modified` instead of re-downloading the full image on repeat views; currently no cache headers are set on this endpoint | ⬜ Pending | ⬜ Pending |
| 28  | `server-side-spring-cache`  | Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required | ⬜ Pending | ⬜ Pending |
| 29  | `exif-cache-service`        | Move the `Map<number, ExifMetadata \| null>` from `ExifPanelComponent` (destroyed on every navigation) to a singleton `ExifCacheService`; the cache currently lives only for the lifetime of the component instance, so navigating away and back to the viewer discards all fetched EXIF data and triggers redundant API calls; a service-level cache persists for the entire session | ⬜ Pending | ⬜ Pending |
| 30  | `image-rotation-viewer`     | Apply the `imageRotation` field already stored on `Asset` as a CSS `transform: rotate()` in `ThumbnailComponent` and the full-size viewer `<img>`; photos taken in portrait orientation are currently displayed sideways because the raw file is served without orientation correction; no backend change and no new API call are required | ⬜ Pending | ⬜ Pending |
| 31  | `full-text-search`          | Extend search beyond filename-only `LIKE` matching to cover tags, EXIF camera model, and the `description` field (#37) using PostgreSQL native `tsvector`/`tsquery` with a `GIN` index; a generated `search_vector` column is maintained automatically by a trigger; the existing `findByFolderWithFilters` JPQL method gains a `search_vector @@ to_tsquery(...)` predicate; ranked results via `ts_rank` | ⬜ Pending | ⬜ Pending |
| 32  | `folder-watch-service`      | Add a Java NIO `WatchService` that monitors all configured root catalog folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events and triggers incremental catalog updates automatically; reuses the existing `CatalogAssetsUseCase` and `catalog_run_state` distributed lock as the execution path; keeps the catalog in sync without any manual user action | ⬜ Pending | ⬜ Pending |
| 33  | `role-based-access-control` | Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value | ⬜ Pending | ⬜ Pending |
| 34  | `openapi-documentation`     | Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`; annotate all 11 `@RestController` classes with `@Operation` and `@ApiResponse`; exposes live Swagger UI at `/swagger-ui.html` and a machine-readable spec at `/v3/api-docs`; no schema change required; document that the `/swagger-ui.html` endpoint must be exempted from JWT authentication in `SecurityConfig` | ⬜ Pending | ⬜ Pending |
| 35  | `thumbnail-regeneration`    | Add `POST /api/assets/regenerate-thumbnails` (optionally scoped by `folderPath` query param) that deletes existing `.bin` thumbnail files via `ThumbnailPort` and re-generates them through `StoragePort`; covers corrupted thumbnails, thumbnail size changes, and retroactive EXIF-rotation correction; reuses existing infrastructure adapters with no schema change | ⬜ Pending | ⬜ Pending |
| 36  | `global-error-handler`      | Override Angular's `ErrorHandler` to display a `MatSnackBar` notification for all unhandled component errors; extend the existing backend `GlobalExceptionHandler` to return a consistent `{ status, message, timestamp }` JSON body for every 4xx and 5xx response so the frontend interceptor can surface a human-readable message rather than showing a raw HTTP status | ⬜ Pending | ⬜ Pending |
| 37  | `asset-description`         | Add a `description` VARCHAR column to `assets` (new Flyway migration); expose `PATCH /api/assets/{id}/description`; display an editable text field in the EXIF panel in the viewer; the description field feeds directly into the `full-text-search` (#31) index once both are in place | ⬜ Pending | ⬜ Pending |
| 38  | `folder-stats-in-tree`      | Show asset count and total size as a secondary line per folder node in the folder navigation tree; backed by `GET /api/folders/stats?path=...` running a lightweight `SELECT COUNT(*), SUM(file_size)` query per folder; distinct from the full analytics dashboard (#20) — this is inline contextual data in the tree, not a separate page | ⬜ Pending | ⬜ Pending |
| 39  | `api-rate-limiting`         | Add `bucket4j-spring-boot-starter` with per-IP token-bucket limits on `POST /api/auth/login` (brute-force prevention) and `GET /api/assets/catalog` (prevent accidental concurrent runs from multiple browser tabs); returns `429 Too Many Requests` with a `Retry-After` header when the bucket is exhausted; no schema change required | ⬜ Pending | ⬜ Pending |
| 40  | `circuit-breaker`           | Add Resilience4j `@CircuitBreaker` on the `GeocodingPort` adapter introduced by `gps-map-view` (#13); if the external geocoding API (e.g. Nominatim) is slow or unavailable the circuit opens and the adapter returns `null` coordinates immediately rather than stalling the EXIF panel; scope extends to any future outbound HTTP adapters (cloud storage, email); not applicable to the current backend which makes no external calls — PostgreSQL and filesystem failure modes are already covered by HikariCP | ⬜ Pending | ⬜ Pending |
| 41  | `actuator-health-indicators` | `spring-boot-starter-actuator` is already in `pom.xml` but has no `management.*` configuration and `/actuator/**` is blocked by the JWT filter; expose `/actuator/health` in `SecurityConfig.permitAll()`; add `management.endpoints.web.exposure.include=health,info` to `application.yml`; implement three custom `HealthIndicator` beans: disk space on the thumbnails directory, thumbnails directory writability, and PostgreSQL connectivity — covering the two most likely runtime failures | ⬜ Pending | ⬜ Pending |
| 42  | `metrics-prometheus`        | Add `micrometer-registry-prometheus` to `pom.xml` and expose `/actuator/prometheus`; instrument three application-specific metrics not covered by Spring Boot's default JVM/HTTP metrics: a `Timer` on catalog duration per folder, a `Timer` on thumbnail generation, and a `Gauge` tracking active SSE connections (catalog, sync, convert); add Prometheus and Grafana services to `docker-compose.yml` with a pre-built dashboard | ⬜ Pending | ⬜ Pending |
| 43  | `request-correlation-mdc`   | Add a servlet `Filter` that injects a `requestId` UUID and the authenticated `username` into SLF4J `MDC` at the start of each request and clears it on completion; `logstash-logback-encoder` is already configured in `logback-spring.xml` and will automatically include both fields in every JSON log line; also set `X-Request-ID` on the response so the Angular frontend can log the correlation ID alongside client-side errors from `global-error-handler` (#36) | ⬜ Pending | ⬜ Pending |
| 44  | `database-backup`           | Add `DatabaseBackupService` with `@Scheduled` that runs `pg_dump` via `ProcessBuilder`, compresses the output with GZip to a temp file, and uploads it through a new `CloudStoragePort` interface (domain) with swappable infrastructure implementations for AWS S3, Google Cloud Storage, and Azure Blob; enforce a configurable retention policy (delete backups older than N days); expose `POST /api/admin/backup` for on-demand trigger and `GET /api/admin/backups` to list stored backups with timestamps and sizes; schedule, retention, cloud provider, bucket, and prefix are all configurable in `application.yml`; Option B (Docker sidecar using `prodrigestivill/postgres-backup-local` + `rclone`) is documented as a deployment alternative for environments where backup must be decoupled from application health | ⬜ Pending | ⬜ Pending |
| 45  | `postgres-dockerize`        | The application currently connects to a PostgreSQL instance running on the host (`POSTGRES_HOST` defaults to `localhost`); `docker-compose.yml` already declares a `db` service and a `pgdata` named volume but is not the active deployment path; complete the transition by: (1) fixing the volume mount from `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data` to align with the PostgreSQL `PGDATA` default, (2) setting `PGDATA: /var/lib/postgresql/data` explicitly in the `db` service environment, (3) making `docker-compose up` the canonical deployment command and updating `README.md` accordingly, and (4) providing a one-time data migration script (`pg_dump` on host → `pg_restore` into the container) so existing data is not lost on first deployment | ⬜ Pending | ⬜ Pending |

---

## Dependencies

### Hard implementation dependencies

**None.** Every improvement can be built and deployed independently.

### Soft implementation dependencies (order affects cleanliness)

**Improvement 11 → Improvement 7**

Both share the `findByFolderWithFilters` JPQL method in `AssetRepository`. If improvement 7 (search-and-filter) is implemented first, improvement 11 (star-ratings) simply extends the existing JPQL with an additional `minRating` predicate. If improvement 11 is implemented first, it must create the full method from scratch. The tasks.md for improvement 11 documents both paths explicitly.

### Functional dependencies (feature is significantly less useful without)

**Improvement 12 → Improvements 7 + 11**

Saved search presets store `{ search, dateFrom, dateTo, minRating }`. Without improvement 7, there are no `search`/`dateFrom`/`dateTo` filter fields to persist. Without improvement 11, there is no `minRating`. A preset system that can only store an empty object provides no practical value. Improvement 12 should be implemented after both 7 and 11 are in place.

### Deployment (migration) dependencies

Flyway migration versions must be applied in ascending order. Skipping a version causes Flyway to halt on startup. The planned migration sequence is:

| Migration | Feature                                                     |
| --------- | ----------------------------------------------------------- |
| V7        | `exif-metadata-panel` — EXIF columns on `assets`            |
| V8        | `virtual-albums` — `albums` and `album_assets` tables       |
| V9        | `refresh-token` — `refresh_tokens` table                    |
| V10       | `soft-delete-recycle-bin` — `deleted_at` column on `assets` |
| V11       | `star-ratings` — `rating` column on `assets`                |
| V12       | `saved-search-presets` — `search_presets` table             |

Improvements that touch no database schema (2, 3, 5 frontend parts, 6, 8, 10) have no migration and no deployment ordering constraint relative to each other.

Among the pending improvements, those that require a Flyway migration are:

| Migration | Feature                                                                        |
| --------- | ------------------------------------------------------------------------------ |
| V13       | `smart-albums` — `filter_json` column on `albums`                              |
| V14       | `shareable-album-links` — `shared_albums` table                                |
| V15       | `wallpaper-suggestion` — `aspect_ratio` column on `assets`                    |
| V16       | `asset-description` — `description` column on `assets`                        |
| V17       | `full-text-search` — `search_vector` generated column + `GIN` index on `assets` |

Note: `pixel_width` and `pixel_height` are already present on `assets`; only the derived `aspect_ratio` column is new. The backfill (`aspect_ratio = pixel_width / pixel_height`) must be included in the V15 migration to populate existing rows. Assets where either dimension is zero are left as `NULL` and excluded from wallpaper queries.

The V17 migration must run after V16 because the `search_vector` generated column combines `file_name`, `description`, and tag data; the `description` column must exist before the generated column can reference it.

### Hard implementation dependencies (new improvements)

**Improvement 13 → Improvement 1**

`gps-map-view` requires GPS coordinates already stored in `asset_exif`. Without `exif-metadata-panel` the coordinates are never persisted.

**Improvement 14 → Improvements 4, 7, 11**

`smart-albums` stores a `filter_json` that references the same fields introduced by `search-and-filter` (7) and `star-ratings` (11), and it extends the album schema introduced by `virtual-albums` (4). All three should be in place first.

**Improvement 19 → Improvement 4**

`shareable-album-links` requires the `albums` table introduced by `virtual-albums`.

**Improvement 22 → Improvement 9**

`duplicate-auto-resolve` routes deleted assets through the soft-delete path introduced by `soft-delete-recycle-bin`.

### Soft implementation dependencies (new improvements)

**Improvement 16 → Improvement 8**

`keyboard-shortcuts` extends the viewer shortcuts already present in `slideshow-mode`. Implementing 8 first avoids re-doing viewer key handling.

**Improvement 23 → Improvement 10**

`progressive-web-app` is significantly more valuable once `mobile-responsive-layout` is in place, since PWA installs are most common on mobile.

### Recommended implementation order

For a clean, incremental delivery the natural order within the data-model cluster is:

```
7 (search-and-filter) → 11 (star-ratings) → 12 (saved-search-presets)
```

For the new improvements, the recommended order within dependent clusters is:

```
1 (exif-metadata-panel, already done) → 13 (gps-map-view)
4 (virtual-albums, already done) + 7 + 11 → 14 (smart-albums)
4 (virtual-albums, already done) → 19 (shareable-album-links)
9 (soft-delete-recycle-bin, already done) → 22 (duplicate-auto-resolve)
8 (slideshow-mode, already done) → 16 (keyboard-shortcuts)
10 (mobile-responsive-layout, already done) → 23 (progressive-web-app)
```

Improvements 15 (dark-mode), 17 (batch-rename), 18 (timeline-view), 20 (storage-analytics), 21 (video-file-support), 24 (wallpaper-suggestion), 26 (thumbnail-http-cache), 27 (image-etag-cache), 28 (server-side-spring-cache), and 29 (exif-cache-service) have no hard dependencies and can be delivered in any order.

**Improvement 26 and 27 → Improvement 23**

`thumbnail-http-cache` sets browser-level `Cache-Control` headers on the thumbnail endpoint. `progressive-web-app` (#23) adds a service-worker cache-first strategy for the same endpoint. Implementing #26 first means the service worker inherits already-correct cache semantics and does not need to re-define them. Implementing them in reverse order still works but creates redundancy.

**Improvement 27 → no schema change**

The `ETag` value is derived from the SHA-256 `hash` column already present on the `Asset` entity; no Flyway migration is needed.

**Improvement 28 — cache eviction boundary**

`@CacheEvict` must be applied to every write path that invalidates a cached result: cataloging (evicts folder and stats caches), rating (evicts stats), tagging (evicts stats), and soft-delete (evicts folder, stats, and EXIF caches). Failing to wire eviction on any write path will serve stale data silently.

**Improvement 25 → Improvement 2**

`on-push-change-detection` should be applied after `virtual-scrolling-gallery` is completed. The list layout introduced by #2 uses fixed-height items with immutable `*cdkVirtualFor` bindings; applying OnPush before that layout is in place risks masking change-detection bugs while the old grid and IntersectionObserver are still present.

**Improvement 31 → Improvement 37**

`full-text-search` is more valuable when the `description` field from `asset-description` is available to index. The V17 migration (search vector) hard-depends on the V16 migration (description column) being applied first. Implementing #37 before #31 avoids rewriting the generated column definition.

**Improvement 31 → Improvement 37 (functional)**

A full-text search that can only index filename and tags is still useful, but the `description` field is the primary free-text input that justifies the investment in a PostgreSQL GIN index. Implementing both together delivers the full value.

**Improvement 37 → Improvement 31 (soft)**

`asset-description` can be delivered standalone and provides immediate value in the EXIF panel before full-text search is wired up.

**Improvement 30 — no dependencies**

`image-rotation-viewer` reads `imageRotation` already stored on `Asset`; it is a pure frontend change with no backend or schema involvement.

**Improvement 33 — enforcement boundary**

`role-based-access-control` must apply `@PreAuthorize` to every write-path use case method, not just controller endpoints, to prevent bypasses if new controllers are added later. The `VIEWER` role must be blocked at the use-case layer.

**Improvement 35 — ordering note**

`thumbnail-regeneration` is most useful after `image-rotation-viewer` (#30) is in place, since one motivation for re-generation is applying orientation correction to existing thumbnails. The two can be delivered independently but pair naturally.

**Improvements 32, 33, 34, 36, 38, 39 — no dependencies**

`folder-watch-service`, `role-based-access-control`, `openapi-documentation`, `global-error-handler`, `folder-stats-in-tree`, and `api-rate-limiting` have no hard dependencies on other pending improvements and can be delivered in any order.

**Improvement 42 → Improvement 41**

`metrics-prometheus` adds a second Actuator endpoint (`/actuator/prometheus`). Implementing `actuator-health-indicators` (#41) first means the `SecurityConfig` and `management.*` configuration are already in place; #42 only needs to add the Prometheus registry dependency and extend the `exposure.include` property rather than setting it up from scratch.

**Improvement 43 → Improvement 36**

`request-correlation-mdc` pairs with `global-error-handler` (#36). The Angular `ErrorHandler` can read the `X-Request-ID` response header and include it in the error snackbar or log payload, linking a user-visible error directly to the backend log entries for that request.

**Improvements 41, 42, 43 — no schema changes**

All three observability improvements are purely operational: no Flyway migrations, no domain model changes, and no new API endpoints visible to end users.

**Improvement 44 → Improvement 33**

`database-backup` exposes admin-only endpoints (`POST /api/admin/backup`, `GET /api/admin/backups`). Once `role-based-access-control` (#33) is in place these endpoints should be restricted to the `ADMIN` role. They can be delivered before #33 with a simple `@PreAuthorize("hasRole('ADMIN')")` placeholder, but the RBAC enforcement is only meaningful after #33 is complete.

**Improvement 44 — no schema change**

`database-backup` adds no Flyway migration; the backup metadata (listing stored files) is read directly from cloud storage at request time rather than persisted to the database.

**Improvement 45 → Improvement 44**

`postgres-dockerize` must be completed before `database-backup` (#44). The backup service runs `pg_dump` against the database container over the Docker network (`POSTGRES_HOST: db`); this only works once the database is running inside the Compose stack. Running `pg_dump` against a host-based PostgreSQL from inside a container requires additional network configuration (`host.docker.internal` or `--network host`) that would be rendered unnecessary once #45 is done. The one-time host-to-container data migration script required by #45 also serves as the first real test of the dump/restore tooling that #44 will depend on in production.

**Improvement 40 → Improvement 13**

`circuit-breaker` is a follow-up to `gps-map-view`. It is not warranted in the current architecture because the backend makes no outbound calls to external services — PostgreSQL and the filesystem are the only dependencies, and the HikariCP connection pool already handles their failure modes. Once #13 introduces an outbound HTTP call to a geocoding API (e.g. Nominatim or Google Maps), that call becomes the first concrete circuit breaker target: if the geocoding service is slow or unavailable it will stall the EXIF panel for every viewed image. At that point, wrapping the `GeocodingPort` adapter with Resilience4j `@CircuitBreaker` and returning a fallback of `null` coordinates is the right response. Additional future improvements that would extend the scope of #40 include cloud storage integration and external email notifications if those are ever added.
