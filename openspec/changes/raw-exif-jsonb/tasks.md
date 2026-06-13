## 1. Database migration

- [x] 1.1 Create `V26__add_raw_exif_jsonb.sql`: `ALTER TABLE asset_exif ADD COLUMN raw_exif JSONB NULL`

## 2. AssetExifEntity update

- [x] 2.1 Add `@Column(columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) Map<String, String> rawExif` to `AssetExifEntity`

## 3. StorageServiceImpl — collect all EXIF fields

- [x] 3.1 In `readExif(filePath)`, after extracting the 13 structured fields, iterate `metadata.getExif().getDirectories()` → for each directory, iterate `dir.getAllFields()` → add `rawExif.put(field.getTagInfo().name, field.getValueDescription())` skipping entries where `getValueDescription().length() > 1000`
- [x] 3.2 Set `rawExif` on the returned `AssetExif` domain model

## 4. Domain model and DTO update

- [x] 4.1 Add `Map<String, String> rawExif` to `AssetExif` domain model
- [x] 4.2 Add `Map<String, String> rawExif` to `ExifMetadataDto` (returned by `GET /api/assets/{id}/exif`)
- [x] 4.3 Update the `AssetExifEntityMapper` (MapStruct) to map `rawExif`

## 5. Backend unit tests

- [x] 5.1 Test that `StorageServiceImpl.readExif()` populates `rawExif` with all EXIF fields from a test image
- [x] 5.2 Test that fields with value description longer than 1000 chars are skipped
- [x] 5.3 Test that `rawExif = null` for images that produce no EXIF directories

## 6. Frontend — ExifPanelComponent

- [x] 6.1 Add `rawExif: Record<string, string> | null` to the `ExifMetadata` TypeScript interface
- [x] 6.2 In `ExifPanelComponent`: add `filterText = ''` property
- [x] 6.3 Template: `@if (exif.rawExif)` → `<mat-expansion-panel>` with header "All EXIF data"; inside: `<input matInput placeholder="Search fields..." [(ngModel)]="filterText">`; `@for (entry of filteredRawExif(); track entry.key)` → two-column row
- [x] 6.4 Add `filteredRawExif()` computed method: `Object.entries(exif.rawExif ?? {}).filter(([k]) => k.toLowerCase().includes(filterText.toLowerCase()))`

## 7. Testing and Commit

- [x] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
