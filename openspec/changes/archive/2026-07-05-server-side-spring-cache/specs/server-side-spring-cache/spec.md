# server-side-spring-cache

Home statistics, sub-folder listings, and EXIF data are served from an in-memory Caffeine cache. Cache entries are populated on first access and evicted when the underlying data changes via write-path use cases.

---

## ADDED Requirements

### Requirement: Home statistics are served from cache after first load

The `GetHomeStatsUseCase` SHALL cache its result under the `home-stats` cache name. Subsequent calls SHALL return the cached value without executing a database query.

#### Scenario: Second stats request is served from cache

- **GIVEN** the `home-stats` cache is populated after an initial request
- **WHEN** `GetHomeStatsUseCase.execute()` is called again
- **THEN** the result is returned from cache without a database roundtrip

#### Scenario: Cache is evicted after cataloging

- **GIVEN** the `home-stats` cache is populated
- **WHEN** `CatalogAssetsUseCase` completes
- **THEN** the `home-stats` cache entry is evicted so the next request reflects the updated catalog

### Requirement: Sub-folder listings are cached per folder path

The `GetSubFoldersUseCase` SHALL cache its result keyed by `folderPath`. Each unique path is cached independently.

#### Scenario: Cached sub-folder listing is returned

- **GIVEN** the sub-folder listing for `/photos/vacation` is cached
- **WHEN** `GetSubFoldersUseCase.execute("/photos/vacation")` is called again
- **THEN** the cached result is returned without a database query

#### Scenario: Sub-folder cache is evicted on move

- **GIVEN** the sub-folder cache has entries for multiple paths
- **WHEN** `MoveAssetsUseCase` completes
- **THEN** all `sub-folders` cache entries are evicted

### Requirement: EXIF data is cached per asset

The `GetAssetExifUseCase` SHALL cache its result keyed by `assetId`. Cache entries persist for 30 minutes or until evicted by a catalog operation.

#### Scenario: Repeated EXIF panel opens use cached data

- **GIVEN** EXIF data for asset 42 is cached
- **WHEN** `GetAssetExifUseCase.execute(42)` is called again
- **THEN** the cached result is returned without a database query

### Requirement: `@EnableCaching` and Caffeine `CacheManager` are configured

The application SHALL define a `CacheManager` bean backed by Caffeine with named caches `home-stats`, `sub-folders`, and `asset-exif`, each with a configured TTL and maximum size.

#### Scenario: Spring context loads with cache configuration

- **WHEN** the Spring Boot application starts
- **THEN** a Caffeine-backed `CacheManager` bean is available with the three named caches registered
