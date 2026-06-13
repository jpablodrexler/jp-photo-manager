## Context

Apache Commons Imaging's `JpegImageMetadata.getExif().getDirectories()` returns all IFD directories. Each directory has `getAllFields()` returning a list of `TiffField` objects. Each field has `getTagInfo().name` and `getValueDescription()`. Hibernate 6 maps `Map<String, String>` to JSONB via `@JdbcTypeCode(SqlTypes.JSON)` without additional dependencies (Jackson is already present). PostgreSQL JSONB supports GIN indexing but none is required for this use case.

## Goals / Non-Goals

**Goals:**
- Flyway V26: `ALTER TABLE asset_exif ADD COLUMN raw_exif JSONB NULL`
- `StorageServiceImpl.readExif(filePath)`: after extracting the 13 structured fields, iterate all directories and fields; add each as `rawExif.put(field.getTagInfo().name, field.getValueDescription())`; store in `AssetExifEntity.rawExif`
- Hibernate mapping: `@Column(columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) Map<String, String> rawExif`
- `ExifMetadataDto` and `AssetExif` domain model gain `Map<String, String> rawExif` (null for assets cataloged before V26)
- `ExifPanelComponent`: below the 13 structured fields, `@if (exif.rawExif)` → `<mat-expansion-panel>` labeled "All EXIF data"; inside: `<input matInput>` for key filter bound to `filterText`; `@for` over filtered entries (entries where `key.toLowerCase().includes(filterText.toLowerCase())`) rendering a compact two-column `<span>` per entry

**Non-Goals:**
- GIN index on `raw_exif` (not needed for display; no backend query filters by raw exif content)
- Backfill of existing assets (re-cataloging handles it)
- Storing non-string EXIF values (all field values are converted to `getValueDescription()` strings)

## Decisions

### 1. `@JdbcTypeCode(SqlTypes.JSON)` on `Map<String, String>`

**Decision:** Use Hibernate 6's native JSON support rather than converting the map to a string manually.

**Rationale:** Hibernate 6 supports `Map<String, String>` to JSONB natively when `@JdbcTypeCode(SqlTypes.JSON)` is present. No additional serialization/deserialization code needed.

### 2. Client-side search filter (no backend query)

**Decision:** The search filter in `ExifPanelComponent` filters the already-loaded `rawExif` map in the browser.

**Rationale:** The `rawExif` map is already in the `ExifMetadataDto` response. Backend filtering would add a new endpoint and require query parameter handling for what is ultimately a UI convenience feature.

### 3. Null-safe display

**Decision:** The "All EXIF data" panel is hidden when `rawExif` is null. No backfill migration is needed.

**Rationale:** Users can re-catalog any folder to populate `raw_exif`. Hiding the panel for legacy assets is simpler than an empty panel or a migration that re-processes all images.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| MakerNote fields produce very large raw_exif maps (100+ entries per image) | Low | JSONB storage handles maps of this size efficiently; the frontend search filter makes them navigable |
| `getValueDescription()` returns binary data for some embedded thumbnail fields | Low | Skip fields where `getValueDescription()` produces excessively long values (> 1000 chars) |
