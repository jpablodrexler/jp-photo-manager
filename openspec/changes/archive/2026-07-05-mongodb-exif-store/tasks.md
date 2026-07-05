## 1. Infrastructure provisioning

- [x] 1.1 Add a `mongo` service (`mongo:8`, port `27017`, named volume) to `JPPhotoManagerWeb/docker-compose.yml`, following the same pattern used for the existing `kafka` and `redis` services
- [x] 1.2 Add `spring-boot-starter-data-mongodb` to `backend/pom.xml`
- [x] 1.3 Add `spring.data.mongodb.uri` to `application.yml` with an env-overridable default (`${MONGO_URI:mongodb://localhost:27017/photomanager}`), and wire `MONGO_URI` through the `backend` service environment in `docker-compose.yml`
- [x] 1.4 Document the local MongoDB quickstart (`docker run -d --name photomanager-mongo -p 27017:27017 mongo:8`) in `CLAUDE.md` alongside the existing PostgreSQL/Kafka quickstart commands

## 2. Domain document model and mapper

- [x] 2.1 Create `infrastructure/persistence/document/AssetExifDocument.java` annotated `@Document(collection = "asset_exif")`, with fields mirroring `AssetExifEntity` (`assetId`, `cameraMake`, `cameraModel`, `lensModel`, `exposureTime`, `fNumber`, `isoSpeed`, `focalLength`, `dateTaken`, `widthPixels`, `heightPixels`, `gpsLatitude`, `gpsLongitude`, `rawExif`), plus a `GeoJsonPoint location` field
- [x] 2.2 Create `infrastructure/persistence/mapper/AssetExifDocumentMapper.java` (MapStruct) converting between `AssetExifDocument` and the domain `AssetExif` model, deriving/unpacking `location` from `gpsLatitude`/`gpsLongitude` on write and ignoring `location` on read (scalars are the source of truth for the API response)

## 3. MongoDB repository adapter

- [x] 3.1 Create `infrastructure/persistence/mongo/MongoAssetExifRepository.java` as a Spring Data `MongoRepository<AssetExifDocument, String>` with a `findByAssetId(Long assetId)` query method
- [x] 3.2 Rewrite `infrastructure/persistence/adapter/AssetExifRepositoryImpl.java` to implement `AssetExifRepository` using `MongoAssetExifRepository` and `AssetExifDocumentMapper` instead of the JPA repository — `findByAssetId`, `save` (upsert keyed by `assetId`), and `deleteByAssetId` all delegate to the Mongo repository
- [ ] 3.3 Remove `AssetExifEntity.java`, `JpaAssetExifRepository.java`, and `AssetExifEntityMapper.java` once the new adapter is verified working (keep `JpaAssetExifRepository` temporarily if still referenced by the migration runner in section 5; remove in section 7 after the schema-drop deploy) — **deferred**: these three files must stay in place as long as `AssetExifMongoMigrationRunner` (section 6) exists, since the runner reads old PostgreSQL rows through `JpaAssetExifRepository`. Removal now happens together with 8.2.

## 4. Index creation

- [x] 4.1 Create a Spring `ApplicationRunner`/`@PostConstruct` component that ensures a unique index on `assetId` exists on the `asset_exif` collection at startup (idempotent — `ensureIndex` is a no-op if the index already exists)
- [x] 4.2 In the same component, ensure a `2dsphere` index on `location` exists on the `asset_exif` collection at startup

## 5. Cascade delete wiring

- [x] 5.1 Add an explicit `assetExifRepository.deleteByAssetId(assetId)` call to the use case that permanently purges an `Asset` (`PurgeAssetsUseCaseImpl` or equivalent), replacing the PostgreSQL `ON DELETE CASCADE` behavior
- [x] 5.2 Verify soft-delete use cases (`SoftDeleteAssetUseCaseImpl` or equivalent) do NOT call `deleteByAssetId`, so EXIF data survives a soft-delete/restore cycle

