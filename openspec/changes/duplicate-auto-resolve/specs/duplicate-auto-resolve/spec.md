# duplicate-auto-resolve

A "Clean up automatically" action on the duplicates page allows bulk resolution of all duplicate groups via a configurable policy. Non-winner assets are soft-deleted through the existing deletion path.

---

## ADDED Requirements

### Requirement: Auto-resolve endpoint accepts a policy and resolves all duplicate groups

The `POST /api/assets/duplicates/auto-resolve` endpoint SHALL accept a `ResolutionPolicy` and an optional `preferredFolderPath`, resolve all non-deleted duplicate groups according to the policy, soft-delete non-winners, and return the count of assets deleted.

#### Scenario: Auto-resolve with KEEP_OLDEST policy

- **GIVEN** two duplicate groups each containing three assets
- **WHEN** `POST /api/assets/duplicates/auto-resolve` is called with `{ "policy": "KEEP_OLDEST" }`
- **THEN** the oldest asset in each group is retained, the other four are soft-deleted, and the response body contains `{ "deletedCount": 4 }`

#### Scenario: Auto-resolve with KEEP_PREFERRED_FOLDER falls back to KEEP_OLDEST when no match

- **GIVEN** a duplicate group where no asset resides in the preferred folder
- **WHEN** `POST /api/assets/duplicates/auto-resolve` is called with `{ "policy": "KEEP_PREFERRED_FOLDER", "preferredFolderPath": "/photos/archive" }`
- **THEN** the oldest asset in the group is retained (fallback policy)

#### Scenario: Dry run returns count without deleting

- **GIVEN** three duplicate groups
- **WHEN** `POST /api/assets/duplicates/auto-resolve` is called with `{ "policy": "KEEP_NEWEST", "dryRun": true }`
- **THEN** the response contains the count of assets that would be deleted and no assets are soft-deleted

### Requirement: ResolutionPolicy enum covers four policies

The domain SHALL define a `ResolutionPolicy` enum with values `KEEP_OLDEST`, `KEEP_NEWEST`, `KEEP_HIGHEST_RESOLUTION`, and `KEEP_PREFERRED_FOLDER`.

#### Scenario: KEEP_HIGHEST_RESOLUTION selects the asset with the largest pixel area

- **GIVEN** a duplicate group with assets having dimensions 1920×1080, 800×600, and 3840×2160
- **WHEN** `KEEP_HIGHEST_RESOLUTION` policy is applied
- **THEN** the 3840×2160 asset (highest `pixelWidth * pixelHeight`) is retained

#### Scenario: KEEP_OLDEST retains the asset with the earliest creation date

- **GIVEN** a duplicate group with three assets created on different dates
- **WHEN** `KEEP_OLDEST` policy is applied
- **THEN** the asset with the earliest `creationDateTime` is retained

### Requirement: Auto-resolve dialog shows a confirmation step

The `AutoResolveDialogComponent` SHALL display a policy selector, call the endpoint in dry-run mode to show a preview count, and require explicit user confirmation before executing the deletion.

#### Scenario: User sees preview count before confirming

- **WHEN** the user clicks "Clean up automatically" and selects a policy
- **THEN** the dialog shows "This will delete N assets. Proceed?" before the Confirm button is enabled

#### Scenario: User cancels and no changes are made

- **WHEN** the user opens the auto-resolve dialog and clicks Cancel
- **THEN** no API call with `dryRun: false` is made and the duplicates list is unchanged
