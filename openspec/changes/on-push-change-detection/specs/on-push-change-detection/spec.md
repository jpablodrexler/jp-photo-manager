# on-push-change-detection

All Angular components in the application use `ChangeDetectionStrategy.OnPush`. Mutable state patterns are replaced with immutable assignments to ensure OnPush detects all relevant changes. Components that update state from SSE callbacks call `ChangeDetectorRef.markForCheck()`.

---

## ADDED Requirements

### Requirement: All components declare OnPush change detection

Every `@Component` decorator in the application SHALL include `changeDetection: ChangeDetectionStrategy.OnPush`.

#### Scenario: ThumbnailComponent uses OnPush

- **WHEN** a developer inspects `ThumbnailComponent`
- **THEN** its `@Component` decorator includes `changeDetection: ChangeDetectionStrategy.OnPush`

#### Scenario: GalleryComponent uses OnPush

- **WHEN** a developer inspects `GalleryComponent`
- **THEN** its `@Component` decorator includes `changeDetection: ChangeDetectionStrategy.OnPush`

#### Scenario: All feature components use OnPush

- **WHEN** a developer inspects any component under `features/` or `shared/`
- **THEN** its `@Component` decorator includes `changeDetection: ChangeDetectionStrategy.OnPush`

### Requirement: State assigned to template bindings uses new references

Components SHALL NOT mutate arrays or Sets that are bound in templates in-place. Every update SHALL produce a new reference.

#### Scenario: Asset list is reassigned after load

- **WHEN** `GalleryComponent` receives a paginated asset response
- **THEN** `this.assets` is assigned a new array (`= data.items`) rather than mutated with `push()`

#### Scenario: Selected assets uses immutable Set operations

- **WHEN** the user selects or deselects a thumbnail
- **THEN** `this.selectedAssets` is assigned a new `Set(...)` rather than calling `.add()` or `.delete()` on the existing Set in-place

### Requirement: SSE callbacks call markForCheck()

Components that update state from `EventSource` event listeners SHALL inject `ChangeDetectorRef` and call `cdr.markForCheck()` after updating component properties.

#### Scenario: Catalog progress updates are visible in the template

- **GIVEN** a running catalog operation streaming SSE events
- **WHEN** the SSE callback updates `this.catalogProgress`
- **THEN** `cdr.markForCheck()` is called so the progress bar re-renders under OnPush

#### Scenario: Sync progress updates are visible in the template

- **GIVEN** a running sync operation streaming SSE events
- **WHEN** the SSE callback updates progress state in `SyncComponent`
- **THEN** `cdr.markForCheck()` is called after the state update
