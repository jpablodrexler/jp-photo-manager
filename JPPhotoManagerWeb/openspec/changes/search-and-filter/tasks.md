## 1. Backend — Repository

- [ ] 1.1 Add the following JPQL query method to `AssetRepository`:
  ```java
  @Query("""
      SELECT a FROM Asset a
      WHERE a.folder = :folder
        AND (:search IS NULL OR LOWER(a.fileName) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:dateFrom IS NULL OR a.fileCreationDateTime >= :dateFrom)
        AND (:dateTo   IS NULL OR a.fileCreationDateTime <= :dateTo)
      """)
  Page<Asset> findByFolderWithFilters(
      @Param("folder") Folder folder,
      @Param("search") String search,
      @Param("dateFrom") LocalDateTime dateFrom,
      @Param("dateTo")   LocalDateTime dateTo,
      Pageable pageable);
  ```
  Import `LocalDateTime` from `java.time`, `@Param` from `org.springframework.data.repository.query.Param`, and `@Query` from `org.springframework.data.jpa.repository.Query`

## 2. Backend — Facade interface and implementation

- [ ] 2.1 Change the `getAssets` signature in `PhotoManagerFacade` to:
  ```java
  PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria,
                                  String search, LocalDate dateFrom, LocalDate dateTo);
  ```
  Add `import java.time.LocalDate;`
- [ ] 2.2 Update `PhotoManagerFacadeImpl.getAssets`: after resolving the `Folder`, convert:
  ```java
  LocalDateTime dateFromDt = (dateFrom != null) ? dateFrom.atStartOfDay() : null;
  LocalDateTime dateToDt   = (dateTo   != null) ? dateTo.atTime(LocalTime.MAX)  : null;
  ```
  Then call `assetRepository.findByFolderWithFilters(folder.get(), search, dateFromDt, dateToDt, pageRequest)` instead of `assetRepository.findByFolder(folder.get(), pageRequest)`; add `import java.time.LocalTime;`

## 3. Backend — Controller

- [ ] 3.1 Update `AssetController.getAssets` to add three new `@RequestParam(required = false)` parameters:
  ```java
  @RequestParam(required = false) String search,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
  ```
  Import `org.springframework.format.annotation.DateTimeFormat` and `java.time.LocalDate`
- [ ] 3.2 Pass all three new parameters to `facade.getAssets(folderPath, page, sort, search, dateFrom, dateTo)`

## 4. Backend — Tests

- [ ] 4.1 Create `AssetControllerSearchTest` (`@WebMvcTest(AssetController.class)`): mock `PhotoManagerFacade` and `ThumbnailStorageService`; assert `GET /api/assets?folderPath=/photos&page=0&sort=FILE_NAME` (no filters) calls `facade.getAssets` with `null, null, null` for the filter arguments and returns `200 OK`
- [ ] 4.2 Add test: `GET /api/assets?folderPath=/photos&search=vacation` calls `facade.getAssets` with `search="vacation"`, `dateFrom=null`, `dateTo=null`
- [ ] 4.3 Add test: `GET /api/assets?folderPath=/photos&dateFrom=2024-01-01&dateTo=2024-12-31` calls `facade.getAssets` with `search=null`, `dateFrom=LocalDate.of(2024,1,1)`, `dateTo=LocalDate.of(2024,12,31)`
- [ ] 4.4 Add unit test for `PhotoManagerFacadeImpl.getAssets` with filters: mock `assetRepository.findByFolderWithFilters`; call with non-null `search` and `dateFrom`; assert the repository receives a `LocalDateTime` with time `00:00:00` for the lower bound and `LocalTime.MAX` for the upper bound
- [ ] 4.5 Run `mvn test` and confirm all tests pass

## 5. Frontend — AssetService

- [ ] 5.1 Update `getAssets` in `asset.service.ts` to accept three optional parameters:
  ```typescript
  getAssets(folderPath: string, page = 0, sort: SortCriteria = 'FILE_NAME',
            search?: string, dateFrom?: string, dateTo?: string): Observable<PaginatedData<Asset>>
  ```
- [ ] 5.2 Append each optional parameter to `HttpParams` only when it is non-null and non-empty:
  ```typescript
  let params = new HttpParams()
    .set('folderPath', folderPath)
    .set('page', page)
    .set('sort', sort);
  if (search) params = params.set('search', search);
  if (dateFrom) params = params.set('dateFrom', dateFrom);
  if (dateTo)   params = params.set('dateTo', dateTo);
  return this.http.get<PaginatedData<Asset>>(this.baseUrl, { params });
  ```

## 6. Frontend — GalleryComponent

- [ ] 6.1 Add state fields to `GalleryComponent`:
  ```typescript
  searchTerm = '';
  dateFrom: Date | null = null;
  dateTo:   Date | null = null;
  private readonly searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  ```
  Import `Subject`, `Subscription` from `rxjs`; `debounceTime`, `distinctUntilChanged` from `rxjs/operators`
