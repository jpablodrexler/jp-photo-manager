## Context

The gallery filter toolbar is introduced by the search-and-filter improvement (improvement 7) with fields `searchTerm`, `dateFrom`, `dateTo`. The star-ratings improvement (improvement 11) adds a `minRating` field. Saved search presets capture any combination of these fields and restore them in one click.

`User.java` has a `UUID id` primary key. The virtual-albums improvement (improvement 8) established the pattern of per-user data: a `user_id UUID REFERENCES users(id) ON DELETE CASCADE` FK on the feature table.

The authentication context is accessed in controllers via `SecurityContextHolder.getContext().getAuthentication().getName()` (username string), then resolved to a `UUID` via `UserRepository.findByUsername`. This pattern is already used in `AlbumController`.

Flyway migrations run to V6 in the codebase. Planned: V7 (EXIF), V8 (virtual albums), V9 (refresh token), V10 (soft delete), V11 (star ratings). This spec targets **V12**.

## Goals / Non-Goals

**Goals:**

- Users can save the current filter state (any combination of search, dates, minRating) as a named preset.
- Presets are listed in a dropdown in the gallery filter toolbar for one-click application.
- Presets are scoped per user (not shared across accounts).
- Presets can be deleted individually.
- No separate "presets" page or route is needed.

**Non-Goals:**

- Editing a preset's name or filters in place (delete and recreate instead).
- Sharing presets between users.
- Applying a preset to a different folder (presets are filter-only; the folder is independently selected).
- Importing/exporting presets.

## Decisions

### 1. Filter stored as `TEXT` containing JSON — not individual columns

**Decision:** `search_presets.filter_json TEXT NOT NULL` holds a JSON object such as `{"search":"vacation","dateFrom":"2024-01-01","minRating":3}`. Absent fields are omitted. The backend uses Jackson to serialize/deserialize via a `FilterPreset` record.

**Rationale:** Filter fields evolve with new features (search-and-filter adds three fields, star-ratings adds one more, future features may add more). Storing each field as a separate column would require a migration every time a new filter type is introduced. A JSON blob remains schema-stable. The `FilterPreset` record is a simple Java value object; Jackson handles null/absent fields gracefully via `@JsonInclude(NON_NULL)`.

### 2. Per-user scoping via `user_id` FK — consistent with virtual-albums

**Decision:** `search_presets` has `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`. All reads and writes are filtered to the authenticated user's UUID. `SearchPresetRepository` exposes `findByUser_Id(UUID)` and `findByPresetIdAndUser_Id(Long, UUID)`.

**Rationale:** Filter presets represent personal workflow shortcuts and should not be visible across accounts. The `ON DELETE CASCADE` ensures orphan cleanup when a user is deleted, as established by virtual albums. Using the same scoping pattern keeps the codebase consistent.

### 3. Dedicated `SearchPresetService` — following the service-per-feature pattern

**Decision:** A `SearchPresetService` interface in `domain/service/` and `SearchPresetServiceImpl` in `infrastructure/service/` handle all preset CRUD. The facade delegates to this service. The service resolves the `User` entity from the UUID and handles `SearchPresetNotFoundException`.

**Rationale:** Consistent with `RecycleBinService` and `AlbumService` — each significant feature area has its own domain service. This avoids bloating `PhotoManagerFacadeImpl` with unrelated logic and makes the service independently testable.

### 4. `GET / POST / DELETE` only — no update endpoint

**Decision:** The API surface is three endpoints: `GET /api/search-presets` (list all for the user), `POST /api/search-presets` (create), `DELETE /api/search-presets/{id}` (delete one).

**Rationale:** Preset editing (rename, change filters) requires a form UI that adds engineering effort for a marginal use case. Users can delete and recreate a preset in two actions. Keeping the API minimal reduces both implementation and test surface.

### 5. `MatSelect` dropdown in the filter toolbar — no separate presets page

