# Feature 82 — redis-search-tag-cache

**As a** administrator, **I want** gallery search results and the tag list cached in Redis, **so that** the hottest gallery endpoints respond fast and stay correct across instances.

## Acceptance Criteria
- AC1: Given a repeated identical search, when it runs, then it is served from cache for 5 minutes; different filter values give different entries.
- AC2: Given a new asset is cataloged or one is deleted, when the event arrives, then that folder's search cache is invalidated and other folders' entries are untouched.
- AC3: Given the unfiltered tag list, when it is requested again, then it is served from cache; a prefix-filtered tag search bypasses the cache.
- AC4: Given a tag is added or removed, when the change applies, then the cached tag list is evicted.
- AC5: Given the application context, when it loads, then the Redis cache configuration is valid.
- AC6: Given a Redis outage, when a cache read happens, then it is tolerated.

## Technical notes

Cache the two highest-frequency gallery read paths in Redis: (1) paginated asset search results (`GET /api/assets`) keyed by `assets:{sha256(folderPath+page+sort+filters)}` with a 5-minute TTL, invalidated on `asset.cataloged` and `asset.deleted` Kafka events from #75; (2) the tag list with counts (`GET /api/tags`) keyed `tags:all` with a 5-minute TTL, invalidated on `AddTagToAssetUseCase` and `RemoveTagFromAssetUseCase`; extends `server-side-spring-cache` (#28) which targets home stats, folder tree, and EXIF lookups with per-JVM Caffeine — this feature covers the two hottest gallery endpoints and uses Redis for distributed invalidation that works correctly across multiple instances; the `@Cacheable`/`@CacheEvict` annotations from #28 can be reused by switching the Spring cache manager from `CaffeineCacheManager` to a Lettuce-backed `RedisCacheManager`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