- [ ] 6.2 In `ngOnInit`, subscribe to the debounced search:
  ```typescript
  this.searchSubscription = this.searchSubject.pipe(
    debounceTime(400),
    distinctUntilChanged()
  ).subscribe(() => { this.pageIndex = 0; this.loadAssets(); });
  ```
- [ ] 6.3 Implement `ngOnDestroy`: call `this.searchSubscription?.unsubscribe()` — add `implements OnDestroy` to the class declaration and import `OnDestroy` from `@angular/core`
- [ ] 6.4 Add `onSearchChange(value: string)`: set `this.searchTerm = value`; call `this.searchSubject.next(value)`
- [ ] 6.5 Add `onDateChange()`: set `this.pageIndex = 0`; call `this.loadAssets()`
- [ ] 6.6 Add `clearFilters()`: set `this.searchTerm = ''`, `this.dateFrom = null`, `this.dateTo = null`, `this.pageIndex = 0`; call `this.loadAssets()`
- [ ] 6.7 Update `onFolderSelected()`: call `this.clearFilters()` before calling `this.loadAssets()` (replace the direct `this.pageIndex = 0` reset)
- [ ] 6.8 Update `loadAssets()` to format and pass filters:
  ```typescript
  const search = this.searchTerm.trim() || undefined;
  const dateFrom = this.dateFrom ? this.dateFrom.toISOString().substring(0, 10) : undefined;
  const dateTo   = this.dateTo   ? this.dateTo.toISOString().substring(0, 10)   : undefined;
  this.assetService.getAssets(this.currentFolder, this.pageIndex, this.sortCriteria, search, dateFrom, dateTo)
    .subscribe({ ... });
  ```
- [ ] 6.9 Add to `GalleryComponent` imports array: `MatInputModule`, `MatDatepickerModule`, `MatNativeDateModule` from `@angular/material`; `ReactiveFormsModule` is not needed — plain template binding is sufficient

## 7. Frontend — Template

- [ ] 7.1 Add a filter toolbar row in `gallery.component.html` directly below the `<mat-toolbar>`, inside `@if (viewMode === 'thumbnails')`:
  ```html
  <div class="filter-toolbar">
    <mat-form-field appearance="outline" class="filter-search">
      <mat-label>Search by filename</mat-label>
      <input matInput [value]="searchTerm" (input)="onSearchChange($any($event.target).value)" />
      <mat-icon matSuffix>search</mat-icon>
    </mat-form-field>

    <mat-form-field appearance="outline" class="filter-date">
      <mat-label>Date from</mat-label>
      <input matInput [matDatepicker]="pickerFrom" [(ngModel)]="dateFrom" (dateChange)="onDateChange()" />
      <mat-datepicker-toggle matIconSuffix [for]="pickerFrom" />
      <mat-datepicker #pickerFrom />
    </mat-form-field>

    <mat-form-field appearance="outline" class="filter-date">
      <mat-label>Date to</mat-label>
      <input matInput [matDatepicker]="pickerTo" [(ngModel)]="dateTo" (dateChange)="onDateChange()" />
      <mat-datepicker-toggle matIconSuffix [for]="pickerTo" />
      <mat-datepicker #pickerTo />
    </mat-form-field>

    <button mat-icon-button (click)="clearFilters()" title="Clear filters">
      <mat-icon>filter_alt_off</mat-icon>
    </button>
  </div>
  ```

## 8. Frontend — Styles

- [ ] 8.1 Add to `gallery.component.scss`:
  ```scss
  .filter-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 4px 16px;
    background: rgba(0, 0, 0, 0.15);

    .filter-search { flex: 1; min-width: 180px; max-width: 320px; }
    .filter-date   { width: 160px; }

    mat-form-field { margin-bottom: -1.25em; } // collapse Material bottom padding
  }
  ```

## 9. Frontend — Tests

- [ ] 9.1 In `gallery.component.cy.ts`, add test: mount `GalleryComponent` with stubbed `AssetService`; type "vacation" in the search input; after 400 ms (use `cy.clock` + `cy.tick`); assert `assetService.getAssets` was called with `search = 'vacation'`
- [ ] 9.2 Add test: set a `dateFrom` via the datepicker; assert `assetService.getAssets` is called with the correct ISO date string
- [ ] 9.3 Add test: click the "Clear filters" button; assert `searchTerm`, `dateFrom`, `dateTo` are reset; assert `assetService.getAssets` is called with no filter parameters
- [ ] 9.4 Add test: changing the folder calls `clearFilters()` then `loadAssets()` — assert filters are empty when the new folder request is made
- [ ] 9.5 Run `npm test` and confirm all tests pass
