## 1. Redis refresh-token store

- [x] 1.1 Create `infrastructure/persistence/redis/RedisRefreshTokenStore.java` (`@Component`, injects `StringRedisTemplate`) encapsulating the key scheme: `refresh_token:{token}` (hash: `userId`, `tokenId`, `issuedAt`), `refresh_tokens:user:{userId}` (set of token values), `refresh_token:id:{tokenId}` (string → token value), and `refresh_tokens:token_id_seq` (INCR counter)
- [x] 1.2 Implement `mirrorSave(RefreshToken token)`: computes TTL as `Duration.between(Instant.now(), token.getExpiresAt())` (clamped to a minimum of 1 second to avoid a negative `EXPIRE`), obtains a new `tokenId` via `INCR refresh_tokens:token_id_seq`, writes the hash via `opsForHash().putAll(...)`, sets the hash TTL via `expire(...)`, writes `refresh_token:id:{tokenId}` with the same TTL, and adds the token to `refresh_tokens:user:{userId}` via `opsForSet().add(...)`
- [x] 1.3 Implement `mirrorRevoke(String token, Long userId, Long tokenId)`: deletes `refresh_token:{token}`, deletes `refresh_token:id:{tokenId}` (if `tokenId` is known), and removes the token from `refresh_tokens:user:{userId}` via `opsForSet().remove(...)`
- [x] 1.4 Wrap every public method body in a `try/catch` around `DataAccessException` (Spring Data Redis's exception hierarchy, covers connection failures) that logs a `WARN` (`log.warn("Redis mirror write failed for refresh token: {}", e.getMessage())`) and returns normally — mirroring must never propagate an exception to the caller

## 2. Wire the dual-write into the existing adapter

- [x] 2.1 Inject `RedisRefreshTokenStore` into `infrastructure/persistence/adapter/RefreshTokenRepositoryImpl.java`
- [x] 2.2 In `save(RefreshToken token)`: after the existing JPA save completes and the domain object is mapped back, call `redisRefreshTokenStore.mirrorRevoke(...)` when `token.isRevoked()` is true, otherwise call `redisRefreshTokenStore.mirrorSave(...)` using the tokenId/user/expiresAt from the just-saved JPA-mapped domain object; return the same mapped domain object as before (no return-type change)
- [x] 2.3 Confirm `findByToken`, `deleteByUserId`, and `deleteById` are NOT modified to read from or mirror-delete in Redis yet — Phase 1 is write-only mirroring; reads stay on PostgreSQL exclusively (this task is a verification/no-op checkpoint, not a code change)

## 3. Testing

- [x] 3.1 Add `RedisRefreshTokenStoreTest` (unit, Mockito on `StringRedisTemplate`/`HashOperations`/`SetOperations`/`ValueOperations`) covering: `mirrorSave` writes the hash with correct fields and TTL, `mirrorSave` obtains a new id via `INCR` and writes the id-index key, `mirrorRevoke` deletes the hash/id-index/set-membership, and both methods swallow a simulated `DataAccessException` without throwing
- [x] 3.2 Extend `RefreshTokenRepositoryImplTest` (unit, Mockito) to verify `save()` calls `mirrorSave` on the Redis store when the token is not revoked, and `mirrorRevoke` when it is revoked, in addition to the existing JPA assertions; verify a Redis store exception does not prevent the method from returning normally
- [x] 3.3 Add/extend an integration test (extending `PostgresIntegrationTest`, with a Testcontainers Redis module or the existing local `redis` service) verifying that after `POST /api/auth/login`, a Redis hash exists at `refresh_token:{token}` with the expected fields and a positive TTL, and that after `POST /api/auth/refresh` the old token's Redis hash is gone while the new token's Redis hash exists

## 4. Documentation

- [x] 4.1 Update the `Persistence` section of `CLAUDE.md` (Web Architecture) and `JPPhotoManagerWeb/CLAUDE.md` to note that refresh tokens are now dual-written to PostgreSQL and Redis (PostgreSQL still authoritative for reads), referencing this as Phase 1 of the `redis-refresh-tokens` migration
- [x] 4.2 Add a short note to `openspec/improvements.md` (or leave for the archive step) indicating that #79's Deploy 2 (Redis-only reads/writes + `refresh_tokens` table drop) remains a separate, future change
