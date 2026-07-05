## Why

JWT refresh tokens are currently stored in the `refresh_tokens` PostgreSQL table, which is being used purely as a TTL key-value store: every read is a lookup by opaque token value, every row naturally expires after 30 days, and revocation just flips a boolean or deletes the row. PostgreSQL charges a full `SELECT`/`UPDATE` round-trip (~5 ms) for what Redis serves in ~0.1 ms with a native `GET`/`EXPIRE`, and Redis's TTL mechanism removes the need for any background cleanup job for expired tokens. Moving refresh tokens to Redis also makes revocation an atomic `DEL` instead of an `UPDATE ... SET revoked = true`.

## What Changes

- Add a Redis-backed mirror of every refresh-token write so `refresh_token:{token}` (a Redis hash with `userId`, `tokenId`, `issuedAt`, TTL = remaining lifetime) is populated for every token issued or rotated from this deploy onward, alongside the existing PostgreSQL row. This is **Deploy 1** of a two-deploy migration (see design.md) — PostgreSQL remains the read source of truth during this phase, so no user-visible behavior changes.
- Add a Redis set `refresh_tokens:user:{userId}` mirroring which tokens belong to a user, enabling a future Redis-only `deleteByUserId`.
- Add a Redis string `refresh_token:id:{tokenId}` mirroring token-by-id lookups (`tokenId` generated via a Redis `INCR` sequence), enabling a future Redis-only `deleteById` — this keeps the door open for `session-management` (#46)'s planned `DELETE /api/auth/sessions/{id}` endpoint without requiring a numeric database identity.
- No change to the `RefreshTokenRepository` domain port, the `RefreshToken` domain model, `RefreshTokenServiceImpl`, `AuthController`, or any HTTP request/response shape — this change only adds a mirrored write path inside `RefreshTokenRepositoryImpl`.
- **Not included in this change** (deferred to a follow-up change once the 30-day dual-write observation window has passed, mirroring the two-deploy pattern already used for `mongodb-exif-store`, #72): switching reads to Redis-only, writing to Redis only, and the Flyway migration that drops the `refresh_tokens` PostgreSQL table.
- No new Maven dependency and no `docker-compose.yml` change — `spring-boot-starter-data-redis` and the `redis` service were already introduced by `redis-distributed-rate-limiting` (#39) and are reused as-is.

## Capabilities

### New Capabilities
- `redis-refresh-tokens`: dual-write mirroring of refresh tokens into Redis (token hash, user-to-tokens set, id-to-token index) alongside the existing PostgreSQL persistence, as the first phase of migrating refresh-token storage off PostgreSQL.

### Modified Capabilities
(none — the `refresh-token` capability's observable behavior, as described in `openspec/specs/refresh-token/spec.md`, is unchanged in this phase: PostgreSQL remains the read path and the sole source of truth. The `refresh-token` spec will gain a MODIFIED delta in the follow-up change that cuts reads over to Redis and drops the PostgreSQL table.)

## Impact

- **Backend**: `infrastructure/persistence/adapter/RefreshTokenRepositoryImpl.java` (gains Redis mirroring calls alongside the existing JPA calls), new `infrastructure/persistence/redis/RedisRefreshTokenStore.java` (encapsulates the Redis key scheme: hash, set, id-index, TTL), no change to `domain/port/out/RefreshTokenRepository.java` or `domain/model/RefreshToken.java`.
- **Infrastructure**: none — Redis is already provisioned in `docker-compose.yml` and `application.yml` (`spring.data.redis.host`/`port`) by `redis-distributed-rate-limiting` (#39).
- **Testing**: unit tests for `RefreshTokenRepositoryImpl` extended to cover the mirrored Redis writes (Mockito on a `StringRedisTemplate`/`RedisRefreshTokenStore` mock); new unit tests for `RedisRefreshTokenStore` covering key construction, TTL computation, and the user-set/id-index bookkeeping.
- **Future work (out of scope here)**: a follow-up change flips reads to Redis-only, then a Flyway migration (V32 or later, after the currently-planned V28–V31 migrations) drops the `refresh_tokens` PostgreSQL table once row-count/behavioral parity has been observed for one full refresh-token lifetime (30 days).
