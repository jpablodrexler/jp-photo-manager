## Context

The `spring-boot-starter-cache` dependency is already in `pom.xml` but `@EnableCaching` has not been added and no `CacheManager` bean is defined. Without a `CacheManager`, `@Cacheable` annotations are no-ops. The three most expensive repeated queries are:

1. **Home stats** (`GetHomeStatsUseCase`): aggregates total asset count, total size, folder count, and recent assets — an expensive multi-table query run on every dashboard load.
2. **Sub-folder listings** (`GetSubFoldersUseCase`): reads the folder hierarchy for a given path, run on every folder navigation.
3. **EXIF data** (`GetAssetExifUseCase`): reads and parses EXIF metadata from the `asset_exif` table, run every time the viewer panel opens for an asset.

## Goals / Non-Goals

**Goals:**
- Enable Spring Cache with a Caffeine in-memory `CacheManager`
- Cache home stats, sub-folder listings, and EXIF data
- Evict caches on all write paths that invalidate the cached data

**Non-Goals:**
- Distributed caching (Redis, Hazelcast)
- Caching API-layer responses (belongs at the HTTP layer)
- Caching asset list queries (invalidation would be too complex)
- New Flyway migration

## Decisions

### 1. Caffeine over other cache providers

**Decision:** Use `com.github.ben-manes.caffeine:caffeine` as the cache implementation.

**Rationale:** Caffeine is the recommended in-memory implementation for Spring Cache in single-instance Spring Boot apps. It provides high-throughput caching with a W-TinyLFU eviction policy and requires zero new infrastructure. It is the Spring Boot auto-configuration default when present on the classpath.

**Alternative considered:** Redis via `spring-boot-starter-data-redis`. Rejected because it introduces a new infrastructure service and adds latency for a single-instance deployment.

### 2. Three cache names with separate TTLs

**Decision:** Define three named caches in `AppConfig`: `home-stats` (10-minute TTL, max 1 entry), `sub-folders` (5-minute TTL, max 500 entries), `asset-exif` (30-minute TTL, max 2000 entries).

**Rationale:** Different data has different volatility. Home stats change only on catalog/delete operations; 10 minutes is safe. Sub-folders change on catalog/move; 5 minutes prevents stale trees. EXIF data never changes after cataloging; 30 minutes reduces viewer panel latency.

### 3. `@CacheEvict` eviction boundaries

**Decision:** Evict caches on the following write paths:
- `CatalogAssetsUseCase`: evict `home-stats`, `sub-folders` (allEntries=true), `asset-exif` (allEntries=true)
- `SoftDeleteAssetUseCase` / `RestoreAssetUseCase`: evict `home-stats`
- `UpdateAssetRatingUseCase`: evict `home-stats`
- `MoveAssetsUseCase` / `CopyAssetsUseCase`: evict `home-stats`, `sub-folders` (allEntries=true)

**Rationale:** The TTL acts as a safety net for any missed eviction path. Evicting `allEntries=true` for `sub-folders` and `asset-exif` is necessary because at eviction time the specific cache keys affected by a catalog run are not known.

### 4. `@Cacheable` on use case implementations, not interfaces

**Decision:** Apply `@Cacheable` to the `@Service`-annotated implementation class methods, not to the port interface methods.

**Rationale:** Spring AOP proxies work on concrete beans. Annotating the interface has no effect; the annotation must be on the concrete class.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Missed `@CacheEvict` on a write path causes stale data | Medium | TTL provides a time-bounded safety net; document all eviction points |
| Cache grows unbounded if max-size is not set | Low | All caches have `maximumSize` configured |
| `@Cacheable` self-invocation bypass | Low | Only external calls through the Spring proxy trigger caching; document and avoid internal method calls between cached methods |
