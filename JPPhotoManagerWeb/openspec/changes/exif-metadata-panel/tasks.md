## 1. Backend — Domain layer

- [ ] 1.1 Create `ExifMetadata.java` as a Java record in `domain/service/` with 13 nullable fields: `cameraMake`, `cameraModel`, `dateTaken` (`LocalDateTime`), `fNumber` (`Double`), `exposureTime` (`String`), `isoSpeed` (`Integer`), `focalLength` (`Double`), `flash` (`String`), `exposureProgram` (`String`), `whiteBalance` (`String`), `meteringMode` (`String`), `gpsLatitude` (`Double`), `gpsLongitude` (`Double`)
- [ ] 1.2 Add `ExifMetadata getExifMetadata(String filePath) throws IOException;` to the `StorageService` interface

## 2. Backend — Infrastructure layer

- [ ] 2.1 Implement `StorageServiceImpl.getExifMetadata(String filePath)`: call `Imaging.getMetadata(file)`, cast to `JpegImageMetadata`, navigate to `TiffImageMetadata`; return an all-null `ExifMetadata` record for non-JPEG files or any exception at the top level
- [ ] 2.2 Extract `cameraMake` and `cameraModel` from `TiffTagConstants.TIFF_TAG_MAKE` / `TIFF_TAG_MODEL`
- [ ] 2.3 Extract `dateTaken` from `ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL`, parse the `"yyyy:MM:dd HH:mm:ss"` string to `LocalDateTime`
- [ ] 2.4 Extract `fNumber` from `ExifTagConstants.EXIF_TAG_FNUMBER` as a double (rational numerator/denominator)
- [ ] 2.5 Extract `exposureTime` from `ExifTagConstants.EXIF_TAG_EXPOSURE_TIME` as a human-readable fraction string (e.g. `"1/120"`)
- [ ] 2.6 Extract `isoSpeed` from `ExifTagConstants.EXIF_TAG_ISO_SPEED_RATINGS` as an integer
- [ ] 2.7 Extract `focalLength` from `ExifTagConstants.EXIF_TAG_FOCAL_LENGTH` as a double
- [ ] 2.8 Extract `flash`, `exposureProgram`, `whiteBalance`, `meteringMode` from their respective `ExifTagConstants` tags; convert integer enum values to human-readable strings
- [ ] 2.9 Extract GPS: read `GpsTagConstants.GPS_TAG_GPS_LATITUDE` + `GPS_TAG_GPS_LATITUDE_REF` and `GPS_TAG_GPS_LONGITUDE` + `GPS_TAG_GPS_LONGITUDE_REF` via `TiffImageMetadata.getGPS()`; convert DMS rationals to signed decimal degrees
- [ ] 2.10 Wrap each individual tag extraction in a null-check or try-catch so a malformed tag never aborts the rest of the method

## 3. Backend — Application layer

- [ ] 3.1 Add `ExifMetadata getAssetExif(Long assetId) throws IOException;` to the `PhotoManagerFacade` interface
- [ ] 3.2 Implement `PhotoManagerFacadeImpl.getAssetExif(Long assetId)`: call `assetRepository.findById(assetId).orElseThrow(EntityNotFoundException::new)`, then delegate to `storageService.getExifMetadata(asset.getFullPath())`

## 4. Backend — API layer

- [ ] 4.1 Create `ExifMetadataDto.java` in `api/dto/` as a `@Data` Lombok class mirroring all 13 fields of `ExifMetadata`
- [ ] 4.2 Add `GET /{assetId}/exif` handler to `AssetController`: call `facade.getAssetExif(assetId)`, map via `toExifDto()`, return `200 OK`; return `404` for `EntityNotFoundException` or `IOException`
- [ ] 4.3 Add private `toExifDto(ExifMetadata exif)` mapper method in `AssetController` that copies all 13 fields

## 5. Backend — Tests

