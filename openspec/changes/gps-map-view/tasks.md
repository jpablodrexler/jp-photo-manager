> **Prerequisite:** The `exif-metadata-panel` improvement must be implemented and deployed before this change. GPS coordinates (`gps_latitude`, `gps_longitude`) must already be present in the `asset_exif` table.

## 1. Backend — Domain Layer

- [ ] 1.1 Add a nullable `String locationName` field to `domain/model/AssetExif.java` (Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` is already present — add the field only)
- [ ] 1.2 Create `domain/port/out/GeocodingPort.java` as a plain Java interface with one method: `Optional<String> reverseGeocode(double lat, double lon)`
- [ ] 1.3 Create `domain/model/MapPin.java` as a Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` class with fields: `Long assetId`, `Double gpsLatitude`, `Double gpsLongitude`, `String thumbnailUrl`
- [ ] 1.4 Create `domain/port/in/asset/GetAssetsMapUseCase.java` as a single-method interface: `PaginatedResult<MapPin> execute(AssetFilter filter)`
- [ ] 1.5 Add a `boolean hasGps` field to `application/dto/AssetFilter.java` (default `false`); this field gates the `WHERE gps_latitude IS NOT NULL AND gps_longitude IS NOT NULL` predicate

## 2. Backend — Application Layer

- [ ] 2.1 Update `application/usecase/asset/GetAssetExifUseCaseImpl.java`: inject `GeocodingPort`; after loading `AssetExif` from the repository, call `geocodingPort.reverseGeocode(lat, lon)` when both `gpsLatitude` and `gpsLongitude` are non-null; set the result (or `null` if `Optional.empty()`) on `assetExif.setLocationName(...)`
- [ ] 2.2 Create `application/usecase/asset/GetAssetsMapUseCaseImpl.java` annotated `@Service @Transactional(readOnly = true)`; inject `AssetRepository`; call `assetRepository.findFiltered(filter)` where `filter.hasGps = true`; map each returned `Asset` to a `MapPin` using its `assetId` and the corresponding `AssetExif` GPS fields (load `AssetExif` via `AssetExifRepository`); return a `PaginatedResult<MapPin>`
- [ ] 2.3 Verify that the updated `GetAssetExifUseCaseImpl` constructor is wired: Spring will inject `GeocodingPort` automatically because `NominatimGeocodingAdapter` is `@Service`; no manual bean registration needed

## 3. Backend — Infrastructure — Geocoding Adapter

- [ ] 3.1 Create `infrastructure/service/NominatimGeocodingAdapter.java` annotated `@Service @Slf4j`; inject `RestClient` (or build one with `RestClient.builder().baseUrl("https://nominatim.openstreetmap.org").defaultHeader("User-Agent", "JPPhotoManager/1.0").build()` in the constructor)
- [ ] 3.2 Implement `reverseGeocode(double lat, double lon)`: call `GET /reverse?lat={lat}&lon={lon}&format=json&accept-language=en`; parse the JSON response to extract `address.city` (fall back to `address.town`, then `address.village`); append `, {address.country_code.toUpperCase()}` to form the result string; return `Optional.of(result)` on success
- [ ] 3.3 Wrap the entire `reverseGeocode` body in a try-catch for `Exception`; on any exception log a warning at `WARN` level and return `Optional.empty()`; do NOT re-throw
- [ ] 3.4 Implement a private record or inner class `NominatimResponse` with fields `NominatimAddress address` and implement `NominatimAddress` with fields `String city`, `String town`, `String village`, `String country_code`; use `@JsonIgnoreProperties(ignoreUnknown = true)` to tolerate additional Nominatim response fields

## 4. Backend — Infrastructure — Persistence

- [ ] 4.1 Add a `hasGps` predicate to the `AssetRepositoryImpl.findFiltered()` JPQL builder: when `filter.isHasGps()` is `true`, add `AND ae.gpsLatitude IS NOT NULL AND ae.gpsLongitude IS NOT NULL` (requires a JOIN to `AssetExif`); the existing query must be unchanged when `hasGps = false`
- [ ] 4.2 Verify that `AssetExifRepository` (already in `domain/port/out/`) has a method `Optional<AssetExif> findByAssetId(Long assetId)` — add it if absent; this is used by `GetAssetsMapUseCaseImpl` to load GPS fields when building `MapPin` objects

## 5. Backend — Infrastructure — Web

- [ ] 5.1 Add `String locationName` to `infrastructure/web/dto/ExifMetadataDto.java`
- [ ] 5.2 Update `infrastructure/web/mapper/AssetDtoMapper.java` (or whichever mapper maps `AssetExif → ExifMetadataDto`) to include the new `locationName` field
- [ ] 5.3 Create `infrastructure/web/dto/MapPinDto.java` as a Java record: `record MapPinDto(Long assetId, Double gpsLatitude, Double gpsLongitude, String thumbnailUrl) {}`
- [ ] 5.4 Add a `toMapPinDto(MapPin pin)` method to `infrastructure/web/mapper/AssetDtoMapper.java`
- [ ] 5.5 Add `GET /api/assets/map` endpoint to `infrastructure/web/controller/AssetController`: inject `GetAssetsMapUseCase`; accept `@RequestParam(required = false) String folderPath`, `@RequestParam(required = false) Long albumId`, and `@RequestParam(defaultValue = "0") int page`; build an `AssetFilter` with `hasGps = true`, `folderPath`, `albumId`, and `page`; call `getAssetsMapUseCase.execute(filter)`; map the result items to `MapPinDto` via `AssetDtoMapper`; return `ResponseEntity<PaginatedData<MapPinDto>>`

## 6. Backend — Unit Tests

- [ ] 6.1 Create `GetAssetExifUseCaseImplTest`: mock `AssetExifRepository` and `GeocodingPort`; cover: GPS present + geocoding succeeds → locationName populated; GPS present + geocoding returns empty → locationName null; GPS absent → GeocodingPort never called
- [ ] 6.2 Create `GetAssetsMapUseCaseImplTest`: mock `AssetRepository` and `AssetExifRepository`; cover: folder with GPS-tagged assets returns correct MapPin list; folder with no GPS assets returns empty result; pagination parameters forwarded correctly
- [ ] 6.3 Create `NominatimGeocodingAdapterTest`: mock `RestClient` (or use `MockRestServiceServer`); cover: successful response with `city` field returns `"City, CC"`; successful response with `town` fallback; Nominatim returns 500 → returns `Optional.empty()`; connection exception → returns `Optional.empty()`
- [ ] 6.4 Update `AssetControllerTest` (existing `@WebMvcTest`): add a test for `GET /api/assets/map` covering success (returns 200 with pin list), empty result (returns 200 with empty list), and unauthenticated request (returns 401); mock `GetAssetsMapUseCase`

## 7. Frontend — npm Dependencies

- [ ] 7.1 Add `leaflet` and `@types/leaflet` to `package.json` dependencies: `npm install leaflet @types/leaflet --save`
- [ ] 7.2 Add `leaflet.markercluster` and `@types/leaflet.markercluster` to `package.json` dependencies: `npm install leaflet.markercluster @types/leaflet.markercluster --save`
- [ ] 7.3 Add the Leaflet CSS and marker-cluster CSS to the `styles` array in `angular.json`:
  ```json
  "node_modules/leaflet/dist/leaflet.css",
  "node_modules/leaflet.markercluster/dist/MarkerCluster.css",
  "node_modules/leaflet.markercluster/dist/MarkerCluster.Default.css"
  ```

## 8. Frontend — Models and Service

- [ ] 8.1 Create `core/models/map-pin.model.ts` with interface `MapPin { assetId: number; gpsLatitude: number; gpsLongitude: number; thumbnailUrl: string; }`
- [ ] 8.2 Add `locationName: string | null` to the `ExifMetadata` interface in `core/models/exif-metadata.model.ts`
- [ ] 8.3 Add `getMapPins(folderPath: string | null, albumId: number | null, page: number): Observable<PaginatedData<MapPin>>` to `core/services/asset.service.ts`; build the query string using `HttpParams`; call `GET /api/assets/map`

## 9. Frontend — MapViewComponent

- [ ] 9.1 Create the `features/map/` directory; create `map-view.component.ts` as a standalone Angular component; import `CommonModule`, `MatProgressSpinnerModule`, `MatIconModule`; inject `AssetService`, `ActivatedRoute`, and `Router`
- [ ] 9.2 Declare a `<div #mapContainer style="height: 100%; width: 100%">` in `map-view.component.html`; reference it via `@ViewChild('mapContainer') mapContainer!: ElementRef`
- [ ] 9.3 In `ngAfterViewInit()`, dynamically import Leaflet (`import * as L from 'leaflet'`) and `import 'leaflet.markercluster'`; initialise a `L.Map` on `this.mapContainer.nativeElement` with `tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap contributors' })`; fix the Leaflet default icon path issue by setting `L.Icon.Default.mergeOptions({ iconUrl: ... })` using the correct `node_modules/leaflet/dist/images/` paths
- [ ] 9.4 Call `assetService.getMapPins(folderPath, albumId, 0)` in `ngAfterViewInit()` after map initialisation; for each returned `MapPin`, create an `L.marker([lat, lon])` with a popup showing `assetId` and add it to a `L.markerClusterGroup()`; add the cluster group to the map; fit map bounds to the cluster group bounds
- [ ] 9.5 On marker click, call `router.navigate(['/gallery'], { queryParams: { folderPath, assetId: pin.assetId } })`
- [ ] 9.6 Implement an empty-state message: when `total === 0`, show a `<p>No photos with GPS data in this folder</p>` overlay centered on the map container
- [ ] 9.7 In `map-view.component.scss`, set the host to `display: block; height: calc(100vh - 64px)` so the map fills the content area below the nav bar
- [ ] 9.8 Call `this.mapInstance.remove()` in `ngOnDestroy()` to clean up the Leaflet instance

## 10. Frontend — ExifPanelComponent Mini-Map

- [ ] 10.1 Add a `<div #miniMap class="exif-mini-map">` inside `ExifPanelComponent`'s template, rendered with `@if (exifData?.gpsLatitude !== null && exifData?.gpsLongitude !== null)`
- [ ] 10.2 In `shared/components/exif-panel/exif-panel.component.scss`, add `.exif-mini-map { width: 100%; height: 150px; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }`
- [ ] 10.3 In `ExifPanelComponent`, declare `@ViewChild('miniMap') miniMapRef?: ElementRef` and a class field `private miniMapInstance: any = null`
- [ ] 10.4 Create a private `initMiniMap()` method that dynamically imports Leaflet, initialises a non-interactive map (`dragging: false, zoomControl: false, scrollWheelZoom: false`) centred on the asset's coordinates at zoom level 13, adds an OpenStreetMap tile layer and a single marker; call `initMiniMap()` from `ngOnChanges()` whenever `exifData` changes and GPS fields are non-null
- [ ] 10.5 Add a "Location" row in `ExifPanelComponent`'s template: `@if (exifData?.locationName) { <tr><td>Location</td><td>{{ exifData.locationName }}</td></tr> }` — insert it above the GPS coordinate rows
- [ ] 10.6 In `ngOnDestroy()`, call `this.miniMapInstance?.remove()` and set `this.miniMapInstance = null`

## 11. Frontend — Route and Navigation

- [ ] 11.1 Add a lazy route to `app.routes.ts`:
  ```typescript
  {
    path: 'map',
    loadComponent: () =>
      import('./features/map/map-view.component').then(m => m.MapViewComponent),
    canActivate: [authGuard]
  }
  ```
- [ ] 11.2 Add a "Map" navigation link to `app.component.html` inside the `@if (isLoggedIn)` block alongside the existing Gallery/Sync/Convert/Duplicates links:
  ```html
  <a mat-button routerLink="/map" routerLinkActive="active-link">
    <mat-icon>map</mat-icon> Map
  </a>
  ```
- [ ] 11.3 Add `MatIconModule` and `MatButtonModule` to `AppComponent` imports if not already present (verify — they are likely already imported)

## 12. Frontend — GalleryComponent assetId Query Parameter

- [ ] 12.1 In `GalleryComponent.ngOnInit()`, subscribe to `route.queryParams`; when an `assetId` query parameter is present, store it in a `targetAssetId: number | null` field; after the first page of assets loads, check if any asset in the loaded list matches `targetAssetId`; if found, set `selectedAsset` to that asset and open viewer mode; if not found in the first page, make a direct call to `assetService.getAssetById(assetId)` and open viewer mode with the returned asset
- [ ] 12.2 Add `getAssetById(assetId: number): Observable<Asset>` to `core/services/asset.service.ts`; call `GET /api/assets/{id}` (add the corresponding backend endpoint if absent — see task 12.3)
- [ ] 12.3 Verify that `GET /api/assets/{id}` exists in `AssetController`; if absent, add a `@GetMapping("/{id}")` method in `AssetController` delegating to a new `GetAssetByIdUseCase` (create use case interface and implementation following the standard hexagonal pattern)

## 13. Frontend — Tests

- [ ] 13.1 Create `features/map/map-view.component.cy.ts` as a Cypress component test; mock `AssetService.getMapPins()` to return a stub list of pins; verify: map container element is rendered; clicking a pin stub emits navigation (stub `Router.navigate` with `cy.stub`); empty state message shown when `total = 0`
- [ ] 13.2 Update `shared/components/exif-panel/exif-panel.component.cy.ts` to add coverage for: mini-map container is rendered when `gpsLatitude` and `gpsLongitude` are non-null; mini-map container is absent when GPS fields are null; "Location" row is shown when `locationName` is non-null; "Location" row is absent when `locationName` is null
- [ ] 13.3 Create a Cypress component test `core/services/asset.service.cy.ts` (or update if it exists) verifying that `getMapPins(folderPath, null, 0)` issues a `GET /api/assets/map?folderPath=...&page=0` request and maps the response to `PaginatedData<MapPin>`

## 14. Testing and Commit

- [ ] 14.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 14.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 14.3 Commit all changes (only after both test suites pass)
