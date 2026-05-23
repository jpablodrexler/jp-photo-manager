# Planned Improvements

This document records all planned improvements to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them.

---

## Improvement List

| #   | Change name                 | Brief description                                                                                                                                                                       | Artifacts  | Implementation |
| --- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- |
| 1   | `exif-metadata-panel`       | Store full EXIF metadata during cataloging; display a collapsible panel in the viewer showing camera, exposure, GPS, and date-taken fields                                              | ✅ Created | ✅ Implemented |
| 2   | `virtual-scrolling-gallery` | Replace the CSS grid with a fixed-height list layout; render only visible items in the DOM via `CdkVirtualScrollViewport` with `*cdkVirtualFor`; remove the unused `ScrollingModule` import and the IntersectionObserver sentinel; each list row shows the thumbnail on the left and filename, size, and date metadata on the right | ✅ Created | ✅ Implemented |
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
| 13  | `gps-map-view`              | Add a Leaflet.js map panel to the EXIF viewer and a `/map` route showing clustered photo pins for the current folder or album; clicking a pin navigates to that asset in the gallery   | ✅ Created | ⬜ Pending      |
| 14  | `smart-albums`              | Extend albums with an optional `filter_json` column (same shape as saved search presets) so albums can be dynamically populated at query time; UI toggle between static and dynamic mode | ✅ Created | ⬜ Pending      |
| 15  | `dark-mode`                 | Angular Material dark palette toggle stored in `localStorage` with `prefers-color-scheme` fallback; preference persisted per user on the backend; toolbar toggle button                 | ✅ Created | ⬜ Pending      |
| 16  | `keyboard-shortcuts`        | Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings               | ✅ Created | ⬜ Pending      |
| 17  | `batch-rename`              | Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint             | ✅ Created | ⬜ Pending      |
| 18  | `timeline-view`             | Third gallery view mode grouping assets by year then month with sticky date headers; `GET /api/assets/timeline-groups` returns counts per bucket; buckets load lazily via Intersection Observer | ✅ Created | ✅ Implemented |
| 19  | `shareable-album-links`     | `POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication | ✅ Created | ⬜ Pending |
| 20  | `storage-analytics`         | `/analytics` route with `ngx-charts` visualizations: storage-per-folder treemap, file-format pie, photos-per-month histogram, rating distribution bar; backed by aggregate JPQL queries  | ✅ Created | ⬜ Pending      |
| 21  | `video-file-support`        | Thumbnail generation via FFmpeg (`ProcessBuilder`) for `.mp4`/`.mov`/`.mkv`; catalog service accepts video MIME types; frontend shows play-overlay icon and `<video>` tag in the viewer  | ✅ Created | ⬜ Pending      |
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
| 32  | `folder-watch-service`      | Add a Java NIO `WatchService` that monitors all configured root catalog folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events and triggers incremental catalog updates automatically; reuses the existing `CatalogAssetsUseCase` and `catalog_run_state` distributed lock as the execution path; keeps the catalog in sync without any manual user action | ✅ Created | ⬜ Pending |
| 33  | `role-based-access-control` | Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value | ✅ Created | ⬜ Pending |
| 34  | `openapi-documentation`     | Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`; annotate all 11 `@RestController` classes with `@Operation` and `@ApiResponse`; exposes live Swagger UI at `/swagger-ui.html` and a machine-readable spec at `/v3/api-docs`; no schema change required; document that the `/swagger-ui.html` endpoint must be exempted from JWT authentication in `SecurityConfig` | ✅ Created | ⬜ Pending |
| 35  | `thumbnail-regeneration`    | Add `POST /api/assets/regenerate-thumbnails` (optionally scoped by `folderPath` query param) that deletes existing `.bin` thumbnail files via `ThumbnailPort` and re-generates them through `StoragePort`; covers corrupted thumbnails, thumbnail size changes, and retroactive EXIF-rotation correction; reuses existing infrastructure adapters with no schema change | ✅ Created | ⬜ Pending |
| 36  | `global-error-handler`      | Override Angular's `ErrorHandler` to display a `MatSnackBar` notification for all unhandled component errors; extend the existing backend `GlobalExceptionHandler` to return a consistent `{ status, message, timestamp }` JSON body for every 4xx and 5xx response so the frontend interceptor can surface a human-readable message rather than showing a raw HTTP status | ✅ Created | ⬜ Pending |
| 37  | `asset-description`         | Add a `description` VARCHAR column to `assets` (new Flyway migration); expose `PATCH /api/assets/{id}/description`; display an editable text field in the EXIF panel in the viewer; the description field feeds directly into the `full-text-search` (#31) index once both are in place | ✅ Created | ⬜ Pending |
| 38  | `folder-stats-in-tree`      | Show asset count and total size as a secondary line per folder node in the folder navigation tree; backed by `GET /api/folders/stats?path=...` running a lightweight `SELECT COUNT(*), SUM(file_size)` query per folder; distinct from the full analytics dashboard (#20) — this is inline contextual data in the tree, not a separate page | ✅ Created | ⬜ Pending |
| 39  | `api-rate-limiting`         | Add `bucket4j-spring-boot-starter` with per-IP token-bucket limits on `POST /api/auth/login` (brute-force prevention) and `GET /api/assets/catalog` (prevent accidental concurrent runs from multiple browser tabs); returns `429 Too Many Requests` with a `Retry-After` header when the bucket is exhausted; no schema change required | ✅ Created | ⬜ Pending |
| 40  | `circuit-breaker`           | Add Resilience4j `@CircuitBreaker` on the `GeocodingPort` adapter introduced by `gps-map-view` (#13); if the external geocoding API (e.g. Nominatim) is slow or unavailable the circuit opens and the adapter returns `null` coordinates immediately rather than stalling the EXIF panel; scope extends to any future outbound HTTP adapters (cloud storage, email); not applicable to the current backend which makes no external calls — PostgreSQL and filesystem failure modes are already covered by HikariCP | ✅ Created | ⬜ Pending |
| 41  | `actuator-health-indicators` | `spring-boot-starter-actuator` is already in `pom.xml` but has no `management.*` configuration and `/actuator/**` is blocked by the JWT filter; expose `/actuator/health` in `SecurityConfig.permitAll()`; add `management.endpoints.web.exposure.include=health,info` to `application.yml`; implement three custom `HealthIndicator` beans: disk space on the thumbnails directory, thumbnails directory writability, and PostgreSQL connectivity — covering the two most likely runtime failures | ✅ Created | ⬜ Pending |
| 42  | `metrics-prometheus`        | Add `micrometer-registry-prometheus` to `pom.xml` and expose `/actuator/prometheus`; instrument three application-specific metrics not covered by Spring Boot's default JVM/HTTP metrics: a `Timer` on catalog duration per folder, a `Timer` on thumbnail generation, and a `Gauge` tracking active SSE connections (catalog, sync, convert); add Prometheus and Grafana services to `docker-compose.yml` with a pre-built dashboard | ✅ Created | ⬜ Pending |
| 43  | `request-correlation-mdc`   | Add a servlet `Filter` that injects a `requestId` UUID and the authenticated `username` into SLF4J `MDC` at the start of each request and clears it on completion; `logstash-logback-encoder` is already configured in `logback-spring.xml` and will automatically include both fields in every JSON log line; also set `X-Request-ID` on the response so the Angular frontend can log the correlation ID alongside client-side errors from `global-error-handler` (#36) | ✅ Created | ⬜ Pending |
| 44  | `database-backup`           | Add `DatabaseBackupService` with `@Scheduled` that runs `pg_dump` via `ProcessBuilder`, compresses the output with GZip to a temp file, and uploads it through a new `CloudStoragePort` interface (domain) with swappable infrastructure implementations for AWS S3, Google Cloud Storage, and Azure Blob; enforce a configurable retention policy (delete backups older than N days); expose `POST /api/admin/backup` for on-demand trigger and `GET /api/admin/backups` to list stored backups with timestamps and sizes; schedule, retention, cloud provider, bucket, and prefix are all configurable in `application.yml`; Option B (Docker sidecar using `prodrigestivill/postgres-backup-local` + `rclone`) is documented as a deployment alternative for environments where backup must be decoupled from application health | ✅ Created | ⬜ Pending |
| 45  | `postgres-dockerize`        | The application currently connects to a PostgreSQL instance running on the host (`POSTGRES_HOST` defaults to `localhost`); `docker-compose.yml` already declares a `db` service and a `pgdata` named volume but is not the active deployment path; complete the transition by: (1) fixing the volume mount from `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data` to align with the PostgreSQL `PGDATA` default, (2) setting `PGDATA: /var/lib/postgresql/data` explicitly in the `db` service environment, (3) making `docker-compose up` the canonical deployment command and updating `README.md` accordingly, and (4) providing a one-time data migration script (`pg_dump` on host → `pg_restore` into the container) so existing data is not lost on first deployment | ✅ Created | ⬜ Pending |
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
| 59  | `audio-asset-support`       | Catalog audio files (.mp3, .flac, .wav, .aac, .ogg); extract ID3/Vorbis/FLAC metadata (title, artist, album, duration, bitrate, sample rate, embedded album art) via `org.jaudiotagger:jaudiotagger` (Maven); store audio-specific fields in a new `asset_audio` table mirroring `asset_exif` (Flyway migration); use embedded album art as the asset thumbnail if present, or generate a waveform PNG via FFmpeg (`ffmpeg -i input.mp3 -filter_complex "showwavespic=s=200x150" -frames:v 1 waveform.png`) as fallback; the frontend viewer switches on asset type and renders an `<audio controls>` tag for audio assets with title, artist, album, duration, and bitrate displayed alongside playback controls; once implemented, `video-from-images` (#58) can offer a "select from audio assets" music picker | ✅ Created | ⬜ Pending |
| 60  | `archive-support`           | Two related capabilities sharing the same archive-reading infrastructure (`org.apache.commons:commons-compress` for tar.gz; `java.util.zip` built-in for zip): (1) **virtual folders** — zip and tar.gz files appear as expandable nodes in the folder navigation tree using a `!` path separator convention (e.g. `/photos/album.zip!/summer/`); the catalog service extracts images to a temp location, generates thumbnails, and stores assets with the virtual path; (2) **download formats** — the bulk-download endpoint (`GET /api/assets/download`) gains a `format` query parameter (`zip` / `tar.gz`) so users can choose the archive type; the existing `ZipOutputStream` path is joined by a `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`; no Flyway migration required for either capability | ✅ Created | ⬜ Pending |
| 61  | `asset-backup`              | Backup a configurable scope of assets (folder, album, saved search result, or entire catalog) to one or more sequentially numbered archive files (e.g. `backup_photos_001.zip`, `backup_photos_002.zip`) split at a configurable volume size; format is zip or tar.gz (reuses `archive-support` #60 writing infrastructure); trigger is manual (`POST /api/backup/{id}/run`) or a per-definition cron expression scheduled dynamically via Spring `TaskScheduler` + `CronTrigger` (cancelled and rescheduled on definition update); new `/backup` frontend route mirrors the convert page structure (definitions list → configure → run → results with SSE progress and per-file logging); backend stores definitions in `backup_definitions` and run history in `backup_run_log` (timestamps, status, files written, bytes, errors); new Flyway migration | ✅ Created | ⬜ Pending |
| 63  | `raw-exif-jsonb`            | Add a `raw_exif JSONB` column to the existing `asset_exif` table (new Flyway migration); during cataloging, after extracting the 13 known fields, iterate all EXIF directories returned by Apache Commons Imaging (`JpegImageMetadata.getExif().getDirectories()` → `dir.getAllFields()`) and collect every `TiffField` into a `Map<String, String>` keyed by `field.getTagInfo().name` with value `field.getValueDescription()`; the map is serialized to JSONB using Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, String> rawExif` field in `AssetExifEntity` (no new Maven dependency — Hibernate 6 handles JSON natively via Jackson, already present); `AssetExif` domain model and `AssetExifEntityMapper` (MapStruct) gain the `rawExif` field; `ExifMetadataDto` exposes it as `Map<String, String> rawExif`; `ExifMetadata` TypeScript interface adds `rawExif: Record<string, string> \| null`; in `ExifPanelComponent`, below the existing 13 structured fields, a collapsible `MatExpansionPanel` labeled "All EXIF data" renders every key-value pair from `rawExif` as a compact two-column list; a `MatFormField` search input above the list filters entries by key name in real time using a component-level computed signal so the full raw map is never re-fetched; a single image from a modern DSLR or mirrorless camera typically carries 80–300 EXIF fields across the IFD0, ExifIFD, GPS IFD, and MakerNote directories; the search filter is essential because MakerNote alone can add 100+ manufacturer-specific fields (Nikon colour modes, Canon lens correction data, Sony face detection coordinates, etc.); existing assets cataloged before this migration have `raw_exif = NULL` and the panel section is hidden when `rawExif` is null; re-cataloging any folder populates the column for all assets in that folder; new Flyway migration | ✅ Created | ⬜ Pending |
| 66  | `video-player`              | Right-side video player pane and fullscreen support for both video and audio; the gallery's existing `mat-sidenav-container` already supports a second `MatSidenav` with `position="end"` — a new `VideoPlayerComponent` occupies this right-side drawer, opening when a video asset is played and closing when playback stops or the user dismisses it; the `<video>` element fills the full width of the drawer pane with `width: 100%; aspect-ratio: 16/9` (letterboxing for other ratios); the title and any available video metadata appear directly below the video element, followed by the progress bar and the five control buttons (`skip_previous`, `stop`, `play_arrow`/`pause`, `skip_next`, `fullscreen`); `AudioPlayerService` from `audio-player` (#65) is superseded by a unified `MediaPlayerService` (`providedIn: 'root'`) that manages both media types: it holds an internal `HTMLAudioElement` for audio and accepts a reference to the `VideoPlayerComponent`'s `HTMLVideoElement` via `registerVideoElement(el: HTMLVideoElement)` called from `VideoPlayerComponent.ngAfterViewInit()`; `isVideoAsset(asset: Asset): boolean` checks the file extension (`.mp4`, `.mov`, `.mkv`, `.avi`, `.webm`) and routes `play()` calls to the appropriate element, stopping the other if it was active; all queue signals (`currentTrack`, `queue`, `currentIndex`, `isPlaying`, `currentTime`, `duration`) and methods (`play()`, `loadFolder()`, `loadPlaylist()`, `togglePause()`, `stop()`, `prev()`, `next()`, `seek()`) remain on `MediaPlayerService` and are consumed by both `AudioPlayerComponent` (bottom bar) and `VideoPlayerComponent` (right pane); **fullscreen for video** uses `HTMLVideoElement.requestFullscreen()` — browser-native fullscreen that shows the video filling the entire screen with the browser's built-in controls overlay; **fullscreen for audio** uses a custom `MediaFullscreenOverlayComponent` (`position: fixed; inset: 0; z-index: 9999; background: #000`) rendered in `AppComponent`'s template and toggled by an `isAudioFullscreen` signal on `MediaPlayerService`; the audio fullscreen overlay shows the album art cover centered and enlarged, the track title and artist name in large type below it, the progress bar spanning the full overlay width, and the five control buttons with a close/exit-fullscreen button at top-right; **video streaming** uses a new unified `GET /api/assets/{id}/stream` endpoint that replaces the audio-only `GET /api/assets/{id}/audio` proposed in `audio-player` (#65) — the endpoint returns the file via Spring MVC `Resource`-based streaming with `Accept-Ranges: bytes` and detects the MIME type from the stored `fileName` extension; video MIME types added: `.mp4` → `video/mp4`, `.mov` → `video/quicktime`, `.mkv` → `video/x-matroska`, `.avi` → `video/x-msvideo`, `.webm` → `video/webm`; the `AudioController` from #65 is renamed `MediaController` and the endpoint path changes from `/api/assets/{id}/audio` to `/api/assets/{id}/stream`; playlist parsing (`GET /api/audio/playlist/{id}`) is unchanged; no new Flyway migration (video assets are regular catalog rows, their catalog support is from `video-file-support` #21); no new Maven dependency (Spring MVC Resource streaming already covers video); no new npm package (HTML5 `<video>` element and Fullscreen API are built-in) | ✅ Created | ⬜ Pending |
| 65  | `audio-player`              | Persistent audio player toolbar fixed at the bottom of `AppComponent` (below all routes) so music continues while the user browses the gallery; the player supports three queue modes: (1) **single** — clicking "Play" on an individual audio asset in the gallery loads it alone; (2) **folder** — an "Play all audio" action on a folder loads every audio asset in that folder ordered by `file_name`; (3) **playlist** — clicking an `.m3u`, `.m3u8`, or `.pls` asset calls `GET /api/audio/playlist/{assetId}` which parses the file and returns an ordered `List<AssetDto>`; a new singleton `AudioPlayerService` (`providedIn: 'root'`) wraps a private `HTMLAudioElement` and exposes Angular signals: `currentTrack`, `queue`, `currentIndex`, `isPlaying`, `currentTime`, `duration`; the service methods are `play(assets, startIndex?)`, `loadFolder(folderPath)`, `loadPlaylist(assetId)`, `togglePause()`, `stop()`, `prev()`, `next()`, and `seek(seconds)`; `prev()` restarts the current track if `currentTime > 3` (matching standard player convention), otherwise steps back one track; `AudioPlayerComponent` in `shared/components/audio-player/` is hidden via `@if (audioPlayer.currentTrack())` and shows only when a track is loaded; the top row of the player bar shows the album art cover image (the asset's `thumbnailUrl` — album art thumbnail from `audio-asset-support` #59 when available, generic music-note icon otherwise), the track title (from `asset_audio.title` if #59 is implemented, else `fileName`), and the artist name (from `asset_audio.artist`); the second row shows five control buttons using Material icons — `skip_previous`, `stop`, `play_arrow`/`pause` (toggling), `skip_next` — followed by the progress bar and a `currentTime / duration` counter; the progress bar is a native `<input type="range">` element whose `value` is updated on the `<audio>` element's `timeupdate` event and whose `change` event calls `audioPlayerService.seek()`; audio streaming uses a new backend endpoint `GET /api/assets/{id}/audio` that returns the file using Spring MVC `Resource`-based streaming with `Accept-Ranges: bytes` so the browser can send `Range: bytes=X-Y` requests for seeking without downloading the entire file first — the existing `GET /{id}/image` endpoint loads all bytes into memory and cannot seek; a new `AudioController` in `infrastructure/web/controller/` handles both `/audio/{id}` (stream) and `/audio/playlist/{id}` (parse); two playlist parser adapters implement a `PlaylistParserPort` in `domain/port/out/`: `M3uPlaylistParserAdapter` splits lines, skips `#EXTM3U` and `#EXTINF` annotations, and resolves each file path against the `assets` table by matching `folder_path + file_name`; `PlsPlaylistParserAdapter` reads the INI-style `[playlist]` section and extracts `FileN=` entries; the catalog service is extended to accept audio extensions (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`) and playlist extensions (`.m3u`, `.m3u8`, `.pls`) — playlist file thumbnails use a static playlist-icon placeholder; no new Flyway migration (audio metadata table is from #59; playlist files are regular assets); no new Maven dependency (Spring MVC `Resource` streaming is built-in); no new npm package (HTML5 `<audio>` element is built-in) | ✅ Created | ⬜ Pending |
| 64  | `catalog-spring-batch`      | Replace the custom `@Async` + `@Transactional` catalog loop with a Spring Batch job; add `spring-boot-starter-batch` to `pom.xml`; the new job lives entirely in a new `infrastructure/batch/` package and is composed of six classes: `CatalogJobConfig` (`@Configuration` defining the `Job`, `Step`, and `Partitioner` beans), `CatalogFolderPartitioner` (reads all root and sub-folders and emits one `ExecutionContext` partition per folder), `CatalogFileItemReader` (lists new files in one partition's folder — those not yet in the `assets` table), `CatalogAssetItemProcessor` (computes SHA-256 hash, generates thumbnail, reads EXIF — same logic as `CatalogFolderServiceImpl.createAsset()` today), `CatalogAssetItemWriter` (saves `Asset` + `AssetExif` rows, deletes assets whose files were removed, fires SSE notification via `SseNotificationRegistry`), and `CatalogItemWriteListener` (looks up the active SSE consumer from `SseNotificationRegistry` by job execution ID and forwards write events); `CatalogAssetsUseCaseImpl` becomes a thin adapter that calls `JobLauncher.run(catalogJob, jobParameters)` and returns the `JobExecution` ID; `CatalogScheduler` continues to exist but calls the use case rather than managing its own `CompletableFuture`; the `catalog_run_state` table and all related classes (`CatalogStateRepository`, `CatalogRunStateEntity`, `CatalogStateRepositoryImpl`, `JpaCatalogStateRepository`) are removed — Spring Batch's own `JobRepository` (`BATCH_JOB_EXECUTION` table) replaces their run-state role; the new Flyway migration (V27) creates the 9 Spring Batch schema tables and drops `catalog_run_state`; chunk size is configurable via `photomanager.catalog-chunk-size` (default 50) — every 50 assets are committed as one transaction, replacing the current single transaction per folder; folders are processed in parallel via a `PartitionStep` with a configurable grid size (`photomanager.catalog-partition-grid-size`, default 4, meaning up to 4 folders concurrently); the SSE `Consumer<CatalogChangeNotification>` is no longer passed as a method argument — instead `AssetController` registers it in a singleton `SseNotificationRegistry` keyed by job execution ID when the SSE connection opens, and `CatalogItemWriteListener` looks it up; the registry entry is removed when the SSE connection closes or the job completes | ✅ Created | ⬜ Pending |
| 62  | `social-media-crop`         | Canvas-based interactive crop tool for 12 social media format presets; a scissors icon button added to the viewer toolbar toggles `showCropPanel` (same pattern as `showExifPanel`); the `<img>` is replaced by a `<canvas>` in crop mode — the image is drawn on the canvas and a semi-transparent overlay renders the draggable crop box with corner handles; the crop box aspect ratio is always locked to the selected format; dragging inside the box moves it, dragging a corner handle resizes it while keeping the ratio; for profile-image formats (Instagram Profile, Facebook Profile, LinkedIn Profile, Twitter/X Profile) a `ctx.arc()` circle outline is drawn inside the crop box to preview the platform's circular display; when the format changes the crop box snaps to maximum fit centered on the image; the 12 presets are: `INSTAGRAM_POST` 1080×1080 (1:1), `INSTAGRAM_PORTRAIT` 1080×1350 (4:5), `INSTAGRAM_LANDSCAPE` 1080×566 (~1.91:1), `INSTAGRAM_STORY` 1080×1920 (9:16), `INSTAGRAM_PROFILE` 110×110 (1:1 circle), `FACEBOOK_POST` 1200×630 (~1.91:1), `FACEBOOK_PROFILE` 170×170 (1:1 circle), `LINKEDIN_POST` 1200×627 (~1.91:1), `LINKEDIN_PROFILE` 400×400 (1:1 circle), `TWITTER_POST` 1600×900 (16:9), `TWITTER_PROFILE` 400×400 (1:1 circle), `TWITTER_HEADER` 1500×500 (3:1); on confirm the frontend translates canvas-display coordinates to original image pixel coordinates using `asset.pixelWidth` / `asset.pixelHeight` (already on the `Asset` model) and sends `POST /api/assets/{id}/crop` with `{ formatKey, x, y, width, height }`; the backend uses Java2D `BufferedImage.getSubimage(x, y, w, h)` to extract the crop region and `Graphics2D.drawImage()` to scale it to the format's target dimensions, then saves the result as a new `Asset` in the same folder (non-destructive) with a freshly generated thumbnail; after the backend returns the new `AssetResponse` the frontend immediately triggers a browser download of the cropped image by creating a temporary `<a download>` element pointing to `GET /api/assets/{newId}/image`; no Flyway migration (cropped outputs are regular assets in existing tables), no new Maven dependency (Java2D is built-in), no new npm package (Canvas API is built into all browsers) | ✅ Created | ⬜ Pending |

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
| V27       | `catalog-spring-batch` — 9 Spring Batch schema tables; drop `catalog_run_state`     |

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

**Improvement 64 — two specific problems in the current catalog implementation**

The current `CatalogFolderServiceImpl.catalogFolder()` method is annotated `@Transactional`. This means every new file discovered in a folder — including its hash computation, thumbnail generation, EXIF extraction, and all database writes — runs inside a single database transaction that stays open for the entire folder. For a folder containing thousands of new images this transaction can run for many minutes. If the JVM crashes or the database connection is lost partway through, the entire transaction is rolled back and the whole folder must be reprocessed from scratch on the next run:

```
CURRENT: one transaction per folder

  folder/   file_1   file_2   file_3  ...  file_4999   COMMIT ✓
                                                │
                              crash here ───────┘
                              → all 4999 rolled back, start over next run
```

The second problem is that `doRunCatalog()` processes folders in a `for` loop — strictly sequential, one folder at a time, on a single thread. A catalog of 200 folders that each take 30 seconds processes for 100 minutes total even if the host machine has 8 idle CPU cores and fast NVMe storage.

**Improvement 64 — what Spring Batch adds and what the current code already handles**

The current implementation is more capable than it looks at first glance. Spring Batch's built-in features cover some ground already covered by custom code:

```
  Spring Batch brings                     Current code already handles
  ───────────────────────────────────     ──────────────────────────────────────
  ✓ Chunk-level transactions              ✓ Distributed lock (catalog_run_state)
  ✓ Restart from checkpoint within folder ✓ Stale lock / heartbeat detection
  ✓ Parallel folder processing            ✓ Thread.isInterrupted() per folder
  ✓ Persistent job history (9 tables)     ✓ Per-folder try/catch error isolation
  ✓ Built-in skip / retry per item        ✓ Per-file try/catch (createAsset)
  ✓ Step-level statistics                 ✓ SSE progress reporting (Consumer)
  ✓ JobRepository run-state management    ✓ Scheduled execution (TaskScheduler)
                                          ✓ Idempotency (skips catalogued files)
```

The highest-value additions are chunk transactions (data integrity on crash) and parallel folder processing (throughput). The persistent job history and step statistics are a bonus that replaces the limited information currently stored in `catalog_run_state`.

**Improvement 64 — chunk transaction fix**

Spring Batch's chunk-oriented step commits every N items regardless of folder boundaries. With a chunk size of 50, a crash at file 4,999 loses at most 49 items:

```
SPRING BATCH: commit every 50 items (chunk size = 50)

  [1..50] COMMIT  [51..100] COMMIT  ...  [4951..4999] COMMIT
                                                    │
                              crash here ───────────┘
                              → restart from item 4951, at most 49 re-processed
```

Chunk size is configurable via `photomanager.catalog-chunk-size` (default 50). Larger chunks reduce commit overhead; smaller chunks reduce the re-processing window after a crash.

**Improvement 64 — parallel folder processing via partitioned step**

The `CatalogFolderPartitioner` implements Spring Batch's `Partitioner` interface: it collects all folders (same recursion as the current `collectSubFolders()`) and emits one `ExecutionContext` partition per folder, each containing the folder path. The `PartitionStep` distributes these partitions across a configurable thread pool (`photomanager.catalog-partition-grid-size`, default 4). Four folders are processed concurrently, each running its own `CatalogFileItemReader` → `CatalogAssetItemProcessor` → `CatalogAssetItemWriter` pipeline with independent chunk commits:

```
PARTITIONED STEP (grid-size = 4)

  CatalogFolderPartitioner
  (emits one partition per folder)
         │
         ├── partition:0  folder /Photos/2023  → Reader → Processor → Writer
         ├── partition:1  folder /Photos/2024  → Reader → Processor → Writer
         ├── partition:2  folder /Photos/Raw   → Reader → Processor → Writer
         └── partition:3  folder /Photos/Work  → Reader → Processor → Writer
                     ↑
              4 threads run concurrently; next folder assigned as each finishes
```

**Improvement 64 — layer mapping**

Spring Batch types (`Job`, `Step`, `ItemReader`, `ItemProcessor`, `ItemWriter`) are framework types and belong in `infrastructure/`. The use case interface and its thin launcher adapter remain in their current layers:

```
  domain/port/in/catalog/
    CatalogAssetsUseCase          unchanged — interface with execute(Consumer) method

  application/usecase/catalog/
    CatalogAssetsUseCaseImpl      thin adapter: calls JobLauncher.run(catalogJob,
                                  jobParameters) and registers the SSE consumer in
                                  SseNotificationRegistry keyed by JobExecution ID

  infrastructure/batch/           NEW PACKAGE — all Spring Batch types live here
    CatalogJobConfig              @Configuration: defines Job, PartitionStep,
                                  CatalogFolderPartitioner bean, thread pool executor
    CatalogFolderPartitioner      Partitioner: recursively collects folders,
                                  emits one ExecutionContext per folder
    CatalogFileItemReader         ItemReader<String>: lists file paths in one
                                  partition's folder that are not yet in assets table
    CatalogAssetItemProcessor     ItemProcessor<String, Asset>: hash + thumbnail +
                                  EXIF extraction — same logic as createAsset() today
    CatalogAssetItemWriter        ItemWriter<Asset>: saves Asset + AssetExif rows,
                                  deletes removed assets, fires SSE via registry
    CatalogItemWriteListener      ItemWriteListener: resolves SSE consumer from
                                  SseNotificationRegistry by JobExecution ID

  infrastructure/service/
    CatalogScheduler              unchanged in role — now calls CatalogAssetsUseCase
                                  instead of managing CompletableFuture directly
    SseNotificationRegistry       NEW: @Component singleton Map<Long, Consumer<
                                  CatalogChangeNotification>>; registered by
                                  AssetController when SSE opens, removed on close
                                  or job completion
```

**Improvement 64 — SSE consumer registry**

The current design passes the SSE `Consumer<CatalogChangeNotification>` directly as a method argument through `CatalogAssetsUseCaseImpl` → `CatalogFolderServiceImpl`. Spring Batch `ItemWriteListener` callbacks are wired by the framework and cannot receive non-serializable objects through `JobParameters` or `ExecutionContext`. The registry pattern decouples the SSE consumer from the Spring Batch execution graph:

```
SSE CONSUMER REGISTRY — HOW IT WORKS

  ┌───────────────────────────────────────────────────────────────┐
  │  SseNotificationRegistry                                      │
  │  Map<Long jobExecutionId, Consumer<CatalogChangeNotification>>│
  └───────────────────────────────────────────────────────────────┘
         ▲ register(jobId, consumer)          ▲ lookup(jobId)
         │                                    │
  AssetController                     CatalogItemWriteListener
  on GET /api/assets/catalog:          on each chunk write:
    1. call use case → get jobId          consumer = registry.lookup(jobId)
    2. create SseEmitter                  if consumer != null:
    3. registry.register(jobId,             consumer.accept(notification)
         notification → emitter.send())
    4. on emitter complete/timeout:
         registry.remove(jobId)
```

**Improvement 64 — what the V27 migration does**

The V27 Flyway migration has two responsibilities:

1. Creates the 9 Spring Batch schema tables (using the standard Spring Batch PostgreSQL DDL script as a base): `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_JOB_EXECUTION_PARAMS`, `BATCH_JOB_EXECUTION_CONTEXT`, `BATCH_STEP_EXECUTION`, `BATCH_STEP_EXECUTION_CONTEXT`, `BATCH_JOB_SEQ`, `BATCH_JOB_EXECUTION_SEQ`, `BATCH_STEP_EXECUTION_SEQ`.

2. Drops the `catalog_run_state` table, which is fully replaced by `BATCH_JOB_EXECUTION`. The `CatalogStateRepository` port interface, `CatalogRunStateEntity`, `CatalogStateRepositoryImpl`, and `JpaCatalogStateRepository` are all deleted as part of this improvement — no data migration is needed since `catalog_run_state` holds only transient run-state (not user data).

**Improvement 64 — Maven dependency**

Adds `spring-boot-starter-batch` to `pom.xml`. This pulls in `spring-batch-core` and `spring-batch-infrastructure` transitively. Spring Boot's auto-configuration wires the `JobRepository` against the existing PostgreSQL `DataSource` automatically — no additional `DataSource` or `TransactionManager` configuration is required. Spring Batch's auto-schema creation must be disabled (`spring.batch.jdbc.initialize-schema=never`) so that Flyway owns the schema exclusively.

**Improvement 64 — ordering note**

`catalog-spring-batch` is a backend-only refactor with no API contract change — `GET /api/assets/catalog` continues to return an SSE stream of the same `CatalogChangeNotification` events. It can be delivered independently of all other improvements. If `raw-exif-jsonb` (#63) is implemented first, `CatalogAssetItemProcessor` must include the raw EXIF extraction step; if #64 lands first, #63 extends `CatalogAssetItemProcessor` with the additional extraction pass.

**Improvement 65 → Improvement 59 (soft)**

`audio-player` works without `audio-asset-support` (#59): the player toolbar is shown, playback works, and the progress bar and controls function fully. Without #59 the top row falls back to displaying `fileName` as the title with no artist and a generic music-note icon instead of album art. With #59 in place, the player reads `asset_audio.title`, `asset_audio.artist`, and the album-art thumbnail from `thumbnailUrl` to populate the top row. The catalog extension for audio file types (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`) required by both improvements should be consolidated: whichever lands first adds the extension acceptance; the other inherits it.

**Improvement 65 — why byte-range streaming is required**

The existing `GET /api/assets/{id}/image` endpoint loads the entire file into a `byte[]` and writes it to the response body in one pass. The `<audio>` element requires byte-range support (`Accept-Ranges: bytes`, `206 Partial Content`) to enable two browser behaviours: (1) seeking — the browser sends a `Range: bytes=N-M` request to jump to a position without re-downloading from the start; (2) progressive loading — the browser fetches only what it needs to start playback immediately. Without byte-range support, seeking is disabled and large audio files must fully transfer before playback begins. Spring MVC's `Resource`-based response handles range requests automatically:

```
BYTE-RANGE STREAMING — HOW IT WORKS

  Browser sends:     GET /api/assets/{id}/audio
                     Range: bytes=1048576-2097151

  Backend responds:  HTTP/1.1 206 Partial Content
                     Accept-Ranges: bytes
                     Content-Range: bytes 1048576-2097151/8388608
                     Content-Type: audio/mpeg
                     [bytes 1MB–2MB of the file]

  Implementation:
    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> streamAudio(@PathVariable Long id,
            @RequestHeader HttpHeaders headers) {
        Path filePath = resolveFilePath(id);   // look up asset, get path
        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .contentType(detectAudioMediaType(filePath))
            .body(resource);
        // Spring MVC's ResourceHttpMessageConverter handles Range header automatically
    }
```

**Improvement 65 — audio MIME type detection**

The existing `detectMediaType()` in `AssetController` inspects magic bytes and handles only image types. Audio MIME type detection for the new streaming endpoint uses the stored `fileName` extension (simpler and reliable for well-formed files):

```
  Extension          MIME type
  ─────────────────  ─────────────
  .mp3               audio/mpeg
  .flac              audio/flac
  .wav               audio/wav
  .aac               audio/aac
  .ogg               audio/ogg
  .m4a               audio/mp4
```

**Improvement 65 — player toolbar layout**

The `AudioPlayerComponent` is rendered inside `AppComponent`'s template beneath the router outlet, visible on all routes when a track is loaded:

```
AUDIO PLAYER TOOLBAR — FIXED AT BOTTOM OF SCREEN

  ┌─────────────────────────────────────────────────────────────────────┐
  │  ┌──────┐  Song Title                                               │
  │  │cover │  Artist Name                                              │
  │  └──────┘                                                           │
  │           ──────────────────────────○─────────────  2:34 / 4:12    │
  │                                                                     │
  │           [⏮ prev]  [⏹ stop]  [▶/⏸ play·pause]  [⏭ next]         │
  └─────────────────────────────────────────────────────────────────────┘

  Material icons used:
    skip_previous   stop   play_arrow / pause   skip_next

  Cover image: 48×48 px, rounded corners, asset thumbnailUrl
  Progress bar: <input type="range"> updating on <audio> timeupdate event
  prev() behaviour: if currentTime > 3s → seek(0); else → step back one track
```

**Improvement 65 — playlist file format parsing**

M3U and PLS are the two formats to support, covering Windows Media Player, VLC, and all major audio players:

```
M3U FORMAT (.m3u / .m3u8)
  #EXTM3U                              ← optional header line (skip)
  #EXTINF:180,Artist - Title           ← optional annotation (skip for now)
  C:\Music\track01.mp3                 ← absolute or relative path
  ../other/track02.flac
  /home/user/Music/track03.ogg

  Parsing:
    - Skip lines starting with '#'
    - Resolve each path: strip drive letters / root prefixes,
      match against assets table on (folder_path + file_name)
    - Order of resolved assets = order of lines in the file

PLS FORMAT (.pls)
  [playlist]
  NumberOfEntries=2
  File1=C:\Music\track01.mp3          ← FileN= lines carry the paths
  Title1=Track One
  Length1=180
  File2=/home/user/Music/track02.ogg
  Title2=Track Two
  Length2=240
  Version=2

  Parsing:
    - Find lines matching /^File\d+=(.+)/i
    - Resolve paths against assets table in ascending N order
```

Both parsers implement `PlaylistParserPort` in `domain/port/out/`; `ParsePlaylistUseCaseImpl` in `application/usecase/audio/` selects the correct parser from the playlist asset's file extension.

**Improvement 65 — catalog extension for audio and playlist files**

`CatalogFolderServiceImpl` currently skips any file not matching the image extensions set in `StorageServiceAdapter`. The extension to accept audio and playlist files requires:

1. Adding `.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`, `.m3u`, `.m3u8`, `.pls` to the accepted extensions in the catalog filter
2. For audio files: thumbnail generation must be bypassed (generating an image thumbnail from audio bytes will fail); use a pre-rendered music-note placeholder icon as the thumbnail `.bin` file instead, or defer to a `SKIP_THUMBNAIL` sentinel understood by `ThumbnailStorageServiceAdapter`
3. For playlist files: thumbnail is a pre-rendered playlist-icon placeholder
4. EXIF extraction is skipped for both audio and playlist files (no EXIF in these formats)
5. The `hash` field is still computed (SHA-256 of the file bytes) for deduplication consistency

This catalog extension is a prerequisite for `audio-asset-support` (#59) and should be consolidated with it if both are delivered together.

**Improvement 65 — no Flyway migration**

The `audio-player` improvement adds no database schema changes. Audio metadata (title, artist, album, duration) is stored in the `asset_audio` table introduced by `audio-asset-support` (#59). Playlist files are cataloged as regular rows in the `assets` table with no additional columns. The player reads existing asset data through existing API endpoints plus the two new audio endpoints.

**Improvement 65 — gallery integration points**

Three entry points load audio into the player queue:

```
  Entry point 1 — single asset
    Audio asset card in gallery → "Play" button (mat-icon-button with
    play_arrow icon) → audioPlayerService.play([asset], 0)

  Entry point 2 — folder
    Folder node in FolderNavComponent → context menu item "Play all audio"
    → audioPlayerService.loadFolder(folderPath)
    → internally calls GET /api/assets?folderPath=...&type=audio
      (or filters the response by file extension on the client)

  Entry point 3 — playlist file
    .m3u / .m3u8 / .pls asset card in gallery → single click
    → audioPlayerService.loadPlaylist(assetId)
    → calls GET /api/audio/playlist/{assetId}
    → backend parses file, returns List<AssetDto>
    → audioPlayerService.play(resolvedAssets, 0)
```

**Improvement 66 → Improvement 65 (hard)**

`video-player` replaces `AudioPlayerService` from `audio-player` (#65) with `MediaPlayerService`. The queue management, playlist parsing, and all three load modes (single, folder, playlist) are defined in #65 and inherited by #66 without duplication. `audio-player` must be delivered first, or both improvements must be implemented together as a single unit.

**Improvement 66 → Improvement 21 (soft)**

`video-player` needs video assets in the catalog to be useful. `video-file-support` (#21) extends the catalog service to accept video MIME types and generates video thumbnails via FFmpeg. Without #21 there are no cataloged video assets to play. The video streaming endpoint and `VideoPlayerComponent` can be built independently, but the feature has nothing to play until #21 is in place.

**Improvement 66 — VideoPlayerComponent placement**

The gallery template wraps everything in `mat-sidenav-container`. It already has one `MatSidenav` on the left (folder tree). A second `MatSidenav` with `position="end"` (Angular Material's built-in right-side drawer) opens alongside the thumbnail grid when a video is playing. On desktop (`mode="side"`) the grid narrows automatically to share width with the drawer; on mobile (`mode="over"`) the drawer overlays the grid. The drawer width is fixed at 400 px on desktop, full-screen-width on mobile.

```
GALLERY LAYOUT — VIDEO PANE OPEN

  mat-sidenav-container
  ┌──────────────┬──────────────────────┬───────────────────┐
  │ MatSidenav   │                      │ MatSidenav        │
  │ position=    │  mat-sidenav-content │ position="end"    │
  │ "start"      │  (gallery grid,      │                   │
  │              │   narrower when      │  <video>          │
  │  folder      │   end drawer open)   │  ──────────────── │
  │  tree        │                      │  Title            │
  │              │                      │  ─────────○────   │
  │              │                      │  2:34/4:12        │
  │              │                      │  [⏮][⏹][▶][⏭][⛶] │
  └──────────────┴──────────────────────┴───────────────────┘
                                         ↑ width: 400px desktop
                                           full-width mobile
```

**Improvement 66 — MediaPlayerService refactor of AudioPlayerService**

`AudioPlayerService` from #65 is renamed and extended. The public API for audio consumers (`AudioPlayerComponent`) is unchanged — the same signals and methods. The additions for video:

```
MediaPlayerService (replaces AudioPlayerService)
  ─────────────────────────────────────────────────────────
  Private state (unchanged from #65):
    audioEl: HTMLAudioElement           internal, created in constructor
    queue: WritableSignal<Asset[]>
    currentIndex: WritableSignal<number>
    isPlaying: WritableSignal<boolean>
    currentTime: WritableSignal<number>
    duration: WritableSignal<number>

  New private state:
    videoEl: HTMLVideoElement | null    set by VideoPlayerComponent.ngAfterViewInit()
    isVideoPlaying: WritableSignal<boolean>
    isAudioFullscreen: WritableSignal<boolean>   ← toggled by audio fullscreen button

  New methods:
    registerVideoElement(el: HTMLVideoElement): void
    isVideoAsset(asset: Asset): boolean   checks extension against VIDEO_EXTENSIONS set
    enterAudioFullscreen(): void          sets isAudioFullscreen(true)
    exitAudioFullscreen(): void           sets isAudioFullscreen(false)

  play() routing:
    if isVideoAsset(currentTrack()):
      audioEl.pause(); audioEl.src = ''
      videoEl.src = streamUrl(currentTrack())   → /api/assets/{id}/stream
      videoEl.play()
    else:
      videoEl?.pause(); videoEl.src = ''
      audioEl.src = streamUrl(currentTrack())
      audioEl.play()
```

**Improvement 66 — fullscreen behaviour for video and audio**

The two fullscreen mechanisms are intentionally different because `requestFullscreen()` on a `<video>` element delivers the native browser experience (keyboard shortcuts, browser controls overlay, PiP), while audio has no visual element to fullscreen natively:

```
VIDEO FULLSCREEN — browser native
  videoEl.requestFullscreen()
  ┌──────────────────────────────────────────────────────┐
  │████████████████████████████████████████████████████│
  │█                                                  █│
  │█              <video> fills screen                █│
  │█                                                  █│
  │█                                                  █│
  │████████████████████████████████████████████████████│
  │  browser native controls (hover to show)           │
  └──────────────────────────────────────────────────────┘
  Exiting: Esc key (browser default) or clicking [⛶] again

AUDIO FULLSCREEN — custom MediaFullscreenOverlayComponent
  position: fixed; inset: 0; z-index: 9999; background: #111;
  ┌──────────────────────────────────────────────────────┐
  │                                       [✕ exit]       │
  │                                                      │
  │              ┌─────────────────┐                     │
  │              │                 │                     │
  │              │   Album art     │                     │
  │              │  (256×256 px)   │                     │
  │              │                 │                     │
  │              └─────────────────┘                     │
  │                                                      │
  │              Song Title  (mat-h2)                    │
  │              Artist Name (mat-subtitle-1)            │
  │                                                      │
  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━○━━━━━━  2:34 / 4:12   │
  │           [⏮]  [⏹]  [▶/⏸]  [⏭]                     │
  └──────────────────────────────────────────────────────┘
  Rendered in AppComponent template:
    @if (mediaPlayer.isAudioFullscreen()) {
      <app-media-fullscreen-overlay />
    }
```

**Improvement 66 — GET /api/assets/{id}/stream replaces GET /api/assets/{id}/audio**

The unified streaming endpoint serves both audio and video using the same `Resource`-based implementation. MIME type is resolved from `fileName` extension:

```
  Extension    MIME type             Type
  ──────────   ───────────────────   ─────
  .mp3         audio/mpeg            audio
  .flac        audio/flac            audio
  .wav         audio/wav             audio
  .aac         audio/aac             audio
  .ogg         audio/ogg             audio
  .mp4         video/mp4             video
  .mov         video/quicktime       video
  .mkv         video/x-matroska      video
  .avi         video/x-msvideo       video
  .webm        video/webm            video
```

When implementing #65 alongside or after #66, `GET /api/assets/{id}/audio` is implemented as `GET /api/assets/{id}/stream` in `MediaController` instead of `AudioController`. The playlist endpoint `GET /api/audio/playlist/{id}` remains unchanged in path and behaviour.

**Improvement 66 — gallery entry points for video**

Video assets appear as thumbnail cards in the gallery with a `play_circle` overlay icon (same pattern as #21 `video-file-support` proposes for the play indicator). The three entry points mirror those from #65:

```
  Entry point 1 — single asset
    Video asset card in gallery → clicking the play_circle overlay icon
    → mediaPlayerService.play([asset], 0)
    → VideoPlayerComponent's MatSidenav opens

  Entry point 2 — folder
    Folder node → context menu item "Play all video"
    → mediaPlayerService.loadFolder(folderPath, 'video')
    → filters assets by VIDEO_EXTENSIONS on client side

  Entry point 3 — playlist file
    .m3u / .pls asset → single click
    (playlist may contain both audio and video paths;
     MediaPlayerService routes each asset to the correct element)
```
