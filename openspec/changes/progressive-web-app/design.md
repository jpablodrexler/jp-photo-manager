## Context

The frontend is an Angular 19 SPA served by Spring Boot in production. Currently every page load re-fetches all compiled JS/CSS bundles and every gallery view re-fetches all thumbnails from `GET /api/assets/{id}/thumbnail`. There is no offline capability, no install prompt, and no manifest.

Angular's official `@angular/pwa` schematic (`ng add @angular/pwa`) installs `@angular/service-worker` and generates two key configuration artefacts:

- **`ngsw-config.json`** — declarative config consumed by the Angular CLI at build time to generate the service worker's precache manifest. It defines _asset groups_ (versioned app-shell files, cached at install time) and _data groups_ (runtime API responses, cached on first access).
- **`manifest.webmanifest`** — standard PWA manifest referenced from `index.html`; enables the browser's "Add to Home Screen" / install prompt and controls the standalone window chrome.

Authentication in this project uses HttpOnly cookies. The service worker intercepts all fetch events but cannot add custom `Authorization` headers because it has no access to HttpOnly values — this is actually the safe, correct design. The browser automatically attaches the cookie to every same-origin request the service worker forwards, so thumbnails cached by the SW are fetched with the same credentials as any other request.

Offline mutations (rating/tag edits via `PATCH /api/assets/{id}`) require a separate mechanism because:
1. The Angular service worker's built-in data groups do not queue failed write requests.
2. The Background Sync API (`ServiceWorkerRegistration.sync.register()`) allows the SW to be woken up when connectivity resumes to replay stored requests.

## Goals / Non-Goals

**Goals:**

- Install `@angular/pwa` and register the service worker in production builds only (Angular CLI default).
- Define an `ngsw-config.json` asset group covering the app shell (cache-first, versioned by build hash).
- Define an `ngsw-config.json` data group for `GET /api/assets/{id}/thumbnail` using the `performance` (cache-first) strategy with `maxAge: 1y` and `maxSize: 200`.
- Provide a `manifest.webmanifest` with name, icons (192×192, 512×512), `display: standalone`, `start_url: /`, and colours matching the existing dark green Material theme.
- Implement `BackgroundSyncService` that stores failed PATCH requests in IndexedDB (`photomanager-sync-queue` object store) and calls `registration.sync.register('asset-mutations')`.
- Implement a `sync` event handler in the service worker that drains the queue and replays each request when online.
- Wrap `AssetService` PATCH calls to use `BackgroundSyncService` as the offline fallback.
- Ensure the existing HttpOnly cookie auth is not broken.

**Non-Goals:**

- Caching authenticated list API responses (e.g. `GET /api/assets`, `GET /api/folders`) — these contain user-specific, frequently changing data; caching them would risk serving stale results and is excluded from `ngsw-config.json` data groups.
- Full offline gallery browsing (serving the asset list from cache) — a separate, larger change.
- Push notifications.
- Background sync for any mutations other than PATCH rating/tag edits.
- iOS Safari Background Sync polyfill (the queue will drain on next app open when online).

## Decisions

### 1. `ngsw-config.json` asset group + thumbnail data group

**Decision:** Configure two groups in `ngsw-config.json`:

```json
{
  "assetGroups": [
    {
      "name": "app-shell",
      "installMode": "prefetch",
      "updateMode": "prefetch",
      "resources": {
        "files": ["/favicon.ico", "/index.html", "/*.css", "/*.js"]
      }
    }
  ],
  "dataGroups": [
    {
      "name": "thumbnails",
      "urls": ["/api/assets/*/thumbnail"],
      "cacheConfig": {
        "strategy": "performance",
        "maxSize": 200,
        "maxAge": "365d",
        "timeout": "10s"
      }
    }
  ]
}
```

**Rationale:** The `performance` strategy serves the cached response immediately and refreshes in the background, which is ideal for thumbnails that change only when an asset is replaced. A `maxAge` of 1 year with a `maxSize` of 200 entries keeps the most-recently-viewed thumbnails warm without unbounded cache growth. Authenticated API list endpoints (`/api/assets`, `/api/folders`) are deliberately absent from `dataGroups` to prevent stale data being served.

