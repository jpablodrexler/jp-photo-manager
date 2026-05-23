## Context

The backend runs inside Docker (after `postgres-dockerize`). `pg_dump` is available in the `postgres:15` image and must be invoked against the database container. The backup file is a GZip-compressed SQL dump named `backup-<timestamp>.sql.gz`. `CloudStoragePort` is a new domain interface; the initial implementation targets AWS S3 via the AWS SDK v2.

## Goals / Non-Goals

**Goals:**
- `DatabaseBackupService.backup()` runs `pg_dump` via `ProcessBuilder` with environment variables from `application.yml`, pipes stdout through `GZIPOutputStream`, and uploads via `CloudStoragePort`
- `@Scheduled(cron = "${photomanager.backup.schedule}")` calls `backup()` automatically
- Retention: after each backup, delete files from cloud storage older than `${photomanager.backup.retention-days}` days
- `POST /api/admin/backup` triggers `backup()` synchronously (or `@Async`) and returns the backup filename
- `GET /api/admin/backups` calls `CloudStoragePort.list()` and returns `[ { filename, sizeBytes, createdAt } ]`
- Both endpoints annotated `@PreAuthorize("hasRole('ADMIN')")`
- S3 adapter implemented; GCS and Azure adapters are stubs with `throw new UnsupportedOperationException()`

**Non-Goals:**
- Restoring from backup via the API (restore requires manual operator action)
- Encrypting the backup file (S3 server-side encryption covers this at rest)
- Docker sidecar option (documented in the proposal as an alternative but not implemented)

## Decisions

### 1. `ProcessBuilder` for `pg_dump`

**Decision:** Invoke `pg_dump` via `ProcessBuilder`, reading its stdout in a separate thread and piping to `GZIPOutputStream`.

**Rationale:** Running `pg_dump` as a subprocess avoids a JDBC-level dump and matches how DBAs typically back up PostgreSQL. The subprocess has access to the same Docker network as the Spring Boot container.

### 2. `CloudStoragePort` domain interface with S3 implementation

**Decision:** Define `CloudStoragePort` in `domain/port/out/backup/` with `upload(String key, InputStream data, long size)`, `list(): List<BackupEntry>`, and `delete(String key)`. Inject the implementation via `@ConditionalOnProperty("photomanager.backup.provider")`.

**Rationale:** The port/adapter pattern keeps the backup service independent of the cloud provider. Swapping providers requires only adding a new adapter and changing a config value.

### 3. Retention enforced after each backup

**Decision:** After a successful upload, call `CloudStoragePort.list()` and delete any entry older than `retentionDays`.

**Rationale:** Simpler than a separate scheduled job; retention is evaluated once per backup cycle.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| `pg_dump` process hangs | Medium | Set a timeout on `process.waitFor(30, TimeUnit.MINUTES)`; log and alert if exceeded |
| S3 credentials misconfigured | Medium | Health indicator (see `actuator-health-indicators`) can validate cloud storage connectivity |
| Large dumps exhaust disk in temp directory | Low | Stream directly to cloud storage without fully materializing on disk |
