## Why

Several backend use cases run aggregation queries on every request that are expensive but change infrequently. Home statistics aggregate counts across the entire catalog; sub-folder listings scan the folder hierarchy; EXIF data is read from the database on every viewer open. Enabling Spring Cache with Caffeine and annotating these use cases with `@Cacheable` / `@CacheEvict` eliminates redundant database I/O without any new infrastructure.

## What Changes

- Add `com.github.ben-manes.caffeine:caffeine` to `pom.xml`
- Add `@EnableCaching` to `AppConfig` and define three named `CaffeineCache` beans: `home-stats`, `sub-folders`, `asset-exif`
- Annotate `GetHomeStatsUseCaseImpl`, `GetSubFoldersUseCaseImpl`, and `GetAssetExifUseCaseImpl` with `@Cacheable`
- Annotate write-path use cases (`CatalogAssetsUseCase`, `SoftDeleteAssetUseCase`, `UpdateAssetRatingUseCase`, `MoveAssetsUseCase`, `CopyAssetsUseCase`) with `@CacheEvict`

## Capabilities

### New Capabilities

- `server-side-spring-cache`: Home stats, sub-folder listings, and EXIF data are served from an in-memory Caffeine cache and evicted automatically when the underlying data changes.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `caffeine` dependency
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/AppConfig.java` — add `@EnableCaching`, `CacheManager` bean
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/` — add `@Cacheable` to three read use cases; add `@CacheEvict` to five write use cases
- `JPPhotoManagerWeb/backend/src/test/` — new cache behavior unit tests
