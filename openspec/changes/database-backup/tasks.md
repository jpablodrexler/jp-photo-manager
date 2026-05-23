## 1. Domain — CloudStoragePort and model

- [ ] 1.1 Create `domain/port/out/backup/CloudStoragePort.java` with methods: `void upload(String key, InputStream data)`, `List<BackupEntry> list()`, `void delete(String key)`
- [ ] 1.2 Create `domain/model/BackupEntry.java` record with `String filename`, `long sizeBytes`, `Instant createdAt`

## 2. Infrastructure — S3 adapter

- [ ] 2.1 Add `software.amazon.awssdk:s3` to `pom.xml`
- [ ] 2.2 Create `infrastructure/adapter/out/backup/S3CloudStorageAdapter.java` implementing `CloudStoragePort`; inject bucket name and prefix from `application.yml`; annotate with `@ConditionalOnProperty(name = "photomanager.backup.provider", havingValue = "s3")`
- [ ] 2.3 Create stub implementations `GcsCloudStorageAdapter` and `AzureBlobStorageAdapter` that throw `UnsupportedOperationException`

## 3. DatabaseBackupService

- [ ] 3.1 Create `application/service/DatabaseBackupService.java` annotated with `@Service`
- [ ] 3.2 Inject `CloudStoragePort`, database connection properties, and `@Value("${photomanager.backup.retention-days:7}")`
- [ ] 3.3 Implement `String backup()`: build `pg_dump` command via `ProcessBuilder`; pipe stdout through `GZIPOutputStream` directly to `CloudStoragePort.upload()`; return backup filename
- [ ] 3.4 Set `process.waitFor(30, TimeUnit.MINUTES)` timeout; log error if exceeded
- [ ] 3.5 After upload, call retention cleanup: list all backups, delete any older than `retentionDays`
- [ ] 3.6 Annotate `backup()` with `@Scheduled(cron = "${photomanager.backup.schedule}")`

## 4. HTTP adapter

- [ ] 4.1 Create `infrastructure/web/controller/BackupController.java`
- [ ] 4.2 Add `POST /api/admin/backup` annotated `@PreAuthorize("hasRole('ADMIN')")`; call `databaseBackupService.backup()` and return `{ "filename": <result> }`
- [ ] 4.3 Add `GET /api/admin/backups` annotated `@PreAuthorize("hasRole('ADMIN')")`; call `cloudStoragePort.list()` sorted by `createdAt` descending

## 5. Configuration

- [ ] 5.1 Add to `application.yml`:
  - `photomanager.backup.schedule: "0 0 2 * * ?"` (2 AM daily)
  - `photomanager.backup.retention-days: 7`
  - `photomanager.backup.provider: s3`
  - `photomanager.backup.s3.bucket: <bucket-name>`
  - `photomanager.backup.s3.prefix: backups/`

## 6. Backend unit tests

- [ ] 6.1 Test that `DatabaseBackupService.backup()` invokes `ProcessBuilder` with correct `pg_dump` arguments
- [ ] 6.2 Test that retention cleanup deletes entries older than `retentionDays`
- [ ] 6.3 Test that `POST /api/admin/backup` returns 403 for VIEWER role
- [ ] 6.4 Test that `GET /api/admin/backups` returns a list sorted by `createdAt` descending

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
