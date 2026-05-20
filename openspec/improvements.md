# Planned Improvements

This document records all planned improvements to the JPPhotoManagerWeb application, their descriptions, implementation status, and the dependencies between them.

---

## Improvement List

| #   | Change name                 | Brief description                                                                                                                                                                       | Artifacts  | Implementation |
| --- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- |
| 1   | `exif-metadata-panel`       | Store full EXIF metadata during cataloging; display a collapsible panel in the viewer showing camera, exposure, GPS, and date-taken fields                                              | ✅ Created | ✅ Implemented |
| 2   | `virtual-scrolling-gallery` | Replace the CSS grid with a fixed-height list layout; render only visible items in the DOM via `CdkVirtualScrollViewport` with `*cdkVirtualFor`; remove the unused `ScrollingModule` import and the IntersectionObserver sentinel; each list row shows the thumbnail on the left and filename, size, and date metadata on the right | ⬜ Pending | ⬜ Pending |
| 3   | `drag-and-drop-upload`      | Upload image files directly from the browser by dropping them onto the gallery grid or clicking an Upload button; files are saved to the selected folder and indexed immediately        | ✅ Created | ✅ Implemented |
| 4   | `virtual-albums`            | User-scoped named collections; assets can belong to multiple albums; albums have their own paginated asset view and CRUD API                                                            | ✅ Created | ✅ Implemented |
| 5   | `refresh-token`             | Long-lived HttpOnly refresh-token cookie with rotate-on-use; Angular service proactively refreshes before expiry; interceptor retries 401s transparently                                | ✅ Created | ✅ Implemented |
| 6   | `bulk-download-zip`         | Download a selection of assets as a ZIP archive via a streaming `ZipOutputStream`; configurable per-request size cap                                                                    | ✅ Created | ✅ Implemented |
| 7   | `search-and-filter`         | Filter gallery assets by filename keyword, date-from, and date-to via a `findByFolderWithFilters` JPQL query; 400 ms debounced search input                                             | ✅ Created | ✅ Implemented |
| 8   | `slideshow-mode`            | Third `ViewMode` in the gallery that auto-advances through assets on a configurable interval; animated progress bar; keyboard shortcuts; pause on manual navigation                     | ✅ Created | ✅ Implemented |
| 9   | `soft-delete-recycle-bin`   | "Remove from catalog" sets `deleted_at` instead of deleting; recycle-bin page lists, restores, and purges soft-deleted assets; scheduled auto-purge after configurable retention period | ✅ Created | ✅ Implemented |
| 10  | `mobile-responsive-layout`  | Navigation collapses to a hamburger menu below 768 px; folder tree becomes a `MatSidenav` overlay drawer on mobile; thumbnail grid column minimum reduces to 140 px                     | ✅ Created | ✅ Implemented |
| 11  | `star-ratings`              | 0–5 star rating per asset stored in the database; rating widget on thumbnails and in the viewer; filter by minimum rating; sort by rating descending                                    | ✅ Created | ✅ Implemented |
| 12  | `saved-search-presets`      | Save the current filter state (search, date range, minimum rating) as a named preset scoped to the authenticated user; restore with one click from a dropdown in the filter toolbar     | ✅ Created | ✅ Implemented |
| 13  | `gps-map-view`              | Add a Leaflet.js map panel to the EXIF viewer and a `/map` route showing clustered photo pins for the current folder or album; clicking a pin navigates to that asset in the gallery   | ⬜ Pending | ⬜ Pending      |
| 14  | `smart-albums`              | Extend albums with an optional `filter_json` column (same shape as saved search presets) so albums can be dynamically populated at query time; UI toggle between static and dynamic mode | ⬜ Pending | ⬜ Pending      |
| 15  | `dark-mode`                 | Angular Material dark palette toggle stored in `localStorage` with `prefers-color-scheme` fallback; preference persisted per user on the backend; toolbar toggle button                 | ⬜ Pending | ⬜ Pending      |
| 16  | `keyboard-shortcuts`        | Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings               | ⬜ Pending | ⬜ Pending      |
| 17  | `batch-rename`              | Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint             | ⬜ Pending | ⬜ Pending      |
| 18  | `timeline-view`             | Third gallery view mode grouping assets by year then month with sticky date headers; `GET /api/assets/timeline-groups` returns counts per bucket; buckets load lazily via Intersection Observer | ⬜ Pending | ⬜ Pending |
| 19  | `shareable-album-links`     | `POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication | ⬜ Pending | ⬜ Pending |
| 20  | `storage-analytics`         | `/analytics` route with `ngx-charts` visualizations: storage-per-folder treemap, file-format pie, photos-per-month histogram, rating distribution bar; backed by aggregate JPQL queries  | ⬜ Pending | ⬜ Pending      |
| 21  | `video-file-support`        | Thumbnail generation via FFmpeg (`ProcessBuilder`) for `.mp4`/`.mov`/`.mkv`; catalog service accepts video MIME types; frontend shows play-overlay icon and `<video>` tag in the viewer  | ⬜ Pending | ⬜ Pending      |
| 22  | `duplicate-auto-resolve`    | "Clean up automatically" dialog on the duplicates page with policies (keep oldest, keep newest, keep highest resolution, keep preferred folder); delegates to existing soft-delete path   | ⬜ Pending | ⬜ Pending      |
| 23  | `progressive-web-app`       | `ng add @angular/pwa` with a cache-first strategy for thumbnails; background sync queue for offline rating/tag edits replayed on reconnect; HttpOnly cookie auth remains intact          | ⬜ Pending | ⬜ Pending      |
| 24  | `wallpaper-suggestion`      | Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button | ⬜ Pending | ⬜ Pending |
| 25  | `on-push-change-detection`  | Apply `ChangeDetectionStrategy.OnPush` to all 18 components; replace mutable state mutations with immutable assignments so Angular's OnPush check can detect changes; prioritise `ThumbnailComponent` (one instance per visible image) and `GalleryComponent` (the most complex); inject `ChangeDetectorRef` where manual `markForCheck()` calls are needed (e.g. after SSE events or async callbacks outside the Angular zone) | ⬜ Pending | ⬜ Pending |
| 26  | `thumbnail-http-cache`      | Add `Cache-Control: public, max-age=31536000, immutable` to the `GET /api/assets/{id}/thumbnail` response; thumbnails are content-addressed by asset ID and never mutated once written, making them safe for permanent browser and CDN caching; currently every gallery load re-fetches every thumbnail from disk with no cache headers set | ⬜ Pending | ⬜ Pending |
| 27  | `image-etag-cache`          | Add `ETag` (derived from the SHA-256 `hash` already stored on the `Asset` entity) and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response; enables conditional `If-None-Match` requests so the browser receives a `304 Not Modified` instead of re-downloading the full image on repeat views; currently no cache headers are set on this endpoint | ⬜ Pending | ⬜ Pending |
| 28  | `server-side-spring-cache`  | Enable `@EnableCaching` with a Caffeine in-memory cache; annotate `GetHomeStatsUseCase`, `GetSubFoldersUseCase`, and EXIF lookup use cases with `@Cacheable`; add `@CacheEvict` on the corresponding write use cases; avoids repeated database aggregation queries for data that changes infrequently; no new Flyway migration required | ⬜ Pending | ⬜ Pending |
| 29  | `exif-cache-service`        | Move the `Map<number, ExifMetadata \| null>` from `ExifPanelComponent` (destroyed on every navigation) to a singleton `ExifCacheService`; the cache currently lives only for the lifetime of the component instance, so navigating away and back to the viewer discards all fetched EXIF data and triggers redundant API calls; a service-level cache persists for the entire session | ⬜ Pending | ⬜ Pending |

---

## Dependencies

### Hard implementation dependencies

**None.** Every improvement can be built and deployed independently.

### Soft implementation dependencies (order affects cleanliness)

**Improvement 11 → Improvement 7**

Both share the `findByFolderWithFilters` JPQL method in `AssetRepository`. If improvement 7 (search-and-filter) is implemented first, improvement 11 (star-ratings) simply extends the existing JPQL with an additional `minRating` predicate. If improvement 11 is implemented first, it must create the full method from scratch. The tasks.md for improvement 11 documents both paths explicitly.

### Functional dependencies (feature is significantly less useful without)

**Improvement 12 → Improvements 7 + 11**

Saved search presets store `{ search, dateFrom, dateTo, minRating }`. Without improvement 7, there are no `search`/`dateFrom`/`dateTo` filter fields to persist. Without improvement 11, there is no `minRating`. A preset system that can only store an empty object provides no practical value. Improvement 12 should be implemented after both 7 and 11 are in place.

### Deployment (migration) dependencies

Flyway migration versions must be applied in ascending order. Skipping a version causes Flyway to halt on startup. The planned migration sequence is:

| Migration | Feature                                                     |
| --------- | ----------------------------------------------------------- |
| V7        | `exif-metadata-panel` — EXIF columns on `assets`            |
| V8        | `virtual-albums` — `albums` and `album_assets` tables       |
| V9        | `refresh-token` — `refresh_tokens` table                    |
| V10       | `soft-delete-recycle-bin` — `deleted_at` column on `assets` |
| V11       | `star-ratings` — `rating` column on `assets`                |
| V12       | `saved-search-presets` — `search_presets` table             |

Improvements that touch no database schema (2, 3, 5 frontend parts, 6, 8, 10) have no migration and no deployment ordering constraint relative to each other.

Among the pending improvements, those that require a Flyway migration are:

| Migration | Feature                                                                        |
| --------- | ------------------------------------------------------------------------------ |
| V13       | `smart-albums` — `filter_json` column on `albums`                              |
| V14       | `shareable-album-links` — `shared_albums` table                                |
| V15       | `wallpaper-suggestion` — `aspect_ratio` column on `assets`                    |

Note: `pixel_width` and `pixel_height` are already present on `assets`; only the derived `aspect_ratio` column is new. The backfill (`aspect_ratio = pixel_width / pixel_height`) must be included in the V15 migration to populate existing rows. Assets where either dimension is zero are left as `NULL` and excluded from wallpaper queries.

### Hard implementation dependencies (new improvements)

**Improvement 13 → Improvement 1**

`gps-map-view` requires GPS coordinates already stored in `asset_exif`. Without `exif-metadata-panel` the coordinates are never persisted.

**Improvement 14 → Improvements 4, 7, 11**

`smart-albums` stores a `filter_json` that references the same fields introduced by `search-and-filter` (7) and `star-ratings` (11), and it extends the album schema introduced by `virtual-albums` (4). All three should be in place first.

**Improvement 19 → Improvement 4**

`shareable-album-links` requires the `albums` table introduced by `virtual-albums`.

**Improvement 22 → Improvement 9**

`duplicate-auto-resolve` routes deleted assets through the soft-delete path introduced by `soft-delete-recycle-bin`.

### Soft implementation dependencies (new improvements)

**Improvement 16 → Improvement 8**

`keyboard-shortcuts` extends the viewer shortcuts already present in `slideshow-mode`. Implementing 8 first avoids re-doing viewer key handling.

**Improvement 23 → Improvement 10**

`progressive-web-app` is significantly more valuable once `mobile-responsive-layout` is in place, since PWA installs are most common on mobile.

### Recommended implementation order

For a clean, incremental delivery the natural order within the data-model cluster is:

```
7 (search-and-filter) → 11 (star-ratings) → 12 (saved-search-presets)
```

For the new improvements, the recommended order within dependent clusters is:

```
1 (exif-metadata-panel, already done) → 13 (gps-map-view)
4 (virtual-albums, already done) + 7 + 11 → 14 (smart-albums)
4 (virtual-albums, already done) → 19 (shareable-album-links)
9 (soft-delete-recycle-bin, already done) → 22 (duplicate-auto-resolve)
8 (slideshow-mode, already done) → 16 (keyboard-shortcuts)
10 (mobile-responsive-layout, already done) → 23 (progressive-web-app)
```

Improvements 15 (dark-mode), 17 (batch-rename), 18 (timeline-view), 20 (storage-analytics), 21 (video-file-support), 24 (wallpaper-suggestion), 26 (thumbnail-http-cache), 27 (image-etag-cache), 28 (server-side-spring-cache), and 29 (exif-cache-service) have no hard dependencies and can be delivered in any order.

**Improvement 26 and 27 → Improvement 23**

`thumbnail-http-cache` sets browser-level `Cache-Control` headers on the thumbnail endpoint. `progressive-web-app` (#23) adds a service-worker cache-first strategy for the same endpoint. Implementing #26 first means the service worker inherits already-correct cache semantics and does not need to re-define them. Implementing them in reverse order still works but creates redundancy.

**Improvement 27 → no schema change**

The `ETag` value is derived from the SHA-256 `hash` column already present on the `Asset` entity; no Flyway migration is needed.

**Improvement 28 — cache eviction boundary**

`@CacheEvict` must be applied to every write path that invalidates a cached result: cataloging (evicts folder and stats caches), rating (evicts stats), tagging (evicts stats), and soft-delete (evicts folder, stats, and EXIF caches). Failing to wire eviction on any write path will serve stale data silently.

**Improvement 25 → Improvement 2**

`on-push-change-detection` should be applied after `virtual-scrolling-gallery` is completed. The list layout introduced by #2 uses fixed-height items with immutable `*cdkVirtualFor` bindings; applying OnPush before that layout is in place risks masking change-detection bugs while the old grid and IntersectionObserver are still present.
