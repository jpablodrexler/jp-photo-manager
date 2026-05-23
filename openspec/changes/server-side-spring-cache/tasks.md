## 1. Maven dependency

- [ ] 1.1 Add `com.github.ben-manes.caffeine:caffeine` to `pom.xml` (latest stable version compatible with Spring Boot 3.4)

## 2. Cache configuration in AppConfig

- [ ] 2.1 Add `@EnableCaching` to `AppConfig`
- [ ] 2.2 Define a `CacheManager` bean using `CaffeineCacheManager` with three named caches and individual specs:
  - `home-stats`: `Caffeine.newBuilder().maximumSize(1).expireAfterWrite(10, TimeUnit.MINUTES).build()`
  - `sub-folders`: `Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5, TimeUnit.MINUTES).build()`
  - `asset-exif`: `Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build()`

## 3. Annotate read use cases with @Cacheable

- [ ] 3.1 Add `@Cacheable("home-stats")` to `GetHomeStatsUseCaseImpl.execute()`
- [ ] 3.2 Add `@Cacheable(value = "sub-folders", key = "#folderPath")` to `GetSubFoldersUseCaseImpl.execute(String folderPath)`
- [ ] 3.3 Add `@Cacheable(value = "asset-exif", key = "#assetId")` to `GetAssetExifUseCaseImpl.execute(Long assetId)`

## 4. Annotate write use cases with @CacheEvict

- [ ] 4.1 Add `@CacheEvict(value = {"home-stats", "sub-folders", "asset-exif"}, allEntries = true)` to `CatalogAssetsUseCaseImpl.execute()`
- [ ] 4.2 Add `@CacheEvict("home-stats")` to `SoftDeleteAssetUseCaseImpl.execute()`
- [ ] 4.3 Add `@CacheEvict("home-stats")` to `RestoreAssetUseCaseImpl.execute()`
- [ ] 4.4 Add `@CacheEvict("home-stats")` to `UpdateAssetRatingUseCaseImpl.execute()`
- [ ] 4.5 Add `@CacheEvict(value = {"home-stats", "sub-folders"}, allEntries = true)` to `MoveAssetsUseCaseImpl.execute()`
- [ ] 4.6 Add `@CacheEvict(value = {"home-stats", "sub-folders"}, allEntries = true)` to `CopyAssetsUseCaseImpl.execute()`

## 5. Unit tests

- [ ] 5.1 Add a unit test that calls `GetHomeStatsUseCaseImpl.execute()` twice and verifies the underlying repository method is called only once (second call served from cache)
- [ ] 5.2 Add a unit test that calls `GetHomeStatsUseCaseImpl.execute()`, then `CatalogAssetsUseCaseImpl.execute()`, then `GetHomeStatsUseCaseImpl.execute()` again, and verifies the repository is called twice (cache evicted after catalog)

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
