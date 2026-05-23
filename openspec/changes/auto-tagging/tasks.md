## 1. AutoTaggingService

- [ ] 1.1 Create `application/service/AutoTaggingService.java` annotated with `@Service`
- [ ] 1.2 Implement `List<String> deriveTags(AssetExif exif, Optional<String> geocodedCity, Optional<String> geocodedCountry)`:
  - If `exif.dateTaken()` is non-null, add `String.valueOf(exif.dateTaken().getYear())`
  - If `exif.cameraMake()` is non-null and non-blank, add `exif.cameraMake().trim().toLowerCase().split("\\s+")[0]`
  - If `geocodedCity` is present and non-blank, add it lowercased
  - If `geocodedCountry` is present and non-blank, add it lowercased
  - Return deduplicated list

## 2. CatalogAssetsUseCaseImpl integration

- [ ] 2.1 Inject `AutoTaggingService` and `TagRepositoryPort` into `CatalogAssetsUseCaseImpl`
- [ ] 2.2 After EXIF extraction for each asset, call `autoTaggingService.deriveTags(exif, geocodedCity, geocodedCountry)`
- [ ] 2.3 For each derived tag name, call `tagRepositoryPort.findOrCreate(name)` and link to the asset via `tagRepositoryPort.addTagToAsset(assetId, tagId)`
- [ ] 2.4 Catch any exceptions per-asset to avoid aborting the catalog operation

## 3. Backend unit tests

- [ ] 3.1 Test that `deriveTags()` returns `["2024"]` for `dateTaken = 2024-07-15`
- [ ] 3.2 Test that `deriveTags()` returns `["nikon"]` for `cameraMake = "NIKON CORPORATION"`
- [ ] 3.3 Test that `deriveTags()` returns `["apple"]` for `cameraMake = "Apple"`
- [ ] 3.4 Test that `deriveTags()` returns `["montevideo", "uruguay"]` when both geocoded values are present
- [ ] 3.5 Test that `deriveTags()` returns an empty list when all inputs are null/empty
- [ ] 3.6 Test that duplicate tags are deduplicated (e.g., same year and same city)
- [ ] 3.7 Test that `CatalogAssetsUseCaseImpl` calls `autoTaggingService.deriveTags()` and links the returned tags

## 4. Testing and Commit

- [ ] 4.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 4.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 4.3 Commit all changes (only after both test suites pass)
