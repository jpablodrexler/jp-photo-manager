# auto-tagging

During cataloging, tags are automatically derived from EXIF data (year, camera make) and optionally from reverse geocoding (city/country). Auto-tags are stored alongside manual tags and are removable by the user.

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

### Requirement: Location tags are applied when geocoding is available

When an asset is cataloged with GPS coordinates and reverse geocoding returns a city and/or country, lowercase tags for the city and country SHALL be applied.

#### Scenario: City and country tags applied from GPS

- **GIVEN** an asset with GPS coordinates that reverse-geocode to city "Montevideo" and country "Uruguay"
- **WHEN** the asset is cataloged
- **THEN** tags `"montevideo"` and `"uruguay"` are applied

#### Scenario: No location tags when geocoding is unavailable

- **GIVEN** an asset with GPS coordinates but the geocoding service is unavailable (circuit open)
- **WHEN** the asset is cataloged
- **THEN** no location tags are applied and the catalog operation continues normally

### Requirement: Auto-tags are removable by the user

Auto-applied tags SHALL be stored identically to manual tags and SHALL be removable via the existing tag removal flow.

#### Scenario: User removes an auto-applied tag

- **GIVEN** an asset with auto-applied tag `"2024"`
- **WHEN** the user removes the tag via the UI
- **THEN** the tag is removed from `asset_tags` and is no longer displayed for that asset
