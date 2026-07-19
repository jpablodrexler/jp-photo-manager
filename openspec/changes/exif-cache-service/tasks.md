## 1. Create ExifCacheService

- [ ] 1.1 Create `core/services/exif-cache.service.ts` with `@Injectable({ providedIn: 'root' })`
- [ ] 1.2 Add `private cache = new Map<number, ExifMetadata | null>()`
- [ ] 1.3 Implement `has(assetId: number): boolean`
- [ ] 1.4 Implement `get(assetId: number): ExifMetadata | null | undefined`
- [ ] 1.5 Implement `set(assetId: number, data: ExifMetadata | null): void`

## 2. Refactor ExifPanelComponent

- [ ] 2.1 Inject `ExifCacheService` in `ExifPanelComponent`
- [ ] 2.2 Remove the local `private exifCache = new Map<number, ExifMetadata | null>()` field
- [ ] 2.3 In `loadExif(assetId)`, replace local map checks with `exifCacheService.has(assetId)` and `exifCacheService.get(assetId)`
- [ ] 2.4 After a successful API response, call `exifCacheService.set(assetId, data)` instead of setting the local map

## 3. Frontend tests

- [ ] 3.1 Cypress component test: `ExifCacheService.has()` returns false for uncached asset, true after `set()`
- [ ] 3.2 Cypress component test: `ExifPanelComponent` does not call the API a second time for a cached asset (spy on `AssetService.getExif`)

## 4. Testing and Commit

- [ ] 4.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 4.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 4.3 Commit all changes (only after both test suites pass)
