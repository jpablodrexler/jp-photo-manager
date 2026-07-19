## REMOVED Requirements

### Requirement: Gallery triggers catalog on init

**Reason:** The backend now owns the catalog lifecycle. Having the frontend trigger catalog runs on every page load causes redundant concurrent runs in multi-user deployments and conflicts with the new backend-scheduled approach.

**Migration:** No user-facing migration needed. The catalog runs automatically on a schedule. Users who previously relied on page-load catalog triggering will see catalog results appear after the first scheduled run completes. The `GET /api/assets/catalog` SSE endpoint remains available for manual triggering if needed.

The following SHALL be removed from `GalleryComponent`:
- `startCatalog()` method
- `cataloging: boolean` field
- `catalogProgress: number` field
- `catalogEventSource?: EventSource` field
- The `ngOnDestroy` lifecycle hook (if its only purpose is closing the event source)
- `MatProgressBarModule` from the component's `imports` array
- The `<mat-progress-bar>` element from the template

#### Scenario: Gallery init does not trigger catalog
WHEN `GalleryComponent` is initialized (`ngOnInit`)
THEN `assetService.catalogAssets()` is NOT called
AND no SSE connection to `/api/assets/catalog` is opened

#### Scenario: Gallery renders without progress bar
WHEN the gallery template is rendered
THEN no `<mat-progress-bar>` element is present in the DOM
