# Feature 44 — database-backup

**As a** administrator, **I want** scheduled database backups uploaded to cloud storage, **so that** I can recover the catalog if the database is lost.

## Acceptance Criteria
- AC1: Given the schedule, when it fires, then pg_dump output is compressed and uploaded to the configured provider (S3, Google Cloud Storage or Azure Blob).
- AC2: Given the retention setting, when a backup runs, then backups older than N days are deleted.
- AC3: Given an ADMIN, when they call POST /api/admin/backup, then a backup runs on demand; a VIEWER is refused.
- AC4: Given an ADMIN, when they call GET /api/admin/backups, then stored backups are listed with timestamps and sizes.
- AC5: Given the configuration, when the application starts, then schedule, retention, provider, bucket and prefix are read from application.yml.

## Technical notes

Add `DatabaseBackupService` with `@Scheduled` that runs `pg_dump` via `ProcessBuilder`, compresses the output with GZip to a temp file, and uploads it through a new `CloudStoragePort` interface (domain) with swappable infrastructure implementations for AWS S3, Google Cloud Storage, and Azure Blob; enforce a configurable retention policy (delete backups older than N days); expose `POST /api/admin/backup` for on-demand trigger and `GET /api/admin/backups` to list stored backups with timestamps and sizes; schedule, retention, cloud provider, bucket, and prefix are all configurable in `application.yml`; Option B (Docker sidecar using `prodrigestivill/postgres-backup-local` + `rclone`) is documented as a deployment alternative for environments where backup must be decoupled from application health

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
