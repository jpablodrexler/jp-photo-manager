## ADDED Requirements

### Requirement: Every issued or rotated refresh token is mirrored into Redis

Whenever `RefreshTokenRepository.save(token)` is called with a non-revoked token, the system SHALL write the token to Redis in addition to the existing PostgreSQL write: a hash at key `refresh_token:{token}` containing `userId`, `tokenId`, and `issuedAt`, with a TTL equal to the remaining time until `expiresAt`. This mirroring SHALL NOT change what `findByToken`, `deleteByUserId`, or `deleteById` return in this phase — PostgreSQL remains the sole read source of truth.

#### Scenario: Issuing a new refresh token mirrors it into Redis
- **GIVEN** a user successfully logs in
- **WHEN** `RefreshTokenServiceImpl.issueRefreshToken` calls `RefreshTokenRepository.save()` with a new, non-revoked token
- **THEN** a PostgreSQL `refresh_tokens` row is inserted as before, AND a Redis hash `refresh_token:{token}` is created with `userId`, `tokenId`, and `issuedAt` fields and a TTL matching the token's remaining lifetime

#### Scenario: Rotating a refresh token mirrors the new token into Redis
- **GIVEN** a valid, non-revoked refresh token is submitted to `POST /api/auth/refresh`
- **WHEN** `RefreshTokenServiceImpl.validateAndRotate` issues a replacement token via `save()`
- **THEN** the new token is mirrored into Redis exactly as in the issuance scenario above

#### Scenario: Reads are unaffected by the Redis mirror
- **GIVEN** a refresh token exists in both PostgreSQL and Redis
- **WHEN** `RefreshTokenRepository.findByToken(token)` is called
- **THEN** the result is read from PostgreSQL exactly as before this change; the Redis mirror is not consulted

### Requirement: Revoking a token deletes its Redis mirror immediately

When `RefreshTokenRepository.save(token)` is called with `revoked = true` (the pattern used by `validateAndRotate` to invalidate the token being rotated away from), the system SHALL delete the token's Redis hash (`refresh_token:{token}`), its id-index entry (`refresh_token:id:{tokenId}`), and remove it from its user's set (`refresh_tokens:user:{userId}`), rather than writing a `revoked` field to Redis. The existing PostgreSQL row SHALL still be updated with `revoked = true` as before, unchanged.

#### Scenario: Rotating a token deletes the old token's Redis mirror
- **GIVEN** a refresh token exists in both PostgreSQL and Redis
- **WHEN** `RefreshTokenServiceImpl.validateAndRotate` marks the token `revoked = true` and calls `save()`
- **THEN** the PostgreSQL row for that token has `revoked = true` as before this change, AND the Redis hash, id-index entry, and user-set membership for that token are all removed

### Requirement: Each mirrored token has a Redis-native numeric id for future per-session revocation

The system SHALL generate a `tokenId` for each Redis-mirrored token using an atomic Redis sequence (`INCR` on `refresh_tokens:token_id_seq`), independent of the PostgreSQL `BIGSERIAL` id, and SHALL maintain a `refresh_token:id:{tokenId}` string key mapping the generated id back to the token value, with the same TTL as the token's hash.

#### Scenario: A newly issued token gets a Redis-native id
- **GIVEN** a new refresh token is being mirrored into Redis
- **WHEN** the mirror write executes
- **THEN** a new value is obtained from `INCR refresh_tokens:token_id_seq`, stored as the `tokenId` field in the token's hash, and a `refresh_token:id:{tokenId}` key is created pointing to the token value, with a TTL matching the token's remaining lifetime

### Requirement: Redis mirroring failures never fail a login or refresh request

If any Redis operation performed while mirroring a token write fails (e.g., the Redis connection is unavailable), the system SHALL log a warning and continue — the PostgreSQL write and the HTTP response SHALL proceed unaffected.

#### Scenario: Redis is unavailable during login
- **GIVEN** the Redis connection is down
- **WHEN** a user successfully logs in and a new refresh token is issued
- **THEN** the response is `200 OK` with both cookies set exactly as before this change; a warning is logged noting the Redis mirror write failed; the PostgreSQL `refresh_tokens` row is still created

#### Scenario: Redis is unavailable during token rotation
- **GIVEN** the Redis connection is down
- **WHEN** a valid refresh token is submitted to `POST /api/auth/refresh`
- **THEN** the response is `200 OK` with new cookies set exactly as before this change; a warning is logged noting the Redis mirror delete/write failed; the PostgreSQL rotation (old row revoked, new row inserted) still completes
