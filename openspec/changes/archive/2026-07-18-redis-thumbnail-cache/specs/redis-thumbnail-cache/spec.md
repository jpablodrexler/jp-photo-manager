## ADDED Requirements

### Requirement: Thumbnail reads are served from Redis when cached
`ThumbnailStorageServiceAdapter.loadThumbnail(blobName)` SHALL first attempt to read the thumbnail bytes from Redis at key `asset:thumbnail:{assetId}` (derived from `blobName`) before touching the disk-backed thumbnail store. When the key exists, the cached bytes SHALL be returned with no filesystem read.

#### Scenario: Cached thumbnail is returned without disk I/O
- **GIVEN** thumbnail bytes for asset 42 are present in Redis at `asset:thumbnail:42`
- **WHEN** `loadThumbnail("42.bin")` is called
- **THEN** the Redis-cached bytes are returned and no read of `42.bin` from the disk-backed thumbnails directory occurs

### Requirement: Thumbnail reads fall back to disk on a cache miss and repopulate the cache
When `asset:thumbnail:{assetId}` does not exist in Redis, `loadThumbnail` SHALL read the thumbnail from disk exactly as before this change, and SHALL then write the bytes into Redis with a 24-hour TTL via `SETEX` so the next request for the same asset is served from cache.

#### Scenario: Cache miss reads from disk and populates the cache
- **GIVEN** `asset:thumbnail:77` does not exist in Redis
- **AND** the file `77.bin` exists in the disk-backed thumbnails directory
- **WHEN** `loadThumbnail("77.bin")` is called
- **THEN** the returned bytes match the contents of `77.bin`
- **AND** `asset:thumbnail:77` is subsequently present in Redis with a 24-hour TTL and the same byte contents

#### Scenario: Cache miss and disk miss both return no data
- **GIVEN** neither `asset:thumbnail:999` in Redis nor `999.bin` on disk exist
- **WHEN** `loadThumbnail("999.bin")` is called
- **THEN** the method returns `null`
- **AND** no entry is written to Redis

### Requirement: Newly saved thumbnails are written to both disk and Redis
`ThumbnailStorageServiceAdapter.saveThumbnail(blobName, data)` SHALL write the thumbnail bytes to disk exactly as before this change, and SHALL additionally write the same bytes to Redis at `asset:thumbnail:{assetId}` with a 24-hour TTL, so a freshly generated thumbnail is immediately servable from cache without requiring a first cache-miss read.

#### Scenario: Saving a thumbnail populates the cache immediately
- **WHEN** `saveThumbnail("55.bin", data)` is called
- **THEN** the file `55.bin` is written to the disk-backed thumbnails directory with `data`
- **AND** `asset:thumbnail:55` is present in Redis with `data` and a 24-hour TTL

### Requirement: Deleting a thumbnail evicts its Redis cache entry
`ThumbnailStorageServiceAdapter.deleteThumbnail(blobName)` SHALL delete the disk file exactly as before this change, and SHALL additionally issue a Redis `DEL` for `asset:thumbnail:{assetId}` so a deleted thumbnail is never subsequently served from a stale cache entry. This applies uniformly regardless of which use case (asset deletion, purge, or deleted-folder pruning) triggered the call, since all three call this single adapter method.

#### Scenario: Deleting a thumbnail removes both the disk file and the cache entry
- **GIVEN** `asset:thumbnail:88` exists in Redis and `88.bin` exists on disk
- **WHEN** `deleteThumbnail("88.bin")` is called
- **THEN** `88.bin` no longer exists on disk
- **AND** `asset:thumbnail:88` no longer exists in Redis

#### Scenario: Deleting a thumbnail with no cache entry does not error
- **GIVEN** `asset:thumbnail:100` does not exist in Redis but `100.bin` exists on disk
- **WHEN** `deleteThumbnail("100.bin")` is called
- **THEN** `100.bin` no longer exists on disk
- **AND** no error is raised as a result of the Redis `DEL` targeting a non-existent key

### Requirement: Redis unavailability degrades gracefully to disk-only behavior
Every Redis operation performed by the thumbnail cache SHALL be wrapped so that a Redis connection failure or timeout is caught, logged at `WARN`, and never propagated to the caller. `loadThumbnail`, `saveThumbnail`, and `deleteThumbnail` SHALL continue to behave exactly as they did before this change (disk-only) whenever Redis is unavailable.

#### Scenario: Thumbnail load succeeds from disk when Redis is unreachable
- **GIVEN** Redis is unreachable
- **AND** `33.bin` exists on disk
- **WHEN** `loadThumbnail("33.bin")` is called
- **THEN** the bytes from `33.bin` are returned
- **AND** no exception is thrown

#### Scenario: Thumbnail save succeeds on disk when Redis is unreachable
- **GIVEN** Redis is unreachable
- **WHEN** `saveThumbnail("66.bin", data)` is called
- **THEN** `66.bin` is written to disk with `data`
- **AND** no exception is thrown

#### Scenario: Thumbnail delete succeeds on disk when Redis is unreachable
- **GIVEN** Redis is unreachable
- **AND** `44.bin` exists on disk
- **WHEN** `deleteThumbnail("44.bin")` is called
- **THEN** `44.bin` no longer exists on disk
- **AND** no exception is thrown

### Requirement: Thumbnail caching can be disabled via configuration
The application SHALL expose a `photomanager.thumbnail-cache.enabled` configuration property (default `true`). When set to `false`, `ThumbnailStorageServiceAdapter` SHALL skip all Redis operations and behave exactly as the pre-change disk-only implementation.

#### Scenario: Caching disabled skips all Redis calls
- **GIVEN** `photomanager.thumbnail-cache.enabled` is set to `false`
- **WHEN** `loadThumbnail`, `saveThumbnail`, or `deleteThumbnail` are called
- **THEN** no Redis operations are attempted
- **AND** each method behaves identically to the disk-only implementation that existed before this change
