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
| 46  | `session-management`        | The `refresh_tokens` table already stores `userId`, `tokenHash`, and `expiresAt`; add an optional `user_agent` column and expose `GET /api/auth/sessions` (list active sessions with device hint and last-used time), `DELETE /api/auth/sessions/{id}` (revoke one), and `DELETE /api/auth/sessions` (revoke all others); frontend `/profile/sessions` page lists sessions in a `MatTable` with a revoke button per row and a "sign out everywhere" action | ⬜ Pending | ⬜ Pending |
| 47  | `two-factor-authentication` | TOTP-based 2FA via any RFC 6238-compliant authenticator app (Google Authenticator, Authy, 1Password); backend dependencies: `dev.samstevens.totp:totp` (secret generation, code verification, `otpauth://` URI building) and `com.google.zxing:core` + `com.google.zxing:javase` (QR code PNG encoding returned as base64); new `totp_secret` (AES-encrypted at rest) and `totp_enabled` boolean columns on `users` (Flyway migration); setup flow: `POST /api/auth/2fa/setup` returns a base64 QR code PNG, `POST /api/auth/2fa/verify` validates the first code and commits the secret; login flow: password check passes → if `totp_enabled` return a `202 TOTP_REQUIRED` challenge → `POST /api/auth/2fa/challenge` validates code and sets JWT cookie; generate 10 single-use backup codes (BCrypt-hashed, stored in `totp_backup_codes` table) in case the authenticator device is lost; TOTP verification endpoint must be covered by `api-rate-limiting` (#39) | ⬜ Pending | ⬜ Pending |
| 48  | `email-notifications`       | Spring Mail (`spring-boot-starter-mail`) to send a summary email when long-running operations complete: catalog (N new assets, M updated), sync result, convert result, and backup uploaded; add `email` VARCHAR and `email_notifications_enabled` boolean to `users` (Flyway migration); configurable SMTP host, port, and credentials in `application.yml`; frontend profile page gains an email field and notification toggle; pairs naturally with `notification-center` (#54) as both are triggered by the same operation-completion events | ⬜ Pending | ⬜ Pending |
| 49  | `auto-tagging`              | During cataloging, automatically apply tags derived from EXIF data: the year from `dateTaken`, camera make normalised to lowercase (e.g. `canon`, `sony`, `apple`), and — once `gps-map-view` (#13) is implemented — a reverse-geocoded city or country name; tags are written through the existing `asset_tags` table and tag infrastructure; auto-applied tags are indistinguishable from manual ones and can be removed by the user; no new schema required beyond what the tag feature already provides | ⬜ Pending | ⬜ Pending |
| 50  | `image-comparison-viewer`   | Select exactly two assets in the gallery → "Compare" action opens a split-screen view showing both images side by side at matched zoom levels, with filename, size, dimensions, and rating displayed beneath each panel; most useful as a companion to the duplicates workflow but available from any multi-selection; no new backend endpoint — both images are served by the existing `GET /api/assets/{id}/image`; new `ComparisonViewerComponent` in `features/gallery/` | ⬜ Pending | ⬜ Pending |
| 51  | `folder-bookmarks`          | A pin icon on each node in the folder navigation tree bookmarks it per authenticated user; bookmarks stored in a new `folder_bookmarks` table (`id`, `userId`, `folderPath`, `createdAt`); bookmarked folders appear as a pinned section above the full tree backed by `GET /api/folders/bookmarks`, `POST /api/folders/bookmarks`, and `DELETE /api/folders/bookmarks/{id}`; frontend inserts a `MatDivider` between the pinned section and the main tree; new Flyway migration | ⬜ Pending | ⬜ Pending |
| 52  | `multi-language-i18n`       | Angular `@angular/localize` for the frontend starting with English and Spanish (mirrors open desktop issue #140); Spring Boot `MessageSource` for backend validation and error messages; locale preference stored per user as a `locale` column on `users` (or inside a `user_preferences` JSON column shared with dark-mode preference from #15); language toggle in the top navigation bar; Angular build produces one bundle per locale via `ng build --localize` | ⬜ Pending | ⬜ Pending |
| 53  | `password-strength-policy`  | Enforce minimum password complexity on user creation and password change using the `Passay` library (configurable rules: minimum length 12, at least one uppercase, one digit, one special character); the Angular user-admin form and profile page show a live strength meter powered by the same rule set mirrored client-side; returns a structured `400` with per-rule violation details so the frontend can highlight exactly which rules failed; no schema change | ⬜ Pending | ⬜ Pending |
| 54  | `notification-center`       | In-app notification bell in the top navigation bar showing a history of completed background operations (catalog finished, sync complete, convert complete, backup uploaded); new `notifications` table (`id`, `userId`, `type`, `message`, `read_at`, `created_at`); backend writes a notification row at the end of each SSE stream; `GET /api/notifications` returns unread count and paginated history; `PATCH /api/notifications/read` marks all read; badge count clears when the panel is opened; new Flyway migration | ⬜ Pending | ⬜ Pending |
| 55  | `webp-avif-conversion`      | Extend `ConvertAssetsUseCase` — currently PNG→JPEG only — to support JPEG/PNG→WebP and JPEG/PNG→AVIF; WebP encoding via `cwebp` invoked through `ProcessBuilder` (same pattern as #21 FFmpeg); AVIF encoding via `avifenc` through `ProcessBuilder`; the `convert_assets_directories_definitions` table gains a `target_format` VARCHAR column (Flyway migration); frontend `ConvertComponent` adds a "Target format" dropdown (JPEG / WebP / AVIF) to the directory pair configuration form | ⬜ Pending | ⬜ Pending |
| 56  | `asset-image-editor`        | Add brightness, contrast, and hue adjustment to the viewer; CSS `filter: brightness() contrast() hue-rotate()` applied to the `<img>` tag drives live preview via three `MatSlider` inputs with zero backend calls; saving dispatches `POST /api/assets/{id}/edit` with the adjustment values; the backend processes the image using Java2D (`java.awt.image`) with no external dependency: `RescaleOp` applies brightness and contrast in a single pass, hue is adjusted by converting RGB→HSB via `Color.RGBtoHSB`, rotating the H component, and converting back; the edited file is saved as a new asset alongside the original (non-destructive by default); an optional "replace original" flag covers the destructive case; no new Maven dependency, no Docker image change | ⬜ Pending | ⬜ Pending |
| 57  | `viewer-pan-drag`           | Add mouse and touch drag-to-pan to the zoomed image viewer; the existing `transform: scale(viewerZoom)` is extended to `scale(zoom) translate(panX, panY)`; `mousedown` sets a dragging flag and captures the starting cursor position, `mousemove` updates `panX`/`panY` while dragging, `mouseup` clears the flag; `touchstart`/`touchmove`/`touchend` mirror the same logic for mobile; `panX` and `panY` reset to zero whenever zoom returns to 1× or the displayed asset changes; pure frontend change — no backend endpoint, no schema change, no external dependency | ⬜ Pending | ⬜ Pending |
| 58  | `video-from-images`         | New wizard component lets the user select an ordered list of images, set a per-slide duration, choose a background music file (uploaded on the spot or selected from a cataloged audio asset once `audio-asset-support` #59 is in place), and trigger video generation; the backend invokes FFmpeg via `ProcessBuilder` (`ffmpeg -framerate 1/{duration} -i frame%04d.jpg -i music.mp3 -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p output.mp4`), streams progress via SSE (same pattern as catalog/sync/convert), and saves the output to a user-chosen folder where it is auto-cataloged as a video asset; hard dependency on `video-file-support` (#21) for FFmpeg presence in the container | ⬜ Pending | ⬜ Pending |
| 59  | `audio-asset-support`       | Catalog audio files (.mp3, .flac, .wav, .aac, .ogg); extract ID3/Vorbis/FLAC metadata (title, artist, album, duration, bitrate, sample rate, embedded album art) via `org.jaudiotagger:jaudiotagger` (Maven); store audio-specific fields in a new `asset_audio` table mirroring `asset_exif` (Flyway migration); use embedded album art as the asset thumbnail if present, or generate a waveform PNG via FFmpeg (`ffmpeg -i input.mp3 -filter_complex "showwavespic=s=200x150" -frames:v 1 waveform.png`) as fallback; the frontend viewer switches on asset type and renders an `<audio controls>` tag for audio assets with title, artist, album, duration, and bitrate displayed alongside playback controls; once implemented, `video-from-images` (#58) can offer a "select from audio assets" music picker | ⬜ Pending | ⬜ Pending |
| 60  | `archive-support`           | Two related capabilities sharing the same archive-reading infrastructure (`org.apache.commons:commons-compress` for tar.gz; `java.util.zip` built-in for zip): (1) **virtual folders** — zip and tar.gz files appear as expandable nodes in the folder navigation tree using a `!` path separator convention (e.g. `/photos/album.zip!/summer/`); the catalog service extracts images to a temp location, generates thumbnails, and stores assets with the virtual path; (2) **download formats** — the bulk-download endpoint (`GET /api/assets/download`) gains a `format` query parameter (`zip` / `tar.gz`) so users can choose the archive type; the existing `ZipOutputStream` path is joined by a `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`; no Flyway migration required for either capability | ⬜ Pending | ⬜ Pending |
| 61  | `asset-backup`              | Backup a configurable scope of assets (folder, album, saved search result, or entire catalog) to one or more sequentially numbered archive files (e.g. `backup_photos_001.zip`, `backup_photos_002.zip`) split at a configurable volume size; format is zip or tar.gz (reuses `archive-support` #60 writing infrastructure); trigger is manual (`POST /api/backup/{id}/run`) or a per-definition cron expression scheduled dynamically via Spring `TaskScheduler` + `CronTrigger` (cancelled and rescheduled on definition update); new `/backup` frontend route mirrors the convert page structure (definitions list → configure → run → results with SSE progress and per-file logging); backend stores definitions in `backup_definitions` and run history in `backup_run_log` (timestamps, status, files written, bytes, errors); new Flyway migration | ⬜ Pending | ⬜ Pending |
| 63  | `raw-exif-jsonb`            | Add a `raw_exif JSONB` column to the existing `asset_exif` table (new Flyway migration); during cataloging, after extracting the 13 known fields, iterate all EXIF directories returned by Apache Commons Imaging (`JpegImageMetadata.getExif().getDirectories()` → `dir.getAllFields()`) and collect every `TiffField` into a `Map<String, String>` keyed by `field.getTagInfo().name` with value `field.getValueDescription()`; the map is serialized to JSONB using Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, String> rawExif` field in `AssetExifEntity` (no new Maven dependency — Hibernate 6 handles JSON natively via Jackson, already present); `AssetExif` domain model and `AssetExifEntityMapper` (MapStruct) gain the `rawExif` field; `ExifMetadataDto` exposes it as `Map<String, String> rawExif`; `ExifMetadata` TypeScript interface adds `rawExif: Record<string, string> \| null`; in `ExifPanelComponent`, below the existing 13 structured fields, a collapsible `MatExpansionPanel` labeled "All EXIF data" renders every key-value pair from `rawExif` as a compact two-column list; a `MatFormField` search input above the list filters entries by key name in real time using a component-level computed signal so the full raw map is never re-fetched; a single image from a modern DSLR or mirrorless camera typically carries 80–300 EXIF fields across the IFD0, ExifIFD, GPS IFD, and MakerNote directories; the search filter is essential because MakerNote alone can add 100+ manufacturer-specific fields (Nikon colour modes, Canon lens correction data, Sony face detection coordinates, etc.); existing assets cataloged before this migration have `raw_exif = NULL` and the panel section is hidden when `rawExif` is null; re-cataloging any folder populates the column for all assets in that folder; new Flyway migration | ⬜ Pending | ⬜ Pending |
| 62  | `social-media-crop`         | Canvas-based interactive crop tool for 12 social media format presets; a scissors icon button added to the viewer toolbar toggles `showCropPanel` (same pattern as `showExifPanel`); the `<img>` is replaced by a `<canvas>` in crop mode — the image is drawn on the canvas and a semi-transparent overlay renders the draggable crop box with corner handles; the crop box aspect ratio is always locked to the selected format; dragging inside the box moves it, dragging a corner handle resizes it while keeping the ratio; for profile-image formats (Instagram Profile, Facebook Profile, LinkedIn Profile, Twitter/X Profile) a `ctx.arc()` circle outline is drawn inside the crop box to preview the platform's circular display; when the format changes the crop box snaps to maximum fit centered on the image; the 12 presets are: `INSTAGRAM_POST` 1080×1080 (1:1), `INSTAGRAM_PORTRAIT` 1080×1350 (4:5), `INSTAGRAM_LANDSCAPE` 1080×566 (~1.91:1), `INSTAGRAM_STORY` 1080×1920 (9:16), `INSTAGRAM_PROFILE` 110×110 (1:1 circle), `FACEBOOK_POST` 1200×630 (~1.91:1), `FACEBOOK_PROFILE` 170×170 (1:1 circle), `LINKEDIN_POST` 1200×627 (~1.91:1), `LINKEDIN_PROFILE` 400×400 (1:1 circle), `TWITTER_POST` 1600×900 (16:9), `TWITTER_PROFILE` 400×400 (1:1 circle), `TWITTER_HEADER` 1500×500 (3:1); on confirm the frontend translates canvas-display coordinates to original image pixel coordinates using `asset.pixelWidth` / `asset.pixelHeight` (already on the `Asset` model) and sends `POST /api/assets/{id}/crop` with `{ formatKey, x, y, width, height }`; the backend uses Java2D `BufferedImage.getSubimage(x, y, w, h)` to extract the crop region and `Graphics2D.drawImage()` to scale it to the format's target dimensions, then saves the result as a new `Asset` in the same folder (non-destructive) with a freshly generated thumbnail; after the backend returns the new `AssetResponse` the frontend immediately triggers a browser download of the cropped image by creating a temporary `<a download>` element pointing to `GET /api/assets/{newId}/image`; no Flyway migration (cropped outputs are regular assets in existing tables), no new Maven dependency (Java2D is built-in), no new npm package (Canvas API is built into all browsers) | ⬜ Pending | ⬜ Pending |

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
| V18       | `two-factor-authentication` — `totp_secret`, `totp_enabled` on `users`; `totp_backup_codes` table |
| V19       | `email-notifications` — `email`, `email_notifications_enabled` on `users`         |
| V20       | `folder-bookmarks` — `folder_bookmarks` table                                      |
| V21       | `notification-center` — `notifications` table                                      |
| V22       | `session-management` — `user_agent` column on `refresh_tokens`                     |
| V23       | `webp-avif-conversion` — `target_format` column on `convert_assets_directories_definitions` |
| V24       | `audio-asset-support` — `asset_audio` table                                         |
| V25       | `asset-backup` — `backup_definitions` and `backup_run_log` tables                   |
| V26       | `raw-exif-jsonb` — `raw_exif` JSONB column on `asset_exif`                          |

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

**Improvement 47 → Improvement 39**

`two-factor-authentication` introduces a TOTP verification endpoint that must be rate-limited to prevent brute-force attacks on 6-digit codes. `api-rate-limiting` (#39) should be in place first, or the TOTP endpoint must include its own Bucket4j bucket as part of the #47 implementation.

**Improvement 47 → Improvement 46**

Revoking all sessions (`DELETE /api/auth/sessions`) is the recommended recovery action when a user suspects their account is compromised. Implementing `session-management` (#46) before or alongside #47 gives users the tools to respond to a potential account takeover.

**Improvement 47 — external dependencies detail**

*`dev.samstevens.totp:totp` (Maven, latest stable: 1.7.1)*

The core TOTP library implementing RFC 6238 (TOTP) and RFC 4226 (HOTP). Provides:
- `SecretGenerator` — generates a cryptographically random 160-bit base32 secret
- `CodeVerifier` — validates a 6-digit user-submitted code against the stored secret; the default time-step window of ±1 step (±30 seconds) tolerates typical clock drift between client and server
- `QrData` builder — constructs the `otpauth://totp/JPPhotoManager:{username}?secret={secret}&issuer=JPPhotoManager` URI that authenticator apps (Google Authenticator, Authy, 1Password) parse when scanning the QR code

*`com.google.zxing:core` + `com.google.zxing:javase` (Maven, latest stable: 3.5.3)*

ZXing (Zebra Crossing) encodes the `otpauth://` URI into a QR code bitmap. `QRCodeWriter` produces a `BitMatrix`; `MatrixToImageWriter` renders it to a PNG `ByteArrayOutputStream`. The backend returns the PNG as a base64 string so the frontend displays it as `<img src="data:image/png;base64,...">` — no extra round-trip and the image is never persisted anywhere.

*No additional frontend npm package required*

The base64-PNG-from-backend approach keeps the TOTP secret entirely server-side. An alternative is to return the raw `otpauth://` URI to the frontend and use the `qrcode` npm package (zero dependencies) to render the QR code client-side — but this briefly exposes the secret to JavaScript, which is less secure.

*Secret storage — AES-256-GCM encryption at rest*

The `totp_secret` column must never be stored in plaintext. The recommended implementation is a JPA `@Convert` annotation backed by an `AttributeConverter<String, String>` that AES-256-GCM encrypts the secret using a key loaded from the `TOTP_ENCRYPTION_KEY` environment variable. A database leak then exposes only ciphertext.

*Backup codes — BCrypt-hashed*

The 10 single-use recovery codes are BCrypt-hashed before insertion into `totp_backup_codes` — the same treatment as passwords — so a database leak does not expose usable codes. On use, the matching row is deleted; the plaintext codes are shown to the user exactly once during setup and never stored.

**Improvement 48 → Improvement 54**

`email-notifications` and `notification-center` are triggered by the same operation-completion events (end of catalog, sync, convert, backup SSE streams). Implementing them together avoids wiring the same trigger points twice; if delivered separately, #54 should come first so the notification infrastructure exists when #48 extends it with email dispatch.

**Improvement 49 → Improvement 1**

`auto-tagging` reads `dateTaken` and camera make from EXIF data stored by `exif-metadata-panel` (#1, already implemented). The GPS city/country auto-tag additionally depends on `gps-map-view` (#13) for reverse geocoding.

**Improvement 52 → Improvement 15**

`multi-language-i18n` and `dark-mode` (#15) both store per-user UI preferences. Implementing them together — or implementing #15 first with a `user_preferences` JSON column that #52 can extend — avoids two separate schema changes and two separate backend endpoints for preference persistence.

**Improvements 46, 50, 53 — no schema changes**

`session-management` (beyond the optional `user_agent` column), `image-comparison-viewer`, and `password-strength-policy` require no Flyway migrations.

**Improvements 50, 53 — no new backend endpoints**

`image-comparison-viewer` reuses the existing `GET /api/assets/{id}/image` endpoint. `password-strength-policy` adds validation logic to existing endpoints only.

**Improvement 45 → Improvement 44**

`postgres-dockerize` must be completed before `database-backup` (#44). The backup service runs `pg_dump` against the database container over the Docker network (`POSTGRES_HOST: db`); this only works once the database is running inside the Compose stack. Running `pg_dump` against a host-based PostgreSQL from inside a container requires additional network configuration (`host.docker.internal` or `--network host`) that would be rendered unnecessary once #45 is done. The one-time host-to-container data migration script required by #45 also serves as the first real test of the dump/restore tooling that #44 will depend on in production.

**Improvement 57 — no dependencies**

`viewer-pan-drag` is a pure frontend change with no backend, schema, or external library involvement. It can be delivered at any point independently of all other improvements.

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

**Improvement 56 — no external dependencies**

`asset-image-editor` uses Java2D (`java.awt.image`, standard library) for all three operations: `RescaleOp` for brightness and contrast, RGB→HSB→RGB conversion for hue. No new Maven dependency and no Docker image change are required. ImageMagick was considered but is unnecessary for these specific operations; Java2D handles them in-process and is faster than shelling out to an external tool for a single image.

**Improvement 58 → Improvement 21**

`video-from-images` has a hard dependency on `video-file-support` (#21): FFmpeg must be installed in the backend container before the video generation `ProcessBuilder` call can work. #58 cannot be delivered before #21.

**Improvement 58 → Improvement 59 (soft)**

`video-from-images` can accept a music file upload at video-creation time and does not require audio assets to exist. However, once `audio-asset-support` (#59) is in place the wizard gains a "select from audio assets" picker, making music selection significantly more convenient. Implementing #59 before #58 delivers the better user experience from day one.

**Improvement 59 → Improvement 21 (soft)**

`audio-asset-support` does not require FFmpeg for its core functionality — `jaudiotagger` handles metadata extraction and album art independently. FFmpeg is only needed for the waveform fallback thumbnail when no album art is embedded. The waveform generation can be deferred until #21 is implemented; until then, a generic music-note placeholder icon is used as the thumbnail.

**Improvement 40 → Improvement 13**

`circuit-breaker` is a follow-up to `gps-map-view`. It is not warranted in the current architecture because the backend makes no outbound calls to external services — PostgreSQL and the filesystem are the only dependencies, and the HikariCP connection pool already handles their failure modes. Once #13 introduces an outbound HTTP call to a geocoding API (e.g. Nominatim or Google Maps), that call becomes the first concrete circuit breaker target: if the geocoding service is slow or unavailable it will stall the EXIF panel for every viewed image. At that point, wrapping the `GeocodingPort` adapter with Resilience4j `@CircuitBreaker` and returning a fallback of `null` coordinates is the right response. Additional future improvements that would extend the scope of #40 include cloud storage integration and external email notifications if those are ever added.

**Improvement 62 — no Flyway migration, no external dependencies**

`social-media-crop` adds no database schema changes: the cropped output is stored as a regular `Asset` row in the existing `assets` table via the same catalog path used by all other assets. No new Maven dependency is required — Java2D (`BufferedImage`, `Graphics2D`) is part of the Java standard library and is already in use for `asset-image-editor` (#56). No new npm package is required — the HTML5 Canvas API (`<canvas>`, `CanvasRenderingContext2D`) is available in all modern browsers without installation.

**Improvement 62 → Improvement 56 (soft)**

`social-media-crop` and `asset-image-editor` (#56) share the same backend pattern: send transformation parameters → Java2D processes the original image → result saved as a new `Asset` in the same folder → thumbnail generated → new `AssetResponse` returned. If #56 is implemented first, its "save as new asset" logic can be extracted into a shared utility method (e.g. `AssetSavePort.saveProcessedAsset(...)`) that #62 reuses without duplication. The two improvements can also be delivered in either order: each endpoint (`/edit` and `/crop`) is independent, and the shared utility is a refactoring opportunity rather than a hard requirement.

**Improvement 62 — coordinate translation**

The crop box is drawn and dragged in canvas-display space (pixels on screen), but the backend receives coordinates in original image pixel space. The translation uses `pixelWidth` and `pixelHeight` already present on the `Asset` model and already returned by `GET /api/assets`:

```
imageScaleX = asset.pixelWidth  / canvas.offsetWidth
imageScaleY = asset.pixelHeight / canvas.offsetHeight

actualX = Math.round(cropX * imageScaleX)
actualY = Math.round(cropY * imageScaleY)
actualW = Math.round(cropW * imageScaleX)
actualH = Math.round(cropH * imageScaleY)
```

These four values are sent as integers in the `POST /api/assets/{id}/crop` request body. The backend validates that `actualX + actualW ≤ pixelWidth` and `actualY + actualH ≤ pixelHeight` before processing to guard against floating-point rounding edge cases.

**Improvement 62 — format change behaviour**

When the user selects a new format preset, the crop box is recalculated to the maximum area that fits within the canvas while maintaining the new aspect ratio, centered on the image. This "snap to max fit" avoids the crop box jumping to a tiny or off-screen position. The previous crop position is discarded rather than attempting to preserve it, since a radical aspect ratio change (e.g. 9:16 story → 3:1 header) makes position preservation misleading.

**Improvement 62 — toolbar placement**

The scissors icon (`content_cut` Material icon) is inserted between the EXIF panel toggle and the slideshow button in the viewer toolbar, following the existing pattern for panel toggles. The button carries an `[class.active]="showCropPanel"` binding so it highlights when the crop panel is open, matching the EXIF button's behaviour. Activating crop mode immediately hides the `<img>` element and renders the `<canvas>` in its place; closing the crop panel (via the Cancel button or the toolbar icon) restores the `<img>` and resets all crop state.

```
VIEWER TOOLBAR — CURRENT STATE AND INSERTION POINT

  ┌────────────────────────────────────────────────────────────────────┐
  │  [folder name]                                          [  space ] │
  │  [zoom−] [zoom+] [★][★][★][★][★]  [ⓘ EXIF]  [▶ slide]  [⊞ grid] │
  └────────────────────────────────────────────────────────────────────┘
                                               ↑
                                         insert [✂ crop] here
                                         between EXIF and slideshow

  VIEWER TOOLBAR — AFTER ADDING CROP BUTTON

  ┌──────────────────────────────────────────────────────────────────────────┐
  │  [folder name]                                              [   space  ] │
  │  [zoom−] [zoom+] [★][★][★][★][★]  [ⓘ EXIF]  [✂ crop]  [▶ slide]  [⊞] │
  └──────────────────────────────────────────────────────────────────────────┘
```

**Improvement 62 — UI layout in crop mode**

Entering crop mode replaces the full-size `<img>` with a `<canvas>` on the left and opens the `CropPanelComponent` on the right — the same two-column layout already used by `ExifPanelComponent`. The previous and next navigation arrows remain visible beneath the canvas so the user can switch assets without leaving crop mode (crop state resets on asset change).

```
VIEWER LAYOUT IN CROP MODE

  ┌──────────────────────────────────────────────────────────────────┐
  │  toolbar  (✂ button highlighted/active)                          │
  ├─────────────────────────────────┬────────────────────────────────┤
  │                                 │  CROP PANEL                    │
  │  <canvas>                       │  ┌────────────────────────┐    │
  │                                 │  │ Format                 │    │
  │  ┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┐      │  │ [ Instagram Post ▼   ] │    │
  │  │ ░░░░░░░░░░░░░░░░░░░░░│      │  └────────────────────────┘    │
  │  │ ░╔═══════════════════╗░│    │                                │
  │  │ ░║                   ║░│    │  Preview                       │
  │  │ ░║   crop box        ║░│    │  ┌────────────────────────┐    │
  │  │ ░║   (draggable)     ║░│    │  │   [cropped region]     │    │
  │  │ ░╚═══════════════════╝░│    │  │   1080 × 1080          │    │
  │  │ ░░░░░░░░░░░░░░░░░░░░░│      │  └────────────────────────┘    │
  │  └─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┘      │                                │
  │                                 │  [ Save & Download ]           │
  │  [◀ prev]          [next ▶]    │  [ Cancel ]                    │
  └─────────────────────────────────┴────────────────────────────────┘

  For circle formats (profile images), the crop box contains a
  ctx.arc() circle outline previewing the platform's circular display:

  │ ░╔═══════════════╗░│
  │ ░║   ╭───────╮   ║░│    ← circle outline inside the square crop box
  │ ░║  │       │   ║░│
  │ ░║   ╰───────╯   ║░│
  │ ░╚═══════════════╝░│
```

**Improvement 62 — frontend/backend pixel work split**

The canvas is used exclusively for interactive preview and user interaction. It never sends image bytes to the backend. Instead it sends four integers (crop region in original image pixel space) and a format key. All pixel processing — crop, scale to target dimensions, thumbnail generation — is done by the backend using Java2D.

```
WHO DOES THE PIXEL WORK

  Frontend Canvas                      Backend Java2D
  ───────────────────                  ──────────────────────────────
  Draws image on canvas ✓              img.getSubimage(sx, sy, sw, sh)
  Renders crop overlay ✓               Graphics2D.drawImage() to scale
  Handles drag/resize events ✓         Writes file to disk
  Translates display → pixel coords ✓  Saves new Asset + thumbnail
  Shows circle outline preview ✓       Returns new AssetResponse
          │
          │  POST /api/assets/{id}/crop
          │  { formatKey, x, y, width, height }   ──────────────▶
          │
          │◀──────────────────────────────────────  AssetResponse
          │
  Triggers browser download
  via <a download> → GET /api/assets/{newId}/image
```

**Improvement 62 — canvas interaction state machine**

The canvas tracks three state variables: `cropX`, `cropY` (top-left corner of the crop box in canvas-display space), and `cropW`, `cropH` (dimensions in canvas-display space, always aspect-locked to the selected format). Mouse events on the canvas drive three interaction modes:

```
CANVAS INTERACTION — DRAG MECHANICS

  Component state
  ───────────────
  cropX, cropY    top-left corner of crop box (canvas-display pixels)
  cropW, cropH    size of crop box (aspect ratio always locked to format)
  dragMode        'move' | 'resize' | null
  dragStart       { x, y, cropX, cropY, cropW, cropH } snapshot on mousedown

  Canvas rendering (each frame)
  ──────────────────────────────
  1. ctx.drawImage(imgEl, 0, 0, canvas.width, canvas.height)
  2. Semi-transparent dark overlay on the four regions outside the crop box
  3. Bright 2px border around the crop box
  4. Four corner handles (8 × 8 px squares)
  5. For circle formats: ctx.arc() at center of crop box

  mousedown hit-test
  ──────────────────
       ┌──────────────────────────────────────────────────┐
       │ pointer inside corner handle (8 px radius)?      │
       │         yes ──▶  dragMode = 'resize'             │
       │         no                                       │
       │          └─▶  pointer inside crop box?           │
       │                    yes ──▶  dragMode = 'move'    │
       │                    no  ──▶  (no interaction)     │
       └──────────────────────────────────────────────────┘

  mousemove
  ─────────
  'move' mode:
    cropX = clamp(dragStart.cropX + dx, 0, canvas.width  - cropW)
    cropY = clamp(dragStart.cropY + dy, 0, canvas.height - cropH)

  'resize' mode (aspect-locked, anchor = opposite corner):
    newW  = clamp(dragStart.cropW + dx, minSize, canvas.width)
    newH  = newW / formatAspectRatio          ← ratio enforced here
    cropW = newW
    cropH = newH
    (cropX, cropY adjusted to keep anchor corner fixed)

  mouseup
  ───────
  dragMode = null
  Constraint: crop box always stays fully within canvas bounds
```

**Improvement 63 → Improvement 1 (hard)**

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
  │  │ 🔍 Filter tags…         │    │  ← MatFormField search input
  │  └─────────────────────────┘    │
  │  ExposureTime          1/500    │
  │  FNumber               1.8      │  ← @for over filteredRawExif()
  │  ISOSpeedRatings       400      │
  │  LensModel    FE 85mm F1.4 GM   │
  │  Make                  SONY     │
  │  Model         ILCE-7M4         │
  │  …                              │
  └─────────────────────────────────┘
```

**Improvement 63 → Improvement 31 (soft)**

The `raw_exif` JSONB column is queryable with PostgreSQL's `->` and `@>` operators and GIN-indexable. Once `full-text-search` (#31) is implemented, the search vector trigger could include selected raw EXIF fields — for example, `raw_exif->>'LensModel'` or `raw_exif->>'Model'` — giving users the ability to search by equipment not captured in the 13 fixed columns. This extension requires no schema change beyond what #31 already defines; it is a trigger function update only.

**Improvement 63 — backfill strategy**

The V26 migration adds the column as `ALTER TABLE asset_exif ADD COLUMN raw_exif JSONB`. Existing rows have `raw_exif = NULL`. Backfilling requires re-cataloging the affected folders: `POST /api/assets/catalog` triggers the full extraction pipeline, which now includes the raw EXIF pass. No SQL-level backfill is possible because EXIF extraction reads the image file from disk, not from database data. The `ExifPanelComponent` handles `NULL` gracefully by hiding the "All EXIF data" section for assets not yet re-cataloged.
