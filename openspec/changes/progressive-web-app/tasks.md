## 1. Install @angular/pwa schematic

- [x] 1.1 Run `ng add @angular/pwa` in `JPPhotoManagerWeb/frontend/` to generate `ngsw-config.json`, `manifest.webmanifest`, update `angular.json`, and add `provideServiceWorker()` to `app.config.ts`

## 2. Configure ngsw-config.json

- [x] 2.1 Ensure the `assetGroups` entry covers the app shell: `installMode: prefetch`, `updateMode: prefetch`, files: `["/favicon.ico", "/index.html", "/*.css", "/*.js"]`
- [x] 2.2 Add a `dataGroups` entry for thumbnails:
  ```json
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
  ```
- [x] 2.3 Verify no authenticated list endpoints (`/api/assets`, `/api/folders`) are included in any data group

## 3. Configure manifest.webmanifest

- [x] 3.1 Set `name: "JP Photo Manager"`, `short_name: "PhotoMgr"`, `display: "standalone"`, `start_url: "/"`
- [x] 3.2 Set `background_color: "#121212"` and `theme_color: "#2e7d32"` to match the existing dark Material theme
- [x] 3.3 Add icon entries for 192×192 and 512×512 PNG files under `src/assets/icons/`

## 4. Add app icons

- [x] 4.1 Create `src/assets/icons/icon-192x192.png` (192×192 px)
- [x] 4.2 Create `src/assets/icons/icon-512x512.png` (512×512 px)
- [x] 4.3 Ensure both icons are referenced in `manifest.webmanifest`

## 5. BackgroundSyncService

- [x] 5.1 Install `idb` package: `npm install idb`
- [x] 5.2 Create `core/services/background-sync.service.ts` with `providedIn: 'root'`
- [x] 5.3 On service init, open an IndexedDB database `photomanager-db` (version 1) with an object store `photomanager-sync-queue` (autoIncrement key, fields: `url`, `method`, `body`, `timestamp`)
- [x] 5.4 Implement `queueMutation(url: string, method: string, body: unknown): Promise<void>` that writes to the object store and calls `navigator.serviceWorker.ready.then(r => r.sync?.register('asset-mutations'))` with graceful catch if Background Sync API is not supported
- [x] 5.5 Implement `getPendingCount(): Promise<number>` that counts items in the object store
- [x] 5.6 Implement `replayQueue(): Promise<void>` that reads all items, replays each as a `fetch()` call, and deletes successfully replayed items

## 6. AssetService integration

- [x] 6.1 In `AssetService`, wrap `PATCH /api/assets/{id}` rating calls: catch `HttpErrorResponse` with status 0 (network error) and call `backgroundSyncService.queueMutation()` as fallback

## 7. AppComponent integration

- [x] 7.1 In `AppComponent.ngOnInit()`, check `backgroundSyncService.getPendingCount()` and `navigator.onLine`; if online and pending > 0, call `replayQueue()` and show `MatSnackBar`: "Syncing {count} pending changes…"
- [x] 7.2 Add an `online` event listener on `window` to trigger `replayQueue()` when connectivity is restored (fallback for browsers without Background Sync API)

## 8. Frontend tests

- [x] 8.1 Cypress component test: `BackgroundSyncService.queueMutation()` stores an entry in IndexedDB (mock `idb`)
- [x] 8.2 Cypress component test: `getPendingCount()` returns the correct count after queuing

## 9. Testing and Commit

- [x] 9.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 9.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 9.3 Commit all changes (only after both test suites pass)
