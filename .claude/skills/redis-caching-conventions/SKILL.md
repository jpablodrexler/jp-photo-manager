---
name: redis-caching-conventions
description: >
  Redis caching conventions for the JPPhotoManager Spring Boot backend.
  TRIGGER whenever work adds or changes a `@Cacheable`/`@CacheEvict` usage,
  touches `AppConfig.cacheManager()` or any other Redis-backed bean, adds a
  new named Spring cache, or adds a new non-`@Cacheable` Redis usage
  (thumbnail L2 cache, refresh-token store, rate limiting) — including when
  implementing OpenSpec tasks that add caching. Do not wait to be asked:
  apply these conventions proactively whenever new Redis-backed code is
  written. Encodes the project's serializer-per-cache rule (a shared
  polymorphic serializer already broke the `tags` cache once), the fail-open
  error-handling convention, key-prefix/eviction pattern, and where
  cache-invalidation triggers must be wired up.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Redis Caching Conventions Skill

Redis in this backend serves four distinct purposes, only one of which is the
Spring Cache abstraction. Know which one you're touching before applying a
pattern from this file:

| Purpose | Mechanism | Where |
|---|---|---|
| Query-result caching (`assets`, `tags`, `home-stats`, `sub-folders`, `asset-exif`) | Spring `@Cacheable`/`@CacheEvict` via `RedisCacheManager` | `config/AppConfig.cacheManager()` |
| Thumbnail bytes L2 cache | Manual `RedisTemplate<String, byte[]>` reads/writes in `ThumbnailStorageServiceAdapter` | `AppConfig.thumbnailRedisTemplate()` |
| Refresh-token dual-write | `RedisRefreshTokenStore` mirrors every `RefreshTokenRepositoryImpl.save()`/`deleteBy*()` | `infrastructure/service` (Postgres stays authoritative for reads — see `database-reviewer` §6) |
| Rate limiting | `bucket4j` + `LettuceBasedProxyManager` in `RateLimitFilter` | `AppConfig.rateLimitProxyManager()` |

This skill's checklist (§1–§4) covers the first two — the ones a feature is
most likely to add to or extend. If you're adding a new dual-write pattern
(mirroring a Postgres/Mongo write into Redis) or a new rate-limited endpoint,
follow the existing precedent in the files above rather than inventing a new
shape.

---

## 1. Adding a New Named Spring Cache

Every cache registered in `AppConfig.cacheManager()`'s `perCacheConfigurations`
map needs its own `RedisCacheConfiguration` built by `typedConfig(...)` — never
add a new entry that falls through to the shared `fallbackConfig`
(`GenericJackson2JsonRedisSerializer`) on purpose.

**Why this matters — known incident:** during `redis-search-tag-cache`
development, the `tags` cache (a bare `List<Tag>`) was briefly backed by the
shared generic serializer with polymorphic `@class` type hints. Two problems
surfaced (confirmed via `AssetSearchTagCacheIntegrationTest`):
1. Enabling default typing mutates the `ObjectMapper` instance in place — even
   `ObjectMapper#copy()` only defers the problem.
2. Jackson writes the type hint differently for a top-level JSON object
   (`{"@class":...}`) vs. a top-level JSON array
   (`["java.util.ArrayList",[...]]`) — so the `tags` cache silently
   deserialized into the wrong shape and effectively never hit.

**The fix, and the pattern to follow for any new cache:**

```java
"your-new-cache", typedConfig(baseConfig, objectMapper, YourReturnType.class)
        .entryTtl(Duration.ofMinutes(N)),
```

or, for a generic return type (`List<T>`, `PaginatedResult<T>`), build the
`JavaType` explicitly:

```java
"your-new-cache", typedConfig(baseConfig, objectMapper,
        objectMapper.getTypeFactory().constructCollectionType(List.class, YourType.class))
        .entryTtl(Duration.ofMinutes(N)),
```

