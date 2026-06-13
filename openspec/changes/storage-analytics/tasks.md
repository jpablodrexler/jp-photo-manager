## 1. Backend — Domain Layer

- [x] 1.1 Create `domain/model/AnalyticsData.java` as a Lombok `@Value @Builder` class with four fields: `List<FolderStorageEntry> folderStorage`, `List<FormatEntry> formatDistribution`, `List<MonthlyCountEntry> photosPerMonth`, `List<RatingEntry> ratingDistribution`
- [x] 1.2 Create `domain/model/FolderStorageEntry.java` as a Java record: `record FolderStorageEntry(String folderPath, long bytes) {}`
- [x] 1.3 Create `domain/model/FormatEntry.java` as a Java record: `record FormatEntry(String extension, long count) {}`
- [x] 1.4 Create `domain/model/MonthlyCountEntry.java` as a Java record: `record MonthlyCountEntry(String month, long count) {}`
- [x] 1.5 Create `domain/model/RatingEntry.java` as a Java record: `record RatingEntry(int rating, long count) {}`
- [x] 1.6 Create `domain/port/in/analytics/GetAnalyticsUseCase.java` as a single-method interface: `AnalyticsData execute()`
- [x] 1.7 Add four new aggregate query methods to `domain/port/out/AssetRepository.java`:
  - `List<FolderStorageEntry> sumFileSizeByFolder()` — returns `(folderPath, sum(fileSize))` for non-deleted assets, ordered descending by sum
  - `List<FormatEntry> countByExtension()` — returns `(lowercase extension, count)` for non-deleted assets with a `.` in their filename, ordered descending by count
  - `List<MonthlyCountEntry> countByCreationMonth()` — returns `(YYYY-MM, count)` for non-deleted assets with non-null `fileCreationDateTime`, ordered ascending by month
  - `List<RatingEntry> countByRating()` — returns `(rating, count)` for non-deleted assets, ordered ascending by rating

## 2. Backend — Application Layer

- [x] 2.1 Create `application/usecase/analytics/GetAnalyticsUseCaseImpl.java` annotated `@Service @Transactional(readOnly = true)`; inject `AssetRepository`
- [x] 2.2 Implement `execute()`: call the four repository methods; post-process `sumFileSizeByFolder()` to cap results at the top 20 folders — sum the remainder into a single `FolderStorageEntry` with `folderPath = "other"` if more than 20 entries exist; post-process `countByExtension()` to map entries with an empty or null extension to `"unknown"`; assemble and return `AnalyticsData`

## 3. Backend — Infrastructure — Persistence

- [x] 3.1 Add four `@Query` JPQL methods to `JpaAssetRepository` backing the four new `AssetRepository` port methods:
  - Storage per folder: `SELECT f.path AS folderPath, SUM(a.fileSize) AS bytes FROM AssetEntity a JOIN a.folder f WHERE a.deletedAt IS NULL GROUP BY f.path ORDER BY SUM(a.fileSize) DESC`
  - Format distribution: `SELECT LOWER(SUBSTRING(a.fileName, LOCATE('.', a.fileName) + 1)) AS extension, COUNT(a) AS cnt FROM AssetEntity a WHERE a.deletedAt IS NULL AND LOCATE('.', a.fileName) > 0 GROUP BY LOWER(SUBSTRING(a.fileName, LOCATE('.', a.fileName) + 1)) ORDER BY COUNT(a) DESC`
  - Photos per month: `SELECT FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM') AS month, COUNT(a) AS cnt FROM AssetEntity a WHERE a.deletedAt IS NULL AND a.fileCreationDateTime IS NOT NULL GROUP BY FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM') ORDER BY month ASC`
  - Rating distribution: `SELECT a.rating AS rating, COUNT(a) AS cnt FROM AssetEntity a WHERE a.deletedAt IS NULL GROUP BY a.rating ORDER BY a.rating ASC`
  - Declare projection interfaces `FolderStorageProjection`, `FormatProjection`, `MonthlyCountProjection`, `RatingProjection` as static interfaces inside `JpaAssetRepository` if needed, or use constructor expressions
