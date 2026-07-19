## Context

`AssetExif` is currently persisted in a MongoDB `asset_exif` collection (`mongodb-exif-store`, #72, archived at `openspec/changes/archive/2026-07-05-mongodb-exif-store/`), accessed through `AssetExifRepositoryImpl` (a Spring Data MongoDB adapter behind the unchanged `AssetExifRepository` domain port). Before #72, the same data lived in a PostgreSQL `asset_exif` table with a `raw_exif JSONB` column (`raw-exif-jsonb`, #63). The two approaches were explicitly documented as mutually exclusive; #72 superseded #63 by dropping the PostgreSQL table (`V27__drop_asset_exif.sql`) after copying its rows into MongoDB.

#72's second motivation — a `2dsphere` geospatial index unlocking `$near`/`$geoWithin` proximity queries for the (then-upcoming) `gps-map-view` (#13) — no longer applies: `gps-map-view` has been cancelled and removed from `openspec/features.md`, and no other feature (planned or implemented) queries GPS proximity. A codebase search confirms the index has never actually been queried. #72's first motivation (avoiding JSONB column sprawl) was already satisfied by #63's `raw_exif JSONB` column before #72 replaced it, so reverting loses nothing that wasn't already available under the simpler design.

MongoDB is not being removed from the stack: `mongodb-audit-log` (#73, already implemented) also stores data in MongoDB (`asset_audit_log` collection) and is unaffected by this change.

## Goals / Non-Goals

**Goals:**
- Restore `asset_exif` as a PostgreSQL table with a `raw_exif JSONB` column, matching the pre-#72 (`raw-exif-jsonb`) schema exactly.
- Restore JPA-based persistence (`AssetExifEntity`, `JpaAssetExifRepository`, `AssetExifEntityMapper`) behind the unchanged `AssetExifRepository` domain port and `AssetExif` domain model — no use case, controller, or DTO mapper changes.
- Restore `ON DELETE CASCADE` as the mechanism for removing `asset_exif` rows when an `Asset` is purged, removing the explicit `deleteByAssetId` call #72 introduced.
- Preserve the exact `GET /api/assets/{id}/exif` response shape (no frontend change).

**Non-Goals:**
- Migrating existing MongoDB `asset_exif` data back to PostgreSQL. Per explicit instruction, the new table starts empty. Data for previously-catalogued assets is recovered only by re-cataloging.
- Removing MongoDB, `spring-boot-starter-data-mongodb`, or `MONGO_URI` configuration from the stack — MongoDB remains required for `mongodb-audit-log` (#73).
- Any change to `mongodb-user-preferences` (#74, still pending) — an unrelated, independent pending feature.
- Re-implementing geospatial search in any form. If a future feature genuinely needs GPS proximity queries, that is a new, independently-justified change.

## Decisions

### 1. Single combined migration, not a schema-then-column sequence

**Decision:** Recreate `asset_exif` as one new Flyway migration containing the full original `V7` schema plus the `raw_exif JSONB` column from `V26`, rather than replaying the two original migrations as two new ones.

**Rationale:** The original two-step history (`V7` then `V26`) existed because `raw_exif` was added to an already-populated, already-deployed table later. Here the table does not exist yet at all — there's no reason to recreate that historical sequencing. One `CREATE TABLE` matching the final pre-#72 shape is simpler and equally correct.

**Alternative considered:** Replaying `V7` and `V26` as two separate new migrations. Rejected as pointless ceremony — no intermediate state (table without `raw_exif`) is ever actually deployed.

### 2. No dual-write / verification / two-deploy migration plan

**Decision:** Ship this as a single deploy: the new Flyway migration creates the empty table, the adapter swap takes effect immediately, and there is no `CommandLineRunner`, no completion-marker collection, and no manual row-count verification step.

**Rationale:** #72's two-deploy dual-write pattern (and the similar pattern used for `redis-refresh-tokens`, #79) exists specifically to avoid data loss during a migration between two live stores. That caution does not apply here: the explicit instruction for this change is "don't mind about migrating existing data" — data loss is accepted up front, not a risk to mitigate. Introducing dual-write machinery to protect data that has already been declared out of scope would add complexity with no corresponding benefit.

**Alternative considered:** Mirroring #72's two-deploy caution (write to both stores temporarily, verify counts, then cut over). Rejected — it exists to solve a problem (accidental data loss) that this change has already decided is acceptable.

### 3. `AssetExifEntity` keyed directly on `assetId`, not a surrogate key

**Decision:** `AssetExifEntity` uses `assetId` (the FK to `assets`) as its `@Id`, matching the original pre-#72 `asset_exif` table (`asset_id BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE`) — not a separate auto-generated surrogate key.

**Rationale:** This restores the exact schema that existed before #72 (one row per asset, primary-keyed by the FK itself), which is what `ON DELETE CASCADE` requires to work automatically. `AssetExifDocument`'s use of a separate Mongo `_id` alongside a uniquely-indexed `assetId` field was a MongoDB-idiom accommodation (`_id` must be document identity in Mongo); it has no equivalent need in a relational table.

**Alternative considered:** A surrogate `exif_id` primary key with a unique constraint on `asset_id` (mirroring the ER diagram sketch that appears in `JPPhotoManagerWeb/README.md`). Rejected — that diagram was already inconsistent with the real, previously-deployed schema (`V7__add_asset_exif.sql`) before this change; restoring the real original schema is more correct than perpetuating an already-stale diagram.

### 4. `raw_exif` mapped via Hibernate 6 native JSON support, no new dependency

**Decision:** `AssetExifEntity.rawExif` is a `Map<String, String>` annotated `@JdbcTypeCode(SqlTypes.JSON)`, exactly as it was under #63 before #72 replaced it.

**Rationale:** Hibernate 6 (already in use) maps this natively via Jackson (already a dependency) with no additional library. This is a straight restoration of previously-working, previously-reviewed code, not a new design.

## Risks / Trade-offs

- [Risk] MongoDB `asset_exif` data accumulated since #72 shipped is abandoned — any EXIF data (including reverse-lookups the raw EXIF search panel depends on) for assets not re-catalogued after this change ships appears as `null` until the next catalog run. → Mitigation: this is the explicitly accepted trade-off per instruction; `ExifPanelComponent` already handles `null` EXIF gracefully (pre-existing behavior, unchanged by this revert), and re-cataloging is a single existing action (`POST /api/assets/catalog`) the user can trigger for any folder.
- [Risk] The abandoned MongoDB `asset_exif` collection and its `migration_status` marker document are not cleaned up automatically — they remain in the `photomanager` MongoDB database as inert, unreferenced data. → Mitigation: low severity (no application code reads them after this change; they cost storage only); a manual `mongosh` drop is documented as an optional cleanup step for operators who want it, not required for correctness.
- [Risk] Restoring `ON DELETE CASCADE` instead of the explicit `deleteByAssetId` call means asset-purge behavior for EXIF cleanup is no longer visible/testable at the application layer. → Mitigation: this exactly restores the pre-#72 behavior, which was already relied upon (and tested via integration tests against the real FK constraint) before #72 shipped; no new risk is introduced, only the removal of a workaround for a problem (no FK cascade) that no longer exists once the table is PostgreSQL-native again.

## Migration Plan

Single deploy, no rollback complexity:

1. Ship the new Flyway migration (empty `asset_exif` table with `raw_exif` column), the JPA-backed `AssetExifRepositoryImpl`, and the `MongoIndexInitializer` update in one release.
2. On deploy, Flyway creates the table; the application immediately reads/writes through JPA. No feature flag, no dual-write window, no manual verification step.
3. Operators who want fresh EXIF data for existing assets re-catalog the relevant folders (`POST /api/assets/catalog`) at their convenience — this is a normal, pre-existing user action, not a special migration step.
4. **Rollback**: if this deploy needs to be reverted, roll back to the previous release, which reads/writes MongoDB `asset_exif` again — the MongoDB collection was never touched by this change, so it is still fully intact and immediately usable on rollback. The new (empty) PostgreSQL table is simply left unused; a future Flyway migration can drop it if the rollback is permanent.

## Open Questions

- Should the abandoned MongoDB `asset_exif` collection be dropped as part of this change, or left for a future cleanup? This design leaves it in place (see Risks) since removing it has no functional benefit and the rollback story in the Migration Plan above depends on it still being there.
