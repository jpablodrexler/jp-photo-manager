## Context

`AppConfig.cacheManager()` currently returns a `CaffeineCacheManager` with three named caches
(`home-stats`, `sub-folders`, `asset-exif`) populated via `@Cacheable`/evicted via `@CacheEvict` on
the corresponding use cases (`GetHomeStatsUseCaseImpl`, `GetSubFoldersUseCaseImpl`,
`GetAssetExifUseCaseImpl`). Caffeine caches are per-JVM heap structures: correct for a single
instance, but a write on instance A never evicts the copy held by instance B once the app scales
horizontally.

Redis is already provisioned and reachable from the backend via a shared `RedisConnectionFactory`
bean (Spring Boot auto-configuration, driven by `spring.data.redis.host`/`port`), used today by
`RedisRefreshTokenStore`, the `rateLimitRedisClient`/`LettuceBasedProxyManager` pair, and the
dedicated `thumbnailRedisTemplate`. No new Redis deployment or connection plumbing is needed.

The two endpoints this change targets are the highest-frequency reads in the gallery: every page
load and every filter change hits `GET /api/assets` (`GetAssetsUseCaseImpl` → `AssetRepository.findFiltered`),
and the tag autocomplete/filter chips hit `GET /api/tags` (`ListTagsUseCaseImpl` → `TagRepository.findByNameContaining`).

## Goals / Non-Goals

**Goals:**
- Replace the `CaffeineCacheManager` bean with a `RedisCacheManager` so all five named caches
  (`home-stats`, `sub-folders`, `asset-exif`, `assets`, `tags`) are backed by Redis, shared correctly
  across multiple backend instances.
- Cache `GetAssetsUseCaseImpl.execute(AssetFilter)` results, scoped per folder so a single folder's
  cache entries can be invalidated without touching unrelated folders.
- Cache the unfiltered tag list (`GET /api/tags` with no `q` parameter) under one key, invalidated
  immediately on any tag add/remove.
