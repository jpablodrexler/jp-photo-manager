# Feature 79 — redis-refresh-tokens

**As a** administrator, **I want** refresh tokens mirrored into Redis with native expiry, **so that** token lookups get faster and expiry needs no cleanup job.

## Acceptance Criteria
- AC1: Given a refresh token is issued or rotated, when it is saved, then it is also mirrored into Redis with a 30-day TTL, and the old token's mirror is deleted on rotation.
- AC2: Given reads, when tokens are looked up, then PostgreSQL stays authoritative and reads are unaffected.
- AC3: Given a new token, when it is issued, then it gets a Redis-native id.
- AC4: Given Redis is unavailable, when login or rotation happens, then it still succeeds.
- AC5: Given the second deploy, when it is planned, then Redis-only reads and dropping the table remain a separate change after a 30-day observation window.

## Technical notes

Move JWT refresh token storage from the `refresh_tokens` PostgreSQL table to Redis keys with native TTL (`SET refresh_token:{tokenId} {userId} EX 2592000`); PostgreSQL is being used as a TTL key-value store — a pattern Redis is purpose-built for; token lookup drops from a SQL `SELECT` (~5 ms) to a Redis `GET` (~0.1 ms); Redis auto-expires tokens after 30 days eliminating the background cleanup job implied by the `expires_at` column; revocation is an atomic `DEL`; a `RedisRefreshTokenRepositoryImpl` adapter implements the existing `RefreshTokenRepository` port; the `refresh_tokens` PostgreSQL table is dropped after a dual-write migration window; the `user_agent` column planned by `session-management` (#46) must instead be stored as a Redis hash field (`HSET refresh_token:{tokenId} userId {u} userAgent {ua}`) — implement #46 and #79 together to avoid PostgreSQL column churn. **Deploy 1 (dual-write mirroring, PostgreSQL still authoritative for reads) is implemented via the `redis-refresh-tokens` change; Deploy 2 (Redis-only reads/writes + the `refresh_tokens` table drop) remains a separate, future change**, gated on a 30-day dual-write observation window per the change's design doc

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
