## Why

Power users regularly apply the same filter combinations while managing large photo libraries: "unrated shots from a recent shoot", "5-star photos from 2024", "vacation keyword with a date range". The search-and-filter toolbar requires re-entering all criteria on every session and every folder change. Saving named filter presets eliminates this repetitive work and makes the search feature dramatically more productive for catalog-heavy workflows. It also gives users a lightweight alternative to virtual albums for queries that are inherently dynamic (e.g., "all unrated photos") rather than static curated collections.

## What Changes

- Add a Flyway migration `V12__add_search_presets.sql` creating a `search_presets` table: `(preset_id BIGSERIAL PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, name VARCHAR(255) NOT NULL, filter_json TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW())`.
- Add `SearchPreset` JPA entity and `SearchPresetRepository` extending `JpaRepository`.
- Add `SearchPresetService` interface in `domain/service/` and `SearchPresetServiceImpl` in `infrastructure/service/` with `list`, `create`, and `delete` operations scoped to the authenticated user's UUID.
- Add three facade method signatures and their delegating implementations.
- Add `SearchPresetController` at `/api/search-presets` with `GET` (list), `POST` (create), and `DELETE /{id}` (delete) endpoints, all requiring authentication.
- Add `search-preset.service.ts` in `frontend/src/app/core/services/` wrapping the three API calls.
- Add a "Save preset" button and a `MatSelect` preset loader to the gallery filter toolbar. Selecting a preset from the dropdown populates all filter fields and reloads assets. Clicking "Save preset" opens a `MatDialog` asking for a name; on confirm the current filters are serialized and sent to `POST /api/search-presets`.

## Capabilities

### New Capabilities

- `saved-search-presets`: Authenticated users can save the current gallery filter criteria (filename search, date range, minimum star rating) as a named preset. Saved presets are listed in a dropdown in the gallery filter toolbar. Selecting a preset restores all its filter fields and reloads the current folder. Presets can be deleted individually.

### Modified Capabilities

- **Gallery filter toolbar**: gains a preset selector dropdown and a "Save as preset" button alongside the existing filter controls.

## Impact

- **`V12__add_search_presets.sql`** *(new)*: creates `search_presets` table with user FK and `filter_json TEXT`.
- **`SearchPreset.java`** *(new)*: JPA entity in `domain/entity/`.
- **`SearchPresetRepository.java`** *(new)*: `JpaRepository<SearchPreset, Long>` with `findByUser_Id(UUID)` and `findByPresetIdAndUser_Id(Long, UUID)`.
- **`SearchPresetService.java`** *(new)*: domain interface in `domain/service/`.
- **`SearchPresetServiceImpl.java`** *(new)*: `@Service` implementation in `infrastructure/service/`.
- **`PhotoManagerFacade.java`**: three new method signatures: `listSearchPresets`, `saveSearchPreset`, `deleteSearchPreset`.
- **`PhotoManagerFacadeImpl.java`**: inject `SearchPresetService`; delegate all three methods.
- **`SearchPresetController.java`** *(new)*: `@RestController` at `/api/search-presets`.
- **`CreatePresetRequest.java`** *(new)*: API DTO in `api/dto/`.
- **`SearchPresetDto.java`** *(new)*: API DTO in `api/dto/`.
- **`search-preset.service.ts`** *(new)*: Angular service.
- **`GalleryComponent`**: inject `SearchPresetService`; load presets on init; add `applyPreset()` and `saveCurrentFiltersAsPreset()` methods.
- **`gallery.component.html`**: preset dropdown and save button in filter toolbar.
- **No existing API contracts are altered.**
