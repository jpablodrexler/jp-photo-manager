# database-backup

Automated scheduled database backups run `pg_dump`, compress the output, and upload it to a configurable cloud storage provider (S3, GCS, or Azure Blob). Admins can trigger on-demand backups and list stored backups via REST endpoints.

---

## ADDED Requirements

### Requirement: Database backups run on a configurable schedule

The `DatabaseBackupService` SHALL run `pg_dump` on the schedule defined by `photomanager.backup.schedule` (cron expression), compress the output with GZip, and upload to the configured cloud storage provider.

#### Scenario: Scheduled backup uploads a compressed dump

- **GIVEN** `photomanager.backup.schedule` is set to run at 2:00 AM daily
- **WHEN** 2:00 AM arrives
- **THEN** a file named `backup-<ISO-8601-timestamp>.sql.gz` is uploaded to cloud storage

#### Scenario: Backup retention policy deletes old backups

- **GIVEN** `photomanager.backup.retention-days` is set to 7 and there are 10 backups stored
- **WHEN** a new backup completes
- **THEN** backups older than 7 days are deleted from cloud storage; at most 7 backups remain

### Requirement: On-demand backup can be triggered by admins

`POST /api/admin/backup` SHALL trigger a backup immediately and return the filename of the uploaded backup file. This endpoint SHALL require `ADMIN` role.

#### Scenario: Admin triggers on-demand backup

- **GIVEN** an authenticated ADMIN user
- **WHEN** `POST /api/admin/backup` is called
- **THEN** the response is `200 OK` with body `{ "filename": "backup-2026-05-23T02:00:00Z.sql.gz" }`

#### Scenario: VIEWER cannot trigger backup

- **GIVEN** an authenticated VIEWER user
- **WHEN** `POST /api/admin/backup` is called
- **THEN** the response is `403 Forbidden`

### Requirement: Admins can list stored backups

`GET /api/admin/backups` SHALL return a list of stored backups with filename, size, and creation timestamp. This endpoint SHALL require `ADMIN` role.

#### Scenario: Admin lists stored backups

- **GIVEN** 3 backups are stored in cloud storage
- **WHEN** `GET /api/admin/backups` is called
- **THEN** the response is `200 OK` with a JSON array of `{ filename, sizeBytes, createdAt }` objects sorted by `createdAt` descending
