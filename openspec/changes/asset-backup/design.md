## Context

`ArchiveService` from `archive-support` (#60) provides `writeTarGz()` and (via `java.util.zip`) zip writing. The backup service resolves asset file paths by scope, writes them sequentially into split archives, and streams progress via SSE. Spring `TaskScheduler` + `CronTrigger` manages scheduled runs; the trigger is cancelled and rescheduled when the definition is updated.

## Goals / Non-Goals

**Goals:**
- Flyway V25: `CREATE TABLE backup_definitions (id BIGSERIAL PK, user_id BIGINT FK, name VARCHAR(256), scope_type VARCHAR(20) — FOLDER/ALBUM/ALL, scope_value VARCHAR(1024), output_path VARCHAR(1024), format VARCHAR(10), volume_size_mb INT DEFAULT 0, cron_schedule VARCHAR(100) NULL, created_at TIMESTAMP)` + `CREATE TABLE backup_run_log (id BIGSERIAL PK, definition_id BIGINT FK, started_at TIMESTAMP, finished_at TIMESTAMP, status VARCHAR(20), files_written INT, bytes_written BIGINT, error_message TEXT NULL)`
- `AssetBackupService.run(definitionId, sseConsumer)`:
  1. Resolve asset file paths by scope (FOLDER: assets in folder; ALBUM: assets in album; ALL: all non-deleted assets)
  2. Write archives using `ArchiveService`; split when file size exceeds `volumeSizeMb` (0 = no split)
  3. Emit SSE progress events per asset; emit DONE with statistics
  4. Insert `backup_run_log` row on start and update on completion
- `POST /api/backup/{id}/run` returns `SseEmitter`; requires `ADMIN` role
- `GET /api/backup/definitions`, `POST /api/backup/definitions`, `PUT /api/backup/definitions/{id}`, `DELETE /api/backup/definitions/{id}` — CRUD; on create/update reschedule cron via `taskScheduler.schedule()`
- Frontend `/backup` page: definitions table (same structure as `/convert`) + definition form with scope, output path, format, volume size, schedule fields; run button opens SSE progress dialog

**Non-Goals:**
- Restore from backup via the UI (restore is a manual operator action)
- Cloud upload of backup archives (see `database-backup` for cloud upload; this feature writes to a local path)
- Per-file inclusion/exclusion rules

## Decisions

### 1. Volume splitting by tracking bytes written

**Decision:** Track cumulative bytes written per archive stream. When the threshold is exceeded, close the current archive and open the next (`backup_001.zip`, `backup_002.zip`, ...).

**Rationale:** Simple byte counter works correctly for both zip and tar.gz. No random access needed.

### 2. Spring `TaskScheduler` for scheduling

**Decision:** Keep a `Map<Long, ScheduledFuture<?>>` in `AssetBackupService` (keyed by definition ID). On create/update, cancel the existing future and schedule a new `CronTrigger`.

**Rationale:** Spring's `TaskScheduler` with `CronTrigger` is the standard way to manage dynamic cron schedules in Spring Boot without a separate scheduler library.

### 3. `ADMIN` role restriction

**Decision:** All backup endpoints require `@PreAuthorize("hasRole('ADMIN')")`.

**Rationale:** Backup operations read all assets regardless of folder permissions. Consistent with `database-backup` (#44).

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Backup runs during catalog causing file conflicts | Low | Document that concurrent operations should be avoided; a distributed lock can be added later |
| Scheduled backup fails silently | Low | Always insert a `backup_run_log` row; admin can check run history |
