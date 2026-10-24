# Feature 28 — server-side-spring-cache

**As a** maintainer of the project, **I want** server-side caching of rarely changing data, **so that** the database is not hit with the same aggregate queries repeatedly.

## Acceptance Criteria
- AC1: Given home statistics, sub-folder listings and EXIF lookups, when they are requested again, then they are served from the cache.
- AC2: Given cataloging or a move, when it completes, then the related cache entries are evicted.
- AC3: Given the application, when the context loads, then the cache configuration is valid and no migration is needed.

## Technical notes

Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
