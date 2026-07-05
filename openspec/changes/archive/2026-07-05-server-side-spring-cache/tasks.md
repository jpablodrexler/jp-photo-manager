## 1. Maven dependency

- [x] 1.1 Add `com.github.ben-manes.caffeine:caffeine` to `pom.xml` (latest stable version compatible with Spring Boot 3.4)

## 2. Cache configuration in AppConfig

- [x] 2.1 Add `@EnableCaching` to `AppConfig`
- [x] 2.2 Define a `CacheManager` bean using `CaffeineCacheManager` with three named caches and individual specs:
  - `home-stats`: `Caffeine.newBuilder().maximumSize(1).expireAfterWrite(10, TimeUnit.MINUTES).build()`
  - `sub-folders`: `Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5, TimeUnit.MINUTES).build()`
  - `asset-exif`: `Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build()`

## 3. Annotate read use cases with @Cacheable

- [x] 3.1 Add `@Cacheable("home-stats")` to `GetHomeStatsUseCaseImpl.execute()`
- [x] 3.2 Add `@Cacheable(value = "sub-folders", key = "#folderPath")` to `GetSubFoldersUseCaseImpl.execute(String folderPath)` — actual parameter name in this codebase is `parentPath`. `GET /api/folders` is called with no `parentPath` (root folder listing) via `@RequestParam(required = false)`, and a plain `key = "#parentPath"` evaluates to a null SpEL key, which Spring's cache abstraction rejects with `IllegalArgumentException: Null key returned for cache operation`. Implemented as `key = "#parentPath ?: ''"` to substitute an empty-string key for the root-listing case (caught and fixed during code review; regression tests added in `GetSubFoldersUseCaseCachingTest`)
- [x] 3.3 Add `@Cacheable(value = "asset-exif", key = "#assetId")` to `GetAssetExifUseCaseImpl.execute(Long assetId)`

## 4. Annotate write use cases with @CacheEvict

- [x] 4.1 Add `@CacheEvict(value = {"home-stats", "sub-folders", "asset-exif"}, allEntries = true)` to `CatalogAssetsUseCaseImpl.execute()`
- [x] 4.2 Add `@CacheEvict("home-stats")` to `SoftDeleteAssetUseCaseImpl.execute()` — this codebase has no separate soft/permanent delete use case; implemented on `DeleteAssetsUseCaseImpl.execute(Long[], boolean permanently)`, which handles both. Also evicts `asset-exif` (`allEntries = true`): found during security review that a permanent purge deletes the asset's MongoDB EXIF document (`assetExifRepository.deleteByAssetId`) but `@Cacheable` skips the use case's existence check entirely on a cache hit, so without this eviction a caller could keep retrieving a purged asset's (potentially GPS-bearing) EXIF data from cache for up to the 30-minute TTL
- [x] 4.3 Add `@CacheEvict("home-stats")` to `RestoreAssetUseCaseImpl.execute()` — actual class name is `RestoreAssetsUseCaseImpl`
- [x] 4.4 Add `@CacheEvict("home-stats")` to `UpdateAssetRatingUseCaseImpl.execute()` — actual class name is `RateAssetUseCaseImpl`
- [x] 4.5 Add `@CacheEvict(value = {"home-stats", "sub-folders"}, allEntries = true)` to `MoveAssetsUseCaseImpl.execute()`
- [x] 4.6 Add `@CacheEvict(value = {"home-stats", "sub-folders"}, allEntries = true)` to `CopyAssetsUseCaseImpl.execute()` — this codebase has no separate copy use case; `MoveAssetsUseCaseImpl.execute(Long[], String, boolean preserveOriginal)` already handles both move and copy (preserveOriginal=true), so task 4.5's single annotation covers both

## 5. Unit tests

- [x] 5.1 Add a unit test that calls `GetHomeStatsUseCaseImpl.execute()` twice and verifies the underlying repository method is called only once (second call served from cache)
- [x] 5.2 Add a unit test that calls `GetHomeStatsUseCaseImpl.execute()`, then `CatalogAssetsUseCaseImpl.execute()`, then `GetHomeStatsUseCaseImpl.execute()` again, and verifies the repository is called twice (cache evicted after catalog)

## 6. Testing and Commit

- [x] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test` — 566 tests passed, 2 skipped
- [x] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test` — 324 tests passed
- [ ] 6.3 Commit all changes (only after both test suites pass) — deferred; commits are not made automatically by this workflow
