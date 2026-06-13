# Planned Improvements

This document records all **pending** improvements to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them. For completed improvements see `improvements-implemented.md`.

---

## Improvement List

| #   | Change name                 | Brief description                                                                                                                                                                       | Artifacts  | Implementation |
| --- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- |
| 13  | `gps-map-view`              | Add a Leaflet.js map panel to the EXIF viewer and a `/map` route showing clustered photo pins for the current folder or album; clicking a pin navigates to that asset in the gallery   | ✅ Created | ⬜ Pending      |
| 16  | `keyboard-shortcuts`        | Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings               | ✅ Created | ⬜ Pending      |
| 17  | `batch-rename`              | Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint             | ✅ Created | ⬜ Pending      |
| 19  | `shareable-album-links`     | `POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication | ✅ Created | ⬜ Pending |
| 22  | `duplicate-auto-resolve`    | "Clean up automatically" dialog on the duplicates page with policies (keep oldest, keep newest, keep highest resolution, keep preferred folder); delegates to existing soft-delete path   | ✅ Created | ⬜ Pending      |
| 23  | `progressive-web-app`       | `ng add @angular/pwa` with a cache-first strategy for thumbnails; background sync queue for offline rating/tag edits replayed on reconnect; HttpOnly cookie auth remains intact          | ✅ Created | ⬜ Pending      |
| 24  | `wallpaper-suggestion`      | Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button | ✅ Created | ⬜ Pending |
| 25  | `on-push-change-detection`  | Apply `ChangeDetectionStrategy.OnPush` to all 18 components; replace mutable state mutations with immutable assignments so Angular's OnPush check can detect changes; prioritise `ThumbnailComponent` (one instance per visible image) and `GalleryComponent` (the most complex); inject `ChangeDetectorRef` where manual `markForCheck()` calls are needed (e.g. after SSE events or async callbacks outside the Angular zone) | ✅ Created | ⬜ Pending |
| 26  | `thumbnail-http-cache`      | Add `Cache-Control: public, max-age=31536000, immutable` to the `GET /api/assets/{id}/thumbnail` response; thumbnails are content-addressed by asset ID and never mutated once written, making them safe for permanent browser and CDN caching; currently every gallery load re-fetches every thumbnail from disk with no cache headers set | ✅ Created | ⬜ Pending |
| 27  | `image-etag-cache`          | Add `ETag` (derived from the SHA-256 `hash` already stored on the `Asset` entity) and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response; enables conditional `If-None-Match` requests so the browser receives a `304 Not Modified` instead of re-downloading the full image on repeat views; currently no cache headers are set on this endpoint | ✅ Created | ⬜ Pending |
| 28  | `server-side-spring-cache`  | Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required | ✅ Created | ⬜ Pending |
| 29  | `exif-cache-service`        | Move the `Map<number, ExifMetadata \| null>` from `ExifPanelComponent` (destroyed on every navigation) to a singleton `ExifCacheService`; the cache currently lives only for the lifetime of the component instance, so navigating away and back to the viewer discards all fetched EXIF data and triggers redundant API calls; a service-level cache persists for the entire session | ✅ Created | ⬜ Pending |
| 30  | `image-rotation-viewer`     | Apply the `imageRotation` field already stored on `Asset` as a CSS `transform: rotate()` in `ThumbnailComponent` and the full-size viewer `<img>`; photos taken in portrait orientation are currently displayed sideways because the raw file is served without orientation correction; no backend change and no new API call are required | ✅ Created | ⬜ Pending |
| 31  | `full-text-search`          | Extend search beyond filename-only `LIKE` matching to cover tags, EXIF camera model, and the `description` field (#37) using PostgreSQL native `tsvector`/`tsquery` with a `GIN` index; a generated `search_vector` column is maintained automatically by a trigger; the existing `findByFolderWithFilters` JPQL method gains a `search_vector @@ to_tsquery(...)` predicate; ranked results via `ts_rank` | ✅ Created | ⬜ Pending |
| 32  | `folder-watch-service`      | Add a Java NIO `WatchService` that monitors all configured root catalog folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events and triggers incremental catalog updates automatically; reuses the existing `CatalogAssetsUseCase` and Spring Batch `JobLauncher` as the execution path; keeps the catalog in sync without any manual user action | ✅ Created | ⬜ Pending |
| 33  | `role-based-access-control` | Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value | ✅ Created | ⬜ Pending |
| 35  | `thumbnail-regeneration`    | Add `POST /api/assets/regenerate-thumbnails` (optionally scoped by `folderPath` query param) that deletes existing `.bin` thumbnail files via `ThumbnailPort` and re-generates them through `StoragePort`; covers corrupted thumbnails, thumbnail size changes, and retroactive EXIF-rotation correction; reuses existing infrastructure adapters with no schema change | ✅ Created | ⬜ Pending |
| 36  | `global-error-handler`      | Override Angular's `ErrorHandler` to display a `MatSnackBar` notification for all unhandled component errors; extend the existing backend `GlobalExceptionHandler` to return a consistent `{ status, message, timestamp }` JSON body for every 4xx and 5xx response so the frontend interceptor can surface a human-readable message rather than showing a raw HTTP status | ✅ Created | ⬜ Pending |
| 37  | `asset-description`         | Add a `description` VARCHAR column to `assets` (new Flyway migration); expose `PATCH /api/assets/{id}/description`; display an editable text field in the EXIF panel in the viewer; the description field feeds directly into the `full-text-search` (#31) index once both are in place | ✅ Created | ⬜ Pending |
| 38  | `folder-stats-in-tree`      | Show asset count and total size as a secondary line per folder node in the folder navigation tree; backed by `GET /api/folders/stats?path=...` running a lightweight `SELECT COUNT(*), SUM(file_size)` query per folder; distinct from the full analytics dashboard (#20) — this is inline contextual data in the tree, not a separate page | ✅ Created | ⬜ Pending |
| 40  | `circuit-breaker`           | Add Resilience4j `@CircuitBreaker` on the `GeocodingPort` adapter introduced by `gps-map-view` (#13); if the external geocoding API (e.g. Nominatim) is slow or unavailable the circuit opens and the adapter returns `null` coordinates immediately rather than stalling the EXIF panel; scope extends to any future outbound HTTP adapters (cloud storage, email); not applicable to the current backend which makes no external calls — PostgreSQL and filesystem failure modes are already covered by HikariCP | ✅ Created | ⬜ Pending |
| 43  | `request-correlation-mdc`   | Add a servlet `Filter` that injects a `requestId` UUID and the authenticated `username` into SLF4J `MDC` at the start of each request and clears it on completion; `logstash-logback-encoder` is already configured in `logback-spring.xml` and will automatically include both fields in every JSON log line; also set `X-Request-ID` on the response so the Angular frontend can log the correlation ID alongside client-side errors from `global-error-handler` (#36) | ✅ Created | ⬜ Pending |
| 44  | `database-backup`           | Add `DatabaseBackupService` with `@Scheduled` that runs `pg_dump` via `ProcessBuilder`, compresses the output with GZip to a temp file, and uploads it through a new `CloudStoragePort` interface (domain) with swappable infrastructure implementations for AWS S3, Google Cloud Storage, and Azure Blob; enforce a configurable retention policy (delete backups older than N days); expose `POST /api/admin/backup` for on-demand trigger and `GET /api/admin/backups` to list stored backups with timestamps and sizes; schedule, retention, cloud provider, bucket, and prefix are all configurable in `application.yml`; Option B (Docker sidecar using `prodrigestivill/postgres-backup-local` + `rclone`) is documented as a deployment alternative for environments where backup must be decoupled from application health | ✅ Created | ⬜ Pending |
| 46  | `session-management`        | The `refresh_tokens` table already stores `userId`, `tokenHash`, and `expiresAt`; add an optional `user_agent` column and expose `GET /api/auth/sessions` (list active sessions with device hint and last-used time), `DELETE /api/auth/sessions/{id}` (revoke one), and `DELETE /api/auth/sessions` (revoke all others); frontend `/profile/sessions` page lists sessions in a `MatTable` with a revoke button per row and a "sign out everywhere" action | ✅ Created | ⬜ Pending |
| 47  | `two-factor-authentication` | TOTP-based 2FA via any RFC 6238-compliant authenticator app (Google Authenticator, Authy, 1Password); backend dependencies: `dev.samstevens.totp:totp` (secret generation, code verification, `otpauth://` URI building) and `com.google.zxing:core` + `com.google.zxing:javase` (QR code PNG encoding returned as base64); new `totp_secret` (AES-encrypted at rest) and `totp_enabled` boolean columns on `users` (Flyway migration); setup flow: `POST /api/auth/2fa/setup` returns a base64 QR code PNG, `POST /api/auth/2fa/verify` validates the first code and commits the secret; login flow: password check passes → if `totp_enabled` return a `202 TOTP_REQUIRED` challenge → `POST /api/auth/2fa/challenge` validates code and sets JWT cookie; generate 10 single-use backup codes (BCrypt-hashed, stored in `totp_backup_codes` table) in case the authenticator device is lost; TOTP verification endpoint must be covered by `api-rate-limiting` (#39) | ✅ Created | ⬜ Pending |
| 48  | `email-notifications`       | Spring Mail (`spring-boot-starter-mail`) to send a summary email when long-running operations complete: catalog (N new assets, M updated), sync result, convert result, and backup uploaded; add `email` VARCHAR and `email_notifications_enabled` boolean to `users` (Flyway migration); configurable SMTP host, port, and credentials in `application.yml`; frontend profile page gains an email field and notification toggle; pairs naturally with `notification-center` (#54) as both are triggered by the same operation-completion events | ✅ Created | ⬜ Pending |
| 49  | `auto-tagging`              | During cataloging, automatically apply tags derived from EXIF data: the year from `dateTaken`, camera make normalised to lowercase (e.g. `canon`, `sony`, `apple`), and — once `gps-map-view` (#13) is implemented — a reverse-geocoded city or country name; tags are written through the existing `asset_tags` table and tag infrastructure; auto-applied tags are indistinguishable from manual ones and can be removed by the user; no new schema required beyond what the tag feature already provides | ✅ Created | ⬜ Pending |
| 50  | `image-comparison-viewer`   | Select exactly two assets in the gallery → "Compare" action opens a split-screen view showing both images side by side at matched zoom levels, with filename, size, dimensions, and rating displayed beneath each panel; most useful as a companion to the duplicates workflow but available from any multi-selection; no new backend endpoint — both images are served by the existing `GET /api/assets/{id}/image`; new `ComparisonViewerComponent` in `features/gallery/` | ✅ Created | ⬜ Pending |
| 51  | `folder-bookmarks`          | A pin icon on each node in the folder navigation tree bookmarks it per authenticated user; bookmarks stored in a new `folder_bookmarks` table (`id`, `userId`, `folderPath`, `createdAt`); bookmarked folders appear as a pinned section above the full tree backed by `GET /api/folders/bookmarks`, `POST /api/folders/bookmarks`, and `DELETE /api/folders/bookmarks/{id}`; frontend inserts a `MatDivider` between the pinned section and the main tree; new Flyway migration | ✅ Created | ⬜ Pending |
| 52  | `multi-language-i18n`       | Angular `@angular/localize` for the frontend starting with English and Spanish (mirrors open desktop issue #140); Spring Boot `MessageSource` for backend validation and error messages; locale preference stored per user as a `locale` column on `users` (or inside a `user_preferences` JSON column shared with dark-mode preference from #15); language toggle in the top navigation bar; Angular build produces one bundle per locale via `ng build --localize` | ✅ Created | ⬜ Pending |
| 53  | `password-strength-policy`  | Enforce minimum password complexity on user creation and password change using the `Passay` library (configurable rules: minimum length 12, at least one uppercase, one digit, one special character); the Angular user-admin form and profile page show a live strength meter powered by the same rule set mirrored client-side; returns a structured `400` with per-rule violation details so the frontend can highlight exactly which rules failed; no schema change | ✅ Created | ⬜ Pending |
| 54  | `notification-center`       | In-app notification bell in the top navigation bar showing a history of completed background operations (catalog finished, sync complete, convert complete, backup uploaded); new `notifications` table (`id`, `userId`, `type`, `message`, `read_at`, `created_at`); backend writes a notification row at the end of each SSE stream; `GET /api/notifications` returns unread count and paginated history; `PATCH /api/notifications/read` marks all read; badge count clears when the panel is opened; new Flyway migration | ✅ Created | ⬜ Pending |
| 55  | `webp-avif-conversion`      | Extend `ConvertAssetsUseCase` — currently PNG→JPEG only — to support JPEG/PNG→WebP and JPEG/PNG→AVIF; WebP encoding via `cwebp` invoked through `ProcessBuilder` (same pattern as #21 FFmpeg); AVIF encoding via `avifenc` through `ProcessBuilder`; the `convert_assets_directories_definitions` table gains a `target_format` VARCHAR column (Flyway migration); frontend `ConvertComponent` adds a "Target format" dropdown (JPEG / WebP / AVIF) to the directory pair configuration form | ✅ Created | ⬜ Pending |
| 56  | `asset-image-editor`        | Add brightness, contrast, and hue adjustment to the viewer; CSS `filter: brightness() contrast() hue-rotate()` applied to the `<img>` tag drives live preview via three `MatSlider` inputs with zero backend calls; saving dispatches `POST /api/assets/{id}/edit` with the adjustment values; the backend processes the image using Java2D (`java.awt.image`) with no external dependency: `RescaleOp` applies brightness and contrast in a single pass, hue is adjusted by converting RGB→HSB via `Color.RGBtoHSB`, rotating the H component, and converting back; the edited file is saved as a new asset alongside the original (non-destructive by default); an optional "replace original" flag covers the destructive case; no new Maven dependency, no Docker image change | ✅ Created | ⬜ Pending |
| 57  | `viewer-pan-drag`           | Add mouse and touch drag-to-pan to the zoomed image viewer; the existing `transform: scale(viewerZoom)` is extended to `scale(zoom) translate(panX, panY)`; `mousedown` sets a dragging flag and captures the starting cursor position, `mousemove` updates `panX`/`panY` while dragging, `mouseup` clears the flag; `touchstart`/`touchmove`/`touchend` mirror the same logic for mobile; `panX` and `panY` reset to zero whenever zoom returns to 1× or the displayed asset changes; pure frontend change — no backend endpoint, no schema change, no external dependency | ✅ Created | ⬜ Pending |
| 58  | `video-from-images`         | New wizard component lets the user select an ordered list of images, set a per-slide duration, choose a background music file (uploaded on the spot or selected from a cataloged audio asset once `audio-asset-support` #59 is in place), and trigger video generation; the backend invokes FFmpeg via `ProcessBuilder` (`ffmpeg -framerate 1/{duration} -i frame%04d.jpg -i music.mp3 -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p output.mp4`), streams progress via SSE (same pattern as catalog/sync/convert), and saves the output to a user-chosen folder where it is auto-cataloged as a video asset; hard dependency on `video-file-support` (#21) for FFmpeg presence in the container | ✅ Created | ⬜ Pending |
| 60  | `archive-support`           | Two related capabilities sharing the same archive-reading infrastructure (`org.apache.commons:commons-compress` for tar.gz; `java.util.zip` built-in for zip): (1) **virtual folders** — zip and tar.gz files appear as expandable nodes in the folder navigation tree using a `!` path separator convention (e.g. `/photos/album.zip!/summer/`); the catalog service extracts images to a temp location, generates thumbnails, and stores assets with the virtual path; (2) **download formats** — the bulk-download endpoint (`GET /api/assets/download`) gains a `format` query parameter (`zip` / `tar.gz`) so users can choose the archive type; the existing `ZipOutputStream` path is joined by a `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`; no Flyway migration required for either capability | ✅ Created | ⬜ Pending |
| 61  | `asset-backup`              | Backup a configurable scope of assets (folder, album, saved search result, or entire catalog) to one or more sequentially numbered archive files (e.g. `backup_photos_001.zip`, `backup_photos_002.zip`) split at a configurable volume size; format is zip or tar.gz (reuses `archive-support` #60 writing infrastructure); trigger is manual (`POST /api/backup/{id}/run`) or a per-definition cron expression scheduled dynamically via Spring `TaskScheduler` + `CronTrigger` (cancelled and rescheduled on definition update); new `/backup` frontend route mirrors the convert page structure (definitions list → configure → run → results with SSE progress and per-file logging); backend stores definitions in `backup_definitions` and run history in `backup_run_log` (timestamps, status, files written, bytes, errors); new Flyway migration | ✅ Created | ⬜ Pending |
| 63  | `raw-exif-jsonb`            | Add a `raw_exif JSONB` column to the existing `asset_exif` table (new Flyway migration); during cataloging, after extracting the 13 known fields, iterate all EXIF directories returned by Apache Commons Imaging (`JpegImageMetadata.getExif().getDirectories()` → `dir.getAllFields()`) and collect every `TiffField` into a `Map<String, String>` keyed by `field.getTagInfo().name` with value `field.getValueDescription()`; the map is serialized to JSONB using Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, String> rawExif` field in `AssetExifEntity` (no new Maven dependency — Hibernate 6 handles JSON natively via Jackson, already present); `AssetExif` domain model and `AssetExifEntityMapper` (MapStruct) gain the `rawExif` field; `ExifMetadataDto` exposes it as `Map<String, String> rawExif`; `ExifMetadata` TypeScript interface adds `rawExif: Record<string, string> \| null`; in `ExifPanelComponent`, below the existing 13 structured fields, a collapsible `MatExpansionPanel` labeled "All EXIF data" renders every key-value pair from `rawExif` as a compact two-column list; a `MatFormField` search input above the list filters entries by key name in real time using a component-level computed signal so the full raw map is never re-fetched; a single image from a modern DSLR or mirrorless camera typically carries 80–300 EXIF fields across the IFD0, ExifIFD, GPS IFD, and MakerNote directories; the search filter is essential because MakerNote alone can add 100+ manufacturer-specific fields (Nikon colour modes, Canon lens correction data, Sony face detection coordinates, etc.); existing assets cataloged before this migration have `raw_exif = NULL` and the panel section is hidden when `rawExif` is null; re-cataloging any folder populates the column for all assets in that folder; new Flyway migration | ✅ Created | ⬜ Pending |

---

## Dependencies

### Hard implementation dependencies

**Improvement 13 → Improvement 1** (prerequisite already implemented)

`gps-map-view` requires GPS coordinates already stored in `asset_exif`. Without `exif-metadata-panel` the coordinates are never persisted.

**Improvement 19 → Improvement 4** (prerequisite already implemented)

`shareable-album-links` requires the `albums` table introduced by `virtual-albums`.

**Improvement 22 → Improvement 9** (prerequisite already implemented)

`duplicate-auto-resolve` routes deleted assets through the soft-delete path introduced by `soft-delete-recycle-bin`.

### Soft implementation dependencies (order affects cleanliness)

**Improvement 16 → Improvement 8** (prerequisite already implemented)

`keyboard-shortcuts` extends the viewer shortcuts already present in `slideshow-mode`. Implementing 8 first avoids re-doing viewer key handling.

**Improvement 23 → Improvement 10** (prerequisite already implemented)

`progressive-web-app` is significantly more valuable once `mobile-responsive-layout` is in place, since PWA installs are most common on mobile.

### Recommended implementation order

For the pending dependent clusters:

```
13 (gps-map-view)          — prerequisite #1  already implemented
19 (shareable-album-links) — prerequisite #4  already implemented
22 (duplicate-auto-resolve)— prerequisite #9  already implemented
16 (keyboard-shortcuts)    — prerequisite #8  already implemented
23 (progressive-web-app)   — prerequisite #10 already implemented
58 (video-from-images)     — prerequisite #21 already implemented; #59 also already implemented
63 (raw-exif-jsonb)        — prerequisite #1  already implemented; extend CatalogAssetItemProcessor (#64 already done)
```

Improvements 17 (batch-rename), 24 (wallpaper-suggestion), 26 (thumbnail-http-cache), 27 (image-etag-cache), 28 (server-side-spring-cache), 29 (exif-cache-service), 30 (image-rotation-viewer), 32 (folder-watch-service), 33 (role-based-access-control), 35 (thumbnail-regeneration), 36 (global-error-handler), 38 (folder-stats-in-tree), 40 (circuit-breaker), 43 (request-correlation-mdc), 50 (image-comparison-viewer), 53 (password-strength-policy), 56 (asset-image-editor), 57 (viewer-pan-drag) have no hard dependencies and can be delivered in any order.

Within dependent clusters:

```
37 (asset-description) → 31 (full-text-search)
36 (global-error-handler) → 43 (request-correlation-mdc)
33 (role-based-access-control) → 44 (database-backup), 61 (asset-backup)
39 (api-rate-limiting, already done) → 47 (two-factor-authentication)
46 (session-management) → 47 (two-factor-authentication)
54 (notification-center) → 48 (email-notifications)
60 (archive-support) → 61 (asset-backup)
26 (thumbnail-http-cache) → 23 (progressive-web-app)
```

### Deployment (migration) dependencies

Flyway migration versions must be applied in ascending order. Migrations V7–V13, V24, and V27 have already been applied. The following pending improvements require new migrations:

| Migration | Feature                                                                        |
| --------- | ------------------------------------------------------------------------------ |
| V14       | `shareable-album-links` — `shared_albums` table                                |
| V15       | `wallpaper-suggestion` — `aspect_ratio` column on `assets`                    |
| V16       | `asset-description` — `description` column on `assets`                        |
| V17       | `full-text-search` — `search_vector` generated column + `GIN` index on `assets` |
| V18       | `two-factor-authentication` — `totp_secret`, `totp_enabled` on `users`; `totp_backup_codes` table |
| V19       | `email-notifications` — `email`, `email_notifications_enabled` on `users`         |
| V20       | `folder-bookmarks` — `folder_bookmarks` table                                      |
| V21       | `notification-center` — `notifications` table                                      |
| V22       | `session-management` — `user_agent` column on `refresh_tokens`                     |
| V23       | `webp-avif-conversion` — `target_format` column on `convert_assets_directories_definitions` |
| V25       | `asset-backup` — `backup_definitions` and `backup_run_log` tables                   |
| V26       | `raw-exif-jsonb` — `raw_exif` JSONB column on `asset_exif`                          |

Note: `pixel_width` and `pixel_height` are already present on `assets`; only the derived `aspect_ratio` column is new. The backfill (`aspect_ratio = pixel_width / pixel_height`) must be included in the V15 migration to populate existing rows. Assets where either dimension is zero are left as `NULL` and excluded from wallpaper queries.

The V17 migration must run after V16 because the `search_vector` generated column combines `file_name`, `description`, and tag data; the `description` column must exist before the generated column can reference it.

### Implementation notes

**Improvement 25 → Improvement 2** (prerequisite already implemented)

`on-push-change-detection` should be applied after `virtual-scrolling-gallery` is completed. The list layout introduced by #2 uses fixed-height items with immutable `*cdkVirtualFor` bindings; applying OnPush before that layout is in place risks masking change-detection bugs while the old grid and IntersectionObserver are still present.

**Improvement 26 and 27 → Improvement 23**

`thumbnail-http-cache` sets browser-level `Cache-Control` headers on the thumbnail endpoint. `progressive-web-app` (#23) adds a service-worker cache-first strategy for the same endpoint. Implementing #26 first means the service worker inherits already-correct cache semantics and does not need to re-define them. Implementing them in reverse order still works but creates redundancy.

**Improvement 27 → no schema change**

The `ETag` value is derived from the SHA-256 `hash` column already present on the `Asset` entity; no Flyway migration is needed.

**Improvement 28 — cache eviction boundary**

`@CacheEvict` must be applied to every write path that invalidates a cached result: cataloging (evicts folder and stats caches), rating (evicts stats), tagging (evicts stats), and soft-delete (evicts folder, stats, and EXIF caches). Failing to wire eviction on any write path will serve stale data silently.

**Improvement 30 — no dependencies**

`image-rotation-viewer` reads `imageRotation` already stored on `Asset`; it is a pure frontend change with no backend or schema involvement.

**Improvement 31 → Improvement 37**

`full-text-search` is more valuable when the `description` field from `asset-description` is available to index. The V17 migration (search vector) hard-depends on the V16 migration (description column) being applied first. Implementing #37 before #31 avoids rewriting the generated column definition.

**Improvement 31 → Improvement 37 (functional)**

A full-text search that can only index filename and tags is still useful, but the `description` field is the primary free-text input that justifies the investment in a PostgreSQL GIN index. Implementing both together delivers the full value.

**Improvement 37 → Improvement 31 (soft)**

`asset-description` can be delivered standalone and provides immediate value in the EXIF panel before full-text search is wired up.

**Improvement 33 — enforcement boundary**

`role-based-access-control` must apply `@PreAuthorize` to every write-path use case method, not just controller endpoints, to prevent bypasses if new controllers are added later. The `VIEWER` role must be blocked at the use-case layer.

**Improvement 35 — ordering note**

`thumbnail-regeneration` is most useful after `image-rotation-viewer` (#30) is in place, since one motivation for re-generation is applying orientation correction to existing thumbnails. The two can be delivered independently but pair naturally.

**Improvements 32, 33, 36, 38 — no dependencies**

`folder-watch-service`, `role-based-access-control`, `global-error-handler`, and `folder-stats-in-tree` have no hard dependencies on other pending improvements and can be delivered in any order.

**Improvement 40 → Improvement 13**

`circuit-breaker` is a follow-up to `gps-map-view`. It is not warranted in the current architecture because the backend makes no outbound calls to external services. Once #13 introduces an outbound HTTP call to a geocoding API (e.g. Nominatim or Google Maps), that call becomes the first concrete circuit breaker target: if the geocoding service is slow or unavailable it will stall the EXIF panel for every viewed image. At that point, wrapping the `GeocodingPort` adapter with Resilience4j `@CircuitBreaker` and returning a fallback of `null` coordinates is the right response.

**Improvement 43 → Improvement 36**

`request-correlation-mdc` pairs with `global-error-handler` (#36). The Angular `ErrorHandler` can read the `X-Request-ID` response header and include it in the error snackbar or log payload, linking a user-visible error directly to the backend log entries for that request.

**Improvements 41, 42, 43 — no schema changes** (41 and 42 already implemented)

All three observability improvements are purely operational: no Flyway migrations, no domain model changes, and no new API endpoints visible to end users.

**Improvement 44 → Improvement 33**

`database-backup` exposes admin-only endpoints (`POST /api/admin/backup`, `GET /api/admin/backups`). Once `role-based-access-control` (#33) is in place these endpoints should be restricted to the `ADMIN` role. They can be delivered before #33 with a simple `@PreAuthorize("hasRole('ADMIN')")` placeholder, but the RBAC enforcement is only meaningful after #33 is complete.

**Improvement 44 — no schema change**

`database-backup` adds no Flyway migration; the backup metadata (listing stored files) is read directly from cloud storage at request time rather than persisted to the database.

**Improvement 45 → Improvement 44** (45 already implemented)

`postgres-dockerize` (#45) is now complete. The backup service (#44) can run `pg_dump` against the database container over the Docker network (`POSTGRES_HOST: db`) without additional network configuration.

**Improvement 47 → Improvement 39** (prerequisite already implemented)

`two-factor-authentication` introduces a TOTP verification endpoint that must be rate-limited to prevent brute-force attacks on 6-digit codes. `api-rate-limiting` (#39) is already in place.

**Improvement 47 → Improvement 46**

Revoking all sessions (`DELETE /api/auth/sessions`) is the recommended recovery action when a user suspects their account is compromised. Implementing `session-management` (#46) before or alongside #47 gives users the tools to respond to a potential account takeover.

**Improvement 47 — external dependencies detail**

*`dev.samstevens.totp:totp` (Maven, latest stable: 1.7.1)*

The core TOTP library implementing RFC 6238 (TOTP) and RFC 4226 (HOTP). Provides:
- `SecretGenerator` — generates a cryptographically random 160-bit base32 secret
- `CodeVerifier` — validates a 6-digit user-submitted code against the stored secret; the default time-step window of ±1 step (±30 seconds) tolerates typical clock drift between client and server
- `QrData` builder — constructs the `otpauth://totp/JPPhotoManager:{username}?secret={secret}&issuer=JPPhotoManager` URI that authenticator apps parse when scanning the QR code

*`com.google.zxing:core` + `com.google.zxing:javase` (Maven, latest stable: 3.5.3)*

ZXing (Zebra Crossing) encodes the `otpauth://` URI into a QR code bitmap. `QRCodeWriter` produces a `BitMatrix`; `MatrixToImageWriter` renders it to a PNG `ByteArrayOutputStream`. The backend returns the PNG as a base64 string so the frontend displays it as `<img src="data:image/png;base64,...">` — no extra round-trip and the image is never persisted anywhere.

*No additional frontend npm package required*

The base64-PNG-from-backend approach keeps the TOTP secret entirely server-side.

*Secret storage — AES-256-GCM encryption at rest*

The `totp_secret` column must never be stored in plaintext. The recommended implementation is a JPA `@Convert` annotation backed by an `AttributeConverter<String, String>` that AES-256-GCM encrypts the secret using a key loaded from the `TOTP_ENCRYPTION_KEY` environment variable. A database leak then exposes only ciphertext.

*Backup codes — BCrypt-hashed*

The 10 single-use recovery codes are BCrypt-hashed before insertion into `totp_backup_codes` — the same treatment as passwords — so a database leak does not expose usable codes. On use, the matching row is deleted; the plaintext codes are shown to the user exactly once during setup and never stored.

**Improvement 48 → Improvement 54**

`email-notifications` and `notification-center` are triggered by the same operation-completion events (end of catalog, sync, convert, backup SSE streams). Implementing them together avoids wiring the same trigger points twice; if delivered separately, #54 should come first so the notification infrastructure exists when #48 extends it with email dispatch.

**Improvement 49 → Improvement 1** (prerequisite already implemented)

`auto-tagging` reads `dateTaken` and camera make from EXIF data stored by `exif-metadata-panel` (#1, already implemented). The GPS city/country auto-tag additionally depends on `gps-map-view` (#13) for reverse geocoding.

**Improvements 46, 50, 53 — no schema changes**

`session-management` (beyond the optional `user_agent` column), `image-comparison-viewer`, and `password-strength-policy` require no Flyway migrations.

**Improvements 50, 53 — no new backend endpoints**

`image-comparison-viewer` reuses the existing `GET /api/assets/{id}/image` endpoint. `password-strength-policy` adds validation logic to existing endpoints only.

**Improvement 56 — no external dependencies**

`asset-image-editor` uses Java2D (`java.awt.image`, standard library) for all three operations: `RescaleOp` for brightness and contrast, RGB→HSB→RGB conversion for hue. No new Maven dependency and no Docker image change are required.

**Improvement 57 — no dependencies**

`viewer-pan-drag` is a pure frontend change with no backend, schema, or external library involvement. It can be delivered at any point independently of all other improvements.

**Improvement 58 → Improvement 21** (prerequisite already implemented)

`video-from-images` has a hard dependency on `video-file-support` (#21): FFmpeg must be installed in the backend container before the video generation `ProcessBuilder` call can work. #21 is already implemented.

**Improvement 58 → Improvement 59 (soft)** (soft dependency already implemented)

`video-from-images` can accept a music file upload at video-creation time and does not require audio assets to exist. However, once `audio-asset-support` (#59) is in place the wizard gains a "select from audio assets" picker, making music selection significantly more convenient. Since #59 is now implemented, #58 should include the audio picker from day one.

**Improvement 60 — external dependency**

`archive-support` requires `org.apache.commons:commons-compress` for tar.gz reading and writing. Zip reading and writing use `java.util.zip` from the Java standard library and add no Maven dependency. The virtual-folder and download-format capabilities are independent of each other and can be delivered separately within the same improvement.

**Improvement 61 → Improvement 60**

`asset-backup` has a hard dependency on `archive-support` (#60): it reuses the zip and tar.gz writing infrastructure (`ZipOutputStream` and `TarArchiveOutputStream`) introduced there, and inherits the `commons-compress` Maven dependency without re-adding it. Delivering #60 first also means the volume-splitting logic can be built on top of already-tested archive streams.

**Improvement 61 → Improvement 33 (soft)**

`asset-backup` admin endpoints (`POST /api/backup/{id}/run`, `GET /api/backup/definitions`, etc.) should be restricted to the `ADMIN` role once `role-based-access-control` (#33) is in place, following the same pattern as `database-backup` (#44).

**Improvement 61 — dynamic scheduling note**

Each `backup_definitions` row carries an optional `cron_expression` column. On application startup, `BackupSchedulerService` reads all definitions with a non-null cron expression and registers them with Spring `TaskScheduler`. On definition create/update/delete, the corresponding `ScheduledFuture` is cancelled and a new one registered. This approach requires no third-party scheduler library — Spring's built-in `ThreadPoolTaskScheduler` is sufficient.

**Improvement 61 — scope implementation note**

The four backup scopes map to existing query paths: folder scope reuses `GetAssetsUseCase` filtered by `folderPath`; album scope reuses `GetAlbumAssetsUseCase`; search scope reuses `findByFolderWithFilters` with a stored `SearchPreset`; catalog scope queries all non-deleted assets. No new repository methods are required beyond what existing use cases already expose.

**Improvement 62 → Improvement 56 (soft)**

`social-media-crop` (#62, already implemented) and `asset-image-editor` (#56) share the same backend pattern: send transformation parameters → Java2D processes the original image → result saved as a new `Asset` in the same folder → thumbnail generated → new `AssetResponse` returned. When implementing #56, extract the "save as new asset" logic into a shared utility method (e.g. `AssetSavePort.saveProcessedAsset(...)`) that #56 and #62 can both use, eliminating the duplication already present in #62's implementation.

**Improvement 63 → Improvement 1 (hard)** (prerequisite already implemented)

`raw-exif-jsonb` adds a column to the `asset_exif` table introduced by `exif-metadata-panel` (#1, already implemented). The table must exist before the V26 migration can run.

**Improvement 63 — no new Maven or npm dependency**

Apache Commons Imaging is already a dependency in `pom.xml` and is already used in `StorageServiceAdapter` for EXIF rotation correction. The full tag-iteration API (`getDirectories()` → `getAllFields()`) is part of the same library — no new artifact is needed. Hibernate 6 (shipped with Spring Boot 3.x) maps `jsonb` natively via `@JdbcTypeCode(SqlTypes.JSON)` using the Jackson `ObjectMapper` already on the classpath. No new npm package is needed on the frontend — the raw EXIF data arrives as a plain `Record<string, string>` and is rendered with `@for` and a component-level filter signal.

**Improvement 63 — EXIF directory structure and field volume**

Commons Imaging exposes EXIF data through a directory hierarchy. Each directory is iterated in order and its fields merged into the single `Map<String, String>`:

```
EXIF DIRECTORY HIERARCHY (Commons Imaging)

  JpegImageMetadata
  └── TiffImageMetadata (from getExif())
      ├── IFD0            Image width, height, make, model, software,
      │                   copyright, date modified, resolution (~10–20 fields)
      ├── ExifIFD         Exposure time, f-number, ISO, focal length,
      │                   shutter speed, aperture, date original, flash,
      │                   colour space, subject distance (~40–60 fields)
      ├── GPS IFD         Latitude, longitude, altitude, speed, direction,
      │                   timestamp, map datum (~15 fields)
      ├── MakerNote       Manufacturer-specific; varies widely by brand:
      │   ├── Nikon       Colour modes, noise reduction, active D-Lighting,
      │   │               lens serial number, flash compensation (~80 fields)
      │   ├── Canon       Lens info, AF point, white balance, picture style,
      │   │               owner name, serial number (~100 fields)
      │   └── Sony        Face detection, creative style, lens compensation,
      │                   HDR mode, multi-frame noise reduction (~60 fields)
      ├── Interop IFD     Interoperability index, version (~2 fields)
      └── IFD1            Thumbnail offset and length (~5 fields)

  Typical total field count per image: 80–300
```

If two directories contain a field with the same tag name (uncommon but possible), the later directory's value overwrites the earlier one. The map key is `field.getTagInfo().name` (the human-readable TIFF tag name such as `"ExposureTime"`, `"Make"`, `"GPS GPSLatitude"`); the value is `field.getValueDescription()` (always a string representation).

**Improvement 63 — frontend panel layout**

The raw EXIF section is appended below the 13 structured fields as a collapsible `MatExpansionPanel`. It is hidden entirely when `rawExif` is `null` (assets cataloged before the migration). The search input filters by key name only — values are not searched to avoid partial matches on numeric strings like `"0"` matching hundreds of entries.

```
EXIF PANEL LAYOUT — AFTER raw-exif-jsonb

  ┌─────────────────────────────────┐
  │  EXIF INFO                 [×]  │
  ├─────────────────────────────────┤
  │  Camera    Sony α7 IV           │  ← existing 13 structured fields
  │  Lens       85mm f/1.4          │
  │  Exposure  1/500 s · f/1.8      │
  │  ISO        400                 │
  │  Date       2024-06-15 10:32    │
  │  GPS        40.7128°N 74.006°W  │
  │  …                              │
  ├─────────────────────────────────┤
  │  ▼ All EXIF data  (214 fields)  │  ← MatExpansionPanel (collapsed by default)
  │  ┌─────────────────────────┐    │
  │  │ Filter tags…            │    │  ← MatFormField search input
  │  └─────────────────────────┘    │
  │  ExposureTime          1/500    │
  │  FNumber               1.8      │  ← @for over filteredRawExif()
  │  ISOSpeedRatings       400      │
  │  LensModel    FE 85mm F1.4 GM   │
  │  …                              │
  └─────────────────────────────────┘
```

**Improvement 63 → Improvement 31 (soft)**

The `raw_exif` JSONB column is queryable with PostgreSQL's `->` and `@>` operators and GIN-indexable. Once `full-text-search` (#31) is implemented, the search vector trigger could include selected raw EXIF fields — for example, `raw_exif->>'LensModel'` or `raw_exif->>'Model'` — giving users the ability to search by equipment not captured in the 13 fixed columns. This extension requires no schema change beyond what #31 already defines; it is a trigger function update only.

**Improvement 63 — backfill strategy**

The V26 migration adds the column as `ALTER TABLE asset_exif ADD COLUMN raw_exif JSONB`. Existing rows have `raw_exif = NULL`. Backfilling requires re-cataloging the affected folders: `POST /api/assets/catalog` triggers the full extraction pipeline, which now includes the raw EXIF pass. No SQL-level backfill is possible because EXIF extraction reads the image file from disk, not from database data. The `ExifPanelComponent` handles `NULL` gracefully by hiding the "All EXIF data" section for assets not yet re-cataloged.

**Improvement 64 — ordering note** (64 already implemented)

`catalog-spring-batch` (#64) is now implemented. When implementing `raw-exif-jsonb` (#63), extend `CatalogAssetItemProcessor` with the additional raw EXIF extraction pass (iterate `getDirectories()` → `getAllFields()` after the existing 13-field extraction).
