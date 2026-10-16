[← Back to README](../README.md)

# Backend

## Technologies

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring Web (REST + SSE) | managed by Spring Boot |
| Spring Data JPA | managed by Spring Boot |
| Spring Data MongoDB | managed by Spring Boot |
| Spring Data Redis | managed by Spring Boot |
| Spring Batch | managed by Spring Boot |
| Spring Kafka | managed by Spring Boot |
| Spring Security | managed by Spring Boot |
| Spring Validation | managed by Spring Boot |
| Spring Actuator + Micrometer (Prometheus registry) | managed by Spring Boot |
| Hibernate (PostgreSQL dialect) | managed by Spring Boot |
| PostgreSQL JDBC | managed by Spring Boot |
| Flyway + Flyway PostgreSQL extension | managed by Spring Boot |
| Apache Kafka (KRaft) | 3.9.0 (via Docker) |
| Redis | 7 (via Docker) |
| MongoDB | 8 (via Docker) |
| JJWT (JWT signing/verification) | 0.12.6 |
| Bucket4j + Bucket4j Redis (rate limiting) | 8.10.1 |
| Passay (password complexity validation) | 1.6.6 |
| springdoc-openapi (Swagger UI) | 2.8.9 |
| Lombok | 1.18.46 |
| MapStruct | 1.6.3 |
| Apache Commons Imaging | 1.0-alpha3 |
| jaudiotagger (audio metadata) | 3.0.1 |
| logstash-logback-encoder (JSON logging) | 8.0 |
| GitHub API client | 1.321 |
| JUnit 5 + Mockito + AssertJ | managed by Spring Boot |
| Testcontainers (PostgreSQL) | managed by Spring Boot |
| Spring Kafka Test (`@EmbeddedKafka`) | managed by Spring Boot |

## Internal architecture

