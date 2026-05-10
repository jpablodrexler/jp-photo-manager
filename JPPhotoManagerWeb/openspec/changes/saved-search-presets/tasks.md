## 1. Backend — Database migration

- [x] 1.1 Create `V12__add_search_presets.sql` in `backend/src/main/resources/db/migration/`:
  ```sql
  CREATE TABLE search_presets (
      preset_id  BIGSERIAL PRIMARY KEY,
      user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      name       VARCHAR(255) NOT NULL,
      filter_json TEXT        NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );
  CREATE INDEX ix_search_presets_user_id ON search_presets(user_id);
  ```

## 2. Backend — FilterPreset application DTO

- [x] 2.1 Create `FilterPreset.java` in `application/dto/` as a Java record annotated `@JsonInclude(JsonInclude.Include.NON_NULL)`:
  ```java
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record FilterPreset(
      String search,
      String dateFrom,
      String dateTo,
      Integer minRating
  ) {}
  ```
  Import `com.fasterxml.jackson.annotation.JsonInclude`; this record serializes to/from the `filter_json` TEXT column using Jackson's `ObjectMapper`

## 3. Backend — Entity

- [x] 3.1 Create `SearchPreset.java` in `domain/entity/` annotated `@Entity @Table(name = "search_presets") @Data @NoArgsConstructor`:
  ```java
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "preset_id")
  private Long presetId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "filter_json", nullable = false, columnDefinition = "TEXT")
  private String filterJson;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  ```

## 4. Backend — Repository

- [x] 4.1 Create `SearchPresetRepository.java` in `domain/repository/` extending `JpaRepository<SearchPreset, Long>`:
  ```java
  List<SearchPreset> findByUser_IdOrderByCreatedAtDesc(UUID userId);
  Optional<SearchPreset> findByPresetIdAndUser_Id(Long presetId, UUID userId);
  ```

## 5. Backend — Domain service

- [x] 5.1 Create `SearchPresetService.java` interface in `domain/service/` with methods:
  - `List<SearchPreset> listPresets(UUID userId)`
  - `SearchPreset createPreset(UUID userId, String name, FilterPreset filter)`
  - `void deletePreset(UUID userId, Long presetId)`
- [x] 5.2 Create `SearchPresetServiceImpl.java` in `infrastructure/service/` annotated `@Service @RequiredArgsConstructor @Slf4j`; inject `SearchPresetRepository searchPresetRepository`, `UserRepository userRepository`, `ObjectMapper objectMapper`
- [x] 5.3 Implement `listPresets(UUID userId)` annotated `@Transactional(readOnly = true)`: return `searchPresetRepository.findByUser_IdOrderByCreatedAtDesc(userId)`
- [x] 5.4 Implement `createPreset(UUID userId, String name, FilterPreset filter)` annotated `@Transactional`:
  - Resolve user via `userRepository.findById(userId).orElseThrow()`
  - Serialize `filter` to JSON string: `objectMapper.writeValueAsString(filter)`; wrap the `JsonProcessingException` in a `RuntimeException`
  - Construct `SearchPreset`, set all fields, set `createdAt = Instant.now()`; call `searchPresetRepository.save(preset)`
- [x] 5.5 Implement `deletePreset(UUID userId, Long presetId)` annotated `@Transactional`:
  - Find via `searchPresetRepository.findByPresetIdAndUser_Id(presetId, userId)` — throw `SearchPresetNotFoundException` if empty
  - Call `searchPresetRepository.delete(preset)`

## 6. Backend — Exception

- [x] 6.1 Create `SearchPresetNotFoundException.java` in `api/exception/` (create directory if absent) as a `RuntimeException` subclass:
  ```java
  public class SearchPresetNotFoundException extends RuntimeException {
      public SearchPresetNotFoundException(Long presetId) {
          super("Search preset not found: " + presetId);
      }
  }
  ```
- [x] 6.2 Register a handler in `GlobalExceptionHandler.java` mapping `SearchPresetNotFoundException` to `404 Not Found`

## 7. Backend — API DTOs

- [x] 7.1 Create `CreatePresetRequest.java` in `api/dto/` as a record:
  ```java
  public record CreatePresetRequest(
      @NotBlank String name,
      String search,
      String dateFrom,
      String dateTo,
      Integer minRating
  ) {}
  ```
  Import `jakarta.validation.constraints.NotBlank`
