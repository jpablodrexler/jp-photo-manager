## 1. Database migration

- [ ] 1.1 Create `V25__create_backup_tables.sql`:
  - `CREATE TABLE backup_definitions (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), name VARCHAR(256) NOT NULL, scope_type VARCHAR(20) NOT NULL, scope_value VARCHAR(1024) NULL, output_path VARCHAR(1024) NOT NULL, format VARCHAR(10) NOT NULL DEFAULT 'zip', volume_size_mb INT NOT NULL DEFAULT 0, cron_schedule VARCHAR(100) NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW())`
  - `CREATE TABLE backup_run_log (id BIGSERIAL PRIMARY KEY, definition_id BIGINT NOT NULL REFERENCES backup_definitions(id), started_at TIMESTAMP NOT NULL, finished_at TIMESTAMP NULL, status VARCHAR(20) NOT NULL, files_written INT NOT NULL DEFAULT 0, bytes_written BIGINT NOT NULL DEFAULT 0, error_message TEXT NULL)`

## 2. AssetBackupService

- [ ] 2.1 Create `infrastructure/service/AssetBackupService.java` annotated with `@Service`
- [ ] 2.2 Inject `AssetRepositoryPort`, `ArchiveService`, `TaskScheduler`; maintain `Map<Long, ScheduledFuture<?>> scheduledJobs`
- [ ] 2.3 Implement `void run(long definitionId, Consumer<BackupNotification> consumer)`:
  - Load definition; resolve file paths by `scopeType`
  - Write archives with volume splitting (track bytes; close + open new file when threshold exceeded)
  - Emit progress per asset; log run result to `backup_run_log`
- [ ] 2.4 Implement `void scheduleDefinition(BackupDefinition def)`: if `cronSchedule` non-null, schedule `run()` via `taskScheduler.schedule()`; store future in `scheduledJobs`
- [ ] 2.5 Call `scheduleDefinition()` on service init for all active definitions; call on create/update; cancel on delete

## 3. HTTP adapter

- [ ] 3.1 Create `BackupDefinitionController` with `@PreAuthorize("hasRole('ADMIN')")`:
  - `GET /api/backup/definitions` — list
  - `POST /api/backup/definitions` — create
  - `PUT /api/backup/definitions/{id}` — update; reschedule
  - `DELETE /api/backup/definitions/{id}` — delete; cancel schedule
  - `POST /api/backup/{id}/run` — on-demand run; return `SseEmitter`
  - `GET /api/backup/{id}/runs` — run history

## 4. Backend unit tests

- [ ] 4.1 Test that `AssetBackupService.run()` correctly resolves FOLDER scope to only assets in that folder
- [ ] 4.2 Test that volume splitting creates a new archive file when threshold is exceeded
- [ ] 4.3 Test that `POST /api/backup/definitions` returns 403 for VIEWER role
- [ ] 4.4 Test that run history is created with correct statistics

## 5. Frontend — BackupComponent

- [ ] 5.1 Create `features/backup/backup.component.ts` as a standalone component
- [ ] 5.2 Display definitions in a `MatTable` with columns: Name, Scope, Format, Schedule, Actions (Run, Edit, Delete)
- [ ] 5.3 Definition form: name, scope type (`MatSelect`: Folder/Album/All), scope value (folder picker or album select), output path, format dropdown (zip/tar.gz), volume size, cron schedule
- [ ] 5.4 Run button opens SSE progress dialog (reuse pattern from catalog/sync/convert)
- [ ] 5.5 Register lazy route `/backup` in `app.routes.ts`

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
