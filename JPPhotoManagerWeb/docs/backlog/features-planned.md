# Planned Features

This document records all **pending** features to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them. For completed features see `features-implemented.md`.

---

## Feature List

| # | Change name | Priority | Schema Change | Effort | Area | Summary | Brief | SDD Artifacts | Implementation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 16  | `keyboard-shortcuts`        | P2 | No | M | Frontend | A global KeyboardService with shortcuts for gallery, albums, duplicates, rating, soft-delete and search focus, plus a `?` overlay that lists every binding. | [brief](features/016-keyboard-shortcuts.md) | ✅ Created | ⬜ Pending      |
| 19  | `shareable-album-links`     | P2 | Yes | M | Full-stack | A POST endpoint that generates a signed token for an album (optional expiry, stored in `shared_albums`) and a public `/s/:token` route that shows the album without login. | [brief](features/019-shareable-album-links.md) | ✅ Created | ⬜ Pending |
| 22  | `duplicate-auto-resolve`    | P1 | No | M | Full-stack | A "Clean up automatically" dialog on the duplicates page offering keep-oldest, keep-newest, keep-highest-resolution and keep-preferred-folder policies, using the existing soft-delete. | [brief](features/022-duplicate-auto-resolve.md) | ✅ Created | ⬜ Pending      |
| 24  | `wallpaper-suggestion`      | P3 | Yes | M | Full-stack | Add an `aspect_ratio` column and an endpoint that suggests a random asset fitting the user's screen size and ratio, shown in the frontend with a download button. | [brief](features/024-wallpaper-suggestion.md) | ✅ Created | ⬜ Pending |
| 25  | `on-push-change-detection`  | P1 | No | L | Frontend | Switch all 18 components to OnPush change detection with immutable state updates, starting with ThumbnailComponent and GalleryComponent, and add markForCheck() where needed. | [brief](features/025-on-push-change-detection.md) | ✅ Created | ⬜ Pending |
| 27  | `image-etag-cache`          | P2 | No | S | Backend | Add an ETag (from the stored SHA-256 hash) and Cache-Control header to the asset image endpoint so repeat views get a 304 Not Modified instead of re-downloading the image. | [brief](features/027-image-etag-cache.md) | ✅ Created | ⬜ Pending |
| 29  | `exif-cache-service`        | P3 | No | S | Frontend | Move the per-component EXIF metadata cache into a singleton ExifCacheService so it survives navigation and avoids redundant API calls for the whole session. | [brief](features/029-exif-cache-service.md) | ✅ Created | ⬜ Pending |
| 30  | `image-rotation-viewer`     | P1 | No | S | Frontend | Apply the stored `imageRotation` value as a CSS rotate transform in thumbnails and the full-size viewer so portrait photos stop showing sideways; no backend change. | [brief](features/030-image-rotation-viewer.md) | ✅ Created | ⬜ Pending |
| 31  | `full-text-search`          | P1 | Yes | L | Backend | Extend search beyond filenames to tags, EXIF camera model and description using PostgreSQL tsvector/tsquery with a GIN index, a trigger-maintained search_vector and ts_rank ranking. | [brief](features/031-full-text-search.md) | ✅ Created | ⬜ Pending |
| 32  | `folder-watch-service`      | P2 | No | M | Backend | A Java NIO WatchService that monitors the root catalog folders for create, modify and delete events and triggers incremental catalog updates automatically. | [brief](features/032-folder-watch-service.md) | ✅ Created | ⬜ Pending |
| 35  | `thumbnail-regeneration`    | P2 | No | S | Backend | A POST endpoint (optionally scoped by folder) that deletes and regenerates thumbnail files, covering corrupted thumbnails, size changes and EXIF-rotation fixes; no schema change. | [brief](features/035-thumbnail-regeneration.md) | ✅ Created | ⬜ Pending |
| 37  | `asset-description`         | P1 | Yes | M | Full-stack | Add a `description` column to assets, a PATCH endpoint to edit it, and an editable field in the viewer's EXIF panel; the description also feeds the full-text search index. | [brief](features/037-asset-description.md) | ✅ Created | ⬜ Pending |
| 38  | `folder-stats-in-tree`      | P3 | No | S | Full-stack | Show asset count and total size as a secondary line on each folder node in the navigation tree, backed by a lightweight per-folder stats endpoint. | [brief](features/038-folder-stats-in-tree.md) | ✅ Created | ⬜ Pending |
| 44  | `database-backup`           | P0 | No | L | Backend | A scheduled DatabaseBackupService running pg_dump, gzipping and uploading via a CloudStoragePort (S3, GCS, Azure Blob) with retention, plus admin endpoints to trigger and list backups. | [brief](features/044-database-backup.md) | ✅ Created | ⬜ Pending |
| 47  | `two-factor-authentication` | P0 | Yes | L | Full-stack | TOTP-based two-factor login with any authenticator app: QR-code setup and verify endpoints, a 202 challenge at login, encrypted secret storage and 10 single-use backup codes. | [brief](features/047-two-factor-authentication.md) | ✅ Created | ⬜ Pending |
| 48  | `email-notifications`       | P2 | Yes | M | Full-stack | Send summary emails via Spring Mail when catalog, sync, convert or backup operations finish, with configurable SMTP, an email field and a notification toggle on the profile page. | [brief](features/048-email-notifications.md) | ✅ Created | ⬜ Pending |
| 49  | `auto-tagging`              | P3 | No | S | Backend | During cataloging, automatically apply tags from EXIF data (year taken and lowercase camera make) through the existing tag tables; the user can remove them like manual tags. | [brief](features/049-auto-tagging.md) | ✅ Created | ⬜ Pending |
| 50  | `image-comparison-viewer`   | P2 | No | M | Frontend | A split-screen "Compare" view for exactly two selected assets, showing both images at matched zoom with filename, size, dimensions and rating; no new backend endpoint. | [brief](features/050-image-comparison-viewer.md) | ✅ Created | ⬜ Pending |
| 51  | `folder-bookmarks`          | P3 | Yes | M | Full-stack | A pin icon on folder tree nodes bookmarks folders per user in a new folder_bookmarks table, with CRUD endpoints and a pinned section shown above the full tree. | [brief](features/051-folder-bookmarks.md) | ✅ Created | ⬜ Pending |
| 52  | `multi-language-i18n`       | P2 | No | L | Full-stack | Add English and Spanish support with Angular localize, Spring MessageSource for backend messages, a per-user locale setting and a language toggle in the top bar. | [brief](features/052-multi-language-i18n.md) | ✅ Created | ⬜ Pending |
| 54  | `notification-center`       | P2 | Yes | M | Full-stack | An in-app notification bell with history of completed background operations, a new notifications table, endpoints for unread count, paginated history and mark-all-read. | [brief](features/054-notification-center.md) | ✅ Created | ⬜ Pending |
| 55  | `webp-avif-conversion`      | P3 | Yes | M | Full-stack | Extend the convert feature from PNG-to-JPEG to JPEG/PNG to WebP and AVIF through cwebp and avifenc, with a target_format column and a Target format dropdown in the UI. | [brief](features/055-webp-avif-conversion.md) | ✅ Created | ⬜ Pending |
| 56  | `asset-image-editor`        | P2 | No | M | Full-stack | Brightness, contrast and hue sliders in the viewer with live CSS preview; saving posts the values and the backend applies them with Java2D, saving a new asset (optional replace). | [brief](features/056-asset-image-editor.md) | ✅ Created | ⬜ Pending |
| 58  | `video-from-images`         | P3 | No | L | Full-stack | A wizard to pick ordered images, per-slide duration and background music, then generate an MP4 with FFmpeg, streaming progress over SSE and auto-cataloging the result. | [brief](features/058-video-from-images.md) | ✅ Created | ⬜ Pending |
| 60  | `archive-support`           | P2 | No | L | Full-stack | Treat zip and tar.gz files as virtual folders in the navigation tree, and let the bulk-download endpoint produce either zip or tar.gz; no Flyway migration needed. | [brief](features/060-archive-support.md) | ✅ Created | ⬜ Pending |
| 61  | `asset-backup`              | P1 | Yes | L | Full-stack | Back up a chosen scope (folder, album, search or whole catalog) into numbered zip or tar.gz volumes, run manually or on a cron schedule, with a /backup page and run history. | [brief](features/061-asset-backup.md) | ✅ Created | ⬜ Pending |
| 67  | `event-auto-grouping`       | P3 | Yes | M | Full-stack | Cluster photos into events by time gaps (default 2 hours), with an /events page of cover-photo cards, rename, cover and merge/split overrides stored in a user_events table. | [brief](features/067-event-auto-grouping.md) | ✅ Created | ⬜ Pending |
| 68  | `photo-quality-scoring`     | P2 | Yes | M | Full-stack | Compute a 0-100 quality score at cataloging time from thumbnail sharpness and EXIF signals, store it, and offer a Best quality first sort and a minimum-quality filter. | [brief](features/068-photo-quality-scoring.md) | ✅ Created | ⬜ Pending |
| 69  | `iptc-xmp-metadata-editing` | P3 | Yes | M | Full-stack | Read and write IPTC fields (caption, copyright, creator, keywords, location) from the viewer panel via a PATCH endpoint, mapping keywords to the existing tags. | [brief](features/069-iptc-xmp-metadata-editing.md) | ✅ Created | ⬜ Pending |
| 70  | `dominant-color-palette`    | P3 | Yes | L | Full-stack | Extract five dominant colors per photo at cataloging time with k-means, store them as JSONB, and add a color-family filter, optional thumbnail stripe and analytics donut chart. | [brief](features/070-dominant-color-palette.md) | ✅ Created | ⬜ Pending |
| 71  | `webdav-server`             | P2 | No | L | Backend | Expose the catalog over WebDAV at /webdav/ so it can be mounted as a network drive, with HTTP Basic auth, uploads through the catalog pipeline and deletes via soft-delete. | [brief](features/071-webdav-server.md) | ✅ Created | ⬜ Pending |
| 74  | `mongodb-user-preferences`        | P3 | Yes | M | Backend | Move user_preferences and search_presets from PostgreSQL tables into a MongoDB user_configs collection, changing only the two persistence adapters, then drop the old tables. | [brief](features/074-mongodb-user-preferences.md) | ⬜ Pending | ⬜ Pending |
| 77  | `kafka-catalog-coordination`      | P3 | No | M | Backend | Stop duplicate concurrent catalog scans across app instances by replacing the @Scheduled trigger with a Kafka producer on a single-partition catalog.requests topic. | [brief](features/077-kafka-catalog-coordination.md) | ⬜ Pending | ⬜ Pending |

