## 1. Database migration

- [ ] 1.1 Create `src/main/resources/db/migration/V7__add_asset_exif.sql` with the following DDL:
  ```sql
  CREATE TABLE asset_exif (
      asset_exif_id BIGSERIAL PRIMARY KEY,
      asset_id      BIGINT NOT NULL UNIQUE REFERENCES assets(asset_id) ON DELETE CASCADE,
      camera_make   TEXT,
      camera_model  TEXT,
      date_taken    TIMESTAMP,
      f_number      DOUBLE PRECISION,
      exposure_time TEXT,
      iso_speed     INTEGER,
      focal_length  DOUBLE PRECISION,
      flash         TEXT,
      exposure_program TEXT,
      white_balance TEXT,
      metering_mode TEXT,
      gps_latitude  DOUBLE PRECISION,
      gps_longitude DOUBLE PRECISION
  );
  ```

## 2. Backend — Domain layer

- [ ] 2.1 Create `ExifMetadata.java` as a Java record in `domain/service/` with 13 nullable fields: `cameraMake`, `cameraModel`, `dateTaken` (`LocalDateTime`), `fNumber` (`Double`), `exposureTime` (`String`), `isoSpeed` (`Integer`), `focalLength` (`Double`), `flash` (`String`), `exposureProgram` (`String`), `whiteBalance` (`String`), `meteringMode` (`String`), `gpsLatitude` (`Double`), `gpsLongitude` (`Double`)
- [ ] 2.2 Create `AssetExif.java` as a `@Entity @Table(name = "asset_exif")` Lombok `@Data @NoArgsConstructor` class in `domain/entity/` with: `@Id @GeneratedValue` `assetExifId`, `@OneToOne(fetch = LAZY) @JoinColumn(name = "asset_id") Asset asset`, and one nullable field per EXIF column matching the migration
- [ ] 2.3 Create `AssetExifRepository.java` in `domain/repository/` as a `JpaRepository<AssetExif, Long>` with methods: `Optional<AssetExif> findByAsset_AssetId(Long assetId)` and `void deleteByAsset_AssetId(Long assetId)`
- [ ] 2.4 Add `ExifMetadata getExifMetadata(String filePath) throws IOException;` to the `StorageService` interface

## 3. Backend — Infrastructure layer

- [ ] 3.1 Implement `StorageServiceImpl.getExifMetadata(String filePath)`: call `Imaging.getMetadata(file)`, cast to `JpegImageMetadata`, navigate to `TiffImageMetadata`; return an all-null `ExifMetadata` record for non-JPEG files or any exception at the top level
- [ ] 3.2 Extract `cameraMake` and `cameraModel` from `TiffTagConstants.TIFF_TAG_MAKE` / `TIFF_TAG_MODEL`
- [ ] 3.3 Extract `dateTaken` from `ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL`, parsing the `"yyyy:MM:dd HH:mm:ss"` string to `LocalDateTime`
- [ ] 3.4 Extract `fNumber` from `ExifTagConstants.EXIF_TAG_FNUMBER` as a double (rational numerator/denominator)
- [ ] 3.5 Extract `exposureTime` from `ExifTagConstants.EXIF_TAG_EXPOSURE_TIME` as a human-readable fraction string (e.g. `"1/120"`)
- [ ] 3.6 Extract `isoSpeed` from `ExifTagConstants.EXIF_TAG_ISO_SPEED_RATINGS` as an integer
- [ ] 3.7 Extract `focalLength` from `ExifTagConstants.EXIF_TAG_FOCAL_LENGTH` as a double
- [ ] 3.8 Extract `flash`, `exposureProgram`, `whiteBalance`, `meteringMode` from their respective `ExifTagConstants` tags; convert integer enum values to human-readable strings
- [ ] 3.9 Extract GPS via `TiffImageMetadata.getGPS()`; read `GPS_TAG_GPS_LATITUDE` + `GPS_TAG_GPS_LATITUDE_REF` and `GPS_TAG_GPS_LONGITUDE` + `GPS_TAG_GPS_LONGITUDE_REF`; convert DMS rationals to signed decimal degrees
- [ ] 3.10 Wrap each individual tag extraction in a null-check or try-catch so a malformed tag never aborts the rest of the method

## 4. Backend — Catalog integration

- [ ] 4.1 Inject `AssetExifRepository` and `StorageService` into `CatalogAssetsServiceImpl` (both already resolvable via Spring)
- [ ] 4.2 After saving each new `Asset`, call `storageService.getExifMetadata(filePath)` to get an `ExifMetadata` record
- [ ] 4.3 If an `AssetExif` row already exists for the asset (re-catalog), delete it via `assetExifRepository.deleteByAsset_AssetId(asset.getAssetId())`
- [ ] 4.4 Create a new `AssetExif` entity from the record, set the `asset` reference, and persist via `assetExifRepository.save(assetExif)`

## 5. Backend — Application layer

