# server-side-spring-cache

Home statistics, sub-folder listings, EXIF data, paginated asset search results, and the tag list are served from a Redis-backed cache. Cache entries are populated on first access and evicted when the underlying data changes via write-path use cases or Kafka events.

---

## Requirements

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

### Requirement: `@EnableCaching` and Redis `CacheManager` are configured

The application SHALL define a `CacheManager` bean backed by Redis with named caches `home-stats`,
`sub-folders`, `asset-exif`, `assets`, and `tags`, each with a configured TTL. The bean SHALL use the
existing `RedisConnectionFactory` already provisioned for the refresh-token store and thumbnail
cache. Cache key prefixes SHALL be computed as `<cacheName>:` (single colon) so that folder-scoped
eviction can pattern-match on the `assets:` cache's keys. A `CacheErrorHandler` SHALL catch and log
(at `WARN`) any Redis error raised during a cache get/put/evict operation rather than propagating it,
so a Redis outage degrades to always querying the source of truth instead of failing the request.

#### Scenario: Spring context loads with Redis cache configuration

- **WHEN** the Spring Boot application starts
- **THEN** a Redis-backed `CacheManager` bean is available with the five named caches registered

#### Scenario: Cache read tolerates a Redis outage

- **GIVEN** Redis is unreachable
- **WHEN** a `@Cacheable`-annotated use case is invoked
- **THEN** the error is caught and logged at `WARN`, and the use case executes against its normal
  data source instead of raising an exception to the caller

### Requirement: Paginated asset search results are cached per folder

`GetAssetsUseCaseImpl.execute(AssetFilter)` SHALL cache its result under the `assets` cache, keyed by
a composite of the resolved `folderId` and a SHA-256 hash of the remaining filter fields (`search`,
`dateFrom`, `dateTo`, `minRating`, `sortCriteria`, `page`, `pageSize`, `includeDeleted`, `tags`), so
that the underlying Redis key is `assets:{folderId}:{hash}`. Each distinct filter combination for a
folder is cached independently.

#### Scenario: Repeated identical search is served from cache

- **GIVEN** a search for folder `/photos/vacation` with a given filter combination is cached
- **WHEN** `GetAssetsUseCaseImpl.execute()` is called again with the same `folderId` and filter values
- **THEN** the cached result is returned without a database query

#### Scenario: Different filter values produce different cache entries

- **GIVEN** a search for folder `/photos/vacation` page 0 is cached
- **WHEN** the same folder is searched again with page 1, or a different `minRating`
- **THEN** a distinct cache entry is populated and the database is queried for the new result

### Requirement: Asset search cache is invalidated per folder on catalog and delete events

An `@KafkaListener` on the `asset.cataloged` and `asset.deleted` topics SHALL evict every `assets`
cache entry belonging to the affected folder (all keys matching `assets:{folderId}:*`) using a
cursor-based `SCAN`, never a blocking `KEYS` command. The listener SHALL run under its own
persistent, explicit consumer group so exactly one instance processes each event when multiple
backend replicas are running.

#### Scenario: Cataloging a new asset invalidates that folder's search cache

- **GIVEN** the `assets` cache has entries for folder `/photos/vacation`
- **WHEN** an `asset.cataloged` event for a new asset in `/photos/vacation` is consumed
- **THEN** every `assets:{folderId}:*` entry for that folder's `folderId` is evicted

#### Scenario: Deleting an asset invalidates that folder's search cache

- **GIVEN** the `assets` cache has entries for the folder containing the deleted asset
- **WHEN** an `asset.deleted` event is consumed
- **THEN** every `assets` cache entry for that event's `folderId` is evicted

#### Scenario: Invalidating one folder does not affect another folder's cache entries

- **GIVEN** the `assets` cache has entries for both folder A and folder B
- **WHEN** an `asset.cataloged` or `asset.deleted` event for folder A is consumed
- **THEN** folder B's cache entries remain populated and are not evicted

### Requirement: Unfiltered tag list is cached

`ListTagsUseCaseImpl.execute(String query)` SHALL cache its result under the `tags` cache with the
fixed key `all`, but only when `query` is `null` or blank. Calls with a non-blank `query` SHALL
bypass the cache entirely (neither read nor populate it).

#### Scenario: Repeated unfiltered tag list request is served from cache

- **GIVEN** the `tags` cache is populated after an initial unfiltered request
- **WHEN** `ListTagsUseCaseImpl.execute(null)` (or an empty/blank query) is called again
- **THEN** the cached result is returned without a database query

#### Scenario: Prefix-filtered tag search bypasses the cache

- **GIVEN** the `tags` cache is populated for the unfiltered case
- **WHEN** `ListTagsUseCaseImpl.execute("vac")` is called
- **THEN** the repository is queried directly and the result is not read from or written to the cache

### Requirement: Tag cache is evicted on any tag mutation

`AddTagToAssetUseCaseImpl.execute` and `RemoveTagFromAssetUseCaseImpl.execute` SHALL each evict the
`tags` cache entry keyed `all` upon successful completion.

#### Scenario: Adding a tag evicts the cached tag list

- **GIVEN** the `tags` cache is populated
- **WHEN** `AddTagToAssetUseCaseImpl.execute()` successfully adds a tag to an asset
- **THEN** the `tags` cache entry keyed `all` is evicted

#### Scenario: Removing a tag evicts the cached tag list

- **GIVEN** the `tags` cache is populated
- **WHEN** `RemoveTagFromAssetUseCaseImpl.execute()` successfully removes a tag from an asset
- **THEN** the `tags` cache entry keyed `all` is evicted
