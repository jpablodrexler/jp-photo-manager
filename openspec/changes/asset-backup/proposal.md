## Why

Users have no way to back up a specific folder, album, or search result from within the photo manager. A dedicated asset backup feature with configurable scoping, volume splitting, and scheduling gives users control over which assets are backed up and when, complementing the database backup (`database-backup` #44) which only covers catalog metadata.

## What Changes

- A `backup_definitions` table stores user-defined backup scopes (folder, album, or all) with output path, format, volume size, and schedule
- `backup_run_log` tracks each backup execution with timestamps, status, and statistics
- `POST /api/backup/{id}/run` triggers an on-demand run; cron schedules are managed by Spring `TaskScheduler`
- Progress is streamed via SSE; the output is split at a configurable volume size
- A new `/backup` frontend route provides a definitions list + configure + run UI (mirrors the convert page)
- Requires `archive-support` (#60) for zip/tar.gz writing infrastructure

## Capabilities

### New Capabilities

- `asset-backup`: Users can define backup scopes (folder, album, or full catalog), choose a format (zip or tar.gz), set a volume split size, and configure a schedule. Backups are stored as sequentially numbered archive files and run history is viewable in the frontend.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V25__create_backup_tables.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/AssetBackupService.java` — backup orchestration and scheduling
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/BackupDefinitionController.java` — CRUD for definitions; run endpoint
- `JPPhotoManagerWeb/backend/src/test/` — tests for backup scope resolution and volume splitting
- `JPPhotoManagerWeb/frontend/src/app/features/backup/backup.component.ts` — new feature page
- `JPPhotoManagerWeb/frontend/src/app/app.routes.ts` — add `/backup` route
