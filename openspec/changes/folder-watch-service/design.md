## Context

`CatalogAssetsUseCase` performs a full incremental scan of configured root folders: it finds new files, updates changed files, and marks missing files as deleted. It is currently triggered only by explicit user action (the Catalog button in the UI) or by a scheduled `@Scheduled` task. Java NIO `WatchService` provides OS-level filesystem event notifications without polling, enabling near-real-time catalog updates.

The `catalog_run_state` table (or Spring Batch `BATCH_JOB_EXECUTION` table if `catalog-spring-batch` is implemented) guards against concurrent catalog runs. `FolderWatchService` must respect this lock by calling `CatalogAssetsUseCase` normally rather than bypassing it.

## Goals / Non-Goals

**Goals:**
- Monitor all configured root catalog folders recursively for `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE` events
- Debounce rapid events (e.g., batch file copy) with a configurable window (default 5 seconds)
- Trigger `CatalogAssetsUseCase` through the normal use case path to respect the distributed lock
- Allow watch to be disabled via configuration

**Non-Goals:**
- Watching arbitrary non-catalog folders
- Real-time SSE notifications to the frontend for every file change (that would create excessive traffic)
- Network filesystem (NFS/CIFS) support (NIO `WatchService` may not reliably detect events on network shares)

## Decisions

### 1. Java NIO `WatchService` with recursive directory registration

**Decision:** Use `FileSystems.getDefault().newWatchService()` and register each directory (and subdirectory, recursively) for `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE` events. When a `ENTRY_CREATE` event targets a directory, register the new directory immediately.

**Rationale:** Java NIO `WatchService` uses OS-native APIs (inotify on Linux, kqueue on macOS, ReadDirectoryChangesW on Windows) for efficient event delivery without polling.

**Alternative considered:** Spring's `PathMatchingResourcePatternResolver` with periodic polling. Rejected because polling introduces latency and CPU overhead; inotify-based watching is near-instantaneous.

### 2. Debounce with a `ScheduledExecutorService`

**Decision:** When a filesystem event is received, schedule a catalog trigger for `debounce-seconds` in the future using a `ScheduledExecutorService`. If another event arrives before the timer fires, cancel and reschedule. This coalesces rapid file copy batches into a single catalog run.

**Rationale:** File copy operations often create hundreds of events in quick succession. Triggering a catalog run on every event would cause concurrent run conflicts. Debouncing ensures only one run per quiet period.

### 3. Call `CatalogAssetsUseCase` directly (not async bypass)

**Decision:** The debounce timer calls `CatalogAssetsUseCase.execute()` on the executor thread. The use case internally acquires the `catalog_run_state` lock; if another run is in progress, it skips gracefully.

**Rationale:** Reusing the existing use case path ensures all side effects (SSE notifications, cache eviction, stat updates) work correctly.

### 4. Configurable enable/disable

**Decision:** Add `photomanager.folder-watch.enabled: false` (off by default) to `application.yml`. The service starts watching only when enabled.

**Rationale:** Filesystem watching adds a background thread. Users who rely on manual or scheduled catalog runs should not have it start unexpectedly after an upgrade. Defaulting to off is the safer behavior.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| NIO WatchService may miss events on network filesystems | Medium | Document this limitation; watching local filesystems only |
| Watch thread dies silently on exception | Medium | Wrap the watch loop in a `try/catch` with logging and restart logic |
| Debounce timer fires while a catalog run is already in progress | Low | `CatalogAssetsUseCase` lock prevents concurrent runs; the watch-triggered run is skipped |
