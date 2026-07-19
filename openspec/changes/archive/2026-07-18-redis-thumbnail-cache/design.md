## Context

`ThumbnailStorageServiceAdapter` is the only implementation of `ThumbnailPort` (`domain/port/out/ThumbnailPort.java`): `saveThumbnail`, `loadThumbnail`, `deleteThumbnail`, `thumbnailExists`, all operating on `{assetId}.bin` files under `photomanager.thumbnails-directory` (default `~/.photomanager/thumbnails`). `GetAssetThumbnailUseCaseImpl` calls `loadThumbnail(assetId + ".bin")` on every `GET /api/assets/{id}/thumbnail` request — the highest-volume read in the application, since every visible gallery cell issues one. `DeleteAssetsUseCaseImpl`, `PurgeAssetsUseCaseImpl`, and `PruneDeletedFoldersUseCaseImpl` all call `deleteThumbnail` when an asset or folder is removed.

Redis is already deployed and configured (`spring.data.redis.host`/`port` in `application.yml`, `spring-boot-starter-data-redis` on the classpath) and used in production by two other features:
- `redis-refresh-tokens` (#79) — `RedisRefreshTokenStore` mirrors refresh tokens via a `StringRedisTemplate`.
- `redis-distributed-rate-limiting` (#78) — a raw Lettuce `RedisClient` + `LettuceBasedProxyManager` for `bucket4j-redis` token buckets.

Thumbnail bytes are binary (JPEG), not strings, so neither existing Redis access pattern fits directly: `StringRedisTemplate` assumes UTF-8 string values, and the rate-limiter's raw Lettuce client is scoped to bucket4j's proxy manager. This design adds a third, narrowly-scoped Redis access path: a `RedisTemplate<String, byte[]>` dedicated to thumbnail bytes.

## Goals / Non-Goals

**Goals:**
- Serve repeat thumbnail requests from Redis with no disk I/O and no change to `ThumbnailPort`'s public contract.
- Keep the cache populated automatically on both the write path (`saveThumbnail`, called once per newly cataloged/regenerated asset) and the read-miss path (`loadThumbnail`, called whenever a requested key is not yet cached or has expired).
- Invalidate the cache entry the moment a thumbnail is deleted, from the single method (`deleteThumbnail`) that all three delete-path use cases already call — no use-case-level changes.
- Degrade gracefully: any Redis failure (connection refused, timeout) must fall back to the existing disk-only behavior and never surface as a user-facing error.

**Non-Goals:**
- No change to `ThumbnailPort`'s method signatures, callers, or the `{assetId}.bin` blob-naming convention.
- No change to `thumbnail-http-cache` (#26, browser `Cache-Control` header) or `server-side-spring-cache` (#28, Caffeine `@Cacheable` for home stats/sub-folders/EXIF) — this is an independent, additional tier.
- No cross-keyspace memory isolation from the rate-limiter and refresh-token Redis keys in this change; documented as a follow-up if contention is observed (see Risks).
- No cache warming/pre-population job; the cache fills lazily on first access after each restart.

## Decisions

**1. Cache logic lives inside `ThumbnailStorageServiceAdapter`, not a new decorator/proxy class.**
`ThumbnailPort` has exactly one implementation and three call sites, all already routed through it. Wrapping it in a `CachingThumbnailPort` decorator would add an indirection layer and a second Spring bean to wire, for no behavioral benefit — the adapter already owns "how thumbnails are stored," and the cache is just another storage tier. Alternative considered: a `@Cacheable`/`@CacheEvict` Spring Cache abstraction (matching #28's pattern) backed by a `RedisCacheManager`. Rejected because Spring Cache's default (de)serialization is tuned for cache *values as objects* (JSON/JDK serialization) and adds overhead for a hot path that already has raw bytes ready to write; a direct `RedisTemplate<String, byte[]>` call is simpler and faster for this one case. `server-side-spring-cache` (#28) and `redis-search-tag-cache` (#82) are better fits for `@Cacheable` because they cache derived query results, not raw byte payloads.

**2. Dedicated `RedisTemplate<String, byte[]>` bean with `StringRedisSerializer` (keys) and `RedisSerializer.byteArray()` (values).**
The Spring Boot auto-configured default `RedisTemplate<Object, Object>` uses JDK serialization for values, which would wrap the JPEG bytes in a `byte[]`-inside-`ObjectOutputStream` envelope — extra bytes and extra CPU for no gain, since the value is already a serialized byte payload. A dedicated bean with `RedisSerializer.byteArray()` stores exactly the bytes handed to it. This mirrors `RedisRefreshTokenStore`'s use of `StringRedisTemplate` (a pre-configured `RedisTemplate<String,String>`) — same idea, adapted for a `byte[]` value type that has no Spring Boot-provided equivalent.

**3. Cache key: `asset:thumbnail:{assetId}`, TTL: 24 hours, eviction: `allkeys-lru` (Redis-server-level config, not per-key).**
Thumbnails are content-addressed and immutable once generated (same rationale as `thumbnail-http-cache`'s permanent browser cache header) — the `assetId` is a stable, sufficient key. A 24-hour TTL bounds memory growth for a purely load-balancing/performance cache (unlike refresh tokens, losing a thumbnail cache entry costs one disk read, not a security or correctness issue), and pairs with `allkeys-lru` so popular thumbnails effectively stay resident under normal traffic even as the TTL nominally expires them (a subsequent access before eviction just re-populates via `SETEX`). `allkeys-lru` is configured at the Redis server/deployment level (`redis.conf` or the `docker-compose`/Kubernetes command args), consistent with how the existing Redis deployment is configured for #78/#79.

**4. Fail-open error handling: catch `DataAccessException` around every Redis call, log at `WARN`, fall through to disk.**
Exactly the pattern already established by `RedisRefreshTokenStore` (`mirrorSave`/`mirrorRevoke`). A thumbnail cache is a pure performance optimization; a Redis outage must never turn into a `500` for `GET /api/assets/{id}/thumbnail`.

**5. No new domain port method or interface change.**
`saveThumbnail`, `loadThumbnail`, `deleteThumbnail` keep their exact signatures. The Redis calls are added as private helper methods inside the adapter, invoked at the start (`loadThumbnail` cache check) or end (`saveThumbnail`/`deleteThumbnail` cache write/evict) of the existing method bodies.

**6. Configuration: new `photomanager.thumbnail-cache.enabled` (default `true`) and `photomanager.thumbnail-cache.ttl-seconds` (default `86400`) properties.**
Allows disabling the Redis tier entirely (e.g., for a local single-instance dev setup with no Redis running) without a code change or profile-specific bean exclusion — the adapter checks the flag before attempting any Redis call.

## Risks / Trade-offs

- **[Risk]** Thumbnail cache keys share the same Redis instance/keyspace as rate-limit buckets (#78) and refresh-token mirrors (#79); a burst of newly cataloged thumbnails could evict hot rate-limit or refresh-token keys under `allkeys-lru`, or vice versa. → **Mitigation**: documented as a known limitation in the proposal; if contention is observed in practice, move to a separate Redis logical DB (`SELECT` index) or a dedicated Redis instance — a configuration-only change, no application code changes needed, since the key namespace (`asset:thumbnail:*`) is already distinct.
- **[Risk]** A `RedisTemplate<String, byte[]>` bean must not collide with the auto-configured default `RedisTemplate<Object, Object>` bean name. → **Mitigation**: name the bean explicitly (e.g. `thumbnailRedisTemplate`) and inject it by name/qualifier in `ThumbnailStorageServiceAdapter`, following the existing explicit-bean-naming pattern already used for `taskExecutor` and `catalogTaskScheduler` in `AppConfig`.
- **[Risk]** Disabling the feature flag mid-flight (config reload) is not supported — the flag is read once via `@Value` at bean construction. → **Mitigation**: acceptable; this mirrors how every other `@Value`-configured property in the adapter already behaves (e.g. `thumbnails-directory`), and a flag flip is an intentional restart-time operational decision, not a runtime toggle.
- **[Trade-off]** Cache invalidation on delete adds one extra Redis round-trip to `deleteThumbnail`, executed synchronously. Given delete operations are already multi-step (DB update + disk file delete) and not on a latency-sensitive hot path, this is acceptable.

## Migration Plan

1. Add the `RedisTemplate<String, byte[]>` bean and the two new `application.yml` properties (`photomanager.thumbnail-cache.enabled`, `photomanager.thumbnail-cache.ttl-seconds`).
2. Update `ThumbnailStorageServiceAdapter` to consult/populate/evict the cache around the existing disk operations, guarded by the `enabled` flag.
3. Configure `allkeys-lru` on the Redis deployment used by the running environment (if not already set as the default policy from #78/#79's provisioning).
4. No data migration is required — the cache starts empty and fills lazily; there is no persisted state to backfill or drop.
5. **Rollback**: set `photomanager.thumbnail-cache.enabled: false` (or revert the deploy) — the adapter falls back to disk-only reads/writes exactly as before this change, with no schema or data cleanup needed since no PostgreSQL changes are involved.

## Open Questions

- Should `photomanager.thumbnail-cache.ttl-seconds` be tunable per environment (e.g., shorter in a memory-constrained dev Redis), or is a single hardcoded default acceptable for the first iteration? Current default (24h) is taken directly from the feature description in `openspec/features.md` (#81) and is treated as the accepted default unless operational experience says otherwise.
- If Redis keyspace contention with #78/#79 (see Risks) is observed after rollout, should the fix be a dedicated Redis logical DB, a dedicated Redis instance, or a `maxmemory`-per-keyspace policy? Deferred until real contention data exists.
