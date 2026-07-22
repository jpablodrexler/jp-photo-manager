[← Back to README](../README.md)

# Features

## Gallery

- **Thumbnail grid** — paginated 200×150 thumbnail cards for all images in the selected folder.
- **Full-screen viewer** — double-click any thumbnail to open the original at full resolution with zoom controls; press the grid icon to return.
- **Folder tree navigation** — collapsible sidebar showing the catalogued folder hierarchy; click any folder to load its assets.
- **Timeline view** — browse assets grouped by date instead of a flat grid.
- **Search and filter** — filter assets by file name, date range, minimum star rating, and tags.
- **Sort** — sort by file name, creation date, modification date, file size, or rating.
- **Star rating** — rate each image 0–5 stars directly from the thumbnail grid or viewer.
- **Tagging** — add or remove free-form tags on one or many assets at once, with autocomplete suggestions.
- **EXIF metadata panel** — view camera make and model, ISO, aperture, exposure time, and focal length extracted from image files.
- **Move / copy / rename** — select one or more images and move, copy, or rename them.
- **Crop** — crop and save an asset in place.
- **Drag-and-drop upload** — drag files from the desktop and drop them onto the gallery to upload them.
- **Download** — download a selection of images as a ZIP archive (up to the configured `max-download-assets` limit).
- **Add to album** — add selected images to an existing album or create a new one on the spot.
- **Soft delete** — deleting images sends them to the Recycle Bin rather than removing them permanently.

## Albums

- Create, rename, and delete personal albums.
- Add or remove individual assets from an album.
- Paginated asset grid within each album, with the same thumbnail viewer as the main gallery.

## Duplicate Detection

- Scans the catalog for images that share the same SHA-256 hash.
- Groups duplicate sets side-by-side for visual comparison.
- Select duplicates to delete; originals are preserved.

## Directory Sync

- Define one or more **source → destination** directory pairs.
- Optional per-pair settings: include sub-folders, delete files from the destination that are no longer in the source.
- Execute the sync and watch live progress streamed via Server-Sent Events.

## PNG → JPEG Conversion

- Define one or more **source → destination** directory pairs for conversion.
- Optional per-pair settings: include sub-folders, delete source PNG after conversion.
- Execute the conversion and watch live progress streamed via Server-Sent Events.

## Recycle Bin

- All deleted images land in the Recycle Bin with a `deleted_at` timestamp (soft delete).
- **Restore** — move images back to their original folder and re-add them to the catalog.
- **Purge** — permanently delete selected images from disk and the database.

## Dashboard

- At-a-glance statistics: total catalogued folders, total assets, combined file size, and average star rating across the library.

## Analytics

- Storage breakdown per folder, file-format distribution, photos-per-month trend, and star-rating distribution across the catalog (`/analytics` page).

## Audio Playback

- Stream audio assets (e.g. MP3) directly from the catalog with playback controls.
- Play `.m3u`/`.pls` playlist files as a queue, resolving each referenced track back to a catalogued asset.

## Image Cataloging

- The backend automatically scans all configured root folders on startup and then re-scans after a configurable cooldown (default: 2 minutes).
- Generates 200×150 JPEG thumbnails, computes SHA-256 hashes, and extracts EXIF metadata for every discovered image.
- Implemented as a Spring Batch job, partitioned by folder for parallelism; Spring Batch's own `JobRepository` coordinates runs — see [Catalog Process](catalog-process.md#catalog-process) for the full pipeline.

## Real-Time Progress

- Catalog, sync, and convert operations stream live progress events to the browser using **Server-Sent Events** — no polling required.
- Each event carries the number of processed items, the total count, and the current folder being processed.

## Authentication & User Management

- **JWT authentication** via HttpOnly cookie (`SameSite=Strict`) — tokens are never exposed to JavaScript.
- Proactive token refresh (5 minutes before expiry) keeps sessions alive without requiring re-login.
- **User Administration** page (`/admin/users`) — create users, change passwords, and delete users; no self-registration.
- Default administrator account (`admin`/`admin`) is seeded automatically on first startup.

[← Back to README](../README.md)
