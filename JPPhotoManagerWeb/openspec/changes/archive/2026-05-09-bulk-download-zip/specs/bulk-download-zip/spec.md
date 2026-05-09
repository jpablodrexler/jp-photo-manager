# bulk-download-zip

Specifies how the gallery downloads a selection of assets as a single ZIP archive streamed directly from the server without writing a temporary file on disk.

---

### Requirement: Selected assets are downloaded as a ZIP archive

The system SHALL provide a `POST /api/assets/download` endpoint accepting `{ "assetIds": [...] }`. The backend SHALL resolve each asset, read its bytes via `StorageService.readFileBytes`, assemble a `ZipOutputStream`, and stream it directly to the HTTP response with `Content-Type: application/zip` and `Content-Disposition: attachment; filename="photos.zip"`. The endpoint SHALL return `200 OK` with the ZIP byte stream as the body.

#### Scenario: Valid selection downloaded successfully

- **GIVEN** the user has selected three assets with IDs 10, 20, 30 in the gallery
- **WHEN** the client calls `POST /api/assets/download` with body `{ "assetIds": [10, 20, 30] }`
- **THEN** the response is `200 OK`; `Content-Type` is `application/zip`; `Content-Disposition` is `attachment; filename="photos.zip"`; the response body is a valid ZIP archive containing three entries, one per asset

#### Scenario: Single asset downloaded

- **GIVEN** the user has selected exactly one asset
- **WHEN** the client calls `POST /api/assets/download` with that asset's ID
- **THEN** the response is `200 OK`; the ZIP contains one entry with the asset's filename

---

### Requirement: ZIP entry names are deduplicated when assets share a filename

When two or more selected assets have the same `fileName` (e.g., `IMG_0001.jpg` from different folders), the ZIP entry names SHALL be made unique. The first occurrence keeps its original name; each subsequent occurrence SHALL be named `{assetId}_{fileName}`. No two entries in the ZIP SHALL have the same name.

#### Scenario: Duplicate filenames are disambiguated

- **GIVEN** two selected assets both have `fileName = "IMG_0001.jpg"` with IDs 101 and 102
- **WHEN** the download is requested for both assets
- **THEN** the ZIP contains two entries: `IMG_0001.jpg` (for the first asset) and `101_IMG_0001.jpg` or `102_IMG_0001.jpg` (for the second, with the asset ID of the duplicate)

---

### Requirement: Unreadable files are skipped and do not abort the download

If `StorageService.readFileBytes` throws an `IOException` for any asset (file deleted, moved, or inaccessible), that asset SHALL be omitted from the ZIP. The remaining assets SHALL still be included. The endpoint SHALL still return `200 OK` with a valid (possibly empty) ZIP.

#### Scenario: One unreadable file is skipped

- **GIVEN** three assets are selected; the file for asset 20 has been deleted from disk since cataloging
- **WHEN** `POST /api/assets/download` is called for assets 10, 20, 30
- **THEN** the response is `200 OK`; the ZIP contains two entries (assets 10 and 30); asset 20 is absent; no error response is returned

#### Scenario: All files unreadable returns an empty ZIP

- **GIVEN** all selected assets have unreadable files on disk
- **WHEN** `POST /api/assets/download` is called
- **THEN** the response is `200 OK`; the response body is a valid but empty ZIP archive

---

### Requirement: The endpoint rejects requests exceeding the configured asset limit

If the `assetIds` list in the request body contains more entries than `photomanager.max-download-assets` (default 500), the endpoint SHALL return `400 Bad Request` before reading any files. An empty `assetIds` list SHALL also return `400 Bad Request` (Bean Validation `@NotEmpty`).

#### Scenario: Request exceeds the asset limit

- **GIVEN** `max-download-assets` is configured as 500
- **WHEN** the client sends a request with 501 asset IDs
- **THEN** the response is `400 Bad Request`; no files are read and no ZIP is produced

#### Scenario: Empty asset ID list is rejected

- **GIVEN** the client sends `{ "assetIds": [] }`
- **WHEN** `POST /api/assets/download` is called
- **THEN** the response is `400 Bad Request`

---

### Requirement: The gallery shows a Download action for the current selection

When one or more assets are selected, the gallery actions menu SHALL include a "Download" item. Clicking it SHALL call `POST /api/assets/download` with the selected asset IDs, display a "Preparing download…" snack bar while the request is in flight, and on success trigger a browser save-file dialog for `photos.zip`. On failure it SHALL show a "Failed to download assets" snack bar.

#### Scenario: User downloads selected assets from the gallery

- **GIVEN** the user has selected two assets in the gallery
- **WHEN** the user clicks "Download" from the actions menu
- **THEN** `POST /api/assets/download` is called with the two selected asset IDs; a "Preparing download…" snack bar appears; on success the browser prompts to save `photos.zip`; the gallery selection is unchanged

#### Scenario: Download failure shows error feedback

- **GIVEN** the user has selected one asset and clicks "Download"
- **WHEN** the server returns a non-2xx response
- **THEN** the "Preparing download…" snack bar is dismissed; a "Failed to download assets" snack bar appears

---

### Requirement: The download endpoint requires authentication

The `POST /api/assets/download` endpoint SHALL require a valid JWT cookie. Requests without a valid session SHALL receive `401 Unauthorized` and no ZIP bytes SHALL be written.

#### Scenario: Unauthenticated download request

- **GIVEN** no valid JWT cookie is present
- **WHEN** a client calls `POST /api/assets/download`
- **THEN** the response is `401 Unauthorized`