- [x] 7.2 Create `SearchPresetDto.java` in `api/dto/` as a record:
  ```java
  public record SearchPresetDto(
      Long presetId,
      String name,
      Instant createdAt,
      String search,
      String dateFrom,
      String dateTo,
      Integer minRating
  ) {}
  ```

## 8. Backend — Facade

- [x] 8.1 Add three method signatures to `PhotoManagerFacade`:
  - `List<SearchPresetDto> listSearchPresets(UUID userId)`
  - `SearchPresetDto saveSearchPreset(UUID userId, CreatePresetRequest request)`
  - `void deleteSearchPreset(UUID userId, Long presetId)`
- [x] 8.2 Inject `SearchPresetService searchPresetService` and `ObjectMapper objectMapper` into `PhotoManagerFacadeImpl`
- [x] 8.3 Implement `listSearchPresets`: call `searchPresetService.listPresets(userId)`; for each `SearchPreset`, deserialize `filterJson` via `objectMapper.readValue(filterJson, FilterPreset.class)`; map to `SearchPresetDto`; return list
- [x] 8.4 Implement `saveSearchPreset`: construct `FilterPreset` from the request fields; call `searchPresetService.createPreset(userId, name, filter)`; deserialize and return `SearchPresetDto`
- [x] 8.5 Implement `deleteSearchPreset`: delegate directly to `searchPresetService.deletePreset(userId, presetId)`

## 9. Backend — Controller

- [x] 9.1 Create `SearchPresetController.java` in `api/` annotated `@RestController @RequestMapping("/api/search-presets") @RequiredArgsConstructor`; inject `PhotoManagerFacade facade` and `UserRepository userRepository`
- [x] 9.2 Add private helper `resolveUserId(Authentication auth)`: call `userRepository.findByUsername(auth.getName()).map(User::getId).orElseThrow()`
- [x] 9.3 Add `GET /api/search-presets` handler:
  ```java
  @GetMapping
  public List<SearchPresetDto> list(Authentication auth) {
      return facade.listSearchPresets(resolveUserId(auth));
  }
  ```
- [x] 9.4 Add `POST /api/search-presets` handler returning `201 Created`:
  ```java
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SearchPresetDto create(Authentication auth,
                                 @Valid @RequestBody CreatePresetRequest body) {
      return facade.saveSearchPreset(resolveUserId(auth), body);
  }
  ```
- [x] 9.5 Add `DELETE /api/search-presets/{id}` handler returning `204 No Content`:
  ```java
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication auth, @PathVariable Long id) {
      facade.deleteSearchPreset(resolveUserId(auth), id);
  }
  ```

## 10. Backend — Tests

- [x] 10.1 Create `SearchPresetControllerTest` (`@WebMvcTest(SearchPresetController.class)`): mock `PhotoManagerFacade` and `UserRepository`; stub `resolveUserId`; assert `GET /api/search-presets` returns `200` with the preset list; assert `POST /api/search-presets` with `{ "name": "Test" }` returns `201`; assert `DELETE /api/search-presets/1` returns `204`
- [x] 10.2 Add test: `DELETE /api/search-presets/999` with `facade.deleteSearchPreset` throwing `SearchPresetNotFoundException` returns `404`
- [x] 10.3 Create `SearchPresetServiceTest` (`@ExtendWith(MockitoExtension.class)`): mock `SearchPresetRepository`, `UserRepository`, `ObjectMapper`; test `createPreset` calls `objectMapper.writeValueAsString(filter)` and `searchPresetRepository.save`; test `deletePreset` calls `searchPresetRepository.delete`; test `deletePreset` with unknown ID throws `SearchPresetNotFoundException`
- [x] 10.4 Run `mvn test` and confirm all tests pass

## 11. Frontend — Models and service

- [x] 11.1 Create `search-preset.model.ts` in `frontend/src/app/core/models/`:
  ```typescript
  export interface SearchPreset {
    presetId: number;
    name: string;
    createdAt: string;
    search?: string;
    dateFrom?: string;
    dateTo?: string;
    minRating?: number;
  }

  export interface CreatePresetRequest {
    name: string;
    search?: string;
    dateFrom?: string;
    dateTo?: string;
    minRating?: number;
  }
  ```
