# Feature 28 — server-side-spring-cache

Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required