- Invalidate the asset-search cache automatically when the underlying data changes, driven by the
  `asset.cataloged` / `asset.deleted` Kafka events already published by the catalog and delete paths
  (`kafka-catalog-pipeline`, #75).
- Preserve the existing `@Cacheable`/`@CacheEvict` annotations on `home-stats`, `sub-folders`, and
  `asset-exif` unchanged — only the `CacheManager` bean implementation changes underneath them.

**Non-Goals:**
- Adding per-tag asset counts to `GET /api/tags` — the endpoint's response shape is unchanged.
- Caching prefix-search tag queries (`?q=xyz`) — only the unfiltered "all tags" call is cached, since
  prefix queries have low cardinality-reuse and would otherwise multiply cache entries with little
  hit-rate benefit.
- Migrating rate-limiting or refresh-token Redis usage — those already have their own
  `RedisClient`/`RedisTemplate` beans and are untouched.
- Building a generic distributed-cache abstraction; this change is scoped to the two endpoints named
  in the proposal.

## Decisions

### 1. One `RedisCacheManager` for all five caches, not a second cache manager

Spring Cache supports only one primary `CacheManager` unless caches are looked up by qualified bean
name. Since the three existing caches already work correctly through the generic `CacheManager`
abstraction, the simplest change is to swap the *implementation* of that single bean rather than
introduce a second cache manager just for the two new caches. `RedisCacheManager.builder(redisConnectionFactory)
.cacheDefaults(defaultConfig).withInitialCacheConfigurations(perCacheConfigMap).build()` lets each
named cache keep its own TTL (`home-stats` 10 min, `sub-folders` 5 min, `asset-exif` 30 min, `assets`
5 min, `tags` 5 min) — mirroring the granularity Caffeine's `registerCustomCache` gave per cache.

Alternative considered: keep Caffeine for the original three and add a second, Redis-backed
`CacheManager` bean qualified `@Qualifier("redisCacheManager")` just for `assets`/`tags`. Rejected —
it leaves the original three caches non-distributed (the actual complaint motivating this change per
the `redis-search-tag-cache` dependency note: "upgrades the Spring cache manager from Caffeine to
Redis"), and forces every `@Cacheable` annotation to pick a manager explicitly via `cacheManager=`.

### 2. Composite cache key for asset search: `{folderId}:{sha256(rest of filter)}`

`AssetFilter` carries `folderId` (already resolved from `folderPath` by the controller before the
use case is called), plus `search`, `dateFrom`, `dateTo`, `minRating`, `sortCriteria`, `page`,
`pageSize`, `includeDeleted`, and `tags`. A new `AssetSearchCacheKeyGenerator implements
org.springframework.cache.interceptor.KeyGenerator` builds the key by:
1. Taking `folderId` as a literal prefix segment (`"" + folderId`, or `"none"` if `folderId` is
   `null`, which happens when a caller passes an unrecognized/root path).
2. Concatenating the remaining fields with a delimiter, SHA-256 hashing the result, and
   hex-encoding the digest.
3. Returning `folderId + ":" + hexDigest`.

`GetAssetsUseCaseImpl.execute` is annotated `@Cacheable(cacheNames = "assets", keyGenerator =
"assetSearchCacheKeyGenerator")`. Combined with a `RedisCacheConfiguration.computePrefixWith(name ->
name + ":")` override (Spring's default is `name + "::"`, double colon), the resulting Redis key is
`assets:{folderId}:{hexDigest}` — exactly the format needed for prefix-based eviction.

Alternative considered: key only by `folderId` and let `@Cacheable`'s condition/unless attributes
skip caching on rare filter combinations. Rejected — every distinct filter combination is a distinct
result set; collapsing them to one key per folder would return stale/wrong pages to users applying a
search or rating filter.

Alternative considered: hash the entire filter including `folderId` into one opaque key (no visible
prefix). Rejected — this is what the proposal explicitly avoids, since it removes the ability to
`SCAN`-and-evict all of one folder's entries without touching every other folder's.

### 3. Folder-scoped eviction via `SCAN`, not `KEYS`

`AssetSearchCacheInvalidationListener` is a `@Component` with two `@KafkaListener` methods:
```java
@KafkaListener(topics = "asset.cataloged", groupId = "asset-search-cache-invalidator", ...)
void onAssetCataloged(AssetCatalogedEvent event) { evictFolder(resolveFolderId(event.folderPath())); }

@KafkaListener(topics = "asset.deleted", groupId = "asset-search-cache-invalidator", ...)
void onAssetDeleted(AssetDeletedEvent event) { evictFolder(event.folderId()); }
```
`AssetDeletedEvent` already carries `folderId` directly. `AssetCatalogedEvent` carries only
`folderPath`, so `onAssetCataloged` resolves it to a `folderId` via the existing
`GetFolderIdByPathUseCase` port (already used by `AssetController` for the same lookup) before
evicting — no new repository method needed.

`evictFolder(Long folderId)` uses `RedisConnectionFactory.getConnection().scan(ScanOptions.scanOptions()
.match("assets:" + folderId + ":*").count(200).build())` to enumerate matching keys in bounded
batches and deletes them. `KEYS` is avoided deliberately: it blocks the single-threaded Redis event
loop for the duration of the scan, which is unacceptable in a shared production Redis instance also
serving the rate limiter and refresh-token store.

Uses its own consumer group (`asset-search-cache-invalidator`), following the same
explicit-persistent-group pattern as `AuditLogKafkaListener` (`audit-log-writer`) and the
upload-processing listeners, so exactly one instance processes each event regardless of how many
backend replicas are running.

### 4. Tag cache: single fixed key, declarative eviction

`ListTagsUseCaseImpl.execute(String query)` gets `@Cacheable(cacheNames = "tags", key = "'all'",
condition = "#query == null or #query.isBlank()")` — Spring SpEL `condition` (evaluated *before* the
method runs, unlike `unless`) skips both the cache read and write when a prefix filter is present, so
`?q=vac` style calls always hit the repository. `AddTagToAssetUseCaseImpl.execute` and
`RemoveTagFromAssetUseCaseImpl.execute` each get `@CacheEvict(cacheNames = "tags", key = "'all'")` —
declarative and consistent with how `DeleteAssetsUseCaseImpl` already evicts `home-stats`/`asset-exif`
today. No new Kafka listener needed for tags since the mutation always happens synchronously inside
the same use case call the cache needs to invalidate.

### 5. Fail-open behavior

Both new caches inherit Spring Cache's standard behavior: if Redis is unreachable, `@Cacheable`
propagates the exception by default, which would turn a cache outage into a 500 on `GET /api/assets`.
To match the fail-open convention already established by `redis-thumbnail-cache` and
`redis-refresh-tokens` (Redis errors caught, logged at `WARN`, fall back to the source of truth), the
`RedisCacheManager` is wrapped so lookup/write exceptions are swallowed: Spring Cache exposes this via
a custom `CacheErrorHandler` bean (`SimpleCacheErrorHandler` subclass that logs and returns/no-ops
instead of rethrowing) registered through `CachingConfigurerSupport`/`errorHandler()`. This applies to
all five caches uniformly, extending the existing fail-open convention rather than special-casing the
two new ones.

## Risks / Trade-offs

- **[Risk] Folder ID reuse after a folder is deleted and re-created at the same path could serve a
  stale cache entry under a coincidentally-reused ID.** → Mitigation: folder IDs are database-generated
  and never reused (auto-increment primary key); a deleted folder's ID is retired permanently.
- **[Risk] `SCAN` with `MATCH` still requires iterating the full keyspace on the server side per call,
  though non-blocking.** → Mitigation: acceptable at current data volumes (single-digit thousands of
  folders); the 5-minute TTL bounds the worst case where an eviction is missed or delayed, and this is
  the same trade-off already accepted for `redis-thumbnail-cache`.
- **[Risk] Moving `home-stats`/`sub-folders`/`asset-exif` off Caffeine changes their latency profile
  (network round-trip vs. in-heap lookup).** → Mitigation: these are read on page navigation, not in a
  tight loop; Redis round-trip latency (sub-millisecond on the same Docker network) is not
  user-perceptible, and this is the explicitly intended upgrade per the `82 → 28` dependency note.
- **[Risk] `CacheErrorHandler` swallowing exceptions could mask a persistently broken Redis deployment.**
  → Mitigation: every swallowed exception is logged at `WARN` with the cache name and operation, giving
  operators a signal in logs/alerting without failing user requests, consistent with `redis-thumbnail-cache`.

## Migration Plan

1. Add the `RedisCacheManager` bean and `CacheErrorHandler`, replacing `CaffeineCacheManager` in
   `AppConfig`. No data migration — caches are ephemeral and start empty on deploy.
2. Add `AssetSearchCacheKeyGenerator` bean and annotate `GetAssetsUseCaseImpl.execute`.
3. Add the `@Cacheable`/`@CacheEvict` annotations to `ListTagsUseCaseImpl`,
   `AddTagToAssetUseCaseImpl`, `RemoveTagFromAssetUseCaseImpl`.
4. Add `AssetSearchCacheInvalidationListener` with its own consumer group.
5. Rollback: revert `AppConfig` to `CaffeineCacheManager` and remove the new annotations/listener;
   no schema or data cleanup required since nothing is persisted outside Redis's own ephemeral store.

## Open Questions

- None — all decisions above are considered final for this change; the `folderId` vs `folderPath`
  key-prefix choice was the only ambiguity and is resolved in Decision 2.