- [ ] 5.1 Add a JPEG test fixture with known EXIF data to `src/test/resources/` (or reuse an existing `TestFiles` image)
- [ ] 5.2 Create `StorageServiceImplExifTest` (unit): assert each non-null EXIF field matches the fixture's known values; assert all fields are null for a PNG fixture
- [ ] 5.3 Create `AssetControllerExifTest` (`@WebMvcTest`): mock `PhotoManagerFacade`; assert `GET /api/assets/1/exif` returns `200` with correct JSON; assert `404` when facade throws `EntityNotFoundException`
- [ ] 5.4 Create `ExifMetadataIntegrationTest` (`@SpringBootTest`): catalog a folder containing the JPEG fixture, then call `GET /api/assets/{id}/exif` and assert at least `cameraMake` or `dateTaken` is non-null
- [ ] 5.5 Run `mvn test` and confirm all tests pass

## 6. Frontend — Model and service

- [ ] 6.1 Create `frontend/src/app/core/models/exif-metadata.model.ts` with an exported `ExifMetadata` interface containing all 13 optional fields
- [ ] 6.2 Add `getExifMetadata(assetId: number): Observable<ExifMetadata>` to `AssetService`, calling `GET /api/assets/${assetId}/exif`

## 7. Frontend — ExifPanelComponent

- [ ] 7.1 Create `frontend/src/app/shared/components/exif-panel/` directory and generate the standalone `ExifPanelComponent` with `@Input() assetId!: number`, `@Input() visible = false`, and `@Output() closed = new EventEmitter<void>()`
- [ ] 7.2 On `ngOnChanges`: when `visible` becomes `true` and the `assetId` is not already in the component-level `Map<number, ExifMetadata>` cache, call `assetService.getExifMetadata(assetId)` and store the result; set a `loading` flag while the request is in flight
- [ ] 7.3 Build the template: show `<mat-spinner>` while loading; show a "No EXIF data available" message when all fields are null; otherwise render a `<mat-list>` of non-null fields only, with their human-readable labels
- [ ] 7.4 Add a close button in the panel header that emits `closed`
- [ ] 7.5 Write component SCSS: panel fixed width (`320px`), full viewer height, right-side border; add a `@media (max-width: 768px)` rule that collapses it below the image
- [ ] 7.6 Import `MatListModule`, `MatIconModule`, `MatButtonModule`, `MatProgressSpinnerModule` in the component's `imports` array

## 8. Frontend — GalleryComponent wiring

- [ ] 8.1 Add `showExifPanel = false` property to `GalleryComponent`
- [ ] 8.2 Add `toggleExifPanel()` method: `this.showExifPanel = !this.showExifPanel`
- [ ] 8.3 Reset `showExifPanel = false` inside `closeViewer()`
- [ ] 8.4 Import and declare `ExifPanelComponent` in `GalleryComponent`'s `imports` array
- [ ] 8.5 Add an info icon button (`mat-icon-button`, `info` icon) to the viewer toolbar that calls `toggleExifPanel()`
- [ ] 8.6 Wrap the viewer image area and the panel in a flex-row container `<div class="viewer-layout">`; add `<app-exif-panel [assetId]="currentViewerAsset.assetId" [visible]="showExifPanel" (closed)="showExifPanel = false" />` inside it
- [ ] 8.7 Add `.viewer-layout { display: flex; align-items: flex-start; }` to `gallery.component.scss`

## 9. Frontend — Tests

- [ ] 9.1 Create `exif-panel.component.cy.ts` Cypress component test: assert panel hidden by default; clicking info button shows it; spinner renders while loading; non-null fields are displayed; "No EXIF data" message shown when all null; close button emits `closed`
- [ ] 9.2 Add a unit test to `asset.service.spec.ts` asserting `getExifMetadata(1)` issues `GET /api/assets/1/exif` and returns the mapped object
- [ ] 9.3 Run `npm test` and confirm all tests pass
