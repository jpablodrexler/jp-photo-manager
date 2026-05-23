## Why

The PostgreSQL database contains the entire photo catalog (asset records, EXIF data, tags, folder structure). There is currently no automated backup mechanism. A hardware failure or accidental data deletion would result in permanent catalog loss. Automated scheduled backups with cloud upload and configurable retention prevent catastrophic data loss.

## What Changes

- `DatabaseBackupService` runs `pg_dump` via `ProcessBuilder`, compresses output to GZip, and uploads through a new `CloudStoragePort` domain interface
- Swappable infrastructure implementations for AWS S3, Google Cloud Storage, and Azure Blob
- `@Scheduled` runs backups on a configurable cron schedule; `POST /api/admin/backup` triggers on-demand
- `GET /api/admin/backups` lists stored backups with timestamps and sizes
- Retention policy deletes backups older than N days (configurable)
- Both endpoints are restricted to `ADMIN` role

## Capabilities

### New Capabilities

- `database-backup`: Automated scheduled database backups run `pg_dump`, compress the output, and upload it to a configurable cloud storage provider (S3, GCS, or Azure Blob). Admins can trigger on-demand backups and list stored backups via REST endpoints.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/out/backup/CloudStoragePort.java` — new outbound port interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/adapter/out/backup/S3CloudStorageAdapter.java` — AWS S3 implementation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/service/DatabaseBackupService.java` — backup orchestration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/BackupController.java` — admin REST endpoints
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — backup schedule, retention, cloud provider config
- `JPPhotoManagerWeb/backend/src/test/` — tests for backup service and retention policy
