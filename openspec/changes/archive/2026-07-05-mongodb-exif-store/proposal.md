## Why

EXIF metadata is a natural document — 80-300 loosely-structured tags per image, of which only ~13 are given first-class SQL columns today; everything else is squeezed into a `raw_exif JSONB` column on the PostgreSQL `asset_exif` table. That column has no queryable structure, and the GPS coordinates already stored in `asset_exif.gps_latitude`/`gps_longitude` cannot be searched with proximity queries ("photos within 500m of here") in PostgreSQL without adding PostGIS. Moving `asset_exif` to a MongoDB collection removes the column-sprawl problem, stores the full EXIF tag set as one native document, and unlocks `$near`/`$geoWithin` geospatial queries needed by the upcoming `gps-map-view` (#13) and any future "nearby photos" feature — without adding PostGIS or any new PostgreSQL extension.

## What Changes

- Add a new MongoDB 8 service to local `docker-compose.yml` and document a local run command, mirroring how Kafka/Redis were introduced for prior improvements.
- Add `spring-boot-starter-data-mongodb` dependency and a `spring.data.mongodb.uri` configuration property (env-overridable via `MONGO_URI`).
- Introduce an `AssetExifDocument` (MongoDB `@Document(collection = "asset_exif")`) modeling the same fields currently on `AssetExifEntity`, plus a GeoJSON `Point` field (`location`) derived from `gpsLatitude`/`gpsLongitude` for `2dsphere` indexing.
- Replace `AssetExifRepositoryImpl` (JPA-backed) with a MongoDB-backed implementation of the existing `AssetExifRepository` domain port — the port interface and the `AssetExif` domain model are unchanged, so no use case, controller, or DTO mapper is touched.
- Add a `2dsphere` index on `location` and a unique index on `assetId` at collection startup (`MongoTemplate`-driven index creation, no Flyway migration involved).
- **BREAKING**: Drop the `asset_exif` PostgreSQL table via a Flyway migration once the one-time data migration below has completed. Existing `ON DELETE CASCADE` behavior from `assets` → `asset_exif` is replaced with an explicit `deleteByAssetId` call from `DeleteAssetsUseCaseImpl`/`PurgeAssetsUseCaseImpl`, since MongoDB has no foreign-key cascade.
- One-time data migration: a startup `CommandLineRunner` (guarded by a feature flag / idempotency check) reads all existing `asset_exif` rows from PostgreSQL and upserts them as MongoDB documents before the PostgreSQL table is dropped.
- No change to the `GET /api/assets/{id}/exif` or `PATCH` EXIF endpoints' request/response shape — this is a persistence-layer swap only.

## Capabilities

### New Capabilities
- `mongodb-exif-store`: MongoDB-backed persistence for `AssetExif`, including the `2dsphere` geospatial index on GPS coordinates and the one-time PostgreSQL → MongoDB data migration.

### Modified Capabilities
- `exif-metadata-panel`: the "EXIF metadata is extracted and persisted during cataloging" and "Asset is deleted" requirements change persistence mechanism from a PostgreSQL `asset_exif` table (with `ON DELETE CASCADE`) to a MongoDB `asset_exif` collection (with an explicit delete call on asset deletion). Read/write behavior observed through the REST API is unchanged.
- `raw-exif-jsonb`: the "All EXIF fields are stored during cataloging" requirement changes storage from a PostgreSQL `JSONB` column to a native MongoDB document field; the searchable collapsible panel behavior in the frontend is unchanged.

## Impact

- **Backend**: `infrastructure/persistence/adapter/AssetExifRepositoryImpl.java` (rewritten against MongoDB), new `infrastructure/persistence/document/AssetExifDocument.java`, new `infrastructure/persistence/mongo/MongoAssetExifRepository.java` (Spring Data MongoDB repository), `infrastructure/persistence/mapper/AssetExifDocumentMapper.java` (MapStruct), removal of `AssetExifEntity` and its JPA repository, `pom.xml` (new `spring-boot-starter-data-mongodb` dependency), `application.yml` (new `spring.data.mongodb.uri` property), a new Flyway migration to drop `asset_exif` from PostgreSQL.
- **Infrastructure**: `docker-compose.yml` gains a `mongo` service; local dev docs updated with a `docker run mongo:8` quickstart.
- **Domain**: no change — `AssetExif` model and `AssetExifRepository` port are untouched, preserving the hexagonal architecture boundary.
- **Frontend**: no change — the EXIF panel and raw-EXIF search UI consume the same REST response shape.
- **Testing**: existing `AssetExifRepositoryImpl` unit tests are rewritten against the MongoDB adapter (Mockito on `MongoAssetExifRepository`); integration tests add a Testcontainers MongoDB container alongside the existing PostgreSQL one.