- [x] 3.2 Implement the four new methods in `infrastructure/persistence/adapter/AssetRepositoryImpl.java`, mapping projections to domain records

## 4. Backend — Infrastructure — Web

- [x] 4.1 Create `infrastructure/web/dto/AnalyticsResponseDto.java` as a Java record mirroring `AnalyticsData`:
  ```java
  record AnalyticsResponseDto(
      List<FolderStorageEntryDto> folderStorage,
      List<FormatEntryDto> formatDistribution,
      List<MonthlyCountEntryDto> photosPerMonth,
      List<RatingEntryDto> ratingDistribution
  ) {}
  ```
  with nested records `FolderStorageEntryDto(String folderPath, long bytes)`, `FormatEntryDto(String extension, long count)`, `MonthlyCountEntryDto(String month, long count)`, `RatingEntryDto(int rating, long count)`
- [x] 4.2 Create `infrastructure/web/mapper/AnalyticsMapper.java` as a MapStruct `@Mapper(componentModel = "spring")` interface mapping `AnalyticsData` → `AnalyticsResponseDto` (and the four inner record types)
- [x] 4.3 Create `infrastructure/web/controller/AnalyticsController.java` annotated `@RestController @RequestMapping("/api/analytics") @RequiredArgsConstructor`; inject `GetAnalyticsUseCase` and `AnalyticsMapper`; implement `GET /api/analytics` returning `ResponseEntity<AnalyticsResponseDto>`

## 5. Backend — Unit Tests

- [x] 5.1 Create `GetAnalyticsUseCaseImplTest` using `@ExtendWith(MockitoExtension.class)`; mock `AssetRepository`; cover:
  - `execute_withData_returnsAssembledAnalyticsData`: all four repository methods return non-empty lists; verify the returned `AnalyticsData` contains all four datasets
  - `execute_withMoreThan20Folders_capsTo20PlusOther`: `sumFileSizeByFolder()` returns 25 entries; verify `folderStorage` has 21 entries and the last has `folderPath = "other"` with the correct summed bytes
  - `execute_withExactly20Folders_noOtherEntry`: 20 entries returned; verify no "other" entry is appended
  - `execute_withExtensionlessAsset_mapsToUnknown`: `countByExtension()` returns an entry with blank extension; verify it is mapped to `"unknown"`
  - `execute_withEmptyRepository_returnsEmptyLists`: all four repository methods return empty lists; verify all four fields of `AnalyticsData` are empty
- [x] 5.2 Create `AnalyticsControllerTest` using `@WebMvcTest(AnalyticsController.class)` with `@Import(TestSecurityConfig.class)`; mock `GetAnalyticsUseCase` and `AnalyticsMapper`; cover:
  - `getAnalytics_authenticated_returns200WithBody`: authenticated request returns `200 OK` with all four JSON arrays present
  - `getAnalytics_unauthenticated_returns401`: unauthenticated request returns `401 Unauthorized`

## 6. Frontend — Install Dependency

- [x] 6.1 Run `npm install @swimlane/ngx-charts` in `JPPhotoManagerWeb/frontend` and verify it is added to `dependencies` in `package.json`

## 7. Frontend — Model and Service

- [x] 7.1 Create `core/models/analytics.model.ts` with TypeScript interfaces:
  ```typescript
  export interface FolderStorageEntry { folderPath: string; bytes: number; }
  export interface FormatEntry { extension: string; count: number; }
  export interface MonthlyCountEntry { month: string; count: number; }
  export interface RatingEntry { rating: number; count: number; }
  export interface AnalyticsData {
    folderStorage: FolderStorageEntry[];
    formatDistribution: FormatEntry[];
    photosPerMonth: MonthlyCountEntry[];
    ratingDistribution: RatingEntry[];
  }
  ```