🔴 Flag any new cache added to `perCacheConfigurations` without going through
`typedConfig(...)` — it will inherit the fallback generic serializer and can
reproduce the `tags` cache incident.

🟡 Flag a new `@Cacheable(cacheNames = "...")` whose name isn't registered in
`perCacheConfigurations` at all — it silently falls back to the generic
serializer via `cacheDefaults(fallbackConfig)`, which works for simple types
but carries the same polymorphic-hint risk for anything collection-shaped.
Every cache name used by `@Cacheable` should have an explicit entry.

Current caches, for reference (`AppConfig.cacheManager()`):

| Cache | TTL | Backs |
|---|---|---|
| `home-stats` | 10 min | `GetHomeStatsUseCaseImpl` |
| `sub-folders` | 5 min | `GetSubFoldersUseCaseImpl` |
| `asset-exif` | 30 min | `GetAssetExifUseCaseImpl` |
| `assets` | 5 min | `GetAssetsUseCaseImpl.execute(AssetFilter)` |
| `tags` | 5 min | `ListTagsUseCaseImpl.execute(String query)` (only when `query` is null/blank, fixed key `all`) |

---

## 2. Key Prefixing & Eviction

🔴 Flag any new cache config that doesn't inherit `baseConfig`'s
`computePrefixWith(name -> name + ":")` — every cache key must be
`<cacheName>:<key>` (single colon), not Spring's default `<cacheName>::<key>`
(double colon). The single-colon prefix is what lets `assets` cache keys be
pattern-matched as `assets:{folderId}:*` for folder-scoped eviction.

🔴 Flag any new eviction code that uses Redis `KEYS` instead of a
cursor-based `SCAN`/`DEL`. `KEYS` blocks the whole Redis instance while it
scans the entire keyspace — unacceptable on a shared instance. The existing
pattern (`AssetSearchCachePort` / `AssetSearchCacheServiceAdapter`) is the
reference implementation; reuse it rather than writing a new eviction helper.

🟡 Flag a new cache key that doesn't follow the existing key-generation
pattern for its cache. `assets` is keyed `{folderId}:{sha256 of the remaining
filter fields}` via `AssetSearchCacheKeyGenerator` — a new query-result cache
scoped by folder should reuse that generator or follow the same shape, so a
single `assets:{folderId}:*` eviction sweep continues to catch every variant.

---

## 3. Cache Invalidation Triggers

A cache is only correct if something actually evicts it when the underlying
data changes. When adding a new `@Cacheable` use case, identify **every**
write path that can make its cached result stale, and wire up eviction for
each one — don't rely on TTL expiry alone unless staleness for up to the TTL
window is genuinely acceptable for that data.

The `assets` cache shows the project's two accepted invalidation shapes; pick
whichever fits the write path:

- **Event-driven (async, cross-instance):** an `@KafkaListener` reacts to a
  domain event already being published for other reasons. See
  `AssetSearchCacheInvalidationListener` on `asset.cataloged`/`asset.deleted`
  — appropriate when the write path already publishes an event and the
  eviction can tolerate the same latency as that event's delivery. Cross-check
  with `kafka-events-conventions` before adding a new listener.
- **Synchronous (in the same request):** the mutating use case calls the
  eviction port directly, e.g. `AddTagToAssetUseCaseImpl`/
  `RemoveTagFromAssetUseCaseImpl`/`BulkAddTagUseCaseImpl`/
  `BulkRemoveTagUseCaseImpl` call `AssetSearchCachePort.evictFolder(...)`
  synchronously because a tag mutation has no Kafka event of its own. The
  same four use cases also evict `tags:all` declaratively. Use this shape
  when the write path has no corresponding event, or when the caller needs
  the read-your-own-write guarantee immediately.

🔴 Flag a new `@Cacheable` use case with an identifiable write path (a
use case that inserts/updates/deletes the data it reads) that has **no**
corresponding eviction anywhere — this is a silent staleness bug, not just a
style nit, since the cache will keep serving pre-mutation data until the TTL
expires.

