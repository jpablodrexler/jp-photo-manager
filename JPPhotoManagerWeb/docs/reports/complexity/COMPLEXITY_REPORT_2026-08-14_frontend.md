# Complexity & File-Size Hotspots Report (frontend) — 2026-08-14

**Commit:** 64e7da3
**Generated:** 2026-08-14T23:25:33.972Z
**Scope:** frontend/src/app (excludes *.cy.ts, *.spec.ts)

**Files scanned:** 79
**Functions analyzed:** 656 (average complexity: 1.56; gate threshold: 15, see `npm run complexity`)
**Average file size:** 69 lines

## Top 20 functions by cyclomatic complexity

| Complexity | File | Line | Function |
| --- | --- | --- | --- |
| 21 | src/app/features/gallery/social-media-crop/social-media-crop.component.ts | 234 | getHitArea |
| 13 | src/app/features/gallery/social-media-crop/social-media-crop.component.ts | 190 | resizeFromCorner |
| 11 | src/app/features/albums/albums.component.ts | 78 | createAlbum |
| 11 | src/app/features/gallery/gallery.component.ts | 483 | onKeyDown |
| 10 | src/app/core/interceptors/auth.interceptor.ts | 31 | <arg-callback> |
| 9 | src/app/features/gallery/gallery.component.ts | 303 | loadNextPage |
| 8 | src/app/core/services/asset.service.ts | 20 | getAssets |
| 8 | src/app/features/gallery/exif-panel/exif-panel.component.ts | 82 | ngOnChanges |
| 8 | src/app/features/gallery/gallery.component.ts | 370 | loadTimelinePage |
| 7 | src/app/core/services/media-player.service.ts | 111 | prev |
| 7 | src/app/features/albums/album-detail/album-detail.component.ts | 63 | filterSummary |
| 7 | src/app/features/albums/albums.component.ts | 68 | formatFilterSummary |
| 6 | src/app/core/interceptors/auth.interceptor.ts | 10 | extractErrorMessage |
| 6 | src/app/core/services/asset.service.ts | 35 | getTimeline |
| 6 | src/app/features/gallery/exif-panel/exif-panel.component.ts | 116 | addTag |
| 6 | src/app/features/gallery/gallery.component.ts | 784 | <arg-callback> |
| 5 | src/app/app.component.ts | 93 | ngDoCheck |
| 5 | src/app/core/services/auth.service.ts | 41 | scheduleProactiveRefresh |
| 5 | src/app/core/services/background-sync.service.ts | 34 | replayQueue |
| 5 | src/app/features/albums/album-detail/edit-album-filter-dialog.component.ts | 76 | constructor |

## Top 20 files by line count

| Lines | File |
| --- | --- |
| 995 | src/app/features/gallery/gallery.component.ts |
| 314 | src/app/features/gallery/social-media-crop/social-media-crop.component.ts |
| 186 | src/app/app.component.ts |
| 184 | src/app/features/gallery/drop-zone/drop-zone.component.ts |
| 176 | src/app/core/services/media-player.service.ts |
| 165 | src/app/features/gallery/exif-panel/exif-panel.component.ts |
| 148 | src/app/features/gallery/bulk-tag-dialog/bulk-tag-dialog.component.ts |
| 136 | src/app/features/convert/convert.component.ts |
| 136 | src/app/features/sync/sync.component.ts |
| 132 | src/app/core/services/auth.service.ts |
| 127 | src/app/core/services/asset.service.ts |
| 121 | src/app/features/albums/albums.component.ts |
| 115 | src/app/features/admin/users/user-admin.component.ts |
| 108 | src/app/features/albums/album-detail/album-detail.component.ts |
| 107 | src/app/features/gallery/batch-rename-dialog/batch-rename-dialog.component.ts |
| 107 | src/app/features/recycle-bin/recycle-bin.component.ts |
| 101 | src/app/features/folder-nav/folder-nav.component.ts |
| 99 | src/app/features/albums/album-detail/edit-album-filter-dialog.component.ts |
| 91 | src/app/shared/components/thumbnail/thumbnail.component.ts |
| 86 | src/app/features/gallery/add-to-album-dialog/add-to-album-dialog.component.ts |
