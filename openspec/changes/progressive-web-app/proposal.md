## Why

The JP Photo Manager web application requires a full network connection for every interaction. Users who browse their photo library on a mobile device or in a low-connectivity environment experience degraded performance: thumbnails reload from the network on every visit, and any rating or tag edit attempted while offline is silently lost. Evolving the app into a Progressive Web App (PWA) makes it installable, keeps thumbnails snappy on repeat visits via a service-worker cache, and preserves offline mutations in a replay queue so they are applied as soon as connectivity resumes.

## What Changes

- **Frontend — `ng add @angular/pwa`:** installs `@angular/service-worker`, generates `ngsw-config.json` and `manifest.webmanifest`, registers `ServiceWorkerModule` in `app.config.ts`, and adds the `<link rel="manifest">` tag to `index.html`.
- **Frontend — `ngsw-config.json` app-shell group:** cache-first strategy for all compiled JS/CSS/HTML assets so the app loads instantly after the first visit.
- **Frontend — `ngsw-config.json` thumbnail data group:** `performance` (cache-first) strategy for `GET /api/assets/{id}/thumbnail` with `maxAge: 1y` and `maxSize: 200` entries so thumbnails are served from the cache on repeat gallery views.
- **Frontend — `manifest.webmanifest`:** app name "JP Photo Manager", PWA icons (192×192, 512×512), `display: standalone`, `start_url: /`, `background_color` and `theme_color` aligned with the existing dark Material palette.
- **Frontend — `BackgroundSyncService`:** a new `core/services/background-sync.service.ts` that intercepts failed PATCH requests (rating/tag edits), serialises them into an IndexedDB store (`photomanager-sync-queue`), and registers a `sync` event tag so the service worker can replay them when connectivity is restored.
- **Frontend — service worker sync handler:** a minimal `sync` event listener added to `ngsw-worker.js` extension (or via a custom SW script imported by Angular's SW) that drains the IndexedDB queue and replays each stored request.
- **Backend — no changes required:** the existing `PATCH /api/assets/{id}` endpoint is idempotent enough to handle replayed requests; HttpOnly cookie auth is forwarded automatically by the browser.

## Capabilities

### New Capabilities

- `progressive-web-app`: The application can be installed on desktop and mobile, loads its app shell offline, serves thumbnails from a cache-first store for faster gallery browsing, and queues rating/tag mutations made while offline for automatic replay when connectivity is restored.

### Modified Capabilities

- `asset-rating-and-tagging`: PATCH mutations attempted while offline are now queued rather than silently dropped.

## Impact

- `JPPhotoManagerWeb/frontend/package.json` — `@angular/service-worker` dependency added by `ng add`.
- `JPPhotoManagerWeb/frontend/src/ngsw-config.json` — new file; defines app-shell asset group and thumbnail data group.
- `JPPhotoManagerWeb/frontend/src/manifest.webmanifest` — new file; PWA metadata.
- `JPPhotoManagerWeb/frontend/src/index.html` — `<link rel="manifest">` and `<meta name="theme-color">` added.
- `JPPhotoManagerWeb/frontend/src/app/app.config.ts` — `provideServiceWorker()` added (production only).
- `JPPhotoManagerWeb/frontend/src/app/core/services/background-sync.service.ts` — new service; IndexedDB queue + sync registration.
- `JPPhotoManagerWeb/frontend/src/app/core/services/asset.service.ts` — PATCH calls wrapped with offline fallback via `BackgroundSyncService`.
- `JPPhotoManagerWeb/frontend/angular.json` — `serviceWorker: true` and `ngswConfigPath` set in the production build configuration.
