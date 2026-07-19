# progressive-web-app

The Angular frontend is a Progressive Web App: it is installable on mobile and desktop, thumbnail images are cached for offline viewing via the Angular service worker, and offline rating or tag mutations are queued in IndexedDB and replayed on reconnect.

---

## ADDED Requirements

### Requirement: Application is installable as a PWA

The frontend SHALL serve a `manifest.webmanifest` with `name`, `short_name`, `start_url`, `display: standalone`, and icon entries at 192×192 and 512×512 px. The Angular service worker SHALL be registered in production builds.

#### Scenario: Browser shows install prompt

- **GIVEN** the application is running in a Chromium-based browser in production
- **WHEN** the user visits the site for the first time
- **THEN** the browser shows an "Add to Home Screen" or install prompt

#### Scenario: Service worker is registered in production

- **WHEN** the production build is loaded in a browser
- **THEN** `navigator.serviceWorker.getRegistration()` resolves to a registered service worker

#### Scenario: Service worker is NOT registered in development

- **WHEN** the development server is running and the app loads in a browser
- **THEN** no service worker is registered

### Requirement: Thumbnails are served cache-first by the service worker

The `ngsw-config.json` SHALL define a `dataGroup` for `GET /api/assets/*/thumbnail` with `strategy: performance`, `maxAge: 365d`, and `maxSize: 200`.

#### Scenario: Thumbnail served from cache on second load

- **GIVEN** a thumbnail has been fetched once and cached by the service worker
- **WHEN** the same thumbnail URL is requested again
- **THEN** the service worker serves it from cache without a network request

#### Scenario: Cache miss falls back to network

- **GIVEN** a thumbnail is not in the service worker cache
- **WHEN** the thumbnail URL is requested
- **THEN** the service worker fetches it from the network and stores it in the cache

### Requirement: Offline rating mutations are queued and replayed

The `BackgroundSyncService` SHALL queue failed `PATCH /api/assets/{id}` requests in IndexedDB under the `photomanager-sync-queue` object store. When connectivity is restored, queued requests SHALL be replayed via the Background Sync API `sync` event or the `online` browser event as a fallback.

#### Scenario: Rating saved offline is queued

- **GIVEN** the user is offline
- **WHEN** the user rates an asset and the PATCH request fails with a network error
- **THEN** the failed request payload is stored in IndexedDB and `BackgroundSyncService.getPendingCount()` returns a count greater than 0

#### Scenario: Pending queue is replayed on reconnect

- **GIVEN** there are pending offline mutations in the IndexedDB queue
- **WHEN** connectivity is restored and the `sync` event fires (or the `online` event on unsupported browsers)
- **THEN** each queued PATCH request is replayed in order and removed from the store on success

#### Scenario: User is informed of pending sync

- **GIVEN** there are pending mutations in the IndexedDB queue when the app initializes
- **WHEN** the application starts while online
- **THEN** a `MatSnackBar` informs the user that pending changes are being synced
