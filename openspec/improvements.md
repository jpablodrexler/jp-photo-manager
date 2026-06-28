# Planned Improvements

This document records all **pending** improvements to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them. For completed improvements see `improvements-implemented.md`.

---

## Improvement List

| #   | Change name                 | Brief description                                                                                                                                                                       | Artifacts  | Implementation |
| --- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- |
| 13  | `gps-map-view`              | Add a Leaflet.js map panel to the EXIF viewer and a `/map` route showing clustered photo pins for the current folder or album; clicking a pin navigates to that asset in the gallery   | ✅ Created | ⬜ Pending      |
| 16  | `keyboard-shortcuts`        | Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings               | ✅ Created | ⬜ Pending      |
| 19  | `shareable-album-links`     | `POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication | ✅ Created | ⬜ Pending |
| 22  | `duplicate-auto-resolve`    | "Clean up automatically" dialog on the duplicates page with policies (keep oldest, keep newest, keep highest resolution, keep preferred folder); delegates to existing soft-delete path   | ✅ Created | ⬜ Pending      |
| 24  | `wallpaper-suggestion`      | Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button | ✅ Created | ⬜ Pending |
| 25  | `on-push-change-detection`  | Apply `ChangeDetectionStrategy.OnPush` to all 18 components; replace mutable state mutations with immutable assignments so Angular's OnPush check can detect changes; prioritise `ThumbnailComponent` (one instance per visible image) and `GalleryComponent` (the most complex); inject `ChangeDetectorRef` where manual `markForCheck()` calls are needed (e.g. after SSE events or async callbacks outside the Angular zone) | ✅ Created | ⬜ Pending |
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
| 58  | `video-from-images`         | New wizard component lets the user select an ordered list of images, set a per-slide duration, choose a background music file (uploaded on the spot or selected from a cataloged audio asset once `audio-asset-support` #59 is in place), and trigger video generation; the backend invokes FFmpeg via `ProcessBuilder` (`ffmpeg -framerate 1/{duration} -i frame%04d.jpg -i music.mp3 -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p output.mp4`), streams progress via SSE (same pattern as catalog/sync/convert), and saves the output to a user-chosen folder where it is auto-cataloged as a video asset; hard dependency on `video-file-support` (#21) for FFmpeg presence in the container | ✅ Created | ⬜ Pending |
| 60  | `archive-support`           | Two related capabilities sharing the same archive-reading infrastructure (`org.apache.commons:commons-compress` for tar.gz; `java.util.zip` built-in for zip): (1) **virtual folders** — zip and tar.gz files appear as expandable nodes in the folder navigation tree using a `!` path separator convention (e.g. `/photos/album.zip!/summer/`); the catalog service extracts images to a temp location, generates thumbnails, and stores assets with the virtual path; (2) **download formats** — the bulk-download endpoint (`GET /api/assets/download`) gains a `format` query parameter (`zip` / `tar.gz`) so users can choose the archive type; the existing `ZipOutputStream` path is joined by a `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`; no Flyway migration required for either capability | ✅ Created | ⬜ Pending |
| 61  | `asset-backup`              | Backup a configurable scope of assets (folder, album, saved search result, or entire catalog) to one or more sequentially numbered archive files (e.g. `backup_photos_001.zip`, `backup_photos_002.zip`) split at a configurable volume size; format is zip or tar.gz (reuses `archive-support` #60 writing infrastructure); trigger is manual (`POST /api/backup/{id}/run`) or a per-definition cron expression scheduled dynamically via Spring `TaskScheduler` + `CronTrigger` (cancelled and rescheduled on definition update); new `/backup` frontend route mirrors the convert page structure (definitions list → configure → run → results with SSE progress and per-file logging); backend stores definitions in `backup_definitions` and run history in `backup_run_log` (timestamps, status, files written, bytes, errors); new Flyway migration | ✅ Created | ⬜ Pending |
| 67  | `event-auto-grouping`       | Automatically cluster photos into events based on configurable time gaps between consecutive shots (default threshold: 2 hours); a new `/events` route lists events as date-range cards showing a cover photo and asset count; users can rename events, set a custom cover photo, and merge or split events manually; ungrouped assets infer their event from `dateTaken` at query time — the `user_events` table (`id`, `userId`, `name`, `startAt`, `endAt`, `coverAssetId`) stores only user overrides, keeping the migration small; `GET /api/events` returns computed events for the authenticated user with asset counts; integrates with `timeline-view` (#18) as an alternate grouping mode selectable from the view toolbar | ✅ Created | ⬜ Pending |
| 68  | `photo-quality-scoring`     | Compute a perceptual quality score (0–100) for each image during cataloging using signals already available in the thumbnail and EXIF: a fast Laplacian variance pass over the 200×150 px thumbnail pixels estimates sharpness (high variance = sharp), combined with EXIF signals (ISO penalty for values above 1600, exposure time penalty for values above 1/60 s without image stabilisation, bonus for wider aperture); store the result in a new `quality_score` SMALLINT column on `assets` (Flyway migration); expose as a gallery sort option ("Best quality first") and a minimum quality threshold slider in the search/filter toolbar alongside the existing star-rating filter; pairs with `duplicate-auto-resolve` (#22) to automatically prefer the highest-scoring copy when resolving duplicates; no external Maven dependency — Laplacian variance is computed with `BufferedImage` pixel access using the standard library | ✅ Created | ⬜ Pending |
| 69  | `iptc-xmp-metadata-editing` | Read and write IPTC/XMP fields (caption, copyright notice, creator, keywords, city, country) directly in the viewer panel alongside the existing EXIF display; Apache Commons Imaging (already a dependency) supports IPTC segment read and write in JPEG files via `JpegIptcRewriter`; a new `PATCH /api/assets/{id}/iptc` endpoint writes the updated IPTC segment back to the file on disk and refreshes the `assets` row; caption and copyright are stored in two new VARCHAR columns on `assets` (Flyway migration) so they are queryable without re-reading the file; keywords are mapped to the existing `asset_tags` table, making them immediately available in tag-based filters; the viewer panel shows caption and copyright as editable `MatFormField` inputs below the EXIF panel with auto-save on blur | ✅ Created | ⬜ Pending |
| 70  | `dominant-color-palette`    | During cataloging, extract the top 5 dominant colors from each photo's thumbnail using k-means clustering (k=5, 10 iterations) over the 200×150 px thumbnail pixels; no second file read from the original is required; store the five RGB centroids as a `color_palette JSONB` column on `assets` (Flyway migration); each centroid is snapped to the nearest of 12 color families (red, orange, yellow, green, teal, blue, purple, pink, brown, grey, black, white) using Euclidean distance in HSL space; frontend additions: a color swatch row in the search/filter toolbar (one or more families can be selected, adding a `colorFamily[]` query parameter to `GET /api/assets`), an optional 5-segment color stripe at the bottom of each `ThumbnailComponent` card (toggleable, off by default), and a "Color distribution" donut chart on the `/analytics` dashboard reusing the existing `ngx-charts` dependency; `GET /api/assets/color-families` returns the 12 families with asset counts for the current folder to populate the swatch picker; no external Maven dependency — k-means runs over `BufferedImage` pixel access using the standard library | ✅ Created | ⬜ Pending |
| 71  | `webdav-server`             | Expose the catalog over WebDAV (`PROPFIND`, `GET`, `PUT`, `DELETE`, `MKCOL`) at `/webdav/` so users can mount the catalog as a network drive on Windows (Map Network Drive), macOS (Finder → Connect to Server), and Linux (`davfs2`); the virtual folder tree mirrors the `assets.folder_path` hierarchy; `GET` on an asset path streams the original file via the existing `StoragePort`; `PUT` to any folder path triggers the same catalog pipeline as drag-and-drop upload (#3); `DELETE` routes through the soft-delete path from `soft-delete-recycle-bin` (#9); implemented with `milton-server-ce` (pure Java WebDAV server, no servlet dependency conflicts) or a custom Spring MVC `WebdavController` handling raw `PROPFIND` XML via JAXB; authentication uses HTTP Basic over the existing `UserDetailsService` — JWT cookies are not compatible with native OS WebDAV clients; no schema change | ✅ Created | ⬜ Pending |
| 72  | `mongodb-exif-store`              | Replace the `asset_exif` PostgreSQL table (including the JSONB `raw_exif` column planned in #63) with a MongoDB `asset_exif` collection; the native document model stores all 80–300 EXIF fields per image without column sprawl or JSONB workarounds; MongoDB `$near`/`$geoWithin` operators turn the already-stored GPS coordinates into geospatial "photos within radius" queries that are impossible to express efficiently in SQL; only `AssetExifRepositoryImpl` changes — the `AssetExifRepository` port interface and `AssetExif` domain model are untouched; **mutually exclusive with `raw-exif-jsonb` (#63)**: choose this improvement if geospatial queries or deep EXIF document nesting are priorities; choose #63 if keeping a single PostgreSQL dependency is preferred | ⬜ Pending | ⬜ Pending |
| 73  | `mongodb-audit-log`               | Append-only MongoDB collection `asset_audit_log` recording every user action — view, download, tag, rate, delete, sync-run, convert-run, catalog-run — as a flexible document `{ userId, action, entityType, entityId, timestamp, metadata: { … } }`; each event type carries its own `metadata` shape (e.g. tag events include `tagName`; delete events include `folderId` and `permanent` flag) without schema migrations; time-range queries and per-user activity feeds are native aggregation pipeline operations; a new `AuditLogRepository` port interface (domain) with a `MongoAuditLogRepositoryImpl` adapter (infrastructure) keeps MongoDB out of the use-case layer; pairs with `kafka-catalog-pipeline` (#75) as the natural durable consumer of `asset.cataloged` and `asset.deleted` Kafka events | ⬜ Pending | ⬜ Pending |
| 74  | `mongodb-user-preferences`        | Move `user_preferences` and `search_presets` from rigid PostgreSQL tables to a single MongoDB `user_configs` collection keyed by `userId`; adding a new UI preference (theme, gallery layout, notification toggle) requires no Flyway migration; search preset payloads grow naturally as new filter fields are added without altering existing rows; only the two persistence adapters (`UserPreferenceRepositoryImpl`, `SearchPresetRepositoryImpl`) change — use cases, domain models, and REST controllers are untouched; the existing PostgreSQL tables are dropped after a one-time data migration | ⬜ Pending | ⬜ Pending |
| 76  | `kafka-async-upload`              | Decouple the `POST /api/assets/upload` HTTP thread from SHA-256 hashing, EXIF extraction, and thumbnail generation; the controller saves the file to disk, publishes an `AssetUploadedEvent { filePath, assetId, userId }` to the `asset.uploaded` Kafka topic, and returns HTTP 202; three independent consumer groups process hash computation, EXIF extraction, and thumbnail generation in parallel; eliminates multi-second blocking for large files (RAW 40–80 MB, video) and allows each processing stage to scale independently; requires the Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75) | ⬜ Pending | ⬜ Pending |
| 77  | `kafka-catalog-coordination`      | Prevent duplicate concurrent catalog scans when multiple app instances are deployed; `CatalogScheduler` currently uses `@Scheduled(fixedDelay)` on every JVM — two instances each trigger a full directory traversal simultaneously, doubling disk I/O and risking duplicate database writes; a single-partition `catalog.requests` Kafka topic provides natural leader election via Kafka consumer groups: only one member processes a `CatalogJobRequested` event while others skip; replace the `@Scheduled` trigger in `CatalogScheduler` with a Kafka producer that publishes to `catalog.requests` on the same interval; requires the Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75) | ⬜ Pending | ⬜ Pending |
| 79  | `redis-refresh-tokens`            | Move JWT refresh token storage from the `refresh_tokens` PostgreSQL table to Redis keys with native TTL (`SET refresh_token:{tokenId} {userId} EX 2592000`); PostgreSQL is being used as a TTL key-value store — a pattern Redis is purpose-built for; token lookup drops from a SQL `SELECT` (~5 ms) to a Redis `GET` (~0.1 ms); Redis auto-expires tokens after 30 days eliminating the background cleanup job implied by the `expires_at` column; revocation is an atomic `DEL`; a `RedisRefreshTokenRepositoryImpl` adapter implements the existing `RefreshTokenRepository` port; the `refresh_tokens` PostgreSQL table is dropped after a dual-write migration window; the `user_agent` column planned by `session-management` (#46) must instead be stored as a Redis hash field (`HSET refresh_token:{tokenId} userId {u} userAgent {ua}`) — implement #46 and #79 together to avoid PostgreSQL column churn | ⬜ Pending | ⬜ Pending |
| 80  | `redis-sse-pubsub`                | Complement `kafka-catalog-pipeline` (#75) with Redis Pub/Sub for sub-millisecond SSE fan-out; Kafka provides durable, replayable event storage; Redis delivers progress events in real time across all instances; catalog, sync, and convert workers `PUBLISH` to `job:progress:{jobId}:{type}` channels; all app instances `SUBSCRIBE` and relay messages to their connected `SseEmitter` clients; Kafka and Redis are complementary — Kafka for durability and consumer-group semantics, Redis for the lowest-latency delivery path to the browser; requires the Kafka producers from `kafka-catalog-pipeline` (#75); replaces `SseNotificationRegistry` alongside #75 | ⬜ Pending | ⬜ Pending |
| 81  | `redis-thumbnail-cache`           | Add Redis as a shared L2 thumbnail cache in front of the disk-backed `ThumbnailStorageServiceAdapter`; on a cache hit `GET asset:thumbnail:{assetId}` returns the thumbnail bytes with no disk I/O; on a miss the adapter reads from disk, stores with a 24-hour TTL via `SETEX`, and returns; Redis `allkeys-lru` eviction retains popular thumbnails and evicts cold ones automatically; complements `thumbnail-http-cache` (#26) (browser-level `Cache-Control: immutable`, already implemented) and `server-side-spring-cache` (#28) (per-JVM Caffeine for home stats and EXIF lookups, pending); Redis adds the shared server-side tier that survives instance restarts and eliminates dependency on co-located disk access in load-balanced deployments where the requested thumbnail may reside on a different node's filesystem | ⬜ Pending | ⬜ Pending |
| 82  | `redis-search-tag-cache`          | Cache the two highest-frequency gallery read paths in Redis: (1) paginated asset search results (`GET /api/assets`) keyed by `assets:{sha256(folderPath+page+sort+filters)}` with a 5-minute TTL, invalidated on `asset.cataloged` and `asset.deleted` Kafka events from #75; (2) the tag list with counts (`GET /api/tags`) keyed `tags:all` with a 5-minute TTL, invalidated on `AddTagToAssetUseCase` and `RemoveTagFromAssetUseCase`; extends `server-side-spring-cache` (#28) which targets home stats, folder tree, and EXIF lookups with per-JVM Caffeine — this improvement covers the two hottest gallery endpoints and uses Redis for distributed invalidation that works correctly across multiple instances; the `@Cacheable`/`@CacheEvict` annotations from #28 can be reused by switching the Spring cache manager from `CaffeineCacheManager` to a Lettuce-backed `RedisCacheManager` | ⬜ Pending | ⬜ Pending |

---

## Dependencies

### Hard implementation dependencies

**Improvement 13 → Improvement 1** (prerequisite already implemented)

`gps-map-view` requires GPS coordinates already stored in `asset_exif`. Without `exif-metadata-panel` the coordinates are never persisted.

**Improvement 19 → Improvement 4** (prerequisite already implemented)

`shareable-album-links` requires the `albums` table introduced by `virtual-albums`.

**Improvement 22 → Improvement 9** (prerequisite already implemented)

`duplicate-auto-resolve` routes deleted assets through the soft-delete path introduced by `soft-delete-recycle-bin`.

**Improvement 75 → Improvements 73, 76, 77, 80** (prerequisite already implemented)

`kafka-catalog-pipeline` (#75) is now implemented. `mongodb-audit-log` (#73), `kafka-async-upload` (#76), `kafka-catalog-coordination` (#77), and `redis-sse-pubsub` (#80) are all unblocked and can be delivered in any order.

### Soft implementation dependencies (order affects cleanliness)

**Improvement 16 → Improvement 8** (prerequisite already implemented)

`keyboard-shortcuts` extends the viewer shortcuts already present in `slideshow-mode`. Implementing 8 first avoids re-doing viewer key handling.

**Improvement 46 → Improvement 79** (token field design coordination)

`session-management` (#46) adds a `user_agent` column to `refresh_tokens` in PostgreSQL. `redis-refresh-tokens` (#79) moves the entire token store to Redis, making that column moot. Implementing both together avoids adding a PostgreSQL column that is immediately discarded. If #46 ships first, the `user_agent` data must be migrated to a Redis hash field (`HSET refresh_token:{tokenId} userAgent {ua}`) during the #79 cutover.

**Improvement 28 → Improvement 82** (extend Caffeine to Redis)

`redis-search-tag-cache` (#82) adds the gallery pagination and tag-list endpoints to the caching scope established by #28 and upgrades the Spring cache manager from Caffeine to Redis. Implementing #28 first with Caffeine provides a lower-risk on-ramp; #82 then switches the `CacheManager` bean to `RedisCacheManager` and the `@Cacheable` annotations continue to work without modification.

### Recommended implementation order

For the pending dependent clusters:

```
13 (gps-map-view)          — prerequisite #1  already implemented
19 (shareable-album-links) — prerequisite #4  already implemented
22 (duplicate-auto-resolve)— prerequisite #9  already implemented
16 (keyboard-shortcuts)    — prerequisite #8  already implemented
58 (video-from-images)     — prerequisite #21 already implemented; #59 also already implemented
```

Improvements 24 (wallpaper-suggestion), 27 (image-etag-cache), 28 (server-side-spring-cache), 29 (exif-cache-service), 30 (image-rotation-viewer), 32 (folder-watch-service), 33 (role-based-access-control), 35 (thumbnail-regeneration), 36 (global-error-handler), 38 (folder-stats-in-tree), 40 (circuit-breaker), 43 (request-correlation-mdc), 50 (image-comparison-viewer), 53 (password-strength-policy), 56 (asset-image-editor), 67 (event-auto-grouping), 68 (photo-quality-scoring), 69 (iptc-xmp-metadata-editing), 70 (dominant-color-palette), 71 (webdav-server) have no hard dependencies and can be delivered in any order.

Within dependent clusters:

```
37 (asset-description) → 31 (full-text-search)
36 (global-error-handler) → 43 (request-correlation-mdc)
33 (role-based-access-control) → 44 (database-backup), 61 (asset-backup)
39 (api-rate-limiting, already done) → 47 (two-factor-authentication)
46 (session-management) → 47 (two-factor-authentication)
54 (notification-center) → 48 (email-notifications)
60 (archive-support) → 61 (asset-backup)
75 (kafka-catalog-pipeline, already done) → 76 (kafka-async-upload), 77 (kafka-catalog-coordination), 80 (redis-sse-pubsub)
75 (kafka-catalog-pipeline, already done) → 73 (mongodb-audit-log) [Kafka consumer]
46 (session-management) + 79 (redis-refresh-tokens) — deliver together to avoid PostgreSQL column churn
28 → 81 (redis-thumbnail-cache) [JVM cache first, then Redis L2]
28 → 82 (redis-search-tag-cache) [extend Caffeine scope to Redis]
```

Improvement 74 (mongodb-user-preferences) has no hard dependencies and can be delivered in any order. Improvement 72 (mongodb-exif-store) has no hard dependency; `raw-exif-jsonb` (#63) is now implemented, so #72 becomes a data-migration task (move JSONB data from PostgreSQL to a MongoDB collection) rather than a greenfield design choice.

Priority ordering for the MongoDB, Kafka, and Redis improvements:

```
P1 — high-impact, build on P0 infrastructure:
  80 (redis-sse-pubsub)               — completes multi-instance SSE delivery; requires #75 (already implemented)
  79 (redis-refresh-tokens)           — implement before or with #46 (session-management) to avoid schema churn
  72                                  — EXIF upgrade from PostgreSQL JSONB to MongoDB; #63 (raw-exif-jsonb) is already implemented