**Decision:** The gallery filter toolbar (introduced by search-and-filter) gains two additions: a `<mat-select placeholder="Load preset">` bound to `selectedPresetId`, and a `<button mat-icon-button title="Save as preset">`. Selecting a preset calls `applyPreset(preset)` which populates all filter fields and triggers `loadAssets()`. The save button opens a `MatDialog` prompting for a name; on confirm, `saveCurrentFiltersAsPreset(name)` is called.

**Rationale:** Presets are consumed while browsing the gallery. A separate route would break the user's folder context. Inline controls keep everything in one view. `MatSelect` is already imported in `GalleryComponent` for the sort dropdown, so no new module import is needed for the preset selector.

### 6. `FilterPresetDto` carries decoded filter fields — not raw JSON

**Decision:** `GET /api/search-presets` returns `List<SearchPresetDto>` where each DTO includes `presetId`, `name`, `createdAt`, and the decoded filter fields (`search`, `dateFrom`, `dateTo`, `minRating`). The frontend receives typed fields rather than a raw JSON string.

**Rationale:** If the backend returned the raw `filter_json` string, the frontend would need to parse JSON inside JSON (the HTTP response body is already JSON). Deserializing in the backend and returning typed fields is cleaner, keeps parsing logic server-side, and makes the TypeScript model straightforward.

## Data Flow

```
User sets filters: searchTerm="vacation", minRating=3
User clicks "Save as preset"
  → MatDialog opens: user types "Vacation 3-star"
  → GalleryComponent.saveCurrentFiltersAsPreset("Vacation 3-star")
    → searchPresetService.createPreset({ name: "Vacation 3-star",
                                          search: "vacation", minRating: 3 })
      → POST /api/search-presets { name, filterJson: {...} }
        → SearchPresetController.create(body, principal)
        → facade.saveSearchPreset(userId, name, filterJson)
          → searchPresetService.create(userId, name, filterJson)
            → assetRepository.save(new SearchPreset(...))
        → 201 Created SearchPresetDto
    → presets[] updated; snack bar "Preset saved"

User selects "Vacation 3-star" from the dropdown
  → GalleryComponent.applyPreset(preset)
    → searchTerm = preset.search ?? ''
    → dateFrom = preset.dateFrom ?? null
    → dateTo = preset.dateTo ?? null
    → minRating = preset.minRating ?? 0
    → pageIndex = 0
    → loadAssets()  // triggers GET /api/assets with all restored filters

User clicks delete icon next to "Vacation 3-star" in the dropdown
  → searchPresetService.deletePreset(presetId)
    → DELETE /api/search-presets/7
      → SearchPresetController.delete(7, principal)
      → facade.deleteSearchPreset(userId, 7)
        → searchPresetService.delete(userId, 7)
          → searchPresetRepository.findByPresetIdAndUser_Id(7, userId)
          → searchPresetRepository.delete(preset)
    → 204 No Content; presets[] filtered; selectedPresetId reset
```

## File Change List

**New files:**

- `backend/.../db/migration/V12__add_search_presets.sql`
- `backend/.../domain/entity/SearchPreset.java`
- `backend/.../domain/repository/SearchPresetRepository.java`
- `backend/.../domain/service/SearchPresetService.java`
- `backend/.../infrastructure/service/SearchPresetServiceImpl.java`
- `backend/.../api/SearchPresetController.java`
- `backend/.../api/dto/CreatePresetRequest.java`
- `backend/.../api/dto/SearchPresetDto.java`
- `backend/.../application/dto/FilterPreset.java`
- `frontend/.../core/models/search-preset.model.ts`
- `frontend/.../core/services/search-preset.service.ts`

**Modified files:**

- `backend/.../application/PhotoManagerFacade.java` — three new method signatures
- `backend/.../application/PhotoManagerFacadeImpl.java` — inject `SearchPresetService`; implement three methods
- `frontend/.../features/gallery/gallery.component.ts` — preset state, `applyPreset`, `saveCurrentFiltersAsPreset`, `deletePreset`
- `frontend/.../features/gallery/gallery.component.html` — preset dropdown and save button in filter toolbar
- `frontend/.../features/gallery/gallery.component.scss` — preset control styling
