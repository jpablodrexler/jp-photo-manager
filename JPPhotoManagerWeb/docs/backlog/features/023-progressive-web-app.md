# Feature 23 — progressive-web-app

**As a** photo library owner using a phone, **I want** to install the app and keep working offline, **so that** my thumbnails load fast and my rating edits are not lost without a connection.

## Acceptance Criteria
- AC1: Given a supported browser, when I visit the production app, then it offers an install prompt; the service worker is registered in production and not in development.
- AC2: Given a thumbnail, when I load it a second time, then it is served from the cache; on a cache miss it falls back to the network.
- AC3: Given I am offline, when I rate or tag a photo, then the edit is queued and I am told it is pending.
- AC4: Given I reconnect, when the connection returns, then the pending queue is replayed.
- AC5: Given cookie authentication, when the service worker is active, then the HttpOnly cookies keep working.

## Technical notes

`ng add @angular/pwa` with a cache-first strategy for thumbnails; background sync queue for offline rating/tag edits replayed on reconnect; HttpOnly cookie auth remains intact

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