**Column legend:**
- **Priority** — `P0` production-safety gap (security, data-loss prevention, observability needed to run safely in production), `P1` high-impact user-facing feature or fix, `P2` scalability / meaningful non-critical enhancement, `P3` operational convenience / nice-to-have.
- **Schema Change** — `Yes` if the feature requires a new Flyway migration (new table, column, or index); `No` if it is additive-free (reuses existing schema, or is a pure frontend/config/infra change). Deliberately does not record the migration *number*, since pending features are frequently reordered and renumbered before implementation.
- **Effort** — rough size estimate: `S` (small, self-contained, ~1 file/endpoint, no new dependency), `M` (moderate, a new endpoint/component or a single new table, a few days), `L` (large, touches many files/components, a new external dependency, or a non-trivial algorithm).
- **Area** — primary layer of work: `Backend`, `Frontend`, `Full-stack` (both), or `Infra` (deployment/provisioning-only, no application code).
- **Summary** — a short hand-written plain-text summary of the brief (at most 250 characters, no pipes) for humans and for `features-next`'s display only — never spec input.
- **Brief** — link to the feature's full brief, `features/NNN-<change-name>.md` (`NNN` = zero-padded feature number): the verbatim text handed to the spec-creation step. The file never moves; whether a feature is planned or implemented is decided only by which table holds its row.