The backend follows the same Hexagonal (Ports and Adapters) layout described in [Backend Hexagonal Architecture](architecture.md#backend-hexagonal-architecture):

```
application/
  dto/                   → Application DTOs: progress messages (CatalogProgressMessage,
                           SyncProgressMessage, ConvertProgressMessage), domain events, results…
  exception/              → Domain/application exceptions (AssetNotFoundException,
                           UserNotFoundException, PasswordPolicyException, …), handled centrally by
                           `infrastructure/web/exception/GlobalExceptionHandler`
  service/                → Framework-free application services shared across use cases
                           (PasswordValidationService, backed by Passay)
  usecase/                → One implementation class per port/in interface, grouped by subdomain:
                           album, analytics, asset, audit, auth, catalog, convert, folder, home,
                           preference, recycle, search, sync, tag, user
domain/
  model/                 → Pure domain POJOs (Asset, Folder, User, AuditEvent, …)
  enums/                 → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum
  port/in/               → Use-case interfaces, grouped the same way as usecase/
  port/out/              → Repository and service port interfaces
infrastructure/
  web/
    controller/          → REST controllers (AssetController, AlbumController, AuthController, …)
    dto/                  → HTTP request/response DTOs
    mapper/               → MapStruct HTTP DTO ↔ domain mappers
    filter/               → RateLimitFilter (Bucket4j, backed by Redis),
                            RequestCorrelationFilter (requestId/username MDC + X-Request-ID header)
    exception/            → GlobalExceptionHandler
  persistence/
    entity/               → @Entity JPA classes
    jpa/                  → Spring Data JPA repositories
    adapter/              → Repository port implementations (XxxRepositoryImpl)
    mapper/                → MapStruct entity ↔ domain mappers
    mongo/                 → MongoAuditLogRepository (`asset_audit_log` collection)
    document/              → AuditLogDocument (Mongo document)
    redis/                 → RedisRefreshTokenStore (refresh-token mirror)
  service/                → Service adapters: StorageServiceAdapter, ThumbnailStorageServiceAdapter,
                           JwtTokenServiceAdapter, RefreshTokenServiceAdapter, JwtAuthenticationFilter, CatalogScheduler,
                           AssetSearchCacheServiceAdapter (Redis query-cache eviction),
                           KafkaProgressRegistry (runId → SseEmitter + CompletableFuture map)
  kafka/                  → KafkaProgressListener (SSE bridge), AuditLogKafkaListener,
                           AssetSearchCacheInvalidationListener, and the Batch pipeline's
                           hash/thumbnail/EXIF item processors
  batch/                  → Spring Batch job config and item readers/writers/listeners (catalog pipeline)
  health/                 → Custom Actuator health indicators (database, thumbnail storage, geocoding)
  config/                 → AppConfig, SecurityConfig, MongoConfig, KafkaTopicConfig, …
```

**Dependency flow:** `infrastructure/web` → `application/usecase` → `domain` ← `infrastructure/persistence` | `infrastructure/service`

Controllers are thin: they delegate immediately to use-case interfaces and never touch repositories or service adapters directly.

## Key services

| Service / Use Case | Description |
|---|---|
| `CatalogAssetsUseCase` | Registers a `CompletableFuture<Void>` in `KafkaProgressRegistry`, then launches a Spring Batch job. The Batch item writer publishes `CatalogProgressMessage` events to `job.catalog.progress`; `KafkaProgressListener` forwards each event to the waiting `SseEmitter` and completes the future on `done=true`. |
| `SyncAssetsUseCase` | `@Async void`; publishes `SyncProgressMessage` status events to `job.sync.progress` while syncing, then a final `done=true` message with results when complete. |
| `ConvertAssetsUseCase` | `@Async void`; same pattern — publishes `ConvertProgressMessage` events to `job.convert.progress`. |
| `FindDuplicatedAssetsUseCase` | Groups assets by hash and filters out stale catalog entries. |
| `MoveAssetsUseCase` | Copies or moves files on disk and updates the corresponding DB record. |
| `KafkaProgressRegistry` | Thread-safe `ConcurrentHashMap` keyed by `runId`; holds the `SseEmitter` registered by the controller and the `CompletableFuture<Void>` registered by the catalog use case. Bridges Kafka consumer callbacks back to waiting HTTP connections. |
| `KafkaProgressListener` | `@KafkaListener` on `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress` (consumer group `sse-broadcaster`). Routes each message to the `SseEmitter` for its `runId`; calls `registry.complete(runId)` on `done=true`. |
| `StorageServiceAdapter` | File I/O, thumbnail generation, EXIF rotation reading (Apache Commons Imaging), SHA-256 hashing. |
| `ThumbnailStorageServiceAdapter` | Stores and retrieves thumbnails as `{assetId}.bin` files under the configured thumbnails directory, fronted by a Redis L2 cache with a 24-hour TTL. |
| `JwtTokenServiceAdapter` / `RefreshTokenServiceAdapter` | Issue and validate the JWT access token and the longer-lived refresh token; refresh tokens are dual-written to PostgreSQL and mirrored into Redis (`RedisRefreshTokenStore`). |
| `RateLimitFilter` | Servlet filter backed by Bucket4j + Redis; throttles requests per client IP (or the trusted `X-Forwarded-For` value behind a reverse proxy). |
| `RequestCorrelationFilter` | Servlet filter registered at `Ordered.HIGHEST_PRECEDENCE` (ahead of the entire Spring Security chain); generates a UUID `requestId` per request, puts it and `username` (`"anonymous"` until `JwtAuthenticationFilter` corrects it to the real principal) into SLF4J MDC, echoes the `requestId` back as the `X-Request-ID` response header, and clears MDC in a `finally` block. The `logstash-logback-encoder` file appender includes both MDC fields in every JSON log line for the request, so `requestId` and `username` are directly correlatable end to end. See [Authentication → Request correlation & MDC logging](security.md#request-correlation--mdc-logging). |
| `AssetSearchCacheServiceAdapter` | Evicts the Redis-backed `assets`/`tags` query caches per folder via cursor-based `SCAN`/`DEL`, triggered by `AssetSearchCacheInvalidationListener` (Kafka) and directly by the tag-mutation use cases. |
| `AuditLogKafkaListener` | Consumes `asset.cataloged`/`asset.deleted`/`job.*.progress` events and appends them to the MongoDB `asset_audit_log` collection; tag/rating/view/download actions are logged directly by their use cases. |

## Persistence

- **Primary database:** PostgreSQL 18 — assets, folders, users, albums, refresh tokens, search presets, and sync/convert configuration.
  - **Schema migrations:** Flyway, scripts in `src/main/resources/db/migration/`
  - **ORM:** Spring Data JPA with the Hibernate PostgreSQL dialect
  - **Connection:** configured via `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (defaults: `localhost`, `5432`, `photomanager`, `postgres`, `postgres`)
  - `asset_exif` lives here too (`asset_id BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE`, `raw_exif JSONB` for the full EXIF tag map) — it briefly moved to MongoDB under the `mongodb-exif-store` change and was reverted back to PostgreSQL by `revert-exif-postgres-jsonb` once `gps-map-view` (the feature that had motivated a `2dsphere` geospatial index on it) was cancelled; see `openspec/changes/archive/` for both change folders. MongoDB `asset_exif` data from that period wasn't migrated back — assets catalogued during that window get their EXIF data repopulated the next time their folder is re-catalogued.
- **MongoDB:** append-only store for the `asset_audit_log` collection (user-action history), populated by `AuditLogKafkaListener` and direct `AuditLogRepository.log(...)` calls. Configured via `MONGO_URI`. A compound `userId`/`timestamp` index and a 365-day TTL index are ensured at startup by `MongoIndexInitializer`.
- **Redis:** thumbnail L2 cache, query-result caches (`assets`, `tags`, `home-stats`, `sub-folders`, `asset-exif`), the refresh-token mirror, and the Bucket4j rate-limit counters. Every Redis call fails open — a Redis outage degrades to always querying PostgreSQL/disk rather than failing the request. Configured via `REDIS_HOST`, `REDIS_PORT`. See [Caching](#caching) below for the query-cache/thumbnail-cache mechanics.
  - **Refresh tokens** are dual-written: `RefreshTokenRepositoryImpl` mirrors every `save()` into `RedisRefreshTokenStore` (`refresh_token:{token}` hash, `refresh_tokens:user:{userId}` set, `refresh_token:id:{tokenId}` index) alongside the JPA write. This is Phase 1 of the `redis-refresh-tokens` migration — **PostgreSQL remains authoritative for reads** (`findByToken`/`deleteByUserId`/`deleteById`); a follow-up change will cut reads over to Redis-only and drop the PostgreSQL table.

Note: an earlier revision of this doc (and some older comments/manifests) mentioned a `catalog_run_state` table used as a distributed lock for the catalog job. That table was dropped by `V16__spring_batch_schema.sql` — Spring Batch's own `JobRepository` handles run coordination now. See [Catalog Process](catalog-process.md#catalog-process) for the current mechanism; if you see `catalog_run_state` referenced anywhere else, it's stale.

## Caching

`AppConfig.cacheManager()` is a `RedisCacheManager` (Lettuce, sharing the
`RedisConnectionFactory` used by the refresh-token store and thumbnail
cache) — moved off a per-JVM `CaffeineCacheManager` so a write on one
backend instance correctly invalidates the copy served by another.

Five named caches, each with its own TTL and a `Jackson2JsonRedisSerializer`
built for its exact declared type — **deliberately not** one shared
`GenericJackson2JsonRedisSerializer` with polymorphic `@class` hints (see
`AppConfig.typedConfig`'s Javadoc): enabling default typing mutates the
shared `ObjectMapper` in place, and Jackson writes the type hint
differently for a top-level JSON object vs. a top-level JSON array — this
combination silently broke the `tags` cache (a bare `List<Tag>`) during
development, since it deserialized into the wrong shape and effectively
never hit.

| Cache | TTL | Backs |
|---|---|---|
| `home-stats` | 10 min | Dashboard statistics |
| `sub-folders` | 5 min | Folder tree |
| `asset-exif` | 30 min | EXIF metadata lookups |
| `assets` | 5 min | `GetAssetsUseCaseImpl.execute(AssetFilter)`, keyed `{folderId}:{sha256 of the remaining filter fields}` via `AssetSearchCacheKeyGenerator` |
| `tags` | 5 min | `ListTagsUseCaseImpl.execute(String query)`, fixed key `all`, only when `query` is null/blank |

Keys are prefixed `<cacheName>:` (single colon) so `assets` entries can be
pattern-matched per folder. A `LoggingCacheErrorHandler` catches and logs
(`WARN`) any Redis error during a cache get/put/evict — the same fail-open
convention as everywhere else Redis is used in this app.

**Invalidation:** the `assets` cache is invalidated per folder two ways —
an `@KafkaListener` (`AssetSearchCacheInvalidationListener`, consumer group
`asset-search-cache-invalidator`) on `asset.cataloged`/`asset.deleted`, and
synchronously from the tag-mutation use cases (`AddTagToAssetUseCaseImpl`,
`RemoveTagFromAssetUseCaseImpl`, `BulkAddTagUseCaseImpl`,
`BulkRemoveTagUseCaseImpl` — a tag mutation has no Kafka event of its own).
Both paths share the same cursor-based `SCAN`/`DEL` (never `KEYS`) eviction
logic via `AssetSearchCachePort`/`AssetSearchCacheServiceAdapter`; the same
four tag-mutating use cases also declaratively evict `tags:all`.

**Thumbnails:** a separate Redis L2 cache (not the `RedisCacheManager`
above — a dedicated `thumbnailRedisTemplate` bean) fronts the on-disk
thumbnail store. `loadThumbnail` checks `asset:thumbnail:{assetId}` before
reading disk; `saveThumbnail`/a disk-read repopulate write it with a
24-hour TTL (`photomanager.thumbnail-cache.ttl-seconds`); `deleteThumbnail`
evicts it. Requires the Redis deployment to run with the `allkeys-lru`
eviction policy (see [Running with Docker Compose](docker-compose.md) — the
`redis` service is already configured this way) so cache growth stays
bounded, and can be disabled entirely via
`photomanager.thumbnail-cache.enabled=false`.

Full development conventions for adding to or changing any of the above
live in the `redis-caching-conventions` skill.

## Observability

Auto-instrumented metrics (JVM, HTTP request rate/latency, CPU, Spring
Batch job duration, process memory/thread counts via
`micrometer-jvm-extras`) are scraped from `/actuator/prometheus` — see
[Monitoring](docker-compose.md#monitoring-grafana--prometheus) for the
Grafana/Prometheus setup.

**Custom application metrics** are registered via an injected
`MeterRegistry`, named `photomanager_<name>` with a Prometheus-appropriate
unit suffix (`_total` for counters, `_seconds` for timers) and a
`.description(...)` call — follow this pattern for any new one:

| Metric | Type | Registered in | What it measures |
|---|---|---|---|
| `photomanager_catalog_assets_total` | Counter | `CatalogAssetItemWriter` | Total assets cataloged |
| `photomanager_thumbnail_generation_seconds` | Timer | `StorageServiceAdapter` | Thumbnail generation latency |
| `photomanager_active_sse_connections` | Gauge | `AssetController` | Active SSE connections |

All three have panels in the custom `JP Photo Manager` Grafana dashboard
(`grafana/provisioning/dashboards/photomanager.json`) — a new custom metric
with no matching panel is a gap the `docs-sync` skill checks for: every `photomanager_*` metric registered in code (`Counter.builder("photomanager_`, `Timer.builder(...)`, `Gauge.builder(...)`) has a panel whose `expr` queries it, and every panel queries a metric that still exists. Re-derive the list from the code rather than trusting the table above. Flag a metric with no panel (collected and scraped, but nobody can see it) and a panel for a metric that no longer exists (a dead panel); the fix is a panel in `photomanager.json` following the existing panels' structure, or removing the dead one — a config edit, so call it out apart from prose fixes.

**Health indicators:** `ThumbnailStorageHealthIndicator` and
`GeocodingHealthIndicator` extend the default `/actuator/health` set with
app-specific checks. Full health details require the `ADMIN` role
(`management.endpoint.health.show-details: when-authorized`); an
unauthenticated caller still gets a bare UP/DOWN, which is what the
Kubernetes readiness/liveness probes in `k8s/backend.yaml` rely on.

## REST API

All endpoints below except the three under **Auth** marked *Public* require the `jwt` cookie; endpoints under **Admin** additionally require the `ADMIN` role.

**Auth**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticate; sets `jwt` + `refreshToken` HttpOnly cookies |
| `POST` | `/api/auth/refresh` | Public | Rotate the JWT using the refresh-token cookie |
| `POST` | `/api/auth/logout` | Public | Clears both cookies server-side |
| `GET` | `/api/auth/me` | Required | Current authenticated user |
| `GET` | `/api/auth/sessions` | Required | List the caller's active sessions (device hint parsed from `User-Agent`, `lastUsedAt`, `current` flag) |
| `DELETE` | `/api/auth/sessions/{id}` | Required | Revoke one of the caller's own sessions; `404` if the id doesn't belong to the caller |
| `DELETE` | `/api/auth/sessions` | Required | Revoke every other active session for the caller ("sign out everywhere else") |

**Assets**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/assets` | Paginated asset list for a folder (`folderPath`, `page`, `sort`, `search`, `dateFrom`, `dateTo`, `minRating`, `tags`) |
| `GET` | `/api/assets/timeline` | Assets grouped by date (timeline view) |
| `GET` | `/api/assets/{id}/thumbnail` | 200×150 JPEG thumbnail |
| `GET` | `/api/assets/{id}/image` | Full-size original image |
| `GET` | `/api/assets/{id}/exif` | EXIF metadata for an asset |
| `GET` | `/api/assets/catalog` | SSE stream — run catalog and stream progress |
| `GET` | `/api/assets/catalog/observe` | SSE stream — observe an already-running catalog run |
| `POST` | `/api/assets/move` | Move or copy assets to a destination folder |
| `POST` | `/api/assets/rename` | Rename assets |
| `POST` | `/api/assets/download` | Download assets as a ZIP archive (up to `max-download-assets`) |
| `PATCH` | `/api/assets/{id}/rating` | Rate an asset (0–5; 0 clears the rating) |
| `DELETE` | `/api/assets` | Remove assets from catalog (optionally delete files) |
| `GET` | `/api/assets/duplicates` | Grouped duplicate assets |
| `POST` | `/api/assets/upload` | Upload a file into a folder |
| `GET` | `/api/assets/upload/{assetId}/observe` | SSE stream — observe post-upload processing |
| `POST` | `/api/assets/{id}/reprocess` | Regenerate an asset's thumbnail/hash/EXIF |
| `POST` | `/api/assets/{id}/crop` | Crop and save an asset |
| `POST` | `/api/assets/{id}/tags` | Add a tag to a single asset |
| `DELETE` | `/api/assets/{id}/tags` | Remove a tag from a single asset |
| `POST` | `/api/assets/tags/bulk` | Add a tag to multiple assets at once |
| `DELETE` | `/api/assets/tags/bulk` | Remove a tag from multiple assets at once |

**Tags, Albums, Search Presets, Recycle Bin**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/tags` | Tag suggestions matching a prefix (`q`) |
| `GET` | `/api/albums` | List all albums |
| `POST` | `/api/albums` | Create an album |
| `GET` | `/api/albums/{id}` | Paginated album assets |
| `PUT` | `/api/albums/{id}` | Rename/update an album |
| `DELETE` | `/api/albums/{id}` | Delete an album |
| `POST` | `/api/albums/{id}/assets` | Add assets to an album |
| `DELETE` | `/api/albums/{id}/assets` | Remove assets from an album |
| `GET` | `/api/search-presets` | List saved search presets |
| `POST` | `/api/search-presets` | Save the current filters as a preset |
| `DELETE` | `/api/search-presets/{id}` | Delete a preset |
| `GET` | `/api/recycle-bin` | Paginated soft-deleted assets |
| `POST` | `/api/recycle-bin/restore` | Restore assets from the recycle bin |
| `DELETE` | `/api/recycle-bin` | Purge specific assets (or all, with an empty body) permanently |

**Folders, Sync, Convert**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/folders` | Catalogued folders, optionally filtered by `parentPath` |
| `GET` | `/api/folders/drives` | Available filesystem roots |
| `GET` | `/api/folders/initial` | Configured initial folder |
| `GET` | `/api/folders/recent-paths` | Recently used destination paths |
| `GET` | `/api/sync/configuration` | Load sync directory pairs |
| `PUT` | `/api/sync/configuration` | Save sync directory pairs |
| `GET` | `/api/sync/run` | SSE stream — run sync and stream status |
| `GET` | `/api/convert/configuration` | Load convert directory pairs |
| `PUT` | `/api/convert/configuration` | Save convert directory pairs |
| `GET` | `/api/convert/run` | SSE stream — run conversion and stream status |

**Media, Home/Analytics, Preferences, Audit Log**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/assets/{id}/stream` | Stream an audio asset |
| `GET` | `/api/audio/playlist/{id}` | Resolve a playlist asset (`.m3u`/`.pls`) to its track list |
| `GET` | `/api/home/stats` | Dashboard statistics (total assets, folders, size, average rating) |
| `GET` | `/api/analytics` | Storage-per-folder, format distribution, photos-per-month, rating distribution |
| `GET` | `/api/preferences` | Current user's UI preference (theme mode) |
| `PUT` | `/api/preferences` | Save the current user's UI preference |
| `GET` | `/api/audit-log` | Paginated audit trail (`userId`, `entityId`, `from`, `to`, `page`, `size`); non-admins are scoped to their own `userId` |

**Admin** (requires the `ADMIN` role)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/users` | List all users |
| `POST` | `/api/admin/users` | Create a user |
| `PATCH` | `/api/admin/users/{id}/password` | Change a user's password |
| `DELETE` | `/api/admin/users/{id}` | Delete a user |

`POST /api/admin/users` and `PATCH /api/admin/users/{id}/password` both enforce a password
complexity policy (`application/service/PasswordValidationService`, backed by
[Passay](https://www.passay.org/)): minimum 12 characters, at least one uppercase letter, one
digit, and one special character. A password failing any rule returns `400 Bad Request` with a
`violations` array — one entry per failing rule — via a dedicated `PasswordPolicyException` handler
in `GlobalExceptionHandler`. The frontend mirrors these same four rules client-side in
`PasswordStrengthComponent` (see `docs/frontend.md`) for live feedback without an API round-trip.

The full interactive contract is served by Swagger UI (`/swagger-ui.html`) — see [Running the backend](#running-the-backend) below.

## Configuration

All settings live in `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP server port |
| `photomanager.initial-directory` | `~/Pictures` | Starting folder shown in the UI |
| `photomanager.root-catalog-folders` | `~/Pictures` | Semicolon-separated folder roots to catalog |
| `photomanager.catalog-batch-size` | `1000` | Files processed per catalog pass (also the heartbeat interval) |
| `photomanager.catalog-cooldown-minutes` | `2` | Minimum minutes between catalog runs |
| `photomanager.catalog-timeout` | `60` | Minutes without a heartbeat before a catalog run is considered stale |
| `photomanager.thumbnails-directory` | `~/.photomanager/thumbnails` | Thumbnail storage path — overridden by `THUMBNAILS_DIR` env var |
| `photomanager.thumbnail-cache.enabled` | `true` | Enables the Redis L2 thumbnail cache — overridden by `THUMBNAIL_CACHE_ENABLED` |
| `photomanager.thumbnail-cache.ttl-seconds` | `86400` | TTL for cached thumbnail bytes — overridden by `THUMBNAIL_CACHE_TTL_SECONDS` |
| `photomanager.jwt-secret` | *(empty — must be set)* | HS256 signing secret (≥ 32 bytes); see [Authentication](security.md#authentication) |
| `photomanager.jwt-expiry-hours` | `24` | JWT access-token validity in hours |
| `photomanager.refresh-token-expiry-days` | `30` | Refresh-token validity in days |
| `photomanager.cors-allowed-origins` | `http://localhost:4200` | Allowed CORS origin(s), e.g. the Angular dev server |
| `photomanager.max-download-assets` | `500` | Max assets allowed in a single ZIP download request |
| `photomanager.trusted-proxy-ips` | *(empty)* | Comma-separated reverse-proxy IPs whose `X-Forwarded-For` header is trusted for rate limiting |
| `photomanager.recycle-bin-retention-days` | `30` | Days a soft-deleted asset stays in the recycle bin |
| `photomanager.geocoding-base-url` | `https://nominatim.openstreetmap.org` | Reverse-geocoding endpoint polled by the `geocoding` Actuator health indicator |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `photomanager` | Database name |
| `POSTGRES_USERNAME` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `MONGO_URI` | `mongodb://localhost:27017/photomanager` | MongoDB connection URI (`asset_audit_log` collection) |
| `CATALOG_DIR` | *(unset — falls back to `~/Pictures`)* | Overrides `initial-directory` and `root-catalog-folders` |
| `THUMBNAILS_DIR` | *(unset — falls back to `~/.photomanager/thumbnails`)* | Overrides `thumbnails-directory` |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka bootstrap server address; set to `kafka:9092` in Docker Compose |

## Running the backend

**Prerequisites:** Java 21, Maven 3.9+, and Docker to run PostgreSQL 18, MongoDB, Redis, and Apache Kafka (all four are required — the app also uses MongoDB for the audit log and Redis for caching/rate limiting)

Start local PostgreSQL, MongoDB, Redis, and Kafka instances if you don't have them:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:18

docker run -d --name photomanager-mongo -p 27017:27017 mongo:8

docker run -d --name photomanager-redis -p 6379:6379 redis:7-alpine

docker run -d --name photomanager-kafka \
  -p 9092:9092 -p 9094:9094 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,EXTERNAL://:9094,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,EXTERNAL://localhost:9094 \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:3.9.0
```

You'll also need `photomanager.jwt-secret` set — see [Setup (local development)](security.md#setup-local-development) under Authentication.

```bash
cd JPPhotoManagerWeb/backend

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

The interactive API documentation (Swagger UI) is available at `http://localhost:8080/swagger-ui.html`. The raw OpenAPI JSON spec is at `http://localhost:8080/v3/api-docs`. Both endpoints are accessible without authentication.

## Running backend tests

```bash
cd JPPhotoManagerWeb/backend

# All tests
mvn test

# Single test class
mvn test -Dtest=CatalogAssetsServiceImplTest

# Single test method
mvn test -Dtest=CatalogAssetsServiceImplTest#methodName
```

Tests use the `test` Spring profile (`src/test/resources/application-test.yml`). Unit tests (`@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`) need no database or broker. Integration tests (`@SpringBootTest` with `@EmbeddedKafka`) use Testcontainers for PostgreSQL and Spring's embedded Kafka broker — Docker must be running for the PostgreSQL container.

> **Linux tip:** If integration tests are skipped with a Testcontainers "no valid configuration" error, your user may not have permission to reach the Docker socket. Add yourself to the `docker` group and apply it immediately:
> ```bash
> sudo usermod -aG docker $USER
> newgrp docker
> ```
> The `newgrp` command activates the new group in your current shell without requiring a full logout.


---

## CI/CD

Two GitHub Actions workflows are defined in `.github/workflows/`:

| Workflow | File | Trigger |
|---|---|---|
| Web Test | `web-test.yml` | Every push and pull request |
| Web Release | `web-release.yml` | Tags matching `web-v*` |

Each workflow has separate jobs for the backend (Java 21 + Maven) and frontend (Node 22 + npm). The release workflow additionally creates a GitHub Release with the JAR and a zipped frontend dist as artifacts.

---

## Logging

Application logs are written to two outputs simultaneously:

- **File:** `~/.photomanager/logs/photomanager.log` — structured **JSON** format (one JSON object per line, using `logstash-logback-encoder`). Each entry includes `@timestamp`, `level`, `logger_name`, `thread_name`, `message`, and any MDC fields or exception details.
- **Console:** human-readable plain-text format (`yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message`).

### Log rotation

Logs rotate **daily**. Rotated files are compressed and stored alongside the active log file as `photomanager.log.yyyy-MM-dd.gz`. Files older than **30 days** are deleted automatically.

### Configuration

Logging is configured entirely via `src/main/resources/logback-spring.xml`. The `logging.*` properties in `application.yml` have no effect while `logback-spring.xml` is present — all tuning must happen in that file.

## Code review checks

What the `code-reviewer` skill checks in this project, on top of its generic frame. This doc holds the backend subsections and the ones that apply to both sides (§14, §15, and the shared parts of §18, §19, §21 and §24); the frontend subsections are in `docs/frontend.md`. §15 (known pitfalls) and §14 apply to every review. The section numbers (§N) are the ones the review reports cite, so they stay stable: each lives in exactly one of `docs/backend.md` and `docs/frontend.md`.

### 1. Architecture & Layering (both sub-projects)

#### 1.1 Backend dependency flow

The backend uses **hexagonal (ports and adapters)** architecture. Allowed imports:

```
infrastructure/web/      → may import application/usecase/, domain/
application/usecase/     → may import domain/ only
domain/                  → must NOT import application/, infrastructure/
infrastructure/persistence/
infrastructure/service/  → may import domain/ only
```

🔴 Flag any class in `domain/` that imports from `application/`, `infrastructure/`,
or any Spring / JPA annotation.

🔴 Flag any controller that contains business logic instead of delegating
immediately to a use-case interface from `domain/port/in/`.

🔴 Flag any JPA `@Entity` class placed in `domain/` — entities belong in
`infrastructure/persistence/entity/`; domain has pure POJOs in `domain/model/`.

🟡 Flag any use-case implementation that injects a Spring Data JPA interface
directly — it must go through a `domain/port/out/` repository interface.

#### 1.2 Backend port / adapter split

The project has three port/adapter pairs; all must follow the naming rules:

| Role | Interface location | Naming | Adapter location | Naming |
|------|--------------------|--------|-----------------|--------|
| Use case (driving) | `domain/port/in/<pkg>/` | `FooUseCase` | `application/usecase/<pkg>/` | `FooUseCaseImpl` |
| Service port (driven) | `domain/port/out/` | `FooPort` | `infrastructure/service/` | `FooServiceAdapter` |
| Repository port (driven) | `domain/port/out/` | `FooRepository` | `infrastructure/persistence/adapter/` | `FooRepositoryImpl` |

🔴 Flag any adapter class that is injected directly instead of its port interface.

🔴 Flag any use-case implementation that injects another use-case implementation
directly — use-cases must be composed via port interfaces only.

🟡 Flag any port interface that lives outside `domain/port/in/` or `domain/port/out/`.

🟡 Flag a port/adapter pair whose adapter does nothing but delegate to
*another* port for the same capability (e.g. `FooAdapter.doThing()` just
calls `barPort.doThing()`) — that's not a real abstraction, it's a
pass-through. The test for whether it should exist is whether the adapter
contributes any logic of its own — **not** whether something currently calls
it. Having production callers doesn't save a pure-delegation wrapper: delete
the port and adapter, and repoint every caller to depend on the port/adapter
that actually implements the behavior instead. Only keep the pair if the
adapter does real, non-trivial work beyond forwarding the call. This is a
cross-cutting antipattern, not backend-specific — the same test applies to
any delegate-only wrapper in either sub-project (see §10.2 for the frontend
form) — see the `HashCalculatorPort` / `AssetHashCalculatorAdapter` and
`AudioPlayerService` entries in §15.

### 2. Backend: Spring Proxy Pitfalls

This is the most commonly misunderstood area in the codebase.

#### 2.1 `@Transactional` / `@Async` self-invocation

Spring applies `@Transactional` and `@Async` via a proxy. Calling a method on
`this` bypasses the proxy — the annotation has **no effect**.

🔴 Flag any method annotated `@Transactional` or `@Async` that is called from
another method in the **same class** (i.e., via `this.foo()` or just `foo()`).

The fix is always to extract the callee into a separate `@Service` bean and
inject it.

**Wrong:**

```java
@Service
public class FooServiceImpl implements FooService {
    @Async
    public CompletableFuture<Void> doWork() {
        processItem(); // self-invocation — @Transactional on processItem is ignored
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    protected void processItem() { ... } // never fires
}
```

**Right:**

```java
@Service
public class FooServiceImpl implements FooService {
    private final BarService barService; // separate bean

    @Async
    public CompletableFuture<Void> doWork() {
        barService.processItem(); // proxy intercepts → @Transactional fires
        return CompletableFuture.completedFuture(null);
    }
}
```

#### 2.2 Detached entities from missing transactions

When `@Transactional` is missing (often due to self-invocation), each
repository call runs in its own micro-transaction. Entities returned from one
call are **detached** before the next call starts. Persisting a new entity
that references a detached entity may cause silent failures or
`DetachedObjectException`.

🔴 Flag patterns where repository calls are made in sequence without a
wrapping transaction and entities from one call are passed to another.

---

### 3. Backend: Annotations & Boilerplate

#### 3.1 Lombok

🟡 Flag any manually written getter, setter, constructor, or `toString` that
Lombok could generate:

| Anti-pattern                               | Replacement                |
| ------------------------------------------ | -------------------------- |
| Hand-written getters/setters on entity/DTO | `@Data`                    |
| Explicit no-args constructor on entity     | `@NoArgsConstructor`       |
| Explicit all-args constructor on service   | `@RequiredArgsConstructor` |
| `private static final Logger log = ...`    | `@Slf4j`                   |

#### 3.2 Dependency injection

🔴 Flag `@Autowired` on a field — use constructor injection via
`@RequiredArgsConstructor` instead.

🟡 Flag `@Autowired` on a constructor that Lombok's `@RequiredArgsConstructor`
could generate.

#### 3.3 Logging

🔴 Flag any `System.out.println` or `System.err.println`.

🟡 Flag log statements that don't include enough context (entity ID, file
path, etc.) to diagnose a production failure.

🟡 Flag `e.printStackTrace()` — use `log.error("...", e)` instead.

#### 3.4 OpenAPI controller annotations

Every `@RestController` in this project **must** carry OpenAPI annotations.
`springdoc-openapi-starter-webmvc-ui` is on the classpath and Swagger UI is
served at `/swagger-ui.html`.

🟡 Flag any `@RestController` class that is missing a class-level
`@Tag(name = "...", description = "...")` annotation.

🟡 Flag any `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping`
/ `@DeleteMapping` method that is missing `@Operation(summary = "...")`.

🟡 Flag any endpoint method that is missing `@ApiResponses` with at least one
`@ApiResponse` per reachable HTTP status code.

Expected import block for every annotated controller:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

---

### 4. Backend: Transactions

🔴 Flag write operations (save, delete, update) in a facade or service method
that lacks `@Transactional`.

🟡 Flag read-only facade methods that lack `@Transactional(readOnly = true)`.

🟡 Flag `open-in-view` set to `true` in `application.yml` — it must stay
`false`; load lazy associations within transactions.

🔴 Flag any lazy association (`@ManyToOne(fetch = LAZY)`, `@OneToMany`) that
is accessed outside a transaction — this causes `LazyInitializationException`.

🟡 Flag `@OneToMany` or `@ManyToOne` without `fetch = FetchType.LAZY`.

---

### 5. Backend: JPA Entities

🔴 Flag any entity that is missing `@NoArgsConstructor` (Hibernate requires
a no-args constructor).

🟡 Flag bean-validation annotations (`@NotBlank`, `@NotEmpty`, `@Size`) on an
entity field — they belong on API DTOs, not entities.

🟡 Flag any `CascadeType.ALL` or `CascadeType.REMOVE` on a `@ManyToOne`
relationship — cascading deletes up to a parent is almost never correct.

🟡 Flag any `@Column(nullable = false)` that doesn't have a corresponding
`NOT NULL` in the Flyway migration, or vice versa.

---

### 6. Backend: Database Migrations

🔴 Flag any modification to an existing Flyway migration file (those in
`src/main/resources/db/migration/`). Applied migrations must never change;
create a new `V{n+1}__*.sql` instead.

🔴 Flag a new `@Column` added to an entity that has no corresponding column
in the migrations (schema will drift if `ddl-auto` is `none` or `validate`).

🟡 Flag migration filenames that don't follow `V{n}__{Description}.sql`
(two underscores, sequential version number).

### 7. Backend: Async & SSE

🔴 Flag an `@Async` method that does **not** return `CompletableFuture<T>` —
the async execution machinery needs the future to report completion or errors.

🟡 Flag a long-running operation that blocks a web thread instead of using
`@Async` + `SseEmitter`.

🟡 Flag any `SseEmitter` usage that doesn't call `emitter.complete()` or
`emitter.completeWithError()` at the end of the operation — the connection
will hang open.

🔴 Flag any `SecurityFilterChain` that does **not** include
`.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as the first
authorisation rule. Without it, Tomcat's async dispatch thread (used by
`SseEmitter`) re-runs the Spring Security filter chain without a
`SecurityContext` and throws `AuthorizationDeniedException` — "response is
already committed". This must come before all other `requestMatchers` rules.

---

### 8. Backend: Naming Conventions

| Element              | Expected                                                          | Example                              |
| -------------------- | ----------------------------------------------------------------- | ------------------------------------ |
| Class                | PascalCase                                                        | `CatalogAssetsUseCaseImpl`           |
| Method               | camelCase                                                         | `execute()`, `findByFolder()`        |
| Field / variable     | camelCase                                                         | `folderRepository`, `storagePort`    |
| Constant             | UPPER_SNAKE_CASE                                                  | `THUMBNAIL_MAX_WIDTH`                |
| Enum value           | UPPER_SNAKE_CASE                                                  | `ASSET_CREATED`                      |
| Test class           | `{Class}Test` or `{Class}Tests`                                   | `CatalogAssetsUseCaseImplTest`       |
| Test method          | `method_condition_expected`                                       | `execute_folderExists_returnsAssets` |
| Migration            | `V{n}__{Description}.sql`                                         | `V2__Add_hash_column.sql`            |
| Use-case interface   | `FooUseCase` in `domain/port/in/<pkg>/`                           | `CatalogAssetsUseCase`               |
| Use-case impl        | `FooUseCaseImpl` in `application/usecase/<pkg>/`                  | `CatalogAssetsUseCaseImpl`           |
| Service port         | `FooPort` in `domain/port/out/`                                   | `StoragePort`, `ThumbnailPort`       |
| Service adapter      | `FooServiceAdapter` in `infrastructure/service/`                  | `StorageServiceAdapter`              |
| Repository port      | `FooRepository` in `domain/port/out/`                             | `AssetRepository`, `FolderRepository`|
| Repository adapter   | `FooRepositoryImpl` in `infrastructure/persistence/adapter/`      | `AssetRepositoryImpl`                |
| JPA repository       | `JpaFooRepository` in `infrastructure/persistence/jpa/`           | `JpaAssetRepository`                 |
| JPA entity           | `FooEntity` in `infrastructure/persistence/entity/`               | `AssetEntity`, `FolderEntity`        |
| Domain model         | Plain class in `domain/model/`                                    | `Asset`, `Folder`                    |
| HTTP request DTO     | `FooRequestDto` in `infrastructure/web/dto/request/`              | `CreateAlbumRequestDto`              |
| HTTP response DTO    | `FooResponseDto` in `infrastructure/web/dto/response/`            | `AssetResponseDto`                   |
| HTTP shared DTO      | Unchanged name in `infrastructure/web/dto/shared/`                | `UserPreferenceDto`                  |

🟡 Flag any violation of the above.

🔴 Flag any class placed directly in `infrastructure/web/dto/` instead of one of
its `request/`, `response/`, or `shared/` subpackages.

🟡 Flag a request DTO (used only as `@RequestBody`/`@RequestParam`) that isn't
named `{BaseName}RequestDto`, or a response DTO (used only as a `ResponseEntity<...>`/
return-type payload, including nested inside another response DTO) that isn't named
`{BaseName}ResponseDto`. A DTO belongs in `shared/` only if the exact same class is
verified to appear as both a request and a response payload across every controller
method that references it — don't place it there just because the name is ambiguous.

---

### 9. Backend: Testing

🔴 Flag unit tests that use `@SpringBootTest` — unit tests must use
`@ExtendWith(MockitoExtension.class)` only.

🟡 Flag tests that don't name the system under test `sut`.

🟡 Flag tests that use JUnit's `assertEquals` / `assertTrue` — use AssertJ's
`assertThat(...)` instead.

🟡 Flag integration tests that don't carry `@ActiveProfiles("test")` — they
may run against the real database.

🟡 Flag test methods with more than one `assertThat` that tests a different
concept — split into separate test methods.

### 14. Cross-cutting: Comments & Code Style

🟢 Flag comments that restate what the code does (e.g., `// increment counter`
before `counter++`). Only comments explaining **why** (a non-obvious
constraint, workaround, or invariant) are kept.

🟢 Flag multi-line comment blocks or Javadoc on internal methods — terse
single-line comments are preferred, and only when necessary.

🟢 Flag large imperative loops that could be replaced by a stream or array
method.

---

### 15. Known Project-Specific Pitfalls

These have caused real bugs in this codebase and deserve extra attention:

| Pitfall                                    | What to look for                                                                                                                                 |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `@Transactional` self-invocation           | Method annotated `@Transactional` called from same class without going through proxy                                                             |
| JPA entity in `domain/`                    | `@Entity` class placed under `domain/model/` or `domain/port/` — entities belong in `infrastructure/persistence/entity/`                        |
| Adapter injected directly                  | Caller injects `FooRepositoryImpl` or `FooServiceAdapter` instead of the `domain/port/out/` interface                                           |
| Use case bypassing port interface          | Use-case impl injects another use-case impl directly instead of its `domain/port/in/` interface                                                  |
| Missing catalog directory                  | `root-catalog-folders` pointing to a directory that may not exist on all machines; should use `application-local.yml` for machine-specific paths |
| Lazy association outside transaction       | Accessing `asset.getFolder()` or similar after the session is closed                                                                             |
| SSE emitter not completed                  | `SseEmitter` left open after the async operation finishes                                                                                        |
| `@Async` without `@EnableAsync`            | `@Async` has no effect if `@EnableAsync` is missing from the application class                                                                   |
| Cypress Jasmine matchers                   | Using `toBe` / `toEqual` instead of Chai `to.equal` / `to.deep.equal`                                                                            |
| `*ngIf` / `*ngFor` in new templates        | Angular 17+ control flow must be used; directives are legacy                                                                                     |
| SSE + Spring Security async dispatch       | `SecurityFilterChain` missing `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` → `AuthorizationDeniedException` on the async thread   |
| Token in URL for SSE / image endpoints     | `EventSource` and `<img>` do not send `Authorization` headers; tokens in query params appear in logs — use HttpOnly cookies instead              |
| SecurityConfig circular dependency         | `@Bean UserDetailsService` defined in `SecurityConfig` while `SecurityConfig` injects a filter that depends on it — extract to separate config   |
| JPA delete-then-insert with stale IDs      | `deleteAll()` + `saveAll(incoming)` when incoming entities still have old IDs → Hibernate merges against deleted rows; use `deleteAllInBatch()` + `setId(null)` |
| CORS missing `PATCH`                       | `AppConfig.corsFilter()` `allowedMethods` omitting `"PATCH"` when `@PatchMapping` endpoints exist → 403 on preflight                            |
| Missing OpenAPI annotations on controller  | New `@RestController` added without `@Tag` / `@Operation` / `@ApiResponses` — controller appears in Swagger UI under "default" with no documentation |
| Hand-written mapper                        | Entity ↔ domain model or HTTP DTO ↔ domain model conversion done manually instead of with a MapStruct `@Mapper(componentModel = "spring")`      |
| DTO placed directly in `web/dto/`          | New HTTP DTO added straight to `infrastructure/web/dto/` instead of its `request/`, `response/`, or `shared/` subpackage, or named without the `RequestDto`/`ResponseDto` suffix |
| Delegate-only port/adapter or service      | A port/adapter (backend) or service (frontend) with no logic of its own, just forwarding to another one for the same capability. The keep-or-delete test is whether it contributes its own logic — **not** whether it currently has callers; existing callers just mean they need repointing to the real implementation, not that the wrapper earns a reprieve. Backend incident: `HashCalculatorPort`/`AssetHashCalculatorAdapter` duplicated `StoragePort.computeHash`'s SHA-256 logic; the first fix made the adapter delegate to `StoragePort` instead of deleting it and migrating callers — the pair was pure pass-through and should have been deleted outright, with any real callers repointed to `StoragePort` directly. Frontend incident: `core/services/audio-player.service.ts` was a bare re-export (`export { MediaPlayerService as AudioPlayerService } from './media-player.service'`) with zero importers anywhere in the codebase — deleted outright |
| Method/function past the complexity threshold | `mvn pmd:check` (backend) or `npm run complexity` (frontend) reports something over 15 — see §18 |
| Line coverage below 80%                    | `mvn jacoco:check` (backend) or `npm run coverage:check` (frontend) reports under 80% — see §19 |
| Untyped (`any`) identifier (frontend)      | `npm run type-coverage:report` lists it — see §20 |
| Unused export/file/dependency              | `npm run dead-code:report` (frontend) or `mvn dependency:analyze` (backend) lists it — see §21 |
| Survived mutant (test runs, doesn't assert) | `npm run mutation:report` (frontend) or `bash scripts/mutation-report.sh` (backend) lists it — see §24 |
| `mat-form-field` errors on blur before any submit attempt | An add-row/optional validated field showing its "required" error on focus-then-blur — gate on an explicit "attempted" signal + per-field `[errorStateMatcher]`, not `control.touched` — see §10.4 |
| Outline `mat-form-field` label clipped or truncated | First field in a scrollable container with no `margin-top` (label clipped at the container edge), or a field hard-sized narrower than its `mat-label` (label ellipsised) — see §10.4 |
| Flex row overlaps or a wrapped button row is ragged at mobile width | Not checked at `angular-developer` §20's standard mobile check device — see §10.4 |

### 18. Cyclomatic Complexity (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a McCabe cyclomatic
complexity pass, in addition to the manual checklists above. Complexity is
measured per method/function (each starts at 1; +1 for each `if`, ternary,
loop, `catch`/switch-`case`, and short-circuit operator — `&&`, `||`, `??`
on the frontend; PMD's equivalent counting on the backend). **Max allowed
complexity is 15 per method/function**, in both sub-projects.

Don't count this by hand — each sub-project has its own checked-in analyzer.

#### 18.2 Backend (Java)

Run from `backend/`:

```
mvn pmd:check
```

This invokes the `maven-pmd-plugin` (configured in `backend/pom.xml`,
version 3.28.0, bundling PMD 7.17.0) against `backend/pmd-complexity-ruleset.xml`,
which enables only PMD's built-in `CyclomaticComplexity` rule with
`methodReportLevel` set to 16 (PMD reports a violation when complexity is
**greater than or equal to** the configured level, so 16 is what flags
"over 15") and `classReportLevel` effectively disabled — this check is
scoped to individual methods, not a class's combined total. The plugin is
declared with no `<executions>` binding, so it never runs as part of the
normal build/test/CI lifecycle (`mvn verify`, `mvn package`, ...) — it's
opt-in, invoked only when this check is run, the same way the frontend's
`npm run complexity` isn't part of `npm run build`/`test`. `mvn pmd:check`
fails the command (non-zero exit) and writes `target/pmd.xml` when anything
exceeds the threshold — read that file, or the console output, for the
offending class/method/line.

#### 18.3 Flagging

🟡 Flag any method/function reported over complexity 15 — this is a
maintainability problem (deep, hard-to-test branching), not a correctness
bug, so it's a WARNING rather than CRITICAL, but it should be fixed: extract
guard clauses, split the method/function by responsibility, or replace a
long `if`/`else if` chain with a lookup table/strategy map (backend:
consider a `switch` on an enum, a `Map<Key, Handler>`, or splitting the
use-case into smaller collaborators respecting §1.1/§1.2's port boundaries).
Note the reported complexity number and location in the finding so a fix
can be verified by re-running the relevant command.

🟢 A method/function in the 10–15 range is worth a passing mention if an
obvious, low-effort split exists, but isn't required to be flagged — the
threshold that matters is 15.

#### 18.4 Trending snapshots

For a full-codebase sweep, both the pass/fail gates above have a trending
companion that ranks every function/method by complexity instead of only
flagging the ones over threshold — useful for spotting something climbing
toward 15 before it becomes an actual violation.

- **Frontend:** `npm run complexity:report` (`scripts/complexity-report.js`)
  — dated snapshot under `docs/reports/complexity/`, top 20 functions by
  complexity plus top 20 files by line count.
- **Backend:** `bash scripts/complexity-report.sh` (run from `backend/`) —
  same shape, under `JPPhotoManagerWeb/docs/reports/complexity/`. Uses a
  second, report-only `maven-pmd-plugin` execution
  (`pmd-complexity-report-ruleset.xml`, `methodReportLevel=1` so PMD
  reports every method instead of only violations) — never touches the
  real gate's `pmd-complexity-ruleset.xml` or its threshold.

---

### 19. Code Coverage (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a line-coverage
pass, in addition to the manual checklists above. **Minimum line coverage is
80%**, in both sub-projects, whether the check runs over the whole project
or is scoped to just the files a change touched.

Don't estimate this by eye — each sub-project already has a coverage tool
wired (`cypress-unit-test-developer` §1.3; `java-unit-test-developer` §1);
this section only adds the enforced threshold and the two ways to scope the
check.

#### 19.2 Backend (Java)

Run from `backend/` (populate `target/jacoco.exec`, then check the threshold):

```
mvn test
mvn jacoco:check
```

This invokes the `jacoco-maven-plugin` (configured in `backend/pom.xml`)
against its default rule — a `BUNDLE`-level `LINE` `COVEREDRATIO` minimum
of `${jacoco.check.minimum}` (80%) — reading the exec data `mvn test`
already produced via the plugin's existing `prepare-agent` execution. Like
`mvn pmd:check` (§18.2), the `check` goal has no `<executions>` binding in
the pom, so it never runs as part of the normal build/test lifecycle
(`mvn test`, `mvn verify`, `mvn package`) — it's opt-in, invoked only when
this check is run. `mvn jacoco:check` fails the command (non-zero exit) and
prints the offending counter/ratio when coverage is under threshold;
`target/site/jacoco/index.html` (from the existing `report` execution)
shows the breakdown per package/class.

For a **scoped review**, override `jacoco.check.includes` (default `**/*`,
the whole project) to the package(s) the change touched, so the ratio is
computed only over those classes instead of the whole backend:

```
mvn jacoco:check -Djacoco.check.includes=com/jpablodrexler/photomanager/application/usecase/album/**
```

#### 19.3 Flagging

🟡 Flag any coverage run — whole-project or scoped to the reviewed change —
that reports under 80% line coverage. This is a test-adequacy problem, not
a correctness bug, so it's a WARNING rather than CRITICAL, matching how
§18's complexity threshold is treated — but it should be fixed before the
review is considered clean: add the missing test cases for the uncovered
lines/branches the report lists (`java-unit-test-developer` for backend
gaps, `cypress-unit-test-developer` for frontend gaps), then re-run the
check to confirm it now clears 80%.

🟢 A scope in the 75–80% range is worth a passing mention if the gap is a
small, easily-covered handful of lines, but isn't required to be flagged —
the threshold that matters is 80%.

#### 19.4 Trending snapshots

For a full-codebase sweep, both `npm run coverage:trend-report`
(`scripts/code-coverage-report.js`, frontend) and `bash
scripts/coverage-report.sh` (run from `backend/`, backend) wrap the same
suite runs the gates above use into a dated snapshot under
`docs/reports/code-coverage/`/`JPPhotoManagerWeb/docs/reports/code-coverage/`
— the project-wide percentages plus a table of every file/class still
below 80%. Unlike `coverage:check`/`jacoco:check`, these do not fail the
command; they exist purely to leave a written record of the actual number
over time, so a file that quietly backslid is visible even while the
project-wide number stays above threshold. The backend script parses
`target/site/jacoco/jacoco.xml` directly (via an inline Perl snippet, since
the file is single-line, deeply-nested XML that plain `grep`/`awk` cannot
reliably disambiguate between method/class/package/report-level counters
sharing the same tag name) rather than adding a second JaCoCo plugin
execution the way §18.4's backend complexity report needed — JaCoCo's
existing `report` execution (bound to the `test` phase) already produces
everything this needs.

### 21. Dead Code

Every full-codebase sweep also gets an unused-code pass — the automated
counterpart to whatever "reuse/simplification" findings a manual read
would catch, just extended to catch what a single-file read cannot: an
export or dependency nothing references *anywhere else* in the codebase.

#### 21.2 Backend (Maven)

Run from `backend/`:

```
mvn dependency:analyze
```

(or `bash scripts/dead-code-report.sh` for a dated snapshot under
`JPPhotoManagerWeb/docs/reports/dead-code/`). Maven's own built-in
unused/undeclared-dependency detector — the closest backend equivalent to
knip, though narrower in scope: it only covers dependencies, not unused
application-source exports/classes, since Maven has no direct analog to
knip's source-level dead-code detection.

**Known, expected noise:** every `spring-boot-starter-*` "umbrella"
dependency is reported as "unused declared" because `dependency:analyze`
works by scanning compiled bytecode for direct class references, and a
starter POM has no classes of its own — it exists purely to pull in a
bundle of real dependencies transitively. This is a well-documented
limitation of bytecode-based analysis for Spring Boot specifically, not a
real finding — skim past every `spring-boot-starter-*` entry and focus on
anything else in the "Unused declared dependencies" list.

#### 21.3 Flagging

🟡 Flag an unused file, export, or type (frontend) — either it should be
made module-private or, if nothing in the codebase needs it anymore,
deleted outright per this project's own "no half-finished implementations"
convention.

🟡 Flag an unused declared dependency (either sub-project, excluding
§21.2's Spring Boot starter noise) — dead weight in the build and a wider
(if unused) attack surface for `security-reviewer` §1 to worry about.

🟢 Flag an `unlisted` dependency (frontend: imported but only present
transitively; backend: `dependency:analyze`'s "Used undeclared
dependencies") as a suggestion — it works today only because some other
direct dependency happens to pull it in, fragile across dependency-tree
changes.

---

### 24. Mutation Testing (both sub-projects)

Line coverage (§19) only proves a line *executed* during the test suite —
it says nothing about whether a test actually *asserts* on that line's
behavior. Mutation testing closes that gap: a tool systematically changes
("mutates") small pieces of the code under test — flips a `>` to `>=`,
negates a condition, swaps a boolean literal — reruns the tests, and checks
whether anything failed. A "killed" mutant means some test caught the
change; a "survived" mutant means the whole suite still passed with the
code's behavior altered, i.e. that line has execution coverage but no real
assertion behind it. Unlike §18/§19/§21/§22, this is not wired as a
pass/fail gate anywhere (no CI job, no `npm run *:check` equivalent) — it's
a trending snapshot only, run on demand, the same way §18.4's complexity
trend report and the dead-code/dependency-staleness reports are.

#### 24.2 Backend (PIT)

Run from `backend/`:

```
bash scripts/mutation-report.sh
```

Invokes `org.pitest:pitest-maven` (`backend/pom.xml`) via
`mvn org.pitest:pitest-maven:mutationCoverage@mutation-report` — a
report-only execution bound to `phase>none<`, mirroring
`maven-pmd-plugin`'s `complexity-report` execution (§18.2/§18.4), so it
never runs during `mvn test`/`verify`/`package`. Unlike the frontend, PIT
runs entirely inside the JVM test process alongside the real JUnit
suite — no browser boundary to bridge, and far faster than the frontend's
per-mutant-process command-runner approach for a comparable mutant count
(the full `application.usecase` scope — 65 classes, 443 mutants — runs in
about 4 minutes). `targetClasses`/`targetTests` are scoped to
`com.jpablodrexler.photomanager.application.usecase.*` — the actual
business-logic layer; `infrastructure.web`/`infrastructure.persistence`
are thin translation code not worth mutating.

Writes a dated snapshot to `docs/reports/mutation/` — PIT's own Line
Coverage / Mutation Coverage / Test Strength percentages, killed/survived/
no-coverage/timed-out counts, a per-class table sorted worst-first, and a
full list of surviving/uncovered mutants (class, method, line, mutator).

#### 24.3 Flagging

🟡 Flag a survived mutant on a line the reviewed change touched — the test
suite runs and passes even though that line's behavior changed, meaning
whatever test covers it isn't actually asserting on the behavior a real
bug there would break. Fix by strengthening the existing test's
assertions (not by adding a redundant new test) so it would fail against
the mutant's changed behavior; re-run the relevant report to confirm.

🟡 Flag a "no coverage" mutant on a line the reviewed change touched — no
test reaches that line at all, a stronger gap than a survived mutant (that
line has neither execution coverage nor an assertion), and often a
`java-unit-test-developer`/`cypress-unit-test-developer` gap worth cross-
referencing against §19's coverage report for the same file.

🟢 Don't chase every survived mutant project-wide in a scoped review —
both reports' scope is already narrow (frontend: a curated ~8-file list;
backend: one architectural layer), so treat every surviving mutant inside
that scope as worth a look, but don't expand the mutation-testing scope
itself as part of an unrelated review.

---

### Full-codebase sweep layers

**The layers:**

| Layer key                | Report suffix          | Directory scope                                                                                                    | Primary checklist sections                                    |
| ------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Backend domain           | `backend-domain`         | `backend/.../domain/**`                                                                                                                   | §1.1, §5, §8 (domain-model rows)                                 |
| Backend application      | `backend-application`    | `backend/.../application/usecase/**`                                                                                                      | §1.1, §1.2, §2, §4, §7, §8 (use-case rows)                       |
| Backend api               | `backend-api`             | `backend/.../infrastructure/web/**` (controllers, request/response DTOs)                                                                  | §1.1 (controller delegation), §3.4, §6 (DTO/entity drift), §8 (DTO rows) |
| Backend infrastructure   | `backend-infrastructure` | `backend/.../infrastructure/persistence/**`, `infrastructure/service/**`, `infrastructure/kafka/**`, `infrastructure/batch/**`, `infrastructure/config/**` | §1.2, §3.1–3.3, §5, §6, §7                                       |
| Frontend core             | `frontend-core`           | `frontend/src/app/core/**`                                                                                                                | §10.2, §11, §12                                                  |
| Frontend features         | `frontend-features`       | `frontend/src/app/features/**`                                                                                                            | §10.1, §10.3, §11, §12                                           |
| Frontend shared           | `frontend-shared`         | `frontend/src/app/shared/**`                                                                                                              | §1.3, §10.1, §11, §12                                            |
| Cross-cutting             | `cross-cutting`           | Both sub-projects; no single directory                                                                                                    | §9, §13, §14, §18, §19, §20, §21, §22, delegate-only port/adapter or service pairs (§1.2/§10.2), dependency-direction violations, systemic naming patterns |

If only the backend (or only the frontend) is in scope, skip the layers that
don't apply — e.g. a "review the whole backend" request produces 4 reports
(domain, application, api, infrastructure) plus a backend-scoped
cross-cutting report, not all 8.

**What goes in the cross-cutting report:** findings that don't belong to one
file's home layer — backend testing conventions (§9) and Cypress conventions
(§13), comment/code-style patterns (§14) that recur across many files, and
anything whose root cause spans two layers at once (e.g. a delegate-only
port/adapter pair, where the finding is really about the port *and* the
adapter *and* the callers, not just one of them). A naming or architecture
violation that's local to a single file still goes in that file's own layer
report, even if the rule itself is defined in a "cross-cutting" section like
§8/§12.

## Spec compliance checks

What `spec-compliance-check` reads for this project, for both sub-projects: how a scenario is matched to an automated test (JUnit or Cypress) and run, and the manual fallback. The section numbers (§N) are the ones its reports cite, so they stay stable.

### 3. Scale and example

- Example of the §3 multi-capability change: `asset-tagging`'s change also touching `search-and-filter`'s spec.
- A typical feature change has 5–15 scenarios across 5–10 requirements.
- Example of a `WHEN` in this project: usually a specific API call or UI interaction.
- Example of a partially satisfied requirement: the happy path works but a validation-error scenario under the same requirement doesn't.

### 4.1 / 4.2 Test-file kinds

- Example of a task-to-scenario match (this project's own archived changes show the pattern consistently): `star-ratings`' task *"Add test: `PATCH .../rating` with `{"rating":6}` returns `400 Bad Request`"* maps 1:1 to its spec's *"Scenario: Rating above 5 is rejected"*.
- The test file/class a task's section header names is e.g. `AssetControllerTest`, or the relevant `*.cy.ts` spec.
- Test's fully-qualified name: class#method, or spec file + `it(...)` description (e.g. `ClassName#testMethod`).
- §4.2: a general auth test that happens to also cover this endpoint; search test files for the endpoint/component/behavior the scenario describes.

### 5. Running one test

- Run the single test method if the test framework supports that granularity (`mvn test -Dtest=ClassName#method`, `npx cypress run --spec ...`), otherwise the narrowest containing suite.
- "A test exists" and "the test passes" are easy to conflate — say so explicitly when reporting a failing one.
- Running a test is read-only (`mvn test`, `npx cypress run`).
- Partial manual check example: you confirmed the happy path but not the exact error message the scenario specifies.

### 4.3 Manual verification (fallback)

- Commonly true-visual claims, e.g. "the toolbar SHALL display 5 clickable star icon buttons".
- **API-level scenarios:** issue the actual request with `curl`, following the patterns in `docs/curl-reference.md`, against a running backend (start one per `e2e-testing` skill §1–§4 if none is running), and compare the real response to the scenario's `THEN`.
- **UI-level scenarios:** use the Cypress scratch-spec approach from `e2e-testing` §7 (start the stack per its §1–§4, sign in per its §7.1) to drive the actual interaction and observe the actual result.
- Record exactly what was run and what was observed as the evidence — a specific command/action and its specific output (e.g. `curl ...`).

## Dependency upgrade checks

What `dependency-upgrade` reads for this project. This doc holds the backend survey (§1) and the parts shared by both sub-projects (§3–§6); the frontend survey (§2) is in `docs/frontend.md`. The section numbers (§N) are the ones its reports cite, so they stay stable.

### 1. Backend Survey (Maven)

```bash
cd JPPhotoManagerWeb/backend
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
mvn dependency:analyze   # or: bash scripts/dead-code-report.sh for a dated snapshot under docs/reports/dead-code/
```

The `dependency:analyze` run is a useful pre-check, not a strict
prerequisite: a dependency the survey lists as outdated but
`dependency:analyze` also reports as genuinely unused is a candidate for
removal instead of upgrade — no point spending a bump on a package nothing
references anymore. Ignore every `spring-boot-starter-*` entry under
"Unused declared dependencies" though — that's expected noise from
bytecode-based analysis missing umbrella POMs with no classes of their
own, not a real finding (see `code-reviewer` skill §21.2).

Most library versions in this project are inherited from the
`spring-boot-starter-parent` BOM (see `java-developer` §1) rather than
pinned individually — check the parent POM version itself
(`spring-boot-starter-parent`) as the primary lever; a Spring Boot patch/minor
bump usually moves every managed dependency's version together in a tested,
compatible set. Only bump an individual `<dependency>` version directly when
it's declared outside the BOM (not managed by `spring-boot-starter-parent`).

🟡 Flag any dependency currently overridden to an older version via
`<dependencyManagement>` that the survey shows is now behind — verify
whether the override reason (check the POM for a comment) still applies
before removing it.

---


### 3. Classify & Decide

🔴 Never apply a major-version bump to `spring-boot-starter-parent`,
`@angular/core`, or any other framework-defining dependency without explicit
user approval for that specific package — these routinely carry breaking
changes this project's tests may not fully cover (e.g. a removed
auto-configuration, a changed default).

🟢 A patch/minor bump to a leaf dependency with no direct API usage in this
codebase (a transitive-only dependency bumped for a CVE fix) is lower risk
than a direct, heavily-used dependency (Spring Data JPA, MapStruct, Angular
Material) — note this distinction in the report (§6) but the automatic/
manual split above still applies uniformly by bump type, not by usage
weight.

### 4. Apply & Test, In Groups

1. **Group 1 — backend patch/minor**, applied via the parent POM bump plus
   any individually-managed dependencies from §1. Then:
   ```bash
   cd JPPhotoManagerWeb/backend && mvn clean test
   ```
2. **Group 2 — frontend patch/minor** (`npm update`, or `ng update` for any
   `@angular/*` minor). Then:
   ```bash
   cd JPPhotoManagerWeb/frontend && npm test && npm run lint && npm run build:prod
   ```
3. **Group 3+ — each approved major bump, one at a time**, its own test run
   immediately after. For a Spring Boot major/minor jump, also skim the
   official release notes' "breaking changes"/migration guide section before
   applying — don't rely on the test suite alone to catch a removed
   auto-configuration the tests don't happen to exercise.

If a group's test run fails, narrow it down within that group — the goal is
to land as much of the safe upgrade as possible, not to give up on the first
failure.

### 5. Cross-Check Against Security Findings

```bash
cd JPPhotoManagerWeb/backend && mvn org.owasp:dependency-check-maven:check
cd JPPhotoManagerWeb/frontend && npm audit --audit-level=high
```

The vulnerable dependency, if transitive, needs an explicit
`<dependencyManagement>`/npm `overrides` entry.

### 6. Report

- **Applied** — each bumped dependency, old → new version, grouped by §4's
  groups.

[← Back to README](../README.md)