- [x] 7.2 Create `core/services/analytics.service.ts` as `@Injectable({ providedIn: 'root' })`; inject `HttpClient`; implement `getAnalytics(): Observable<AnalyticsData>` calling `GET /api/analytics`

## 8. Frontend — AnalyticsComponent

- [x] 8.1 Create `features/analytics/analytics.component.ts` as a standalone component; inject `AnalyticsService`; declare state fields: `data: AnalyticsData | null = null`, `loading = true`, `error = false`; call `analyticsService.getAnalytics()` in `ngOnInit()`, setting `loading = false` and populating `data` on success or setting `error = true` on failure
- [x] 8.2 Transform the four backend datasets into `ngx-charts`-compatible `{ name, value }` series in the component:
  - `folderStorageSeries`: map `FolderStorageEntry[]` to `{ name: folderPath, value: bytes }`
  - `formatSeries`: map `FormatEntry[]` to `{ name: extension, value: count }`
  - `photosPerMonthSeries`: wrap as a single series `[{ name: 'Photos', series: MonthlyCountEntry[].map(e => ({ name: e.month, value: e.count })) }]`
  - `ratingSeriesBarData`: map `RatingEntry[]` to `{ name: String(rating), value: count }`
- [x] 8.3 Create `features/analytics/analytics.component.html`; use Angular Material cards (`mat-card`) as containers for each chart; show a `mat-spinner` when `loading` is true; show an error `mat-card` when `error` is true; when data is available, render:
  - `<ngx-charts-tree-map>` for folder storage
  - `<ngx-charts-pie-chart>` for format distribution
  - `<ngx-charts-bar-vertical>` for photos per month
  - `<ngx-charts-bar-vertical>` for rating distribution
- [x] 8.4 Create `features/analytics/analytics.component.scss`; lay out the four chart cards in a responsive 2-column CSS grid using `display: grid; grid-template-columns: repeat(auto-fit, minmax(480px, 1fr)); gap: 24px;`
- [x] 8.5 Add all required imports to `AnalyticsComponent.imports`: `MatCardModule`, `MatProgressSpinnerModule`, `MatIconModule`, `TreeMapModule`, `PieChartModule`, `BarVerticalModule` from `@swimlane/ngx-charts`, and `CommonModule`

## 9. Frontend — Routing and Navigation

- [x] 9.1 Add a lazy route to `app.routes.ts`:
  ```typescript
  {
    path: 'analytics',
    loadComponent: () =>
      import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent),
    canActivate: [authGuard]
  }
  ```
- [x] 9.2 Add an "Analytics" `routerLink` to `/analytics` in `app.component.html` inside the `@if (isLoggedIn)` block in both the desktop toolbar and the mobile menu, consistent with the style of the existing navigation links

## 10. Frontend — Tests

- [x] 10.1 Create `core/services/analytics.service.cy.ts`; cover:
  - `getAnalytics_onSuccess_returnsAnalyticsData`: stub `HttpClient.get` to return a mock `AnalyticsData`; verify the observable emits the data
  - `getAnalytics_onError_propagatesError`: stub `HttpClient.get` to return an error; verify the observable errors
- [x] 10.2 Create `features/analytics/analytics.component.cy.ts` using `cy.mount()`; cover:
  - `analyticsComponent_whenLoading_showsSpinner`: mount with a stub service that never resolves; verify `mat-spinner` is present and no chart is visible
  - `analyticsComponent_onSuccess_rendersFourCharts`: stub `AnalyticsService.getAnalytics` to return a populated `AnalyticsData`; verify four chart containers are present and spinner is absent
  - `analyticsComponent_onError_showsErrorMessage`: stub `AnalyticsService.getAnalytics` to return an error; verify an error message element is present and no chart containers are rendered

## 11. Testing and Commit

- [x] 11.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 11.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 11.3 Commit all changes (only after both test suites pass)
