## Context

The `asset_exif` table stores per-asset EXIF metadata (rotation, GPS, dimensions, etc.). It is the natural place to store a user-supplied description since it is already loaded when displaying the EXIF panel. The frontend `ExifPanelComponent` currently renders EXIF fields as read-only text; adding an editable `<textarea>` that calls `PATCH /api/assets/{id}/description` on blur keeps the change minimal.

## Goals / Non-Goals

**Goals:**
- Add `description VARCHAR(2000)` to `asset_exif` via a Flyway migration
- Expose `PATCH /api/assets/{id}/description` accepting `{ "description": "..." }` (null clears the description)
- `ExifPanelComponent` shows an editable text area; saving on blur triggers the PATCH call
- `GetAssetExifUseCase` returns the description in the existing exif response DTO

**Non-Goals:**
- Full-text search over descriptions (see `full-text-search` improvement)
- Bulk description editing
- Markdown rendering of the description field

## Decisions

### 1. VARCHAR(2000) on `asset_exif`

**Decision:** Add `description VARCHAR(2000) NULL DEFAULT NULL` to `asset_exif` (Flyway V16).

**Rationale:** 2000 characters is ample for a photo caption or notes field while avoiding unbounded TEXT. `NULL` default means existing rows require no backfill.

### 2. `PATCH /api/assets/{id}/description`

**Decision:** A dedicated PATCH endpoint rather than a full PUT on the asset. The request body is `{ "description": "..." }`.

**Rationale:** Minimal surface — only the description changes. A full asset PUT would require the client to re-send all fields, risking accidental overwrites.

### 3. Save-on-blur in `ExifPanelComponent`

**Decision:** The `<textarea>` fires `(blur)` which calls `assetService.updateDescription(assetId, value)`. No explicit Save button.

**Rationale:** Consistent with how most inline-edit UIs behave; avoids a secondary button cluttering the panel.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| User navigates away before textarea blur fires | Low | Bind `(blur)` to the save handler; data is at most one edit lost |
| 2000 char limit too restrictive for some users | Low | Limit is configurable via a later improvement; start conservative |
