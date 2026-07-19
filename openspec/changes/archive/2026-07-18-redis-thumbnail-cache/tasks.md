## 1. Configuration

- [x] 1.1 Add `photomanager.thumbnail-cache.enabled` (default `true`) and `photomanager.thumbnail-cache.ttl-seconds` (default `86400`) properties to `application.yml` (and `application-test.yml` if a different default is needed for tests, e.g. `enabled: false` to keep unit tests Redis-free by default).
- [x] 1.2 Add a `thumbnailRedisTemplate` `RedisTemplate<String, byte[]>` `@Bean` (explicit bean name) in `config/AppConfig.java` (or a new `config/RedisThumbnailCacheConfig.java` if `AppConfig` is already large), configured with `StringRedisSerializer` for keys and `RedisSerializer.byteArray()` for values, backed by the existing auto-configured `RedisConnectionFactory`.
- [x] 1.3 Document the `allkeys-lru` eviction policy expectation and the two new properties in `JPPhotoManagerWeb/CLAUDE.md`'s configuration table (or confirm it's already covered by the existing Redis provisioning notes from #78/#79).

## 2. Adapter implementation

- [x] 2.1 Inject `thumbnailRedisTemplate`, `photomanager.thumbnail-cache.enabled`, and `photomanager.thumbnail-cache.ttl-seconds` into `ThumbnailStorageServiceAdapter`.
- [x] 2.2 Add a private helper to derive the Redis key `asset:thumbnail:{assetId}` from a `blobName` (strip the `.bin` suffix), consistent with the existing `{assetId}.bin` blob-naming convention.
- [x] 2.3 Update `loadThumbnail(blobName)`: when caching is enabled, attempt a Redis `GET` first; on a hit return the cached bytes with no disk read; on a miss (or when caching is disabled), fall back to the existing disk read, and on a successful disk read populate Redis via `SETEX` with the configured TTL.
- [x] 2.4 Update `saveThumbnail(blobName, data)`: keep the existing disk write, then (when enabled) also write to Redis via `SETEX` with the configured TTL.
- [x] 2.5 Update `deleteThumbnail(blobName)`: keep the existing disk delete, then (when enabled) issue a Redis `DEL` for the derived key.
- [x] 2.6 Wrap every Redis call added in 2.3–2.5 in a try/catch for `DataAccessException`, logging at `WARN` and falling through to disk-only behavior, mirroring `RedisRefreshTokenStore`'s pattern.
- [x] 2.7 Confirm `thumbnailExists(blobName)` is intentionally left disk-only (not cached) per the design — no change needed there.

## 3. Tests

- [x] 3.1 Unit test `ThumbnailStorageServiceAdapterTest`: `loadThumbnail` returns cached bytes from Redis without touching disk when a cache entry exists.
- [x] 3.2 Unit test: `loadThumbnail` falls back to disk on a cache miss and populates Redis with the disk bytes and the configured TTL.
- [x] 3.3 Unit test: `loadThumbnail` returns `null` and writes nothing to Redis when both the cache and disk are empty.
- [x] 3.4 Unit test: `saveThumbnail` writes to both disk and Redis.
- [x] 3.5 Unit test: `deleteThumbnail` deletes the disk file and issues a Redis `DEL`, including the case where no Redis entry exists (no error).
- [x] 3.6 Unit test: each of `loadThumbnail`, `saveThumbnail`, `deleteThumbnail` falls back to disk-only behavior without throwing when the injected `RedisTemplate` throws `DataAccessException`.
- [x] 3.7 Unit test: `photomanager.thumbnail-cache.enabled=false` causes all three methods to skip Redis entirely (verify no interaction with the mocked `RedisTemplate`).
- [x] 3.8 Run `mvn test` from `JPPhotoManagerWeb/backend` and confirm all new and existing tests pass.

## 4. Verification

- [x] 4.1 Manually verify against a local Redis instance (`docker run -d --name photomanager-redis -p 6379:6379 redis:7-alpine`) that requesting the same thumbnail twice results in a Redis cache hit on the second request (e.g. via `redis-cli GET asset:thumbnail:{id}` and `redis-cli TTL asset:thumbnail:{id}`).
- [x] 4.2 Manually verify that deleting an asset (or purging, or pruning a deleted folder) removes the corresponding `asset:thumbnail:{id}` key from Redis.
- [x] 4.3 Manually verify graceful degradation by stopping the local Redis container and confirming `GET /api/assets/{id}/thumbnail` still succeeds (served from disk) with a `WARN` log line.
