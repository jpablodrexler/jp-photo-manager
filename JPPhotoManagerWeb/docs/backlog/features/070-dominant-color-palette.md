# Feature 70 — dominant-color-palette

**As a** photo library owner, **I want** to filter photos by dominant colour, **so that** I find photos by their look, such as warm sunsets or blue seas.

## Acceptance Criteria
- AC1: Given an image, when it is cataloged, then its five dominant colours are extracted from the thumbnail and stored.
- AC2: Given each colour, when it is stored, then it is snapped to one of 12 colour families.
- AC3: Given the filter toolbar, when I pick one or more colour families, then the gallery is filtered by them.
- AC4: Given the color-families endpoint, when called, then it returns the 12 families with asset counts for the current folder.
- AC5: Given a thumbnail, when the colour stripe option is on, then a 5-segment stripe is shown (off by default).
- AC6: Given /analytics, when it renders, then a colour distribution donut chart is shown.

## Technical notes

During cataloging, extract the top 5 dominant colors from each photo's thumbnail using k-means clustering (k=5, 10 iterations) over the 200×150 px thumbnail pixels; no second file read from the original is required; store the five RGB centroids as a `color_palette JSONB` column on `assets` (Flyway migration); each centroid is snapped to the nearest of 12 color families (red, orange, yellow, green, teal, blue, purple, pink, brown, grey, black, white) using Euclidean distance in HSL space; frontend additions: a color swatch row in the search/filter toolbar (one or more families can be selected, adding a `colorFamily[]` query parameter to `GET /api/assets`), an optional 5-segment color stripe at the bottom of each `ThumbnailComponent` card (toggleable, off by default), and a "Color distribution" donut chart on the `/analytics` dashboard reusing the existing `ngx-charts` dependency; `GET /api/assets/color-families` returns the 12 families with asset counts for the current folder to populate the swatch picker; no external Maven dependency — k-means runs over `BufferedImage` pixel access using the standard library

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
