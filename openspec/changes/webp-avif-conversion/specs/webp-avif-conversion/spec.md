# webp-avif-conversion

The convert feature now supports conversion to JPEG, WebP, and AVIF formats. The target format is configurable per directory pair.

---

## ADDED Requirements

### Requirement: Convert directory pairs support a configurable target format

Each convert directory pair SHALL have a `targetFormat` field (`JPEG`, `WEBP`, or `AVIF`) that determines the output format for converted images.

#### Scenario: Existing directory pairs default to JPEG

- **GIVEN** a directory pair created before this feature was deployed
- **WHEN** the convert operation runs
- **THEN** output files are in JPEG format (backward-compatible default)

### Requirement: WebP conversion produces WebP output files

When `targetFormat = WEBP`, the convert operation SHALL invoke `cwebp` to convert JPEG and PNG source files to WebP.

#### Scenario: PNG is converted to WebP

- **GIVEN** a directory pair with `targetFormat = WEBP` and a source folder containing `photo.png`
- **WHEN** the convert operation runs
- **THEN** `photo.webp` is created in the destination folder via `cwebp -q 80`

### Requirement: AVIF conversion produces AVIF output files

When `targetFormat = AVIF`, the convert operation SHALL invoke `avifenc` to convert JPEG and PNG source files to AVIF.

#### Scenario: JPEG is converted to AVIF

- **GIVEN** a directory pair with `targetFormat = AVIF` and a source folder containing `photo.jpg`
- **WHEN** the convert operation runs
- **THEN** `photo.avif` is created in the destination folder via `avifenc --speed 6`

### Requirement: Individual conversion failures do not abort the operation

If a single file fails to convert (e.g., the encoder exits with a non-zero code), the error SHALL be logged and the operation SHALL continue with the remaining files.

#### Scenario: One corrupt file does not abort the batch

- **GIVEN** a batch of 10 files where 1 is corrupt
- **WHEN** the convert operation runs
- **THEN** 9 files are converted successfully and the error for the 1 corrupt file is logged; the SSE stream reports `success: false` for that file

### Requirement: ConvertComponent allows selecting the target format

The directory pair configuration form in `ConvertComponent` SHALL include a "Target format" dropdown with options JPEG, WebP, and AVIF.

#### Scenario: User selects AVIF as the target format

- **GIVEN** the user is configuring a directory pair in `ConvertComponent`
- **WHEN** the user selects "AVIF" from the "Target format" dropdown
- **THEN** the pair is saved with `targetFormat = AVIF` and subsequent conversions produce AVIF output
