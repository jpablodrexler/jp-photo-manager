# Feature 55 — webp-avif-conversion

**As a** photo library owner, **I want** to convert images to WebP or AVIF, **so that** I save space while keeping quality.

## Acceptance Criteria
- AC1: Given the convert directory pairs, when the migration runs, then existing pairs default to JPEG.
- AC2: Given the configuration form, when I choose a target format (JPEG, WebP or AVIF), then it is saved.
- AC3: Given a PNG and WebP, when it is converted, then a WebP file is produced; given a JPEG and AVIF, then an AVIF file is produced.
- AC4: Given one corrupt file, when a batch runs, then the rest are still converted.

## Technical notes

Extend `ConvertAssetsUseCase` — currently PNG→JPEG only — to support JPEG/PNG→WebP and JPEG/PNG→AVIF; WebP encoding via `cwebp` invoked through `ProcessBuilder` (same pattern as #21 FFmpeg); AVIF encoding via `avifenc` through `ProcessBuilder`; the `convert_assets_directories_definitions` table gains a `target_format` VARCHAR column (Flyway migration); frontend `ConvertComponent` adds a "Target format" dropdown (JPEG / WebP / AVIF) to the directory pair configuration form

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
