## Context

The `Asset` entity already stores `pixelWidth` and `pixelHeight` (populated during cataloging via EXIF extraction). A wallpaper query needs to filter by minimum dimensions AND aspect ratio. Computing `pixel_width / pixel_height` in a JPQL `WHERE` clause is possible but non-indexable. Storing `aspect_ratio` as a persisted float column enables a filtered query with a potential B-tree index.

## Goals / Non-Goals

**Goals:**
- Add a persisted `aspect_ratio` column populated at catalog time
- Provide an endpoint that returns one random matching asset given screen dimensions
- Show the result in a gallery toolbar dialog with a download button

**Non-Goals:**
- Allowing the backend to set the wallpaper on the OS (not feasible from a web server)
- Storing user screen preferences
- Caching the suggestion (each call returns a fresh random result)

## Decisions

### 1. Store `aspect_ratio` as a persisted float column

**Decision:** Add `FLOAT aspect_ratio` to the `assets` table and populate it at catalog time rather than computing it in the query.

**Rationale:** A computed `WHERE ABS(pixel_width / pixel_height - :ratio) <= 0.02` expression is not indexable. A persisted `aspect_ratio` column can have a B-tree index, making the range filter efficient for large catalogs.

**Alternative considered:** Compute the ratio inline in JPQL. Rejected because it scans the entire table without index support.

### 2. V15 migration with backfill

**Decision:** The V15 migration adds the column and backfills existing rows with `UPDATE assets SET aspect_ratio = CAST(pixel_width AS FLOAT) / pixel_height WHERE pixel_height > 0`. Rows with `pixel_height = 0` are left as `NULL`.

**Rationale:** Existing cataloged assets need to be queryable immediately after the migration. A `NULL` aspect_ratio excludes the asset from wallpaper queries, which is correct (zero-height implies corrupt metadata).

### 3. PostgreSQL `RANDOM()` for random selection

**Decision:** Use `ORDER BY FUNCTION('RANDOM')` in the JPQL query with `PageRequest.of(0, 1)` to return one random matching asset.

**Rationale:** PostgreSQL's `RANDOM()` function is efficient for small result sets after filtering. Fetching all matching assets and picking one in Java wastes memory and is unnecessary.

**Alternative considered:** Fetch all matches into a list and pick a random index in Java. Rejected because it loads potentially thousands of assets into memory.

### 4. 404 when no matching asset exists

**Decision:** Return `404 Not Found` when no asset matches the screen dimensions.

**Rationale:** A 204 No Content response is ambiguous; 404 clearly communicates "no wallpaper found for your screen". The frontend shows a friendly snackbar message.

### 5. Toolbar action (not a separate route)

**Decision:** Implement the wallpaper suggestion as a toolbar button in `GalleryComponent` that opens a `MatDialog` showing the suggestion.

**Rationale:** The feature is a quick action, not a page the user navigates to. A dialog fits the interaction pattern and avoids cluttering the navigation bar with a new route.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| `RANDOM()` slow on large tables before filtering | Low | Dimension filters reduce the result set substantially before ordering |
| `aspect_ratio` NULL for assets with zero pixel dimensions | Low | These assets are excluded from wallpaper queries, which is correct |
| HiDPI displays: `window.screen.width` is logical pixels, not physical | Medium | Document; optionally use `window.screen.width * devicePixelRatio` for HiDPI awareness |
