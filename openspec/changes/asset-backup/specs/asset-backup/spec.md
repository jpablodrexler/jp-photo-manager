# asset-backup

Users can define backup scopes (folder, album, or full catalog), choose a format (zip or tar.gz), set a volume split size, and configure a schedule. Backups are stored as sequentially numbered archive files and run history is viewable in the frontend.

---

## ADDED Requirements

### Requirement: Backup definitions can be created and managed

`POST /api/backup/definitions` SHALL create a new backup definition. `GET /api/backup/definitions` SHALL list all definitions for the authenticated admin user.

#### Scenario: Admin creates a backup definition

- **GIVEN** an authenticated ADMIN user
- **WHEN** `POST /api/backup/definitions` is called with `{ "name": "Photos weekly", "scopeType": "FOLDER", "scopeValue": "/photos", "outputPath": "/backups", "format": "zip", "volumeSizeMb": 500, "cronSchedule": "0 0 3 * * 0" }`
- **THEN** the definition is created and returned with an assigned ID; the cron schedule is registered

#### Scenario: VIEWER cannot manage backup definitions

- **GIVEN** an authenticated VIEWER user
- **WHEN** `POST /api/backup/definitions` is called
- **THEN** the response is `403 Forbidden`

### Requirement: Backup can be triggered on demand

`POST /api/backup/{id}/run` SHALL trigger a backup run immediately and stream progress via SSE.

#### Scenario: On-demand backup runs and produces archives

- **GIVEN** a backup definition scoped to `/photos` with zip format and 500 MB volume size
- **WHEN** `POST /api/backup/{id}/run` is called
- **THEN** the SSE stream reports progress per asset; on completion, sequentially numbered zip files (e.g. `backup_001.zip`, `backup_002.zip`) are written to the output path

### Requirement: Volume splitting produces sequentially numbered archives

When the configured volume size is exceeded during a backup run, the current archive SHALL be closed and a new one SHALL be opened.

#### Scenario: Volume size exceeded triggers new archive

- **GIVEN** a backup with `volumeSizeMb = 100` and 50 assets totaling 250 MB
- **WHEN** the backup runs
- **THEN** three zip files are produced: `backup_001.zip`, `backup_002.zip`, `backup_003.zip`

### Requirement: Backup run history is stored and retrievable

Each backup run SHALL create a `backup_run_log` row with status, start/end time, and statistics.

#### Scenario: Run history shows completed backup statistics

- **GIVEN** a backup definition with run history
- **WHEN** `GET /api/backup/{id}/runs` is called
- **THEN** the response includes a list of run logs with `status`, `startedAt`, `finishedAt`, `filesWritten`, and `bytesWritten`