- [ ] 5.1 Add `ExifMetadata getAssetExif(Long assetId);` to the `PhotoManagerFacade` interface
- [ ] 5.2 Implement `PhotoManagerFacadeImpl.getAssetExif(Long assetId)`: call `assetRepository.findById(assetId).orElseThrow(EntityNotFoundException::new)`; then call `assetExifRepository.findByAsset_AssetId(assetId)`; if present, map `AssetExif` fields to an `ExifMetadata` record and return; if absent (asset cataloged before this feature), return an all-null `ExifMetadata` record

## 6. Backend — API layer

- [ ] 6.1 Create `ExifMetadataDto.java` in `api/dto/` as a `@Data` Lombok class mirroring all 13 fields of `ExifMetadata`
- [ ] 6.2 Add `GET /{assetId}/exif` handler to `AssetController`: call `facade.getAssetExif(assetId)`, map via `toExifDto()`, return `200 OK`; return `404` for `EntityNotFoundException`
- [ ] 6.3 Add private `toExifDto(ExifMetadata exif)` mapper method in `AssetController` that copies all 13 fields

## 7. Backend — Tests

- [ ] 7.1 Add a JPEG test fixture with known EXIF data to `src/test/resources/` (or reuse an existing image from `TestFiles`)
- [ ] 7.2 Create `StorageServiceImplExifTest` (unit): assert each expected non-null EXIF field matches the fixture's known values; assert all fields are null for a PNG fixture
- [ ] 7.3 Create `AssetControllerExifTest` (`@WebMvcTest`): mock `PhotoManagerFacade`; assert `GET /api/assets/1/exif` returns `200` with correct JSON; assert `404` when facade throws `EntityNotFoundException`; assert `200` with all-null fields when facade returns an all-null `ExifMetadata`
- [ ] 7.4 Create `ExifMetadataIntegrationTest` (`@SpringBootTest`): catalog a folder containing the JPEG fixture, query `GET /api/assets/{id}/exif`, and assert that at least `cameraMake` or `dateTaken` is non-null and matches the fixture's known value
- [ ] 7.5 Run `mvn test` and confirm all tests pass

## 8. Frontend — Model and service

- [ ] 8.1 Create `frontend/src/app/core/models/exif-metadata.model.ts` with an exported `ExifMetadata` interface containing all 13 optional fields
- [ ] 8.2 Add `getExifMetadata(assetId: number): Observable<ExifMetadata>` to `AssetService`, calling `GET /api/assets/${assetId}/exif`

## 9. Frontend — ExifPanelComponent

- [ ] 9.1 Create `frontend/src/app/shared/components/exif-panel/` directory with a standalone `ExifPanelComponent` declaring `@Input() assetId!: number`, `@Input() visible = false`, and `@Output() closed = new EventEmitter<void>()`
- [ ] 9.2 On `ngOnChanges`: when `visible` becomes `true` and `assetId` is not already in the component-level `Map<number, ExifMetadata>` cache, call `assetService.getExifMetadata(assetId)`, store the result, and set `loading = false`; set `loading = true` while the request is in flight
- [ ] 9.3 Build the template: show `<mat-spinner>` while `loading`; show "No EXIF data available" when all 13 fields are null; otherwise render a `<mat-list>` of non-null fields only, with human-readable labels
- [ ] 9.4 Add a close button in the panel header that emits `closed`
- [ ] 9.5 Write component SCSS: panel fixed width (`320px`), full viewer height, right-side border; add a `@media (max-width: 768px)` rule that collapses the panel below the image
- [ ] 9.6 Import `MatListModule`, `MatIconModule`, `MatButtonModule`, `MatProgressSpinnerModule` in the component's `imports` array

## 10. Frontend — GalleryComponent wiring

- [ ] 10.1 Add `showExifPanel = false` property to `GalleryComponent`
- [ ] 10.2 Add `toggleExifPanel()` method: `this.showExifPanel = !this.showExifPanel`
- [ ] 10.3 Reset `showExifPanel = false` inside `closeViewer()`
- [ ] 10.4 Import and declare `ExifPanelComponent` in `GalleryComponent`'s `imports` array
- [ ] 10.5 Add an info icon button (`mat-icon-button`, `info` icon) to the viewer toolbar that calls `toggleExifPanel()`
- [ ] 10.6 Wrap the viewer image area and the panel in a flex-row container `<div class="viewer-layout">`; add `<app-exif-panel [assetId]="currentViewerAsset.assetId" [visible]="showExifPanel" (closed)="showExifPanel = false" />` inside it
- [ ] 10.7 Add `.viewer-layout { display: flex; align-items: flex-start; }` to `gallery.component.scss`

## 11. Frontend — Tests

- [ ] 11.1 Create `exif-panel.component.cy.ts` Cypress component test: assert panel hidden by default; spinner shown while loading; non-null fields displayed; "No EXIF data available" shown when all null; close button emits `closed`
- [ ] 11.2 Add a test to `asset.service.spec.ts` asserting `getExifMetadata(1)` issues `GET /api/assets/1/exif` and returns the mapped object
- [ ] 11.3 Run `npm test` and confirm all tests pass