🟡 Flag a new synchronous eviction call bypassing `AssetSearchCachePort`
(or the equivalent port for a different cache) — write directly to a fresh
`SCAN`/`DEL` inline rather than reusing the shared port only if the existing
port's contract genuinely doesn't fit (e.g., evicting a single key vs. a
folder-scoped pattern); otherwise reuse it so there's one eviction
implementation to audit.

---

## 4. Fail-Open Error Handling

🔴 Flag any new Redis-backed code path (a `@Cacheable` use case, a manual
`RedisTemplate` call, a new dual-write mirror) that lets a Redis exception
propagate up and fail the request. The project's convention, applied
consistently across `redis-thumbnail-cache`, `redis-refresh-tokens`, and the
Spring Cache abstraction (`LoggingCacheErrorHandler`, registered via
`AppConfig implements CachingConfigurer` → `errorHandler()`), is: **catch,
log at `WARN`, fall back to the source of truth.** A Redis outage should
degrade performance, never availability.

```java
// Pattern for manual (non-@Cacheable) Redis access — see ThumbnailStorageServiceAdapter
try {
    return redisTemplate.opsForValue().get(key);
} catch (Exception e) {
    log.warn("Redis lookup failed for key={}, falling back to source: {}", key, e.getMessage());
    return null; // caller falls through to disk/DB read
}
```

For `@Cacheable`/`@CacheEvict` methods this is automatic once
`LoggingCacheErrorHandler` is registered — don't wrap the method body in its
own try/catch to "handle" a cache failure; that's already covered by the
`CacheErrorHandler` and duplicating it there only handles the get/put path
Spring already delegates to the error handler.

🟡 Flag a new Redis-backed feature with no kill switch when the existing
precedent has one — `photomanager.thumbnail-cache.enabled=false` lets the
thumbnail L2 cache be disabled entirely, reverting to disk-only. A new
cache doesn't strictly need its own flag (the fail-open behavior already
provides resilience), but a large new Redis dependency added for
performance rather than correctness is worth asking about explicitly if the
task doesn't already specify one.

---

## 5. Eviction Policy & TTL Sanity

🟡 Flag a new Redis usage that assumes unbounded key growth without either a
TTL or reliance on the shared instance's `allkeys-lru` eviction policy (the
policy the `redis-thumbnail-cache`/`redis-refresh-tokens`/rate-limiting
deployment is required to run with). If neither applies, the new usage can
grow the keyspace indefinitely.

🟢 When choosing a TTL for a new cache, use the existing table (§1) as a
reference for how volatile similar data is — `asset-exif` (rarely changes
after catalog) gets 30 min, while `assets`/`tags`/`sub-folders` (mutate more
often) get 5 min, and `home-stats` (dashboard aggregate, tolerant of minor
staleness) gets 10 min.

---

## Code Style Rules

- Never register a new named cache in `AppConfig.cacheManager()` without an
  explicit `typedConfig(...)` entry (§1).
- Never use `KEYS` for Redis eviction — always cursor-based `SCAN`/`DEL` via
  the shared `AssetSearchCachePort` pattern (§2).
- Every new Redis-backed write path for a `@Cacheable` use case needs an
  identified, wired-up eviction trigger — event-driven or synchronous (§3).
- Every new manual Redis call (not going through `@Cacheable`) must catch and
  log at `WARN`, never propagate (§4).
- Reuse the existing `RedisConnectionFactory` unless the new usage genuinely
  needs different serialization (raw bytes vs. JSON) or a different client
  (Lettuce-direct for `bucket4j`) — see `thumbnailRedisTemplate`'s Javadoc for
  why it's a separate, explicitly-named bean rather than reusing Spring
  Boot's auto-configured default `RedisTemplate<Object, Object>`.
