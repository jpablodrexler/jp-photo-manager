# Feature 81 — redis-thumbnail-cache

**As a** administrator, **I want** a shared Redis cache in front of the thumbnail storage, **so that** thumbnails load without disk I/O and without relying on a co-located disk in a load-balanced setup.

## Acceptance Criteria
- AC1: Given a cached thumbnail, when it is requested, then it is returned without disk I/O.
- AC2: Given a cache miss, when it is requested, then it is read from disk, stored with a 24-hour TTL and returned; a miss on disk too returns nothing.
- AC3: Given a thumbnail is saved, when saving completes, then the cache is populated immediately.
- AC4: Given a thumbnail is deleted, when deletion completes, then the disk file and the cache entry are removed, and a missing cache entry causes no error.
- AC5: Given Redis is unreachable, when loading, saving or deleting, then the disk operation still succeeds.
- AC6: Given caching is disabled, when thumbnails are used, then no Redis call is made.

## Technical notes

Add Redis as a shared L2 thumbnail cache in front of the disk-backed `ThumbnailStorageServiceAdapter`; on a cache hit `GET asset:thumbnail:{assetId}` returns the thumbnail bytes with no disk I/O; on a miss the adapter reads from disk, stores with a 24-hour TTL via `SETEX`, and returns; Redis `allkeys-lru` eviction retains popular thumbnails and evicts cold ones automatically; complements `thumbnail-http-cache` (#26) (browser-level `Cache-Control: immutable`, already implemented) and `server-side-spring-cache` (#28) (per-JVM Caffeine for home stats and EXIF lookups, already implemented); Redis adds the shared server-side tier that survives instance restarts and eliminates dependency on co-located disk access in load-balanced deployments where the requested thumbnail may reside on a different node's filesystem

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