**Alternative considered:** `freshness` strategy — always checks the network first with a cache fallback. Rejected because it adds a network round-trip for every thumbnail on every gallery view, eliminating the performance benefit.

### 2. Auth via HttpOnly cookies — service worker passthrough is safe

**Decision:** No custom auth header injection is added to the service worker. The browser forwards the HttpOnly session cookie automatically on every same-origin fetch, including those intercepted and re-issued by the Angular service worker.

**Rationale:** HttpOnly cookies cannot be read by JavaScript, so the service worker cannot forge or manipulate them. The browser's native cookie handling ensures the server always receives valid credentials. This means no special auth logic is needed in the SW, and there is no risk of the SW leaking a token from memory.

**Alternative considered:** Token-in-`localStorage` injection in the SW — rejected because it would require migrating away from HttpOnly cookies, weakening the XSS posture of the application.

### 3. `BackgroundSyncService` + IndexedDB for offline PATCH mutations

**Decision:** When a `PATCH /api/assets/{id}` call fails with a network error (not a 4xx/5xx HTTP error), `BackgroundSyncService` persists the request payload (URL, method, body, timestamp) to an IndexedDB object store named `photomanager-sync-queue` and calls `navigator.serviceWorker.ready.then(r => r.sync.register('asset-mutations'))`. A `sync` event listener in the service worker iterates the store, replays each request, and deletes successfully replayed entries.

**Rationale:** IndexedDB is the only persistent, structured storage accessible to both the page and the service worker. The Background Sync API guarantees replay even if the tab is closed before connectivity is restored (on supporting browsers). Limiting the queue to PATCH mutations keeps the scope manageable and avoids the complexity of conflict resolution for structural changes.

**Alternative considered:** Replay from the Angular app on `online` event — rejected because the `online` event is unreliable (fires even when the connection is metered or slow) and cannot fire when the tab is closed.

### 4. Service worker registered only in production builds

**Decision:** `provideServiceWorker('ngsw-worker.js', { enabled: isDevMode() === false })` in `app.config.ts`. The Angular CLI sets `serviceWorker: true` only in the `production` build configuration in `angular.json`.

**Rationale:** Running a service worker in development causes confusing caching behaviour when iterating on code. Angular CLI's default schematic behaviour matches this decision.

### 5. PWA manifest aligned with existing dark green Material theme

**Decision:** `manifest.webmanifest` uses `"background_color": "#121212"` and `"theme_color": "#2e7d32"` (the primary green from the existing dark theme). Icons are 192×192 and 512×512 PNG files placed in `src/assets/icons/`.

**Rationale:** Consistent colours prevent a visible background flash when the app launches from the home screen before Angular finishes rendering. The icon sizes are the minimum required by the PWA spec for an install prompt to appear on Android and Chrome Desktop.

## Risks / Trade-offs

**Cache invalidation for replaced thumbnails**
If an asset's image file is replaced the thumbnail URL (`/api/assets/{id}/thumbnail`) remains the same. The `performance` cache will continue serving the old thumbnail until the 1-year TTL or the 200-entry LRU eviction removes it. Mitigation: a future `thumbnail-http-cache` change can introduce `ETag` / `If-None-Match` support; until then the edge case is acceptable given that image replacement is rare.

**Background Sync API browser support**
The Background Sync API is well-supported on Chrome/Edge/Android but not on Safari/Firefox as of early 2026. On unsupported browsers `registration.sync.register()` will throw; `BackgroundSyncService` must catch the error gracefully and fall back to replaying on the `online` event within the same tab session.

**IndexedDB version management**
Adding new object stores in future changes requires incrementing the IndexedDB schema version. This change must document the initial version (`photomanager-sync-queue` in DB version 1) so future changes can increment correctly.

**Increased build size and complexity**
Adding a service worker introduces a new generated file (`ngsw-worker.js`) and a precache manifest (`ngsw.json`) into the build output. The production build time increases slightly. The service worker update lifecycle (waiting/activating states) requires users to reload after a new deployment to get the latest app shell — this is standard PWA behaviour and is not expected to surprise users.
