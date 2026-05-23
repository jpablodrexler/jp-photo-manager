# thumbnail-regeneration

Thumbnails can be regenerated for all assets or a specific folder via `POST /api/assets/regenerate-thumbnails`. The operation deletes existing thumbnail files and regenerates them using the existing infrastructure adapters. Progress is streamed via SSE.

---

## ADDED Requirements

### Requirement: Thumbnails are regenerated via a dedicated endpoint

`POST /api/assets/regenerate-thumbnails` (with optional `?folderPath=` query parameter) SHALL delete existing thumbnail files for in-scope non-deleted assets and regenerate them via `StoragePort`. The endpoint SHALL stream progress events via `SseEmitter`.

#### Scenario: All thumbnails regenerated when no folderPath provided

- **GIVEN** 500 assets in the catalog
- **WHEN** `POST /api/assets/regenerate-thumbnails` is called with no `folderPath`
- **THEN** thumbnails for all 500 non-deleted assets are deleted and regenerated; SSE events report progress as each asset is processed

#### Scenario: Scoped regeneration for a specific folder

- **GIVEN** 100 assets in `/photos/vacation`
- **WHEN** `POST /api/assets/regenerate-thumbnails?folderPath=/photos/vacation` is called
- **THEN** only the 100 assets in `/photos/vacation` have their thumbnails regenerated; assets in other folders are unaffected

#### Scenario: Individual thumbnail failure does not abort the operation

- **GIVEN** one asset has a missing source image file
- **WHEN** thumbnail regeneration processes that asset
- **THEN** the error is logged, the asset is skipped, and regeneration continues for all remaining assets

### Requirement: Thumbnail regeneration is restricted to ADMIN role

The `POST /api/assets/regenerate-thumbnails` endpoint SHALL return `403 Forbidden` for authenticated users with the `VIEWER` role.

#### Scenario: VIEWER cannot regenerate thumbnails

- **GIVEN** a user with `VIEWER` role is authenticated
- **WHEN** `POST /api/assets/regenerate-thumbnails` is called
- **THEN** the response is `403 Forbidden`

### Requirement: SSE events report per-asset progress

The endpoint SHALL send an SSE event after each asset is processed containing at minimum: `assetId`, `fileName`, `success: true/false`, and `processed` count.

#### Scenario: SSE event received for each processed asset

- **WHEN** thumbnail regeneration is running
- **THEN** the connected SSE client receives one event per asset with `{ assetId, fileName, success, processed, total }`
