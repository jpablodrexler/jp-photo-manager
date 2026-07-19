# auto-tagging

During cataloging, tags are automatically derived from EXIF data (year, camera make). Auto-tags are stored alongside manual tags and are removable by the user.

---

## ADDED Requirements

### Requirement: Year tag is automatically applied from dateTaken

When an asset is cataloged with a non-null `dateTaken`, a tag equal to the 4-digit year SHALL be automatically applied.

#### Scenario: Year tag applied from dateTaken

- **GIVEN** an asset with `dateTaken = "2024-07-15T10:30:00"`
- **WHEN** the asset is cataloged
- **THEN** the tag `"2024"` is added to the asset in `asset_tags`

#### Scenario: No year tag when dateTaken is null

- **GIVEN** an asset with no `dateTaken` EXIF field
- **WHEN** the asset is cataloged
- **THEN** no year tag is applied

### Requirement: Camera make tag is automatically applied

When an asset is cataloged with a non-null `cameraMake`, a normalised lowercase tag derived from the first word of the make string SHALL be applied.

#### Scenario: Camera make tag normalised to first word lowercase

- **GIVEN** an asset with `cameraMake = "NIKON CORPORATION"`
- **WHEN** the asset is cataloged
- **THEN** the tag `"nikon"` is applied

#### Scenario: Apple iPhone make normalised

- **GIVEN** an asset with `cameraMake = "Apple"`
- **WHEN** the asset is cataloged
- **THEN** the tag `"apple"` is applied

### Requirement: Auto-tags are removable by the user

Auto-applied tags SHALL be stored identically to manual tags and SHALL be removable via the existing tag removal flow.

#### Scenario: User removes an auto-applied tag

- **GIVEN** an asset with auto-applied tag `"2024"`
- **WHEN** the user removes the tag via the UI
- **THEN** the tag is removed from `asset_tags` and is no longer displayed for that asset