P2 — scalability and performance wins:
  76 (kafka-async-upload)             — requires #75 (already implemented); eliminates blocking upload for large files
  81 (redis-thumbnail-cache)          — implement after #28 for best layering (#26 thumbnail-http-cache already done)
  73 (mongodb-audit-log)              — requires #75 (already implemented) for Kafka consumers
  82 (redis-search-tag-cache)         — implement after #28 (Caffeine) for a smooth upgrade path

P3 — operational convenience:
  74 (mongodb-user-preferences)       — standalone; most useful once MongoDB is already provisioned for #72 or #73
  77 (kafka-catalog-coordination)     — requires #75 (already implemented); lower urgency when running a single instance
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
| V28       | `photo-quality-scoring` — `quality_score` SMALLINT column on `assets`               |
| V29       | `iptc-xmp-metadata-editing` — `caption`, `copyright` VARCHAR columns on `assets`    |
| V30       | `dominant-color-palette` — `color_palette` JSONB column on `assets`                 |
| V31       | `event-auto-grouping` — `user_events` table                                          |

Note: `pixel_width` and `pixel_height` are already present on `assets`; only the derived `aspect_ratio` column is new. The backfill (`aspect_ratio = pixel_width / pixel_height`) must be included in the V15 migration to populate existing rows. Assets where either dimension is zero are left as `NULL` and excluded from wallpaper queries.

