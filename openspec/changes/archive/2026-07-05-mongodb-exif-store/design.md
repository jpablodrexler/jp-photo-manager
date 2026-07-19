## Context

`AssetExif` is persisted today as a PostgreSQL table (`asset_exif`, one-to-one with `assets` via `asset_id`), mapped through JPA (`AssetExifEntity` → `JpaAssetExifRepository` → `AssetExifRepositoryImpl implements AssetExifRepository`). The domain port `AssetExifRepository` (`findByAssetId`, `save`, `deleteByAssetId`) and the domain model `AssetExif` are already clean hexagonal boundaries — only the infrastructure adapter needs to change. `raw-exif-jsonb` (#63, already implemented) added a `raw_exif JSONB` column holding all EXIF tags as a `Map<String, String>`; this is the primary pain point driving the move, since JSONB gives no native geospatial or nested-document query capability. GPS coordinates (`gps_latitude`, `gps_longitude`) are already extracted and stored as plain `Double` columns, but PostgreSQL requires the PostGIS extension for efficient proximity queries — not currently installed and out of scope to add.

`gps-map-view` (#13) has SDD artifacts created but is not yet implemented in code (no `GeocodingPort` exists). This change does not implement #13; it only lays the persistence groundwork (`2dsphere` index) that #13 and any future "nearby photos" feature will query against.

## Goals / Non-Goals

**Goals:**
- Replace the PostgreSQL `asset_exif` table with a MongoDB `asset_exif` collection without changing the `AssetExifRepository` domain port or any use case/controller that depends on it.
- Preserve the exact `GET /api/assets/{id}/exif` response shape (no frontend change).
- Add a `2dsphere` index on GPS coordinates so future geospatial queries (`$near`, `$geoWithin`) are possible without a follow-up migration.
- Migrate existing PostgreSQL `asset_exif` rows to MongoDB with zero data loss, then drop the PostgreSQL table.
- Keep the cascade-delete guarantee: deleting an asset must always remove its EXIF document, even though MongoDB has no foreign-key `ON DELETE CASCADE`.

**Non-Goals:**
- Implementing `gps-map-view` (#13) itself, or any UI for geospatial search.
- Migrating any other table (`assets`, `asset_tags`, `users`, etc.) to MongoDB — this change is scoped to `asset_exif` only.
- Introducing MongoDB transactions across `assets` (PostgreSQL) and `asset_exif` (MongoDB) — the two stores are eventually consistent, mitigated as described below.
- Performance tuning beyond the two indexes described (`assetId` unique, `location` 2dsphere).

## Decisions

**Decision: Spring Data MongoDB over a raw `MongoClient`/driver.**
`spring-boot-starter-data-mongodb` gives `MongoTemplate` and repository-style `MongoRepository<AssetExifDocument, String>` support consistent with the JPA pattern already used elsewhere in the codebase, minimizing the learning curve and boilerplate. Alternative considered: raw `mongodb-driver-sync` — rejected, it would require hand-written BSON mapping with no material benefit for this document shape.

**Decision: `assetId` (Long) as the MongoDB document's business key, backed by a unique index — not the Mongo `_id`.**
`_id` remains an auto-generated `ObjectId` for idiomatic Mongo document identity; `assetId` gets a separate unique index for lookups (`findByAssetId`), mirroring the existing `findByAssetAssetId` JPA query. Alternative considered: using `assetId` directly as `_id` — rejected because it complicates the mapper (would need a custom `@Id` type converter) for no query-performance benefit at this data volume (one document per asset).

**Decision: GeoJSON `Point` field (`location`) computed from `gpsLatitude`/`gpsLongitude` at write time, kept alongside the flat scalar fields.**
MongoDB's `2dsphere` index requires a GeoJSON-shaped field (`{ type: "Point", coordinates: [lng, lat] }`). The flat `gpsLatitude`/`gpsLongitude` scalars are retained on the document so the REST response mapping is a direct field copy (no coordinate-format conversion in the mapper for the common case); `location` is populated only when both coordinates are non-null, and is `null` otherwise (excluded from the sparse `2dsphere` index automatically). Alternative considered: storing only `location` and deriving lat/lng in the mapper at read time — rejected as it adds a conditional unpack step to every read for a write-time cost that's cheap and infrequent (once per catalog run per asset).

**Decision: Explicit `deleteByAssetId` call from asset-deletion use cases, not a Mongo change stream or trigger.**
PostgreSQL's `ON DELETE CASCADE` on the FK is replaced with an explicit call from `DeleteAssetsUseCaseImpl` / `PurgeAssetsUseCaseImpl` to `assetExifRepository.deleteByAssetId(assetId)`, executed in the same use-case method right after (or before) the `Asset` row is deleted. This mirrors the pattern already used for other non-PostgreSQL-cascaded cleanup (e.g., thumbnail file deletion via `ThumbnailPort`). Alternative considered: MongoDB Change Streams watching `assets` deletions — rejected as substantial operational complexity (requires a replica set) for a call that a single line of application code already satisfies synchronously and testably.

**Decision: One-time data migration via a `CommandLineRunner` gated by a completion marker document, not a Flyway/Mongock combined migration tool.**
A `@Component` `AssetExifMongoMigrationRunner` implementing `CommandLineRunner` runs at every startup but exits immediately if a marker document (`{ _id: "asset_exif_pg_migration", completedAt: <timestamp> }`) already exists in a small `migration_status` MongoDB collection. If the marker is absent, it reads all PostgreSQL `asset_exif` rows in batches (reusing `JpaAssetExifRepository` kept temporarily for this purpose) and upserts them as `AssetExifDocument`s keyed by `assetId`, then writes the marker document as the last step. Idempotency during a resumed run is guaranteed by the `assetId`-keyed upsert (re-copying an already-migrated row is a harmless no-op overwrite with the same data), so a crash mid-migration simply results in the runner re-processing all batches on the next startup — safe, if slightly redundant — until it completes and writes the marker. Alternative considered: gating on `mongoTemplate.count(asset_exif) == 0` — rejected because a partially-completed run (nonzero but incomplete document count) would look identical to "already migrated," silently skipping the remaining rows on restart. Alternative considered: a dedicated Mongock changelog — rejected as an additional dependency for a migration that runs a handful of times total and is simple enough to express as a guarded runner; the team has no other Mongock usage to amortize the learning cost against.

**Decision: Drop the PostgreSQL `asset_exif` table in a *separate, later* Flyway migration gated behind a manual step, not automatically in the same deploy as the migration runner.**
The migration plan below runs the data copy and the schema drop in two distinct deploys, matching the "dual-write window" caution already established in this codebase for `redis-refresh-tokens` (#79). This avoids an irreversible schema change landing in the same release as an unverified data copy.

## Risks / Trade-offs

- [Risk] MongoDB and PostgreSQL are no longer transactionally consistent — an `Asset` row could exist with no corresponding `AssetExifDocument` (or vice versa) if a write to one store succeeds and the other fails. → Mitigation: `AssetExifRepositoryImpl.save()` is called synchronously within the same catalog-item-writer step that already tolerates a missing EXIF row (`GET /api/assets/{id}/exif` already returns all-null fields when no row exists, per the existing `exif-metadata-panel` spec) — the failure mode is "no EXIF data shown," not a crash or an orphaned reference. Deletion is mitigated by explicit application-level cascade (see Decision above).
- [Risk] The one-time migration runner could time out or partially fail on a very large `asset_exif` table, leaving MongoDB partially populated. → Mitigation: the runner processes in fixed-size batches (e.g. 500 rows) with a log line per batch, is idempotent (safe to re-run), and the PostgreSQL table is not dropped until an operator confirms `SELECT COUNT(*) FROM asset_exif` in PostgreSQL matches the MongoDB collection count.
- [Risk] New infrastructure dependency (MongoDB) increases operational surface — a new container to provision, monitor, and back up. → Mitigation: documented in `docker-compose.yml` and `CLAUDE.md` following the exact pattern already used for Kafka and Redis; no HA/replica-set requirement for this initial scope (single-node MongoDB is acceptable since `asset_exif` data is regenerable from source images via re-cataloging if lost).
- [Risk] Existing unit/integration tests for `AssetExifRepositoryImpl` are JPA-specific and must be fully rewritten, not adapted. → Mitigation: scoped explicitly in tasks; integration tests add a Testcontainers MongoDB module alongside the existing PostgreSQL `PostgresIntegrationTest` base class.

## Migration Plan

1. **Deploy 1 (dual-write groundwork)**: Ship the MongoDB-backed `AssetExifRepositoryImpl`, the `AssetExifMongoMigrationRunner`, and the new `mongo` `docker-compose.yml` service. On startup, the runner copies all existing PostgreSQL `asset_exif` rows into MongoDB. From this point on, all reads and writes for `AssetExif` go through MongoDB; PostgreSQL's `asset_exif` table is left in place, untouched, as a safety net.
2. **Verification**: An operator (or a post-deploy smoke check) confirms `SELECT COUNT(*) FROM asset_exif` in PostgreSQL equals the MongoDB `asset_exif` collection document count, and spot-checks a handful of `GET /api/assets/{id}/exif` responses against pre-migration values.
3. **Deploy 2 (schema drop)**: Once verified, a new Flyway migration drops the PostgreSQL `asset_exif` table (and its FK to `assets`) and the `AssetExifMongoMigrationRunner` component is deleted from the codebase (it has no further purpose once the table is gone).
4. **Rollback strategy**: Between deploy 1 and deploy 2, rollback is trivial — revert to the previous release, which still reads/writes PostgreSQL `asset_exif` (untouched by deploy 1). After deploy 2 (table dropped), rollback requires restoring the table from a database backup and re-running the equivalent of the migration runner in reverse (MongoDB → PostgreSQL); this is the same class of risk already accepted for other irreversible schema drops in this codebase (e.g., the eventual `refresh_tokens` drop planned for #79).

## Open Questions

- Should the `2dsphere` index be created eagerly at application startup (via `MongoTemplate.indexOps` in a `@PostConstruct`/`ApplicationRunner`) or left to be created manually via a one-time ops script? This design assumes the former for parity with how Flyway auto-applies schema changes, but either is compatible with the domain port.
- Should MongoDB use a replica set from day one (required for MongoDB transactions and Change Streams) even though this change doesn't need either? Deferred: single-node is sufficient for the current scope; a replica-set upgrade is non-breaking and can be done later without application changes.
