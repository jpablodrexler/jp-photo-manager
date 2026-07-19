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

- **Primary database:** PostgreSQL 18 — assets, folders, users, albums, refresh tokens, search presets, sync/convert configuration, and the `catalog_run_state` distributed lock.
  - **Schema migrations:** Flyway, scripts in `src/main/resources/db/migration/`
  - **ORM:** Spring Data JPA with the Hibernate PostgreSQL dialect
  - **Connection:** configured via `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (defaults: `localhost`, `5432`, `photomanager`, `postgres`, `postgres`)
- **MongoDB:** append-only store for the `asset_audit_log` collection (user-action history), populated by `AuditLogKafkaListener` and direct `AuditLogRepository.log(...)` calls. Configured via `MONGO_URI`.
- **Redis:** thumbnail L2 cache, query-result caches (`assets`, `tags`, `home-stats`, `sub-folders`, `asset-exif`), the refresh-token mirror, and the Bucket4j rate-limit counters. Every Redis call fails open — a Redis outage degrades to always querying PostgreSQL/disk rather than failing the request. Configured via `REDIS_HOST`, `REDIS_PORT`.

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