The V17 migration must run after V16 because the `search_vector` generated column combines `file_name`, `description`, and tag data; the `description` column must exist before the generated column can reference it.

Note on V22 (`session-management`): if `redis-refresh-tokens` (#79) is implemented, V22 becomes moot — the `user_agent` field is instead stored as a Redis hash field for each token. Coordinate the delivery of #46 and #79 to avoid applying V22 and then immediately discarding the column.

### MongoDB, Kafka, and Redis infrastructure provisioning

Improvements #72–#82 require no Flyway migrations — they do not modify the PostgreSQL schema. However, they do require provisioning new infrastructure components alongside the existing PostgreSQL container:

| Infrastructure | Required by improvements | Notes |
| -------------- | ------------------------ | ----- |
| MongoDB 7+     | #72, #73, #74 | Add `mongo` service to `docker-compose.yml` |
| Apache Kafka 3.7+ (KRaft mode) | #76, #77, #80 | Add `kafka` service; no ZooKeeper required in KRaft mode |
| Redis 7+       | #79, #80, #81, #82 | Add `redis` service with `allkeys-lru` eviction and a memory cap |

Recommended additions to `docker-compose.yml`:

```yaml
mongo:
  image: mongo:8
  ports: ["27017:27017"]

kafka:
  image: apache/kafka:3.9.0
  ports: ["9092:9092"]
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

redis:
  image: redis:7-alpine
  ports: ["6379:6379"]
  command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
```

Corresponding `application.yml` additions:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/photomanager   # improvements #72, #73, #74
    redis:
      host: ${REDIS_HOST:localhost}                 # improvements #78, #79, #80, #81, #82
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}  # improvements #76, #77 (Kafka already provisioned by #75)
```

Exception: `redis-refresh-tokens` (#79) will eventually drop the `refresh_tokens` PostgreSQL table. This requires a Flyway migration (numbered V32 or later, after V31) applied after a dual-write window to ensure no active tokens are lost during the cutover.

### Implementation notes

**Improvement 25 → Improvement 2** (prerequisite already implemented)

`on-push-change-detection` should be applied after `virtual-scrolling-gallery` is completed. The list layout introduced by #2 uses fixed-height items with immutable `*cdkVirtualFor` bindings; applying OnPush before that layout is in place risks masking change-detection bugs while the old grid and IntersectionObserver are still present.

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

**Improvement 67 — event boundary algorithm**

Events are computed on demand rather than persisted. `GET /api/events` sorts all non-deleted assets for the authenticated user by `dateTaken` ascending, then walks the list inserting an event boundary wherever the gap between consecutive shots exceeds the configured threshold (default 2 hours, configurable per user in `user_events` or globally in `application.yml`). Each resulting contiguous run becomes an event. The cover photo defaults to the asset with the highest `rating` in the run, falling back to the first asset chronologically. User overrides in the `user_events` table (custom name, cover, merged or split boundaries) are applied as post-processing on top of the computed runs. Assets with no `dateTaken` value are grouped into a separate "Unknown date" event at the end.

**Improvement 67 — no hard dependencies**

`event-auto-grouping` has no hard dependencies on other pending improvements. It does benefit from `star-ratings` (#11, already implemented) for the default cover photo selection heuristic. `timeline-view` (#18, already implemented) is a natural companion — both group by time — but the two are independent views.

**Improvement 68 — Laplacian variance implementation**

The sharpness estimate reads all pixels of the 200×150 thumbnail into a greyscale array (`0.299R + 0.587G + 0.114B`), applies the 3×3 discrete Laplacian kernel, and computes the variance of the resulting values. A sharp image has high variance (strong edges); a blurry image has low variance (soft gradients). The raw variance is normalised to 0–100 using empirically calibrated floor and ceiling values. The EXIF penalty terms are weighted: sharpness contributes 60%, ISO penalty 25%, and exposure-time penalty 15%. All computation uses `BufferedImage` pixel access from the Java standard library; no Maven dependency is added.

**Improvement 68 — backfill for existing assets**

The V28 migration adds `quality_score SMALLINT` as `NULL`. Existing assets receive a score when their folder is next re-cataloged. The gallery sort option and filter slider gracefully exclude `NULL`-scored assets (treated as unscored, shown last when sorting by quality).

**Improvement 68 → Improvement 22 (soft)**

`photo-quality-scoring` pairs naturally with `duplicate-auto-resolve` (#22): once scores are populated, the "keep highest resolution" policy can be extended with a "keep highest quality" policy that picks the asset with the highest `quality_score` among duplicates rather than requiring the user to compare manually.

**Improvement 69 — IPTC write safety**

`JpegIptcRewriter` from Apache Commons Imaging reads the existing IPTC APP13 segment, merges the updated fields, and writes a new JPEG to a temp file before atomically replacing the original. This avoids partial writes corrupting the file on crash. The backend validates that the target file path resolves to an asset the authenticated user owns before writing.

**Improvement 69 → Improvement 31 (soft)**

`iptc-xmp-metadata-editing` adds `caption` and `copyright` VARCHAR columns to `assets`. Once `full-text-search` (#31) is implemented, both columns should be included in the `search_vector` generated column so captions are full-text-searchable. The V17 migration for #31 should reference these columns if #69 is implemented first; otherwise the V17 trigger is updated to include them when #69 lands.

**Improvement 70 — k-means implementation detail**

K-means initialises centroids using k-means++ seeding (choose first centroid randomly, each subsequent centroid chosen with probability proportional to squared distance from the nearest existing centroid) to reduce sensitivity to random initialisation. The algorithm runs for a fixed 10 iterations regardless of convergence — this is sufficient for k=5 over thumbnail-sized inputs and avoids unbounded runtime during cataloging. The clustering operates in RGB space; the family snapping step converts each final centroid to HSL for comparison against the 12 family reference hues.

**Improvement 70 — no schema dependency ordering constraint**

The V30 migration adding `color_palette JSONB` to `assets` is independent of V28 (`quality_score`) and V29 (`caption`, `copyright`). All three columns are additive and can be applied in any order. Existing assets have `color_palette = NULL`; the gallery color swatch picker hides families with zero matches, so the picker is automatically empty until assets are re-cataloged.

**Improvement 71 — WebDAV compliance scope**

The implementation targets WebDAV Level 1 (RFC 4918) only: `OPTIONS`, `PROPFIND` (depth 0 and 1), `GET`, `HEAD`, `PUT`, `DELETE`, `MKCOL`. Level 2 locking (`LOCK`/`UNLOCK`) is not implemented — Windows and macOS WebDAV clients work without locking when the server advertises `DAV: 1` only. `COPY` and `MOVE` are deferred; clients that need them can use the existing `MoveAssetsUseCase` API endpoints directly.

**Improvement 71 — no schema change**

The WebDAV virtual filesystem is a read/write projection over the existing `assets` and folder data. No new tables or columns are required. `PUT` writes the file via `StoragePort` and queues a catalog job; `DELETE` sets `deleted_at` via `SoftDeleteAssetUseCase`; `MKCOL` creates a directory on disk (the folder appears in the tree after the next catalog run).

**Improvement 71 — authentication note**

JWT cookies cannot be forwarded by OS-level WebDAV mounts. The WebDAV endpoint at `/webdav/**` is exempted from the JWT cookie filter in `SecurityConfig` and instead uses `HttpBasicAuthenticationFilter` backed by the existing `UserDetailsService`. Credentials are sent over HTTPS only; `application.yml` must enforce `server.ssl.enabled=true` or a reverse proxy must terminate TLS before the WebDAV mount is used in production.

**Improvement 72 — technology choice rationale vs #63**

`mongodb-exif-store` (#72) and `raw-exif-jsonb` (#63) are mutually exclusive solutions to the same problem. #63 stores raw EXIF as a `JSONB` column on the existing `asset_exif` PostgreSQL table — simpler, keeps the single-database stack, immediately queryable with PostgreSQL operators (`->`, `@>`, GIN indexing). #72 moves EXIF to a MongoDB collection — adds operational complexity (a second database) but unlocks the native document model, `$near`/`$geoWithin` geospatial operators on the GPS coordinates already stored in `AssetExif`, and rich aggregation pipeline queries across 80–300 nested EXIF fields per image. The hexagonal architecture makes the swap transparent to use cases and controllers: only `AssetExifRepositoryImpl` (infrastructure) changes, implementing the same `AssetExifRepository` domain port. Decision rule: choose #63 if keeping a single PostgreSQL dependency is preferred and geospatial querying is not a current requirement; choose #72 if `gps-map-view` (#13) and geospatial search are key roadmap items. If #63 is implemented first, #72 becomes a data-migration task (copy JSONB data to MongoDB documents, drop the PostgreSQL table).

**Improvement 73 — MongoDB document design**

The audit log collection name is `asset_audit_log`. Every action produces a document with required fields `{ userId, action, entityType, entityId, timestamp }` and an optional `metadata` sub-document whose shape varies by action:

```
AssetTagged:   metadata: { tagName, tagId }
AssetDeleted:  metadata: { folderId, permanent: false }
CatalogRun:    metadata: { foldersScanned, assetsAdded, durationMs }
SyncRun:       metadata: { sourceDir, targetDir, filesCopied, filesDeleted }
ConvertRun:    metadata: { sourceDir, targetDir, filesConverted }
```

A compound index on `{ userId: 1, timestamp: -1 }` supports per-user history queries. A TTL index on `timestamp` (e.g. 365 days) automatically purges old entries. When `kafka-catalog-pipeline` (#75) is in place, audit log writes are handled by a dedicated Kafka consumer group (`audit-log-writer`) subscribed to `asset.cataloged`, `asset.deleted`, and `job.*.progress` topics; without #75, a direct port call from each use case serves as the fallback. The `AuditLogRepository` port interface in `domain/port/out/` declares a single `void log(AuditEvent event)` method; the `MongoAuditLogRepositoryImpl` adapter implements it — no framework type leaks into the domain.

**Improvement 74 — data migration strategy**

`user_preferences` and `search_presets` are small tables (one row per user for preferences, a few rows per user for presets). The one-time data migration exports all rows to MongoDB documents then drops the PostgreSQL tables. Because both tables are user-specific and low-volume, the migration can run online: (1) export to MongoDB, (2) switch the Spring beans from JPA adapters to MongoDB adapters, (3) drop the PostgreSQL tables in subsequent Flyway migrations (V32 for `user_preferences`, V33 for `search_presets`). The `UserPreferenceRepositoryImpl` and `SearchPresetRepositoryImpl` adapters are the only classes that change.

**Improvement 76 — 202 Accepted flow and partial-failure handling**

The `POST /api/assets/upload` response changes from `201 Created` (with the full `AssetResponse` body) to `202 Accepted` (with a `{ jobId, status: "PROCESSING" }` body). The frontend subscribes to an SSE channel `job:upload:progress:{jobId}` (same pattern as catalog SSE). When all three consumers (hash, EXIF, thumbnail) signal completion, the job transitions to `COMPLETED` and the frontend reloads the asset. Each consumer writes its results to the `AssetRepository` or `AssetExifRepository` in a separate transaction, so partial failures — e.g. EXIF extraction fails but hash and thumbnail succeed — leave the asset in a partially-populated state that can be resolved by re-triggering the failed consumer without re-running the others.

**Improvement 77 — single-partition leader election detail**

Kafka's consumer group protocol guarantees that a single-partition topic has exactly one active consumer per group at any time. `catalog.requests` is configured with `partitions=1`. All app instances join the consumer group `catalog-coordinator`. Kafka's group coordinator assigns the single partition to one member; the others idle. When the active member receives a `CatalogJobRequested` event, it checks `CatalogScheduler.isRunning()` before launching — this guards against duplicate triggers if the scheduling interval fires before the previous job completes. The `@Scheduled` timer in `CatalogScheduler` becomes a `kafkaTemplate.send("catalog.requests", ...)` call rather than a direct `JobLauncher.run()` invocation.

**Improvement 79 — dual-write migration window**

To avoid losing active sessions during the PostgreSQL → Redis cutover: (1) deploy a version that writes tokens to both PostgreSQL and Redis simultaneously; (2) run for one full refresh-token lifetime (30 days) to ensure all active tokens exist in Redis; (3) switch reads to Redis-only; (4) deploy a version that writes to Redis only and apply the Flyway migration that drops the `refresh_tokens` table (V32 or later). This gradual approach is safe because the token population is small — one active token per logged-in user — and the 30-day window comfortably covers all active sessions.

Note on `session-management` (#46): #46 plans to add `user_agent VARCHAR` to `refresh_tokens`. If #79 is implemented first, store `userAgent` as a Redis hash field: `HSET refresh_token:{tokenId} userId {userId} userAgent {ua} expiresAt {epoch}`. If #46 is implemented first, the `user_agent` PostgreSQL column is later migrated to the Redis hash structure during the #79 cutover — no data is lost.

**Improvement 80 — Kafka vs Redis Pub/Sub trade-offs for SSE**

Kafka and Redis Pub/Sub serve different purposes and are not interchangeable for SSE delivery. Kafka stores events persistently with consumer-group replay semantics — a consumer can replay missed events from the committed offset. Redis Pub/Sub is fire-and-forget: subscribers receive only events published while they are connected. For SSE (where the browser connection is live and transient), Redis Pub/Sub is the better real-time delivery fit: lower latency (~0.1 ms publish-to-receive), no consumer-group coordination overhead, and no offset management. Kafka remains essential for `asset.cataloged`/`asset.deleted` events consumed by audit log writers and search indexers that need durability. Recommended architecture: catalog/sync/convert workers publish to both the Kafka topic (durable consumers) and the Redis Pub/Sub channel (SSE fan-out) in a single step. Spring Data Redis's `RedisMessageListenerContainer` handles the subscription on each app instance.

**Improvement 81 — cache key stability and eviction**

The cache key `asset:thumbnail:{assetId}` is stable because thumbnails are content-addressed: generated once during cataloging and never mutated (the existing `thumbnail-http-cache` improvement adds `Cache-Control: immutable` for this reason). Invalidation is therefore narrowly scoped: only `PurgeAssetsUseCaseImpl` and `ThumbnailStorageServiceAdapter.deleteThumbnail()` need to call `DEL asset:thumbnail:{assetId}`. The `allkeys-lru` Redis eviction policy retains recently accessed thumbnails automatically. To prevent thumbnail caching from starving other Redis uses (rate-limit buckets, refresh tokens, SSE channels), either use a dedicated Redis instance or a separate key namespace with `maxmemory` configured per keyspace using Redis 7's `redis.conf` keyspace limits.

**Improvement 82 — cache invalidation strategy**

Asset search result cache invalidation is driven by Kafka events from `kafka-catalog-pipeline` (#75): a `@KafkaListener` subscribed to `asset.cataloged` and `asset.deleted` calls `redisTemplate.delete(pattern)` using the folder path as a key prefix (`assets:{folderPath}:*`). Without #75 in place, invalidation falls back to direct calls in `CatalogAssetItemWriter.write()` and `DeleteAssetsUseCaseImpl.execute()`. Tag cache invalidation is simpler: `AddTagToAssetUseCaseImpl` and `RemoveTagFromAssetUseCaseImpl` each call `redisTemplate.delete("tags:all")`. If `server-side-spring-cache` (#28) is implemented first with `CaffeineCacheManager`, upgrading to Redis requires only switching the `CacheManager` bean to `RedisCacheManager` backed by a Lettuce `RedisConnectionFactory` — the `@Cacheable` and `@CacheEvict` annotations on the use cases require no change.

**MongoDB, Kafka, and Redis — overlap with existing improvements (summary)**

The following table lists every new improvement that directly overlaps with or fills a gap in an existing planned improvement:

| New improvement | Overlaps / conflicts with | Relationship |
| --- | --- | --- |
| `redis-refresh-tokens` (#79) | `session-management` (#46) | **Coordinate** — #46 adds `user_agent` to PostgreSQL; #79 moves to Redis; deliver together |
| `redis-thumbnail-cache` (#81) | `server-side-spring-cache` (#28) | **Complementary** — #28 is per-JVM Caffeine; #81 adds distributed Redis |
| `redis-search-tag-cache` (#82) | `server-side-spring-cache` (#28) | **Extension** — #82 covers gallery endpoints #28 omits; upgrades Caffeine to Redis |
