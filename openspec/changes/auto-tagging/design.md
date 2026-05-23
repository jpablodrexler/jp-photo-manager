## Context

The `CatalogAssetsUseCaseImpl` already extracts EXIF metadata per asset (date taken, camera make/model, GPS coordinates). The `asset_tags` table stores `(asset_id, tag_id)` pairs; `tags` stores `(id, name)`. The `TagRepositoryPort` can create tags by name (idempotent: return existing if name already exists) and link them to an asset.

## Goals / Non-Goals

**Goals:**
- `AutoTaggingService.deriveTags(AssetExif exif, Optional<String> geocodedCity, Optional<String> geocodedCountry): List<String>` returns tag names derived from the exif and optional location
- Tag derivation rules:
  1. Year: if `dateTaken` is non-null, add `String.valueOf(year)` (e.g., `"2024"`)
  2. Camera make: if `cameraMake` is non-null, add `cameraMake.trim().toLowerCase()` split on first space (e.g., `"NIKON CORPORATION"` → `"nikon"`)
  3. City: if geocoded city is present, add it lowercased (e.g., `"montevideo"`)
  4. Country: if geocoded country is present, add it lowercased (e.g., `"uruguay"`)
- `CatalogAssetsUseCaseImpl` calls `AutoTaggingService.deriveTags()` after EXIF extraction and saves each tag via `TagRepositoryPort`
- `AutoTaggingService` is `@Service`; no new infrastructure adapters needed

**Non-Goals:**
- AI/ML-based image content tagging
- Configuring which tag rules are active (all rules are always applied)
- Removing auto-applied tags if EXIF changes (re-cataloging does not remove existing tags)

## Decisions

### 1. Auto-tags are indistinguishable from manual tags

**Decision:** Store auto-applied tags in `asset_tags` with no additional metadata (no `auto_applied` flag).

**Rationale:** Keeping auto and manual tags in the same table means existing tag filtering, display, and removal flows work without modification. The tradeoff is that re-cataloging may re-add tags the user removed, but this is an edge case that can be addressed later.

### 2. Idempotent tag creation

**Decision:** Call `tagRepositoryPort.findOrCreate(name)` for each derived tag to avoid duplicate tag rows.

**Rationale:** Multiple assets from the same year/camera share the same tag row. `findOrCreate` is already the expected contract for `TagRepositoryPort`.

### 3. Geocoding is optional (null-safe)

**Decision:** Pass `Optional<String>` for city and country. `AutoTaggingService` only adds a location tag if the `Optional` is present.

**Rationale:** Geocoding may be unavailable (circuit open, GPS absent). The service must not fail catalog if geocoding is absent.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| User removes auto-tag; re-catalog re-adds it | Low | Acceptable for V1; a future `auto_applied` flag could skip re-adding removed auto-tags |
| Camera make strings are inconsistent across vendors | Low | Lowercase + first-word extraction normalizes the most common cases (Apple, Canon, Nikon, Sony) |
