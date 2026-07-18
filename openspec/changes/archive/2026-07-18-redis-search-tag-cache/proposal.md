## Why

The gallery's two hottest read paths — paginated asset search (`GET /api/assets`) and the tag list
(`GET /api/tags`) — currently hit PostgreSQL on every request. `server-side-spring-cache` (#28)
already caches home stats, folder listings, and EXIF lookups with a per-JVM `CaffeineCacheManager`,
but a Caffeine cache is not shared across instances: once the backend is scaled horizontally, each
instance builds and evicts its own copy, so a write on one instance does not clear stale results
being served by another. Moving the cache backing store to Redis, and extending the cached scope to
these two endpoints, removes redundant database load and makes invalidation correct across multiple
instances.

## What Changes

- Switch the `CacheManager` bean in `AppConfig` from `CaffeineCacheManager` to a Lettuce-backed
  `RedisCacheManager`, reusing the existing `RedisConnectionFactory` already configured for the
  refresh-token store and thumbnail cache. The three existing named caches (`home-stats`,
  `sub-folders`, `asset-exif`) move onto Redis unchanged — their `@Cacheable`/`@CacheEvict`
  annotations require no code change.
- Add a new `assets` Redis cache in front of `GetAssetsUseCaseImpl.execute(AssetFilter)`, keyed by
  `assets:{folderId}:{sha256 of the remaining filter fields}` (page, sort, search, date range,
  minRating, tags, includeDeleted) with a 5-minute TTL. A dedicated `KeyGenerator` bean builds this
  composite key so eviction can scan by folder prefix.
- Add a new `tags` Redis cache in front of `ListTagsUseCaseImpl.execute(String query)`, caching only
  the unfiltered ("list all") case under the fixed key `tags:all` with a 5-minute TTL; prefix-search
  queries bypass the cache.
- Add a Kafka-driven invalidation listener that subscribes to the existing `asset.cataloged` and
  `asset.deleted` topics (published by `kafka-catalog-pipeline`, #75) and evicts all `assets:{folderId}:*`
  entries for the affected folder, using a cursor-based `SCAN` (never `KEYS`) against the Redis
  connection factory.
- Add declarative `@CacheEvict(cacheNames = "tags", key = "'all'")` to `AddTagToAssetUseCaseImpl` and
  `RemoveTagFromAssetUseCaseImpl` so the tag cache clears immediately on any tag mutation.
- No Flyway migration — this change touches only the Spring cache configuration and two use cases.

## Capabilities

### New Capabilities

(none — this change extends an existing capability)

### Modified Capabilities

- `server-side-spring-cache`: the `CacheManager` backing store changes from an in-process Caffeine
  cache to a distributed Redis cache; the cached scope is extended to cover paginated asset search
  results and the tag list, with event-driven invalidation for the asset search cache.

## Impact

- **Affected code:** `config/AppConfig.java` (cache manager bean), a new
  `infrastructure/service/AssetSearchCacheKeyGenerator.java`, a new
  `infrastructure/kafka/AssetSearchCacheInvalidationListener.java`,
  `application/usecase/asset/GetAssetsUseCaseImpl.java`,
  `application/usecase/tag/ListTagsUseCaseImpl.java`,
  `application/usecase/tag/AddTagToAssetUseCaseImpl.java`,
  `application/usecase/tag/RemoveTagFromAssetUseCaseImpl.java`.
- **Infrastructure:** no new infrastructure — Redis is already provisioned for
  `redis-distributed-rate-limiting` (#78), `redis-refresh-tokens` (#79), and `redis-thumbnail-cache`
  (#81); this change reuses the existing `RedisConnectionFactory` bean and Redis deployment.
- **Dependencies:** requires `server-side-spring-cache` (#28, done) and `kafka-catalog-pipeline`
  (#75, done), both already implemented.
- **Out of scope:** the tag list continues to return plain names (no per-tag asset counts); adding
  counts to `GET /api/tags` is a separate, unplanned enhancement and is not part of this change.
