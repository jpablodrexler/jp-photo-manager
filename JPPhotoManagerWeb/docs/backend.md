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
                           SyncProgressMessage, ConvertProgressMessage), AssetFilter, PaginatedResult…
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
    filter/               → JwtAuthenticationFilter, RateLimitFilter (Bucket4j, backed by Redis)
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
                           JwtTokenServiceAdapter, RefreshTokenServiceAdapter, CatalogScheduler,
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
with no matching panel is a gap the `web-docs-sync` skill checks for.

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
| `photomanager.jwt-secret` | *(empty — must be set)* | HS256 signing secret (≥ 32 bytes); see [Authentication](authentication.md#authentication) |
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

You'll also need `photomanager.jwt-secret` set — see [Setup (local development)](authentication.md#setup-local-development) under Authentication.

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

[← Back to README](../README.md)