---

## Dependencies

### Hard implementation dependencies

**Feature 19 → Feature 4** (prerequisite already implemented)

`shareable-album-links` requires the `albums` table introduced by `virtual-albums`.

**Feature 22 → Feature 9** (prerequisite already implemented)

`duplicate-auto-resolve` routes deleted assets through the soft-delete path introduced by `soft-delete-recycle-bin`.

**Feature 75 → Feature 77** (prerequisite already implemented)

`kafka-catalog-pipeline` (#75) is now implemented. `kafka-catalog-coordination` (#77) is unblocked. `mongodb-audit-log` (#73), `kafka-sse-broadcast` (#80), and `kafka-async-upload` (#76), also unblocked by #75, have since been implemented — see `features-implemented.md`.

### Soft implementation dependencies (order affects cleanliness)

**Feature 16 → Feature 8** (prerequisite already implemented)

`keyboard-shortcuts` extends the viewer shortcuts already present in `slideshow-mode`. Implementing 8 first avoids re-doing viewer key handling.

### Recommended implementation order

For the pending dependent clusters:

```
19 (shareable-album-links) — prerequisite #4  already implemented
22 (duplicate-auto-resolve)— prerequisite #9  already implemented
16 (keyboard-shortcuts)    — prerequisite #8  already implemented
58 (video-from-images)     — prerequisite #21 already implemented; #59 also already implemented
```

Features 24 (wallpaper-suggestion), 27 (image-etag-cache), 29 (exif-cache-service), 30 (image-rotation-viewer), 32 (folder-watch-service), 35 (thumbnail-regeneration), 38 (folder-stats-in-tree), 50 (image-comparison-viewer), 56 (asset-image-editor), 67 (event-auto-grouping), 68 (photo-quality-scoring), 69 (iptc-xmp-metadata-editing), 70 (dominant-color-palette), 71 (webdav-server) have no hard dependencies and can be delivered in any order.

Within dependent clusters:

```
37 (asset-description) → 31 (full-text-search)
33 (role-based-access-control, already done) → 44 (database-backup), 61 (asset-backup)
39 (api-rate-limiting, already done) → 47 (two-factor-authentication)
46 (session-management, already done) → 47 (two-factor-authentication)
54 (notification-center) → 48 (email-notifications)
60 (archive-support) → 61 (asset-backup)
75 (kafka-catalog-pipeline, already done) → 77 (kafka-catalog-coordination)
```

Feature 74 (mongodb-user-preferences) has no hard dependencies and can be delivered in any order. Both #74 and #77 are `P3` in the Feature List table above — #74 is standalone and most useful now that MongoDB is already provisioned by #72/#73 (both implemented); #77 requires #75 (implemented) and is lower urgency when running a single instance.

### Deployment (migration) dependencies

Flyway migration versions must be applied in ascending order. Migrations V7–V13, V24, and V27 have already been applied. The following pending features require new migrations:

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
| V23       | `webp-avif-conversion` — `target_format` column on `convert_assets_directories_definitions` |
| V25       | `asset-backup` — `backup_definitions` and `backup_run_log` tables                   |
| V28       | `photo-quality-scoring` — `quality_score` SMALLINT column on `assets`               |
| V29       | `iptc-xmp-metadata-editing` — `caption`, `copyright` VARCHAR columns on `assets`    |
| V30       | `dominant-color-palette` — `color_palette` JSONB column on `assets`                 |
| V31       | `event-auto-grouping` — `user_events` table                                          |

Note: `revert-exif-postgres-jsonb` (#84) has been implemented and applied `V33__recreate_asset_exif.sql`, superseding the earlier `V27__drop_asset_exif.sql` — see `features-implemented.md`. This also supersedes the older reservation of "V33 for `search_presets`" mentioned under `mongodb-user-preferences` (#74) below — when #74 is eventually implemented, its `search_presets` drop migration should use the next number available at that time instead. `V34` has since been applied by `session-management` (#46, implemented) — see `features-implemented.md` — so #74 should use `V35` or later.

Note: `pixel_width` and `pixel_height` are already present on `assets`; only the derived `aspect_ratio` column is new. The backfill (`aspect_ratio = pixel_width / pixel_height`) must be included in the V15 migration to populate existing rows. Assets where either dimension is zero are left as `NULL` and excluded from wallpaper queries.

The V17 migration must run after V16 because the `search_vector` generated column combines `file_name`, `description`, and tag data; the `description` column must exist before the generated column can reference it.

### MongoDB and Kafka infrastructure provisioning

Features #74 and #77 require no Flyway migrations — they do not modify the PostgreSQL schema. However, they do require provisioning new infrastructure components alongside the existing PostgreSQL container (Redis is already fully provisioned and in use by `redis-distributed-rate-limiting` #78, `redis-refresh-tokens` #79, `redis-thumbnail-cache` #81, and `redis-search-tag-cache` #82, all now implemented — no pending feature requires further Redis provisioning):

| Infrastructure | Required by features | Notes |
| -------------- | ------------------------ | ----- |
| MongoDB 7+     | #74 | Add `mongo` service to `docker-compose.yml` (already provisioned by `mongodb-exif-store`, #72, and `mongodb-audit-log`, #73, both now implemented) |
| Apache Kafka 3.7+ (KRaft mode) | #77 | Add `kafka` service; no ZooKeeper required in KRaft mode |

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
```

Redis is already provisioned (used by #78, #79, #81, and #82, all implemented) — no `redis:` service addition is needed for the remaining pending features.

Corresponding `application.yml` additions:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/photomanager   # feature #74 (MongoDB already provisioned by #72, #73)
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}  # feature #77 (Kafka already provisioned by #75)
```

### Implementation notes

**Feature 25 → Feature 2** (prerequisite already implemented)

`on-push-change-detection` should be applied after `virtual-scrolling-gallery` is completed. The list layout introduced by #2 uses fixed-height items with immutable `*cdkVirtualFor` bindings; applying OnPush before that layout is in place risks masking change-detection bugs while the old grid and IntersectionObserver are still present.

**Feature 27 → no schema change**

The `ETag` value is derived from the SHA-256 `hash` column already present on the `Asset` entity; no Flyway migration is needed.

**Feature 30 — no dependencies**

`image-rotation-viewer` reads `imageRotation` already stored on `Asset`; it is a pure frontend change with no backend or schema involvement.

**Feature 31 → Feature 37**

`full-text-search` is more valuable when the `description` field from `asset-description` is available to index. The V17 migration (search vector) hard-depends on the V16 migration (description column) being applied first. Implementing #37 before #31 avoids rewriting the generated column definition.

**Feature 31 → Feature 37 (functional)**

A full-text search that can only index filename and tags is still useful, but the `description` field is the primary free-text input that justifies the investment in a PostgreSQL GIN index. Implementing both together delivers the full value.

**Feature 37 → Feature 31 (soft)**

`asset-description` can be delivered standalone and provides immediate value in the EXIF panel before full-text search is wired up.

**Feature 35 — ordering note**

`thumbnail-regeneration` is most useful after `image-rotation-viewer` (#30) is in place, since one motivation for re-generation is applying orientation correction to existing thumbnails. The two can be delivered independently but pair naturally.

**Features 32, 36, 38 — no dependencies**

`folder-watch-service`, `global-error-handler`, and `folder-stats-in-tree` have no hard dependencies on other pending features and can be delivered in any order.

**Feature 44 → Feature 33** (prerequisite already implemented)

`database-backup` exposes admin-only endpoints (`POST /api/admin/backup`, `GET /api/admin/backups`). Now that `role-based-access-control` (#33) is implemented, these endpoints should be restricted to the `ADMIN` role with `@PreAuthorize("hasRole('ADMIN')")`.

**Feature 44 — no schema change**

`database-backup` adds no Flyway migration; the backup metadata (listing stored files) is read directly from cloud storage at request time rather than persisted to the database.

**Feature 45 → Feature 44** (45 already implemented)

`postgres-dockerize` (#45) is now complete. The backup service (#44) can run `pg_dump` against the database container over the Docker network (`POSTGRES_HOST: db`) without additional network configuration.

**Feature 47 → Feature 39** (prerequisite already implemented)

`two-factor-authentication` introduces a TOTP verification endpoint that must be rate-limited to prevent brute-force attacks on 6-digit codes. `api-rate-limiting` (#39) is already in place.

**Feature 47 → Feature 46**

Revoking all sessions (`DELETE /api/auth/sessions`) is the recommended recovery action when a user suspects their account is compromised. Implementing `session-management` (#46) before or alongside #47 gives users the tools to respond to a potential account takeover.

**Feature 47 — external dependencies detail**

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

**Feature 48 → Feature 54**

`email-notifications` and `notification-center` are triggered by the same operation-completion events (end of catalog, sync, convert, backup SSE streams). Implementing them together avoids wiring the same trigger points twice; if delivered separately, #54 should come first so the notification infrastructure exists when #48 extends it with email dispatch.

**Feature 49 → Feature 1** (prerequisite already implemented)

`auto-tagging` reads `dateTaken` and camera make from EXIF data stored by `exif-metadata-panel` (#1, already implemented).

**Features 46, 50, 53 — no schema changes**

`session-management` (beyond the optional `user_agent` column), `image-comparison-viewer`, and `password-strength-policy` require no Flyway migrations.

**Features 50, 53 — no new backend endpoints**

`image-comparison-viewer` reuses the existing `GET /api/assets/{id}/image` endpoint. `password-strength-policy` adds validation logic to existing endpoints only.

**Feature 56 — no external dependencies**

`asset-image-editor` uses Java2D (`java.awt.image`, standard library) for all three operations: `RescaleOp` for brightness and contrast, RGB→HSB→RGB conversion for hue. No new Maven dependency and no Docker image change are required.

**Feature 58 → Feature 21** (prerequisite already implemented)

`video-from-images` has a hard dependency on `video-file-support` (#21): FFmpeg must be installed in the backend container before the video generation `ProcessBuilder` call can work. #21 is already implemented.

**Feature 58 → Feature 59 (soft)** (soft dependency already implemented)

`video-from-images` can accept a music file upload at video-creation time and does not require audio assets to exist. However, once `audio-asset-support` (#59) is in place the wizard gains a "select from audio assets" picker, making music selection significantly more convenient. Since #59 is now implemented, #58 should include the audio picker from day one.

**Feature 60 — external dependency**

`archive-support` requires `org.apache.commons:commons-compress` for tar.gz reading and writing. Zip reading and writing use `java.util.zip` from the Java standard library and add no Maven dependency. The virtual-folder and download-format capabilities are independent of each other and can be delivered separately within the same feature.

**Feature 61 → Feature 60**

`asset-backup` has a hard dependency on `archive-support` (#60): it reuses the zip and tar.gz writing infrastructure (`ZipOutputStream` and `TarArchiveOutputStream`) introduced there, and inherits the `commons-compress` Maven dependency without re-adding it. Delivering #60 first also means the volume-splitting logic can be built on top of already-tested archive streams.

**Feature 61 → Feature 33 (soft, prerequisite already implemented)**

`asset-backup` admin endpoints (`POST /api/backup/{id}/run`, `GET /api/backup/definitions`, etc.) should be restricted to the `ADMIN` role now that `role-based-access-control` (#33) is implemented, following the same pattern as `database-backup` (#44).

**Feature 61 — dynamic scheduling note**

Each `backup_definitions` row carries an optional `cron_expression` column. On application startup, `BackupSchedulerService` reads all definitions with a non-null cron expression and registers them with Spring `TaskScheduler`. On definition create/update/delete, the corresponding `ScheduledFuture` is cancelled and a new one registered. This approach requires no third-party scheduler library — Spring's built-in `ThreadPoolTaskScheduler` is sufficient.

**Feature 61 — scope implementation note**

The four backup scopes map to existing query paths: folder scope reuses `GetAssetsUseCase` filtered by `folderPath`; album scope reuses `GetAlbumAssetsUseCase`; search scope reuses `findByFolderWithFilters` with a stored `SearchPreset`; catalog scope queries all non-deleted assets. No new repository methods are required beyond what existing use cases already expose.

**Feature 62 → Feature 56 (soft)**

`social-media-crop` (#62, already implemented) and `asset-image-editor` (#56) share the same backend pattern: send transformation parameters → Java2D processes the original image → result saved as a new `Asset` in the same folder → thumbnail generated → new `AssetResponse` returned. When implementing #56, extract the "save as new asset" logic into a shared utility method (e.g. `AssetSavePort.saveProcessedAsset(...)`) that #56 and #62 can both use, eliminating the duplication already present in #62's implementation.

**Feature 67 — event boundary algorithm**

Events are computed on demand rather than persisted. `GET /api/events` sorts all non-deleted assets for the authenticated user by `dateTaken` ascending, then walks the list inserting an event boundary wherever the gap between consecutive shots exceeds the configured threshold (default 2 hours, configurable per user in `user_events` or globally in `application.yml`). Each resulting contiguous run becomes an event. The cover photo defaults to the asset with the highest `rating` in the run, falling back to the first asset chronologically. User overrides in the `user_events` table (custom name, cover, merged or split boundaries) are applied as post-processing on top of the computed runs. Assets with no `dateTaken` value are grouped into a separate "Unknown date" event at the end.

**Feature 67 — no hard dependencies**

`event-auto-grouping` has no hard dependencies on other pending features. It does benefit from `star-ratings` (#11, already implemented) for the default cover photo selection heuristic. `timeline-view` (#18, already implemented) is a natural companion — both group by time — but the two are independent views.

**Feature 68 — Laplacian variance implementation**

The sharpness estimate reads all pixels of the 200×150 thumbnail into a greyscale array (`0.299R + 0.587G + 0.114B`), applies the 3×3 discrete Laplacian kernel, and computes the variance of the resulting values. A sharp image has high variance (strong edges); a blurry image has low variance (soft gradients). The raw variance is normalised to 0–100 using empirically calibrated floor and ceiling values. The EXIF penalty terms are weighted: sharpness contributes 60%, ISO penalty 25%, and exposure-time penalty 15%. All computation uses `BufferedImage` pixel access from the Java standard library; no Maven dependency is added.

**Feature 68 — backfill for existing assets**

The V28 migration adds `quality_score SMALLINT` as `NULL`. Existing assets receive a score when their folder is next re-cataloged. The gallery sort option and filter slider gracefully exclude `NULL`-scored assets (treated as unscored, shown last when sorting by quality).

**Feature 68 → Feature 22 (soft)**

`photo-quality-scoring` pairs naturally with `duplicate-auto-resolve` (#22): once scores are populated, the "keep highest resolution" policy can be extended with a "keep highest quality" policy that picks the asset with the highest `quality_score` among duplicates rather than requiring the user to compare manually.

**Feature 69 — IPTC write safety**

`JpegIptcRewriter` from Apache Commons Imaging reads the existing IPTC APP13 segment, merges the updated fields, and writes a new JPEG to a temp file before atomically replacing the original. This avoids partial writes corrupting the file on crash. The backend validates that the target file path resolves to an asset the authenticated user owns before writing.

**Feature 69 → Feature 31 (soft)**

`iptc-xmp-metadata-editing` adds `caption` and `copyright` VARCHAR columns to `assets`. Once `full-text-search` (#31) is implemented, both columns should be included in the `search_vector` generated column so captions are full-text-searchable. The V17 migration for #31 should reference these columns if #69 is implemented first; otherwise the V17 trigger is updated to include them when #69 lands.

**Feature 70 — k-means implementation detail**

K-means initialises centroids using k-means++ seeding (choose first centroid randomly, each subsequent centroid chosen with probability proportional to squared distance from the nearest existing centroid) to reduce sensitivity to random initialisation. The algorithm runs for a fixed 10 iterations regardless of convergence — this is sufficient for k=5 over thumbnail-sized inputs and avoids unbounded runtime during cataloging. The clustering operates in RGB space; the family snapping step converts each final centroid to HSL for comparison against the 12 family reference hues.

**Feature 70 — no schema dependency ordering constraint**

The V30 migration adding `color_palette JSONB` to `assets` is independent of V28 (`quality_score`) and V29 (`caption`, `copyright`). All three columns are additive and can be applied in any order. Existing assets have `color_palette = NULL`; the gallery color swatch picker hides families with zero matches, so the picker is automatically empty until assets are re-cataloged.

**Feature 71 — WebDAV compliance scope**

The implementation targets WebDAV Level 1 (RFC 4918) only: `OPTIONS`, `PROPFIND` (depth 0 and 1), `GET`, `HEAD`, `PUT`, `DELETE`, `MKCOL`. Level 2 locking (`LOCK`/`UNLOCK`) is not implemented — Windows and macOS WebDAV clients work without locking when the server advertises `DAV: 1` only. `COPY` and `MOVE` are deferred; clients that need them can use the existing `MoveAssetsUseCase` API endpoints directly.

**Feature 71 — no schema change**

The WebDAV virtual filesystem is a read/write projection over the existing `assets` and folder data. No new tables or columns are required. `PUT` writes the file via `StoragePort` and queues a catalog job; `DELETE` sets `deleted_at` via `SoftDeleteAssetUseCase`; `MKCOL` creates a directory on disk (the folder appears in the tree after the next catalog run).

**Feature 71 — authentication note**

JWT cookies cannot be forwarded by OS-level WebDAV mounts. The WebDAV endpoint at `/webdav/**` is exempted from the JWT cookie filter in `SecurityConfig` and instead uses `HttpBasicAuthenticationFilter` backed by the existing `UserDetailsService`. Credentials are sent over HTTPS only; `application.yml` must enforce `server.ssl.enabled=true` or a reverse proxy must terminate TLS before the WebDAV mount is used in production.

**Feature 74 — data migration strategy**

`user_preferences` and `search_presets` are small tables (one row per user for preferences, a few rows per user for presets). The one-time data migration exports all rows to MongoDB documents then drops the PostgreSQL tables. Because both tables are user-specific and low-volume, the migration can run online: (1) export to MongoDB, (2) switch the Spring beans from JPA adapters to MongoDB adapters, (3) drop the PostgreSQL tables in subsequent Flyway migrations (V32 for `user_preferences`, V33 for `search_presets`). The `UserPreferenceRepositoryImpl` and `SearchPresetRepositoryImpl` adapters are the only classes that change.

**Feature 77 — single-partition leader election detail**

Kafka's consumer group protocol guarantees that a single-partition topic has exactly one active consumer per group at any time. `catalog.requests` is configured with `partitions=1`. All app instances join the consumer group `catalog-coordinator`. Kafka's group coordinator assigns the single partition to one member; the others idle. When the active member receives a `CatalogJobRequested` event, it checks `CatalogScheduler.isRunning()` before launching — this guards against duplicate triggers if the scheduling interval fires before the previous job completes. The `@Scheduled` timer in `CatalogScheduler` becomes a `kafkaTemplate.send("catalog.requests", ...)` call rather than a direct `JobLauncher.run()` invocation.

