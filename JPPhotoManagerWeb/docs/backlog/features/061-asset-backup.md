# Feature 61 — asset-backup

**As a** administrator, **I want** configurable backups of a chosen scope of assets into numbered archive files, **so that** I keep an offline copy of my photos, split into sizes I can store.

## Acceptance Criteria
- AC1: Given the /backup page, when I create a definition with a scope (folder, album, saved search result or entire catalog), a format (zip or tar.gz), a volume size and an optional cron expression, then it is saved; a VIEWER cannot manage definitions.
- AC2: Given a definition, when I run it on demand, then sequentially numbered archives (for example backup_photos_001.zip) are written.
- AC3: Given the volume size is exceeded, when archiving continues, then a new numbered archive starts.
- AC4: Given a cron expression, when a definition is created or updated, then it is scheduled and rescheduled dynamically.
- AC5: Given a run, when it progresses, then SSE progress and per-file logging are shown.
- AC6: Given the run history, when I open it, then it shows timestamps, status, files written, bytes and errors for completed backups.

## Technical notes

Backup a configurable scope of assets (folder, album, saved search result, or entire catalog) to one or more sequentially numbered archive files (e.g. `backup_photos_001.zip`, `backup_photos_002.zip`) split at a configurable volume size; format is zip or tar.gz (reuses `archive-support` #60 writing infrastructure); trigger is manual (`POST /api/backup/{id}/run`) or a per-definition cron expression scheduled dynamically via Spring `TaskScheduler` + `CronTrigger` (cancelled and rescheduled on definition update); new `/backup` frontend route mirrors the convert page structure (definitions list → configure → run → results with SSE progress and per-file logging); backend stores definitions in `backup_definitions` and run history in `backup_run_log` (timestamps, status, files written, bytes, errors); new Flyway migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