## 6. One-time data migration

- [x] 6.1 Create `infrastructure/migration/AssetExifMongoMigrationRunner.java` implementing `CommandLineRunner`: on startup, check a `migration_status` MongoDB collection for a completion marker document (`_id: "asset_exif_pg_migration"`); if absent, read all PostgreSQL `asset_exif` rows in batches (e.g. 500 rows) via the still-present `JpaAssetExifRepository`, map to `AssetExifDocument`, and upsert into MongoDB keyed by `assetId`
- [x] 6.2 After all batches complete successfully, write the completion marker document to `migration_status` so subsequent startups skip the migration
- [x] 6.3 Log progress per batch (rows processed / total) so migration progress is observable in application logs during the first deploy

## 7. Testing

- [x] 7.1 Rewrite the existing `AssetExifRepositoryImplTest` unit test against the MongoDB-backed adapter, mocking `MongoAssetExifRepository` and `AssetExifDocumentMapper` (Mockito), covering `findByAssetId`, `save` (both insert and update-in-place paths), and `deleteByAssetId`
- [x] 7.2 Add a unit test for `AssetExifDocumentMapper` covering the `location` GeoJSON derivation (both coordinates present, one/both absent)
- [x] 7.3 Add a unit test for `AssetExifMongoMigrationRunner` covering: marker absent + rows to migrate, marker present (no-op), and a resumed run re-upserting already-copied rows without duplication
- [x] 7.4 Add a Testcontainers MongoDB module to the integration test suite (alongside the existing `PostgresIntegrationTest` base class) and add an integration test verifying an asset cataloged end-to-end has its EXIF document persisted and retrievable via `GET /api/assets/{id}/exif`
- [x] 7.5 Add an integration test verifying that purging an asset removes its MongoDB `asset_exif` document, and that soft-deleting an asset does not

## 8. Verification and schema drop (second deploy)

- [ ] 8.1 After deploying section 1-7 and confirming (via ops/manual check) that the MongoDB `asset_exif` document count matches the PostgreSQL `asset_exif` row count, add a new Flyway migration dropping the PostgreSQL `asset_exif` table and its foreign key to `assets` — **intentionally not done in this implementation pass.** This task is gated on a live-deployment verification step (operator confirms row-count parity between the two stores) that has no meaning until sections 1-7 have actually run against production data. Implementing the Flyway drop migration now would also be actively harmful: Flyway migrations execute during Spring context refresh, *before* `CommandLineRunner`s run (`AssetExifMongoMigrationRunner` is a `CommandLineRunner`), so shipping the drop in the same deploy as the migration runner would drop the PostgreSQL `asset_exif` table before the runner ever reads a row, destroying the migration it depends on. This must ship as a separate, later deploy per the design's explicit "Deploy 2" migration plan.
- [ ] 8.2 Remove `AssetExifMongoMigrationRunner`, `JpaAssetExifRepository`, `AssetExifEntity`, and `AssetExifEntityMapper` from the codebase now that PostgreSQL is no longer the source of EXIF data — **deferred alongside 8.1** (same reasoning: only safe to remove once 8.1 has landed).
- [ ] 8.3 Update `CLAUDE.md`'s Web Architecture section to reflect MongoDB as the `AssetExif` persistence store instead of PostgreSQL — **partially done**: the root `CLAUDE.md` and `JPPhotoManagerWeb/CLAUDE.md` Persistence sections already describe `asset_exif` as MongoDB-backed (see 9.1). The remaining piece — removing PostgreSQL `asset_exif` references entirely — depends on 8.1/8.2 landing first.

## 9. Documentation

- [x] 9.1 Update the `Persistence` section of `CLAUDE.md` (Web Architecture) to note that `asset_exif` lives in MongoDB while all other tables remain in PostgreSQL
- [x] 9.2 Add the `mongo` service to the "Local development prerequisites" quickstart list in `CLAUDE.md`
