# Implemented Features

Archive of features to the JPPhotoManagerWeb application that have been fully implemented. For pending features see `features.md`.

---

## Feature List

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
| 14  | `smart-albums`              | Extend albums with an optional `filter_json` column (same shape as saved search presets) so albums can be dynamically populated at query time; UI toggle between static and dynamic mode | ✅ Created | ✅ Implemented |
| 18  | `timeline-view`             | Third gallery view mode grouping assets by year then month with sticky date headers; `GET /api/assets/timeline-groups` returns counts per bucket; buckets load lazily via Intersection Observer | ✅ Created | ✅ Implemented |
| 21  | `video-file-support`        | Thumbnail generation via FFmpeg (`ProcessBuilder`) for `.mp4`/`.mov`/`.mkv`; catalog service accepts video MIME types; frontend shows play-overlay icon and `<video>` tag in the viewer  | ✅ Created | ✅ Implemented |
| 34  | `openapi-documentation`     | Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`; annotate all `@RestController` classes with `@Operation` and `@ApiResponse`; exposes live Swagger UI at `/swagger-ui.html` and a machine-readable spec at `/v3/api-docs`; no schema change required; `/swagger-ui.html` is exempted from JWT authentication in `SecurityConfig` | ✅ Created | ✅ Implemented |
| 39  | `api-rate-limiting`         | Add `bucket4j-spring-boot-starter` with per-IP token-bucket limits on `POST /api/auth/login` (brute-force prevention) and `GET /api/assets/catalog` (prevent accidental concurrent runs from multiple browser tabs); returns `429 Too Many Requests` with a `Retry-After` header when the bucket is exhausted; no schema change required | ✅ Created | ✅ Implemented |
| 41  | `actuator-health-indicators` | `spring-boot-starter-actuator` is already in `pom.xml` but has no `management.*` configuration and `/actuator/**` is blocked by the JWT filter; expose `/actuator/health` in `SecurityConfig.permitAll()`; add `management.endpoints.web.exposure.include=health,info` to `application.yml`; implement three custom `HealthIndicator` beans: disk space on the thumbnails directory, thumbnails directory writability, and PostgreSQL connectivity — covering the two most likely runtime failures | ✅ Created | ✅ Implemented |
| 42  | `metrics-prometheus`        | Add `micrometer-registry-prometheus` to `pom.xml` and expose `/actuator/prometheus`; instrument three application-specific metrics not covered by Spring Boot's default JVM/HTTP metrics: a `Timer` on catalog duration per folder, a `Timer` on thumbnail generation, and a `Gauge` tracking active SSE connections (catalog, sync, convert); add Prometheus and Grafana services to `docker-compose.yml` with a pre-built dashboard | ✅ Created | ✅ Implemented |
| 45  | `postgres-dockerize`        | The application previously connected to a PostgreSQL instance running on the host (`POSTGRES_HOST` defaults to `localhost`); completed the transition by: (1) fixing the volume mount from `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data` to align with the PostgreSQL `PGDATA` default, (2) setting `PGDATA: /var/lib/postgresql/data` explicitly in the `db` service environment, (3) making `docker-compose up` the canonical deployment command and updating `README.md` accordingly, and (4) providing a one-time data migration script (`pg_dump` on host → `pg_restore` into the container) so existing data is not lost on first deployment | ✅ Created | ✅ Implemented |
| 59  | `audio-asset-support`       | Catalog audio files (.mp3, .flac, .wav, .aac, .ogg); extract ID3/Vorbis/FLAC metadata (title, artist, album, duration, bitrate, sample rate, embedded album art) via `org.jaudiotagger:jaudiotagger` (Maven); store audio-specific fields in a new `asset_audio` table mirroring `asset_exif` (Flyway migration); use embedded album art as the asset thumbnail if present, or generate a waveform PNG via FFmpeg (`ffmpeg -i input.mp3 -filter_complex "showwavespic=s=200x150" -frames:v 1 waveform.png`) as fallback; the frontend viewer switches on asset type and renders an `<audio controls>` tag for audio assets with title, artist, album, duration, and bitrate displayed alongside playback controls | ✅ Created | ✅ Implemented |
| 62  | `social-media-crop`         | Canvas-based interactive crop tool for 12 social media format presets; a scissors icon button added to the viewer toolbar toggles `showCropPanel` (same pattern as `showExifPanel`); the `<img>` is replaced by a `<canvas>` in crop mode — the image is drawn on the canvas and a semi-transparent overlay renders the draggable crop box with corner handles; the crop box aspect ratio is always locked to the selected format; dragging inside the box moves it, dragging a corner handle resizes it while keeping the ratio; for profile-image formats a `ctx.arc()` circle outline is drawn inside the crop box to preview the platform's circular display; the 12 presets are: `INSTAGRAM_POST` 1080×1080 (1:1), `INSTAGRAM_PORTRAIT` 1080×1350 (4:5), `INSTAGRAM_LANDSCAPE` 1080×566 (~1.91:1), `INSTAGRAM_STORY` 1080×1920 (9:16), `INSTAGRAM_PROFILE` 110×110 (1:1 circle), `FACEBOOK_POST` 1200×630 (~1.91:1), `FACEBOOK_PROFILE` 170×170 (1:1 circle), `LINKEDIN_POST` 1200×627 (~1.91:1), `LINKEDIN_PROFILE` 400×400 (1:1 circle), `TWITTER_POST` 1600×900 (16:9), `TWITTER_PROFILE` 400×400 (1:1 circle), `TWITTER_HEADER` 1500×500 (3:1); on confirm the frontend translates canvas-display coordinates to original image pixel coordinates and sends `POST /api/assets/{id}/crop`; the backend uses Java2D to crop, scale, and save the result as a new `Asset` with a freshly generated thumbnail; the frontend immediately triggers a browser download of the cropped image | ✅ Created | ✅ Implemented |
| 64  | `catalog-spring-batch`      | Replace the custom `@Async` + `@Transactional` catalog loop with a Spring Batch job; add `spring-boot-starter-batch` to `pom.xml`; the new job lives in a new `infrastructure/batch/` package composed of `CatalogJobConfig`, `CatalogFolderPartitioner`, `CatalogFileItemReader`, `CatalogAssetItemProcessor`, `CatalogAssetItemWriter`, and `CatalogItemWriteListener`; `CatalogAssetsUseCaseImpl` becomes a thin adapter calling `JobLauncher.run(catalogJob, jobParameters)`; the `catalog_run_state` table and related classes are removed — Spring Batch's `JobRepository` replaces their run-state role; chunk size configurable via `photomanager.catalog-chunk-size` (default 50); folders processed in parallel via `PartitionStep` with `photomanager.catalog-partition-grid-size` (default 4); SSE consumers registered in a singleton `SseNotificationRegistry` keyed by job execution ID | ✅ Created | ✅ Implemented |
| 65  | `audio-player`              | Persistent audio player toolbar fixed at the bottom of `AppComponent` (below all routes); three queue modes: single, folder, playlist; `AudioPlayerService` (`providedIn: 'root'`) wraps a private `HTMLAudioElement` and exposes Angular signals (`currentTrack`, `queue`, `currentIndex`, `isPlaying`, `currentTime`, `duration`); `AudioPlayerComponent` in `shared/components/audio-player/` shows only when a track is loaded; audio streaming via `GET /api/assets/{id}/audio` with `Accept-Ranges: bytes` for seeking; `AudioController` in `infrastructure/web/controller/` handles `/audio/{id}` and `/audio/playlist/{id}`; two playlist parser adapters (`M3uPlaylistParserAdapter`, `PlsPlaylistParserAdapter`) implement `PlaylistParserPort`; catalog service extended to accept audio extensions (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`) and playlist extensions (`.m3u`, `.m3u8`, `.pls`) | ✅ Created | ✅ Implemented |
| 66  | `video-player`              | Right-side video player pane and fullscreen support for both video and audio; `VideoPlayerComponent` occupies a `MatSidenav` with `position="end"`; unified `MediaPlayerService` (`providedIn: 'root'`) replaces `AudioPlayerService` and manages both media types via an internal `HTMLAudioElement` for audio and a reference to `VideoPlayerComponent`'s `HTMLVideoElement` registered via `registerVideoElement()`; `isVideoAsset()` routes `play()` calls by file extension; video fullscreen uses `HTMLVideoElement.requestFullscreen()`; audio fullscreen uses a custom `MediaFullscreenOverlayComponent`; unified streaming endpoint `GET /api/assets/{id}/stream` replaces the audio-only `/audio` endpoint; `AudioController` renamed `MediaController` | ✅ Created | ✅ Implemented |
| 15  | `dark-mode`                 | Angular Material dark palette toggle stored in `localStorage` with `prefers-color-scheme` fallback; preference persisted per user on the backend; toolbar toggle button                 | ✅ Created | ✅ Implemented |
| 20  | `storage-analytics`         | `/analytics` route with `ngx-charts` visualizations: storage-per-folder treemap, file-format pie, photos-per-month histogram, rating distribution bar; backed by aggregate JPQL queries  | ✅ Created | ✅ Implemented |
| 23  | `progressive-web-app`       | `ng add @angular/pwa` with a cache-first strategy for thumbnails; background sync queue for offline rating/tag edits replayed on reconnect; HttpOnly cookie auth remains intact          | ✅ Created | ✅ Implemented |
| 26  | `thumbnail-http-cache`      | Add `Cache-Control: public, max-age=31536000, immutable` to the `GET /api/assets/{id}/thumbnail` response; thumbnails are content-addressed by asset ID and never mutated once written, making them safe for permanent browser and CDN caching; currently every gallery load re-fetches every thumbnail from disk with no cache headers set | ✅ Created | ✅ Implemented |
| 17  | `batch-rename`              | Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint             | ✅ Created | ✅ Implemented |
| 63  | `raw-exif-jsonb`            | Add a `raw_exif JSONB` column to the existing `asset_exif` table (new Flyway migration); during cataloging, after extracting the 13 known fields, iterate all EXIF directories returned by Apache Commons Imaging (`JpegImageMetadata.getExif().getDirectories()` → `dir.getAllFields()`) and collect every `TiffField` into a `Map<String, String>` keyed by `field.getTagInfo().name` with value `field.getValueDescription()`; the map is serialized to JSONB using Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, String> rawExif` field in `AssetExifEntity` (no new Maven dependency — Hibernate 6 handles JSON natively via Jackson, already present); `AssetExif` domain model and `AssetExifEntityMapper` (MapStruct) gain the `rawExif` field; `ExifMetadataDto` exposes it as `Map<String, String> rawExif`; `ExifMetadata` TypeScript interface adds `rawExif: Record<string, string> \| null`; in `ExifPanelComponent`, below the existing 13 structured fields, a collapsible `MatExpansionPanel` labeled "All EXIF data" renders every key-value pair from `rawExif` as a compact two-column list; a `MatFormField` search input above the list filters entries by key name in real time using a component-level computed signal so the full raw map is never re-fetched; a single image from a modern DSLR or mirrorless camera typically carries 80–300 EXIF fields across the IFD0, ExifIFD, GPS IFD, and MakerNote directories; the search filter is essential because MakerNote alone can add 100+ manufacturer-specific fields (Nikon colour modes, Canon lens correction data, Sony face detection coordinates, etc.); existing assets cataloged before this migration have `raw_exif = NULL` and the panel section is hidden when `rawExif` is null; re-cataloging any folder populates the column for all assets in that folder; new Flyway migration | ✅ Created | ✅ Implemented |
| 83  | `accent-color-customization`      | Allow users to pick one of eight predefined accent colours for the top bar; replaces the hardcoded `#2e7d32` in `app.component.scss` with `var(--accent-color)`; `ThemeService` gains `setAccentColor()` and `accentColor$`; choice is persisted to `localStorage` and applied instantly including updating the PWA `<meta name="theme-color">` tag; a new `AccentColorPickerComponent` (swatch row) is embedded in the desktop toolbar as a `MatMenu` behind a palette icon button, and inline in the mobile hamburger menu; predefined palette: Forest Green, Ocean Blue, Deep Purple, Teal, Rust Orange, Slate Grey, Crimson Red, Indigo; no backend changes, no schema migration | ✅ Created | ✅ Implemented |
| 75  | `kafka-catalog-pipeline`          | Replace the in-memory `SseNotificationRegistry` (a `ConcurrentHashMap` in a single JVM) with Kafka topics so catalog, sync, and convert progress events are broadcast across all running app instances; topics: `asset.cataloged` (writer: `CatalogAssetItemWriter`), `asset.deleted` (writer: `DeleteAssetsUseCaseImpl`), `job.catalog.progress` (writer: `CatalogItemWriteListener`), `job.sync.progress` (writer: `SyncAssetsUseCaseImpl`), `job.convert.progress` (writer: `ConvertAssetsUseCaseImpl`); SSE controllers on each instance subscribe via a Kafka consumer and fan-out progress events to connected `SseEmitter` clients; the `SseNotificationRegistry` class is deleted; eliminates the single-JVM constraint that currently prevents horizontal scaling; **prerequisite for `kafka-async-upload` (#76), `kafka-catalog-coordination` (#77), `kafka-sse-broadcast` (#80), and `mongodb-audit-log` (#73)** | ⬜ Pending | ✅ Implemented |
| 57  | `viewer-pan-drag`           | Add mouse and touch drag-to-pan to the zoomed image viewer; the existing `transform: scale(viewerZoom)` is extended to `scale(zoom) translate(panX, panY)`; `mousedown` sets a dragging flag and captures the starting cursor position, `mousemove` updates `panX`/`panY` while dragging, `mouseup` clears the flag; `touchstart`/`touchmove`/`touchend` mirror the same logic for mobile; `panX` and `panY` reset to zero whenever zoom returns to 1× or the displayed asset changes; pure frontend change — no backend endpoint, no schema change, no external dependency | ✅ Created | ✅ Implemented |
| 78  | `redis-distributed-rate-limiting` | Upgrade `RateLimitFilter` from in-memory Bucket4j (backed by `ConcurrentHashMap<String, Bucket>`) to a Redis-backed store using the `bucket4j-redis` / `bucket4j-lettuce` extension; the current implementation is per-JVM: two running instances each allow the full configured rate (10 login attempts/min per IP, 5 catalog triggers/hour per IP) independently, giving attackers proportionally more headroom; the Redis upgrade shares token-bucket counters across all instances with zero business-logic change — only `RateLimitFilter.createBucket()` switches from `Bucket.builder()...build()` to `proxyManager.builder().build(bucketKey, configSupplier)`; **fills the multi-instance production-safety gap left by `api-rate-limiting` (#39)**, which is already implemented but only protects single-instance deployments | ✅ Created | ✅ Implemented |
| 33  | `role-based-access-control` | Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value | ✅ Created | ✅ Implemented |
| 80  | `kafka-sse-broadcast`             | Fix multi-instance SSE delivery without adding Redis: today every instance shares one hardcoded Kafka consumer group (`sse-broadcaster`), so `job.sync.progress`/`job.convert.progress` (1 partition each) are only ever consumed by a single instance cluster-wide, and `job.catalog.progress` (3 partitions) events can land on an instance other than the one holding the browser's `SseEmitter` — both cases silently drop progress events (see `KafkaProgressRegistry`/`KafkaProgressListener`, introduced by #75); give each instance its own consumer group instead (`spring.kafka.consumer.group-id: sse-broadcaster-${HOSTNAME:${random.uuid}}`, and drop the hardcoded `groupId` attribute from the three `@KafkaListener` methods); a consumer group with a single member is always assigned every partition of its topic, so every instance now receives every progress message and forwards it to any locally-connected `SseEmitter`/observer; reuses the existing `KafkaProgressRegistry` local-lookup-and-skip-if-absent logic unchanged; no new infrastructure, dependency, topic, or schema change — only the group-id property and the three annotations change; requires `kafka-catalog-pipeline` (#75) | ✅ Created | ✅ Implemented |
| 72  | `mongodb-exif-store`              | Replace the `asset_exif` PostgreSQL table (including the JSONB `raw_exif` column planned in #63) with a MongoDB `asset_exif` collection; the native document model stores all 80–300 EXIF fields per image without column sprawl or JSONB workarounds; MongoDB `$near`/`$geoWithin` operators turn the already-stored GPS coordinates into geospatial "photos within radius" queries that are impossible to express efficiently in SQL; only `AssetExifRepositoryImpl` changes — the `AssetExifRepository` port interface and `AssetExif` domain model are untouched; **mutually exclusive with `raw-exif-jsonb` (#63)**: choose this feature if geospatial queries or deep EXIF document nesting are priorities; choose #63 if keeping a single PostgreSQL dependency is preferred | ✅ Created | ✅ Implemented |
| 79  | `redis-refresh-tokens`            | Move JWT refresh token storage from the `refresh_tokens` PostgreSQL table to Redis keys with native TTL (`SET refresh_token:{tokenId} {userId} EX 2592000`); PostgreSQL is being used as a TTL key-value store — a pattern Redis is purpose-built for; token lookup drops from a SQL `SELECT` (~5 ms) to a Redis `GET` (~0.1 ms); Redis auto-expires tokens after 30 days eliminating the background cleanup job implied by the `expires_at` column; revocation is an atomic `DEL`; a `RedisRefreshTokenRepositoryImpl` adapter implements the existing `RefreshTokenRepository` port; the `refresh_tokens` PostgreSQL table is dropped after a dual-write migration window; the `user_agent` column planned by `session-management` (#46) must instead be stored as a Redis hash field (`HSET refresh_token:{tokenId} userId {u} userAgent {ua}`) — implement #46 and #79 together to avoid PostgreSQL column churn. **Deploy 1 (dual-write mirroring, PostgreSQL still authoritative for reads) is implemented via the `redis-refresh-tokens` change; Deploy 2 (Redis-only reads/writes + the `refresh_tokens` table drop) remains a separate, future change**, gated on a 30-day dual-write observation window per the change's design doc | ✅ Created | ✅ Implemented |
| 73  | `mongodb-audit-log`               | Append-only MongoDB collection `asset_audit_log` recording every user action — view, download, tag, rate, delete, sync-run, convert-run, catalog-run — as a flexible document `{ userId, action, entityType, entityId, timestamp, metadata: { … } }`; each event type carries its own `metadata` shape (e.g. tag events include `tagName`; delete events include `folderId` and `permanent` flag) without schema migrations; time-range queries and per-user activity feeds are native aggregation pipeline operations; a new `AuditLogRepository` port interface (domain) with a `MongoAuditLogRepositoryImpl` adapter (infrastructure) keeps MongoDB out of the use-case layer; pairs with `kafka-catalog-pipeline` (#75) as the natural durable consumer of `asset.cataloged` and `asset.deleted` Kafka events | ✅ Created | ✅ Implemented |
| 28  | `server-side-spring-cache`  | Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required | ✅ Created | ✅ Implemented |
| 76  | `kafka-async-upload`              | Decouple the `POST /api/assets/upload` HTTP thread from SHA-256 hashing, EXIF extraction, and thumbnail generation; the controller saves the file to disk, publishes an `AssetUploadedEvent { filePath, assetId, userId }` to the `asset.uploaded` Kafka topic, and returns HTTP 202; three independent consumer groups process hash computation, EXIF extraction, and thumbnail generation in parallel; eliminates multi-second blocking for large files (RAW 40–80 MB, video) and allows each processing stage to scale independently; requires the Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75) | ✅ Created | ✅ Implemented |

---

## Dependencies (Historical)

### Soft implementation dependencies (resolved)

**Feature 11 → Feature 7**

Both share the `findByFolderWithFilters` JPQL method in `AssetRepository`. If feature 7 (search-and-filter) is implemented first, feature 11 (star-ratings) simply extends the existing JPQL with an additional `minRating` predicate. If feature 11 is implemented first, it must create the full method from scratch. The tasks.md for feature 11 documents both paths explicitly.

### Functional dependencies (resolved)

**Feature 12 → Features 7 + 11**

Saved search presets store `{ search, dateFrom, dateTo, minRating }`. Without feature 7, there are no `search`/`dateFrom`/`dateTo` filter fields to persist. Without feature 11, there is no `minRating`. A preset system that can only store an empty object provides no practical value. Feature 12 should be implemented after both 7 and 11 are in place.

### Deployment (migration) dependencies (applied)

Migrations V7–V13, V24, and V27 have been applied. The following table documents the applied sequence:

| Migration | Feature                                                     |
| --------- | ----------------------------------------------------------- |
| V7        | `exif-metadata-panel` — EXIF columns on `assets`            |
| V8        | `virtual-albums` — `albums` and `album_assets` tables       |
| V9        | `refresh-token` — `refresh_tokens` table                    |
| V10       | `soft-delete-recycle-bin` — `deleted_at` column on `assets` |
| V11       | `star-ratings` — `rating` column on `assets`                |
| V12       | `saved-search-presets` — `search_presets` table             |
| V13       | `smart-albums` — `filter_json` column on `albums`           |
| V24       | `audio-asset-support` — `asset_audio` table                 |
| V26       | `raw-exif-jsonb` — `raw_exif` JSONB column on `asset_exif`  |
| V27       | `catalog-spring-batch` — 9 Spring Batch schema tables; drop `catalog_run_state` |

### Implementation notes

**Feature 14 → Features 4, 7, 11**

`smart-albums` stores a `filter_json` that references the same fields introduced by `search-and-filter` (7) and `star-ratings` (11), and it extends the album schema introduced by `virtual-albums` (4). All three were in place first.

**Feature 42 → Feature 41**

`metrics-prometheus` adds a second Actuator endpoint (`/actuator/prometheus`). Implementing `actuator-health-indicators` (#41) first means the `SecurityConfig` and `management.*` configuration are already in place; #42 only needs to add the Prometheus registry dependency and extend the `exposure.include` property rather than setting it up from scratch.

**Feature 59 → Feature 21 (soft)**

`audio-asset-support` does not require FFmpeg for its core functionality — `jaudiotagger` handles metadata extraction and album art independently. FFmpeg is only needed for the waveform fallback thumbnail when no album art is embedded. The waveform generation can be deferred until #21 is implemented; until then, a generic music-note placeholder icon is used as the thumbnail.

**Feature 62 — no Flyway migration, no external dependencies**

`social-media-crop` adds no database schema changes: the cropped output is stored as a regular `Asset` row in the existing `assets` table via the same catalog path used by all other assets. No new Maven dependency is required — Java2D (`BufferedImage`, `Graphics2D`) is part of the Java standard library and is already in use for `asset-image-editor` (#56). No new npm package is required — the HTML5 Canvas API (`<canvas>`, `CanvasRenderingContext2D`) is available in all modern browsers without installation.

**Feature 62 — coordinate translation**

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

**Feature 62 — format change behaviour**

When the user selects a new format preset, the crop box is recalculated to the maximum area that fits within the canvas while maintaining the new aspect ratio, centered on the image. This "snap to max fit" avoids the crop box jumping to a tiny or off-screen position. The previous crop position is discarded rather than attempting to preserve it, since a radical aspect ratio change (e.g. 9:16 story → 3:1 header) makes position preservation misleading.

**Feature 62 — toolbar placement**

The scissors icon (`content_cut` Material icon) is inserted between the EXIF panel toggle and the slideshow button in the viewer toolbar. The button carries an `[class.active]="showCropPanel"` binding so it highlights when the crop panel is open. Activating crop mode immediately hides the `<img>` element and renders the `<canvas>` in its place; closing the crop panel restores the `<img>` and resets all crop state.

```
VIEWER TOOLBAR — AFTER ADDING CROP BUTTON

  ┌──────────────────────────────────────────────────────────────────────────┐
  │  [folder name]                                              [   space  ] │
  │  [zoom−] [zoom+] [★][★][★][★][★]  [ⓘ EXIF]  [✂ crop]  [▶ slide]  [⊞] │
  └──────────────────────────────────────────────────────────────────────────┘
```

**Feature 62 — UI layout in crop mode**

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
```

**Feature 62 — frontend/backend pixel work split**

The canvas is used exclusively for interactive preview and user interaction. It never sends image bytes to the backend. Instead it sends four integers (crop region in original image pixel space) and a format key. All pixel processing — crop, scale to target dimensions, thumbnail generation — is done by the backend using Java2D.

**Feature 62 — canvas interaction state machine**

The canvas tracks `cropX`, `cropY` (top-left corner), `cropW`, `cropH` (dimensions, always aspect-locked), `dragMode` (`'move' | 'resize' | null`), and `dragStart` (snapshot on mousedown).

```
mousedown hit-test
──────────────────
     ┌──────────────────────────────────────────────────────┐
     │ pointer inside corner handle (8 px radius)?          │
     │         yes ──▶  dragMode = 'resize'                 │
     │         no                                           │
     │          └─▶  pointer inside crop box?               │
     │                    yes ──▶  dragMode = 'move'        │
     │                    no  ──▶  (no interaction)         │
     └──────────────────────────────────────────────────────┘

mousemove
─────────
'move' mode:
  cropX = clamp(dragStart.cropX + dx, 0, canvas.width  - cropW)
  cropY = clamp(dragStart.cropY + dy, 0, canvas.height - cropH)

'resize' mode (aspect-locked, anchor = opposite corner):
  newW  = clamp(dragStart.cropW + dx, minSize, canvas.width)
  newH  = newW / formatAspectRatio
  cropW = newW
  cropH = newH
  (cropX, cropY adjusted to keep anchor corner fixed)
```

**Feature 64 — two specific problems in the current catalog implementation**

The previous `CatalogFolderServiceImpl.catalogFolder()` was annotated `@Transactional`, keeping a single database transaction open for the entire folder. For large folders this transaction could run for many minutes; a JVM crash rolled back the entire folder. The second problem was that `doRunCatalog()` processed folders sequentially in a `for` loop on a single thread regardless of available CPU cores.

**Feature 64 — what Spring Batch adds**

Spring Batch provides chunk-level transactions (commit every N items regardless of folder boundaries) and parallel folder processing via `PartitionStep`. The current implementation already handled distributed locks, stale lock detection, per-folder error isolation, SSE progress reporting, scheduled execution, and idempotency. The highest-value additions are chunk transactions (data integrity on crash) and parallel folder processing (throughput).

**Feature 64 — chunk transaction fix**

With a chunk size of 50, a crash at file 4,999 loses at most 49 items, compared to the entire folder under the old design. Chunk size is configurable via `photomanager.catalog-chunk-size` (default 50).

**Feature 64 — parallel folder processing via partitioned step**

`CatalogFolderPartitioner` implements Spring Batch's `Partitioner` interface: it collects all folders and emits one `ExecutionContext` partition per folder. The `PartitionStep` distributes partitions across a configurable thread pool (`photomanager.catalog-partition-grid-size`, default 4).

**Feature 64 — layer mapping**

Spring Batch types (`Job`, `Step`, `ItemReader`, `ItemProcessor`, `ItemWriter`) live in `infrastructure/batch/`. The use case interface and its thin launcher adapter remain in their current layers. The new package contains: `CatalogJobConfig`, `CatalogFolderPartitioner`, `CatalogFileItemReader`, `CatalogAssetItemProcessor`, `CatalogAssetItemWriter`, `CatalogItemWriteListener`. A new `SseNotificationRegistry` singleton in `infrastructure/service/` holds the SSE consumer map keyed by job execution ID.

**Feature 64 — SSE consumer registry**

Spring Batch `ItemWriteListener` callbacks cannot receive non-serializable objects through `JobParameters` or `ExecutionContext`. The registry pattern decouples the SSE consumer from the Spring Batch execution graph: `AssetController` registers the consumer on SSE open keyed by job execution ID; `CatalogItemWriteListener` looks it up on each chunk write; the registry entry is removed when the SSE connection closes or the job completes.

**Feature 64 — what the V27 migration does**

The V27 Flyway migration creates the 9 Spring Batch schema tables (`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_JOB_EXECUTION_PARAMS`, `BATCH_JOB_EXECUTION_CONTEXT`, `BATCH_STEP_EXECUTION`, `BATCH_STEP_EXECUTION_CONTEXT`, `BATCH_JOB_SEQ`, `BATCH_JOB_EXECUTION_SEQ`, `BATCH_STEP_EXECUTION_SEQ`) and drops the `catalog_run_state` table, which is fully replaced by `BATCH_JOB_EXECUTION`.

**Feature 64 — Maven dependency**

Adds `spring-boot-starter-batch` to `pom.xml`. Spring Boot's auto-configuration wires the `JobRepository` against the existing PostgreSQL `DataSource` automatically. Spring Batch's auto-schema creation must be disabled (`spring.batch.jdbc.initialize-schema=never`) so that Flyway owns the schema exclusively.

**Feature 65 → Feature 59 (soft)**

`audio-player` works without `audio-asset-support` (#59): the player toolbar is shown, playback works, and the progress bar and controls function fully. Without #59 the top row falls back to displaying `fileName` as the title with no artist and a generic music-note icon instead of album art. With #59 in place, the player reads `asset_audio.title`, `asset_audio.artist`, and the album-art thumbnail from `thumbnailUrl` to populate the top row.

**Feature 65 — why byte-range streaming is required**

The existing `GET /api/assets/{id}/image` endpoint loads the entire file into a `byte[]` and writes it to the response body in one pass. The `<audio>` element requires byte-range support (`Accept-Ranges: bytes`, `206 Partial Content`) to enable seeking and progressive loading. Spring MVC's `Resource`-based response handles range requests automatically.

**Feature 65 — audio MIME type detection**

Audio MIME type detection for the streaming endpoint uses the stored `fileName` extension:

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

**Feature 65 — player toolbar layout**

The `AudioPlayerComponent` is rendered inside `AppComponent`'s template beneath the router outlet, visible on all routes when a track is loaded. It shows the album art (48×48 px), track title, artist name, a range-input progress bar updating on `timeupdate`, and five control buttons: `skip_previous`, `stop`, `play_arrow`/`pause`, `skip_next`. `prev()` restarts the current track if `currentTime > 3s`, otherwise steps back one track.

**Feature 65 — playlist file format parsing**

M3U and PLS are the two supported formats. M3U: skip lines starting with `#`, resolve each path against the `assets` table on `(folder_path + file_name)`. PLS: find lines matching `/^File\d+=(.+)/i`, resolve paths in ascending N order. Both parsers implement `PlaylistParserPort` in `domain/port/out/`; `ParsePlaylistUseCaseImpl` selects the correct parser by file extension.

**Feature 65 — catalog extension for audio and playlist files**

`CatalogFolderServiceImpl` was extended to accept audio extensions (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`) and playlist extensions (`.m3u`, `.m3u8`, `.pls`). Audio files bypass thumbnail generation (music-note placeholder used). Playlist files use a playlist-icon placeholder. EXIF extraction is skipped for both. SHA-256 hash is still computed for deduplication consistency.

**Feature 65 — no Flyway migration**

`audio-player` adds no database schema changes. Audio metadata is stored in the `asset_audio` table introduced by `audio-asset-support` (#59). Playlist files are cataloged as regular rows in the `assets` table.

**Feature 65 — gallery integration points**

Three entry points load audio into the player queue: (1) audio asset card → "Play" button → `audioPlayerService.play([asset], 0)`; (2) folder node context menu "Play all audio" → `audioPlayerService.loadFolder(folderPath)`; (3) `.m3u`/`.m3u8`/`.pls` asset card single click → `audioPlayerService.loadPlaylist(assetId)` → calls `GET /api/audio/playlist/{assetId}`.

**Feature 66 → Feature 65 (hard)**

`video-player` replaces `AudioPlayerService` from `audio-player` (#65) with `MediaPlayerService`. The queue management, playlist parsing, and all three load modes are defined in #65 and inherited by #66 without duplication. `audio-player` must be delivered first, or both features must be implemented together as a single unit.

**Feature 66 → Feature 21 (soft)**

`video-player` needs video assets in the catalog to be useful. `video-file-support` (#21) extends the catalog service to accept video MIME types and generates video thumbnails via FFmpeg. Without #21 there are no cataloged video assets to play.

**Feature 66 — VideoPlayerComponent placement**

The gallery template wraps everything in `mat-sidenav-container`. A second `MatSidenav` with `position="end"` opens alongside the thumbnail grid when a video is playing. On desktop (`mode="side"`) the grid narrows automatically; on mobile (`mode="over"`) the drawer overlays the grid. The drawer width is fixed at 400 px on desktop, full-screen-width on mobile.

**Feature 66 — MediaPlayerService refactor of AudioPlayerService**

`AudioPlayerService` from #65 is renamed and extended. New private state: `videoEl: HTMLVideoElement | null` (set by `VideoPlayerComponent.ngAfterViewInit()`), `isVideoPlaying`, `isAudioFullscreen`. New methods: `registerVideoElement()`, `isVideoAsset()` (checks extension against `VIDEO_EXTENSIONS` set), `enterAudioFullscreen()`, `exitAudioFullscreen()`. `play()` routes to the correct element based on `isVideoAsset(currentTrack())`, stopping the other if active.

**Feature 66 — fullscreen behaviour for video and audio**

Video fullscreen uses `HTMLVideoElement.requestFullscreen()` — browser-native fullscreen with keyboard shortcuts and browser controls overlay. Audio fullscreen uses a custom `MediaFullscreenOverlayComponent` (`position: fixed; inset: 0; z-index: 9999; background: #111`) rendered in `AppComponent`'s template via `@if (mediaPlayer.isAudioFullscreen())`, showing album art, title, artist, progress bar, and controls.

**Feature 66 — GET /api/assets/{id}/stream replaces GET /api/assets/{id}/audio**

The unified streaming endpoint serves both audio and video. MIME type is resolved from `fileName` extension. The `AudioController` from #65 is renamed `MediaController`; the endpoint path changes from `/api/assets/{id}/audio` to `/api/assets/{id}/stream`. The playlist endpoint `GET /api/audio/playlist/{id}` is unchanged.

**Feature 66 — gallery entry points for video**

Three entry points: (1) video asset card → clicking the `play_circle` overlay icon → `mediaPlayerService.play([asset], 0)` → `VideoPlayerComponent`'s `MatSidenav` opens; (2) folder node context menu "Play all video" → `mediaPlayerService.loadFolder(folderPath, 'video')`; (3) `.m3u`/`.pls` asset → single click → `mediaPlayerService.loadPlaylist(assetId)` (playlist may contain both audio and video paths; `MediaPlayerService` routes each to the correct element).

**Feature 52 → Feature 15 (soft dependency — prerequisite implemented)**

`multi-language-i18n` and `dark-mode` (#15) both store per-user UI preferences. Implementing them together — or implementing #15 first with a `user_preferences` JSON column that #52 can extend — avoids two separate schema changes and two separate backend endpoints for preference persistence.

**Feature 23 → Feature 10** (prerequisite already implemented)

`progressive-web-app` is significantly more valuable once `mobile-responsive-layout` is in place, since PWA installs are most common on mobile.

**Features 26, 28 → Feature 81** (complementary caching layers)

`redis-thumbnail-cache` (#81) is most impactful when `thumbnail-http-cache` (#26, already implemented) is already in place for browser-level caching and `server-side-spring-cache` (#28) provides per-JVM Caffeine for hot reads. Implementing the three layers in order — browser cache (#26) → JVM Caffeine (#28) → shared Redis (#81) — delivers each tier with clear boundaries and avoids redundant work.

**Feature 26 and 27 → Feature 23**

`thumbnail-http-cache` sets browser-level `Cache-Control` headers on the thumbnail endpoint. `progressive-web-app` (#23) adds a service-worker cache-first strategy for the same endpoint. Implementing #26 first means the service worker inherits already-correct cache semantics and does not need to re-define them. Implementing them in reverse order still works but creates redundancy.

**Feature 72 ↔ Feature 63** (mutually exclusive — same problem, different technology)

`mongodb-exif-store` (#72) and `raw-exif-jsonb` (#63) both address the need for flexible EXIF storage but via incompatible approaches. #63 adds a `raw_exif JSONB` column to the existing `asset_exif` PostgreSQL table; #72 moves the entire `asset_exif` entity to a MongoDB collection. Only one can be in effect at a time. If #63 is implemented first, #72 becomes a data-migration task (move JSONB data to MongoDB documents) rather than a greenfield design choice. Teams should decide upfront based on whether geospatial querying (`$near`/`$geoWithin` on GPS coordinates) or single-database simplicity is the priority.

**Feature 63 → Feature 1 (hard)** (prerequisite already implemented)

`raw-exif-jsonb` adds a column to the `asset_exif` table introduced by `exif-metadata-panel` (#1, already implemented). The table must exist before the V26 migration can run.

**Feature 63 — no new Maven or npm dependency**

Apache Commons Imaging is already a dependency in `pom.xml` and is already used in `StorageServiceAdapter` for EXIF rotation correction. The full tag-iteration API (`getDirectories()` → `getAllFields()`) is part of the same library — no new artifact is needed. Hibernate 6 (shipped with Spring Boot 3.x) maps `jsonb` natively via `@JdbcTypeCode(SqlTypes.JSON)` using the Jackson `ObjectMapper` already on the classpath. No new npm package is needed on the frontend — the raw EXIF data arrives as a plain `Record<string, string>` and is rendered with `@for` and a component-level filter signal.

**Feature 63 — EXIF directory structure and field volume**

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

**Feature 63 — frontend panel layout**

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

**Feature 63 → Feature 31 (soft)**

The `raw_exif` JSONB column is queryable with PostgreSQL's `->` and `@>` operators and GIN-indexable. Once `full-text-search` (#31) is implemented, the search vector trigger could include selected raw EXIF fields — for example, `raw_exif->>'LensModel'` or `raw_exif->>'Model'` — giving users the ability to search by equipment not captured in the 13 fixed columns. This extension requires no schema change beyond what #31 already defines; it is a trigger function update only.

**Feature 63 — backfill strategy**

The V26 migration adds the column as `ALTER TABLE asset_exif ADD COLUMN raw_exif JSONB`. Existing rows have `raw_exif = NULL`. Backfilling requires re-cataloging the affected folders: `POST /api/assets/catalog` triggers the full extraction pipeline, which now includes the raw EXIF pass. No SQL-level backfill is possible because EXIF extraction reads the image file from disk, not from database data. The `ExifPanelComponent` handles `NULL` gracefully by hiding the "All EXIF data" section for assets not yet re-cataloged.

**Feature 64 — ordering note** (64 already implemented)

`catalog-spring-batch` (#64) is now implemented. When implementing `raw-exif-jsonb` (#63), extend `CatalogAssetItemProcessor` with the additional raw EXIF extraction pass (iterate `getDirectories()` → `getAllFields()` after the existing 13-field extraction).

**Feature 83 — no dependencies**

`accent-color-customization` is a pure frontend change. It has no hard or soft dependencies on any other pending feature. It can be delivered at any point independently.

**Feature 75 → Features 73, 76, 77, 80** (prerequisite implemented)

`kafka-catalog-pipeline` introduces the Kafka cluster, Spring Kafka bootstrap configuration, and the core topic producers. `mongodb-audit-log` (#73), `kafka-async-upload` (#76), `kafka-catalog-coordination` (#77), and `kafka-sse-broadcast` (#80) all depend on this infrastructure being in place. Of these, `kafka-sse-broadcast` (#80), `mongodb-audit-log` (#73), and `kafka-async-upload` (#76) have since been implemented.

**Feature 75 — replacing SseNotificationRegistry**

The current `SseNotificationRegistry` stores `Map<Long, Consumer<CatalogChangeNotification>>` in a single JVM. SSE connections on instance A never receive events from a catalog job running on instance B. The Kafka replacement removes this constraint: each app instance runs a `@KafkaListener` subscribed to all progress topics, and maintains its own local `Map<Long, SseEmitter>` for browsers connected to that instance. On receiving a Kafka event, the listener looks up the `runId` in its local map and writes to the `SseEmitter` if present; if the browser is connected to a different instance, that instance's listener handles delivery independently. The `SseNotificationRegistry` class is deleted entirely; the `CatalogItemWriteListener` writes to a `KafkaTemplate` instead of calling `registry.get(runId)`. Spring Kafka is included via `spring-boot-starter` auto-configuration; no new Maven dependency is required.

**Feature 75 — topic retention policy**

`job.*.progress` topics are ephemeral — events older than 1 hour have no value. Set `retention.ms=3600000` on these topics. `asset.cataloged` and `asset.deleted` events are more valuable for audit and search indexing; retain them for 7 days (`retention.ms=604800000`). Consumer groups: `sse-broadcaster` (one consumer per instance, reads all progress topics in round-robin), `audit-log-writer` (#73, reads `asset.cataloged` and `asset.deleted`), `search-indexer` (future, reads `asset.cataloged`).

**Feature 75 — no Flyway migration**

`kafka-catalog-pipeline` requires no database schema change. The Kafka cluster is provisioned as a new Docker service; no PostgreSQL table is added or modified.

**Feature 76 — 202 Accepted flow and partial-failure handling**

The `POST /api/assets/upload` response changes from `201 Created` (with the full `AssetResponse` body) to `202 Accepted` (with a `{ assetId, status: "PROCESSING" }` body). The frontend subscribes to an SSE channel via `GET /api/assets/upload/{assetId}/observe` (same Kafka-to-SSE bridging pattern used for catalog, sync, and convert progress). When all three consumers (hash, EXIF, thumbnail) signal completion, the asset's `processing_status` transitions to `COMPLETED` and the frontend reloads it. Each consumer writes its results to the `AssetRepository` or `AssetExifRepository` in a separate transaction, so partial failures — e.g. EXIF extraction fails but hash and thumbnail succeed — leave the asset in a partially-populated state that can be resolved via `POST /api/assets/{id}/reprocess` without re-running the already-completed stages.

**Feature 57 — no dependencies**

`viewer-pan-drag` is a pure frontend change with no backend, schema, or external library involvement. It can be delivered at any point independently of all other features.

**Feature 33 — enforcement boundary**

`role-based-access-control` must apply `@PreAuthorize` to every write-path use case method, not just controller endpoints, to prevent bypasses if new controllers are added later. The `VIEWER` role must be blocked at the use-case layer.

**Feature 78 — Bucket4j Redis migration steps**

The `bucket4j-redis` module (`com.bucket4j:bucket4j-redis`) provides `LettuceBasedProxyManager` as a drop-in replacement for the in-memory bucket factory. Migration: (1) add `spring-boot-starter-data-redis` and `com.bucket4j:bucket4j-redis` to `pom.xml`; (2) inject a `LettuceBasedProxyManager<String>` bean into `RateLimitFilter`; (3) replace `buckets.computeIfAbsent(bucketKey, k -> createBucket(endpointKey))` with `proxyManager.builder().build(bucketKey, () -> BucketConfiguration.builder().addLimit(createLimit(endpointKey)).build())`; (4) remove the `ConcurrentHashMap<String, Bucket> buckets` field. Zero change to rate-limit business logic, endpoint selection, or IP resolution. The `RateLimitFilterTest` unit tests continue to work because `LettuceBasedProxyManager` accepts an in-memory `LocalBucketBuilder` in test context.

**Feature 80 — redefined from `redis-sse-pubsub` to `kafka-sse-broadcast`**

This feature was originally scoped as `redis-sse-pubsub`: adding a Redis Pub/Sub channel alongside the existing Kafka topics purely for SSE fan-out, on the reasoning that Redis Pub/Sub's fire-and-forget delivery is a better latency fit for transient SSE connections than Kafka's consumer-group/offset model (see the historical trade-off note below). It was redefined as `kafka-sse-broadcast` before implementation: the actual root cause of dropped multi-instance SSE events was not Kafka's delivery model but a hardcoded shared consumer group (`sse-broadcaster`) forcing all instances to compete for the same partitions. Giving each instance its own single-member consumer group (`sse-broadcaster-${HOSTNAME:${random.uuid}}`) fixes delivery with a config-only change, reusing the existing Kafka infrastructure from `kafka-catalog-pipeline` (#75) — no new Redis dependency, topic, or schema change was needed.

**Feature 80 (historical) — Kafka vs Redis Pub/Sub trade-offs for SSE**

*Superseded — retained for context only; the implemented solution does not use Redis.* Kafka and Redis Pub/Sub serve different purposes and are not interchangeable for SSE delivery. Kafka stores events persistently with consumer-group replay semantics — a consumer can replay missed events from the committed offset. Redis Pub/Sub is fire-and-forget: subscribers receive only events published while they are connected. For SSE (where the browser connection is live and transient), Redis Pub/Sub was initially considered the better real-time delivery fit: lower latency (~0.1 ms publish-to-receive), no consumer-group coordination overhead, and no offset management. In practice, the per-instance consumer-group id approach solved the delivery problem without introducing Redis at all — see the redefinition note above.

**Feature 80 — per-instance consumer group id**

`application.yml` and `application-test.yml` both set `spring.kafka.consumer.group-id: sse-broadcaster-${HOSTNAME:${random.uuid}}`; the `@KafkaListener` annotations on `KafkaProgressListener.onCatalogProgress`, `onSyncProgress`, and `onConvertProgress` no longer hardcode `groupId = "sse-broadcaster"`, so each JVM resolves a distinct single-member consumer group. Kafka always assigns every partition of a topic to a single-member group, so every instance receives every progress message regardless of how many instances are running or how many partitions a topic has; the existing `KafkaProgressRegistry` local-lookup-and-skip-if-absent logic (unchanged) ensures only the instance holding the browser's `SseEmitter` actually forwards the event.

**Feature 80 — no schema or infrastructure change**

`kafka-sse-broadcast` adds no Flyway migration, no new Maven dependency, no new Kafka topic, and no Docker service change. It modifies only the `spring.kafka.consumer.group-id` property and the three `@KafkaListener` annotations.

**Feature 72 — technology choice rationale vs #63**

`mongodb-exif-store` (#72) and `raw-exif-jsonb` (#63) were mutually exclusive solutions to the same problem. #63 stored raw EXIF as a `JSONB` column on the `asset_exif` PostgreSQL table — simpler, single-database stack, immediately queryable with PostgreSQL operators (`->`, `@>`, GIN indexing). #72 moved EXIF to a MongoDB collection — added operational complexity (a second database) but unlocked the native document model, `$near`/`$geoWithin` geospatial operators on the GPS coordinates already stored in `AssetExif`, and rich aggregation pipeline queries across 80–300 nested EXIF fields per image. The hexagonal architecture made the swap transparent to use cases and controllers: only `AssetExifRepositoryImpl` (infrastructure) changed, implementing the same `AssetExifRepository` domain port. Since #63 was implemented first, #72 became a data-migration task (copy JSONB data to MongoDB documents) rather than a greenfield design choice.

**Feature 72 — two-deploy migration plan**

The PostgreSQL `asset_exif` table is intentionally **not** dropped in this implementation pass. `AssetExifMongoMigrationRunner` (a `CommandLineRunner`) copies existing rows from PostgreSQL to MongoDB on first startup via the still-present `JpaAssetExifRepository`, guarded by a completion marker document in a `migration_status` MongoDB collection so it runs at most once. Because Flyway migrations execute during Spring context refresh — *before* any `CommandLineRunner` runs — shipping a Flyway migration that drops the PostgreSQL `asset_exif` table in the same deploy as the migration runner would drop the table before the runner ever reads a row, destroying the data it depends on. The schema drop and removal of the now-legacy `AssetExifEntity`, `JpaAssetExifRepository`, `AssetExifEntityMapper`, and `AssetExifMongoMigrationRunner` classes are deferred to a second, later deploy, gated on an operator confirming MongoDB/PostgreSQL row-count parity after the first deploy has run against production data.

**Feature 79 — two-deploy migration plan (Deploy 1 of 2)**

`redis-refresh-tokens` follows the same two-deploy pattern used for `mongodb-exif-store` (#72). **Deploy 1** (implemented via the `redis-refresh-tokens` change) mirrors every refresh-token write into Redis alongside the existing PostgreSQL write: `RefreshTokenRepositoryImpl` writes a hash at `refresh_token:{token}` (`userId`, `tokenId`, `issuedAt`, TTL = remaining lifetime) via a new `RedisRefreshTokenStore`, plus a `refresh_tokens:user:{userId}` set and a `refresh_token:id:{tokenId}` index for future per-session revocation. PostgreSQL remains the sole read source of truth in this phase — `findByToken`, `deleteByUserId`, and `deleteById` are unchanged, and any Redis operation failure is logged as a warning without failing the login/refresh request. **Deploy 2** (Redis-only reads/writes and the Flyway migration, V32 or later, that drops the `refresh_tokens` PostgreSQL table) is deferred to a future change, gated on a 30-day dual-write observation window — one full refresh-token lifetime — to confirm all active tokens exist in Redis before cutting PostgreSQL out of the read path.

**Feature 79 — coordination with `session-management` (#46)**

`session-management` (#46, still pending) originally planned to add a `user_agent` column to the PostgreSQL `refresh_tokens` table (Flyway V22). Since #79 is implemented first, V22 is no longer needed: when #46 is implemented, `userAgent` should be stored as an additional field on the existing Redis hash (`HSET refresh_token:{token} userAgent {ua}`) rather than as a PostgreSQL column that would be immediately superseded by the Redis migration.

**Feature 73 — MongoDB document design**

The audit log collection name is `asset_audit_log`. Every action produces a document with required fields `{ userId, action, entityType, entityId, timestamp }` and an optional `metadata` sub-document whose shape varies by action:

```
AssetTagged:   metadata: { tagName, tagId }
AssetDeleted:  metadata: { folderId, permanent: false }
CatalogRun:    metadata: { foldersScanned, assetsAdded, durationMs }
SyncRun:       metadata: { sourceDir, targetDir, filesCopied, filesDeleted }
ConvertRun:    metadata: { sourceDir, targetDir, filesConverted }
```

A compound index on `{ userId: 1, timestamp: -1 }` supports per-user history queries. A TTL index on `timestamp` (365 days) automatically purges old entries. Audit log writes are handled by a dedicated Kafka consumer group (`audit-log-writer`) subscribed to `asset.cataloged`, `asset.deleted`, and `job.*.progress` topics (using the Kafka infrastructure from `kafka-catalog-pipeline`, #75); actions with no existing topic (asset view, asset download, tag add/remove, star-rating change) call `AuditLogRepository.log(...)` directly and synchronously from their use cases instead. The `AuditLogRepository` port interface in `domain/port/out/` declares a single `void log(AuditEvent event)` method; the `MongoAuditLogRepositoryImpl` adapter implements it — no framework type leaks into the domain. All audit writes (Kafka-consumer and direct-call sites alike) are wrapped in a try/catch that logs at `WARN` on failure without propagating, so a MongoDB outage never fails the primary request.

**Feature 28 — cache eviction boundary**

`@CacheEvict` must be applied to every write path that invalidates a cached result: cataloging (evicts folder and stats caches), rating (evicts stats), tagging (evicts stats), and soft-delete (evicts folder, stats, and EXIF caches). Failing to wire eviction on any write path will serve stale data silently.