- [x] 11.2 Create `search-preset.service.ts` in `frontend/src/app/core/services/` with methods:
  - `listPresets(): Observable<SearchPreset[]>` — `GET /api/search-presets`
  - `createPreset(req: CreatePresetRequest): Observable<SearchPreset>` — `POST /api/search-presets`
  - `deletePreset(presetId: number): Observable<void>` — `DELETE /api/search-presets/${presetId}`

## 12. Frontend — GalleryComponent

- [x] 12.1 Inject `SearchPresetService` and `MatDialog` into `GalleryComponent`
- [x] 12.2 Add state fields: `presets: SearchPreset[] = []` and `selectedPresetId: number | null = null`
- [x] 12.3 In `ngOnInit`, call `this.loadPresets()`
- [x] 12.4 Implement `loadPresets(): void`: call `searchPresetService.listPresets().subscribe({ next: p => this.presets = p })`
- [x] 12.5 Implement `applyPreset(preset: SearchPreset): void`:
  ```typescript
  this.searchTerm = preset.search ?? '';
  this.dateFrom = preset.dateFrom ? new Date(preset.dateFrom) : null;
  this.dateTo   = preset.dateTo   ? new Date(preset.dateTo)   : null;
  this.minRating = preset.minRating ?? 0;
  this.pageIndex = 0;
  this.loadAssets();
  ```
- [x] 12.6 Implement `saveCurrentFiltersAsPreset(): void`: open a `MatDialog` (use `MatInputModule` prompt or a simple inline dialog with `@angular/material/dialog`) asking for a preset name; on confirm, build a `CreatePresetRequest` from current filter state, call `searchPresetService.createPreset(req).subscribe({ next: preset => { this.presets.push(preset); this.snackBar.open('Preset saved', undefined, { duration: 2000 }); } })`
- [x] 12.7 Implement `deletePreset(preset: SearchPreset, event: Event): void`: call `event.stopPropagation()`; call `searchPresetService.deletePreset(preset.presetId).subscribe({ next: () => { this.presets = this.presets.filter(p => p.presetId !== preset.presetId); if (this.selectedPresetId === preset.presetId) this.selectedPresetId = null; this.snackBar.open('Preset deleted', undefined, { duration: 2000 }); } })`
- [x] 12.8 Add `MatDialogModule` and `SearchPresetService` to `GalleryComponent` imports array

## 13. Frontend — Template

- [x] 13.1 In the filter toolbar section of `gallery.component.html`, add preset controls after the existing filter fields:
  ```html
  <mat-select [(ngModel)]="selectedPresetId"
              (ngModelChange)="applyPreset(presets[$any($event)])"
              placeholder="Load preset"
              class="preset-select">
    @for (preset of presets; track preset.presetId) {
      <mat-option [value]="preset.presetId">
        {{ preset.name }}
        <button mat-icon-button class="preset-delete-btn"
                (click)="deletePreset(preset, $event)">
          <mat-icon>close</mat-icon>
        </button>
      </mat-option>
    }
  </mat-select>

  <button mat-icon-button (click)="saveCurrentFiltersAsPreset()" title="Save as preset">
    <mat-icon>bookmark_add</mat-icon>
  </button>
  ```
  Note: use `presets.find(p => p.presetId === selectedPresetId)` in `applyPreset` for correct lookup
- [x] 13.2 Add to `gallery.component.scss`:
  ```scss
  .preset-select {
    width: 180px;
    font-size: 13px;
  }

  .preset-delete-btn {
    position: absolute;
    right: 4px;
    top: 50%;
    transform: translateY(-50%);
    width: 24px;
    height: 24px;
    line-height: 24px;
  }
  ```

## 14. Frontend — Tests

- [x] 14.1 `gallery.component.cy.ts`: mount with stubbed `SearchPresetService` returning two presets; assert the preset `MatSelect` renders both preset names as options
- [x] 14.2 Add test: select the first preset from the dropdown; assert all filter fields (`searchTerm`, `dateFrom`, `dateTo`, `minRating`) are populated from the preset; assert `assetService.getAssets` is called with the restored filter values
- [x] 14.3 Add test: click the save-preset button; type a name in the dialog; assert `searchPresetService.createPreset` is called with the current filter state and the entered name; assert the new preset appears in the dropdown
- [x] 14.4 Add test: click the close icon on a preset option; assert `searchPresetService.deletePreset` is called with the correct `presetId`; assert the preset is removed from the dropdown
- [ ] 14.5 Run `npm test` and confirm all tests pass
