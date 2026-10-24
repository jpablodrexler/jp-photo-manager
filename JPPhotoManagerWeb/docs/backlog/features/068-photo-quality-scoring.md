# Feature 68 — photo-quality-scoring

**As a** photo library owner, **I want** a quality score for each photo, usable for sorting and filtering, **so that** I find my sharpest, best-exposed photos and keep the best copy of duplicates.

## Acceptance Criteria
- AC1: Given an image, when it is cataloged, then a 0–100 score is computed from sharpness and EXIF signals (high ISO, long exposure and wide aperture) and stored.
- AC2: Given the gallery, when I sort by "Best quality first", then higher scores come first.
- AC3: Given the filter toolbar, when I set a minimum quality, then only assets at or above it are shown.
- AC4: Given duplicate auto-resolve, when it resolves a group, then it prefers the highest-scoring copy.

## Technical notes

Compute a perceptual quality score (0–100) for each image during cataloging using signals already available in the thumbnail and EXIF: a fast Laplacian variance pass over the 200×150 px thumbnail pixels estimates sharpness (high variance = sharp), combined with EXIF signals (ISO penalty for values above 1600, exposure time penalty for values above 1/60 s without image stabilisation, bonus for wider aperture); store the result in a new `quality_score` SMALLINT column on `assets` (Flyway migration); expose as a gallery sort option ("Best quality first") and a minimum quality threshold slider in the search/filter toolbar alongside the existing star-rating filter; pairs with `duplicate-auto-resolve` (#22) to automatically prefer the highest-scoring copy when resolving duplicates; no external Maven dependency — Laplacian variance is computed with `BufferedImage` pixel access using the standard library

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
