[← Back to README](../README.md)

# Database

The web backend's stores and schema workflow, and what the `database-reviewer` skill checks in this project. The schema diagram is in [Architecture](architecture.md#database-schema) and the persistence narrative (what lives where, the Redis mirror of refresh tokens) is in [Backend](backend.md#persistence); this doc does not repeat either.

## Stores

| Store | Used for | Managed by |
|---|---|---|
| PostgreSQL | assets, folders, users, albums, refresh tokens, search presets, tags, EXIF and the sync/convert configuration | Flyway migrations in `backend/src/main/resources/db/migration/`; Spring Data JPA entities in `infrastructure/persistence/entity/` |
| MongoDB | the append-only `asset_audit_log` collection | `MongoAuditLogRepository`; no schema |
| Redis | query-result caches, the thumbnail L2 cache, the refresh-token mirror, rate-limit buckets | adapters under `infrastructure/persistence/redis/` and `infrastructure/service/` |

## Migrations

Flyway runs the `V<n>__<description>.sql` scripts in `backend/src/main/resources/db/migration/` in version order at startup; the directory has its own `README.md`. A migration that has been applied is never edited: a change is a new, higher-numbered script. See §5 of the checks below for the rules a review applies.

## Database review checks

What the `database-reviewer` skill checks in this project, on top of its generic checklist. The section numbers (§N) are the ones the review reports cite, so they stay stable.

### Context and workflow

Review the PostgreSQL schema of the JPPhotoManager web backend: Flyway
migrations (`backend/src/main/resources/db/migration/`), JPA entities
(`infrastructure/persistence/entity/`), and the queries issued against them
(`infrastructure/persistence/jpa/*Repository.java`,
`infrastructure/persistence/adapter/*RepositoryImpl.java`). `asset_audit_log` lives in
MongoDB and `refresh_tokens` is mirrored into Redis — both are out of scope
for this skill (schema-less/NoSQL); review those with `java-developer`'s
conventions instead, not this one. `asset_exif` is a PostgreSQL table again
(`V33__recreate_asset_exif.sql`, `AssetExifEntity`), so it is in scope.

#### Workflow (project specifics)

2. Read every migration file in scope **in filename order** (`V{n}__*.sql`,
   sorted numerically, not alphabetically — `V9` comes before `V10`) — schema
   history is cumulative, so a column added in `V7` may be altered in `V14`
   and indexed in `V32`; reviewing a migration in isolation misses that.
3. For each table touched, find every repository method (`@Query`,
   Spring-Data derived query, or native query) that reads or writes it, and
   check whether the query's `WHERE`/`JOIN`/`ORDER BY`/`GROUP BY` columns are
   covered by an actual index (§3) — not just plausible-looking ones.
4. Cross-check `@Table(indexes = ...)` / `@Index` annotations on the JPA
   entity against what the migrations actually created (§3.4) — Hibernate
   `ddl-auto` is `validate`/`none` in this project, so entity-level index
   annotations are metadata only; they do not create anything, and can drift
   silently from the real schema.

#### Severity levels (project wording)

| Level             | Meaning                                                                                     |
| ----------------- | --------------------------------------------------------------------------------------------- |
| 🔴 **CRITICAL**   | Data integrity risk, a query that will corrupt or silently drop data, or a full table scan on a hot path at current/expected row counts. Must be fixed before merging. |

### 1. Normalization

🔴 Flag a new column that duplicates data already derivable from a join
(e.g., storing `folder_path` directly on `assets` when `assets.folder_id →
folders.path` already gives it) — the two can drift out of sync on update.

🟡 Flag a repeating group implemented as multiple similarly-named columns
(`tag_1`, `tag_2`, `tag_3`) instead of a join table — the project's own
`tags`/`asset_tags` (`V13__create_tags_tables.sql`) is the reference pattern
for a proper many-to-many.

🟡 Flag a new many-to-many relationship modeled as a comma-separated or JSON
list of IDs in a single column instead of a join table, unless the values are
genuinely never queried/filtered individually (e.g. `albums.filter_json` in
`V18__add_smart_albums.sql` is opaque application-defined criteria, not a set
of foreign keys, so JSONB is the right call there — don't flag that as a
normalization violation).

🟢 Flag over-normalization: a lookup table with a fixed, small (<10), rarely
changing set of values that's only ever referenced from one table might be
better as a `CHECK` constraint or enum column (see `ProcessingStatus`,
`FileType`, `ImageRotation` — all stored as `VARCHAR` + `@Enumerated(STRING)`,
not as separate lookup tables — that's the project's existing convention for
small closed sets).

### 2. Column Naming & Data Types

#### 2.1 Naming

| Element                | Expected                                        | Example                                  |
| ---------------------- | ------------------------------------------------ | ----------------------------------------- |
| Table                  | `snake_case`, plural                              | `assets`, `refresh_tokens`, `album_assets` |
| Join/association table | `{singular_a}_{singular_b}` (alphabetical or natural read order) | `asset_tags`, `album_assets` |
| Primary key column     | `{table_singular}_id`, `BIGSERIAL`/`SERIAL`       | `asset_id`, `folder_id`, `album_id`       |
| Foreign key column     | Same name as the referenced table's PK column     | `folder_id` referencing `folders.folder_id` |
| Boolean column         | `is_`/`has_` prefix or a clearly boolean noun      | `is_video`, `revoked`, `include_sub_folders` |
| Timestamp column       | `_at` suffix                                      | `created_at`, `deleted_at`, `hash_completed_at` |
| Index name             | `ix_{table}_{column(s)}`                          | `ix_assets_folder_id`, `ix_assets_processing_status` |
| Unique index name      | `uq_{table}_{column(s)}`                          | `uq_refresh_tokens_token`                 |
| Foreign key constraint | inline `REFERENCES` is fine; named constraints use `fk_{table}_{column}` | — |
| Composite PK constraint | `pk_{table}`                                     | `pk_album_assets`                         |

🟡 Flag any new table whose primary key column is bare `id` instead of
`{table_singular}_id`. **Known drift:** `users.id` (`V4__add_users.sql`) is
already inconsistent with every other table (`asset_id`, `folder_id`,
`album_id`, `token_id`, …) — don't propagate that pattern to new tables, and
call it out as a pre-existing `🟢 Suggestion` (not a blocking finding) if it
resurfaces in a full audit, since renaming a live PK used as an FK target
(`refresh_tokens.user_id`, `albums.user_id`, `user_preferences.user_id`, …)
is a breaking, multi-migration change, not a quick fix.

🔴 Flag any new index or constraint that doesn't follow the `ix_`/`uq_`/`fk_`/
`pk_` prefix convention above.

#### 2.2 Data types

🔴 Flag a timestamp column whose SQL type disagrees with its entity's Java
type. The convention in this schema follows the Java type: an `Instant` field
(an exact moment: `created_at`, `issued_at`, `expires_at`, `last_used_at`,
`updated_at`) maps to `TIMESTAMPTZ NOT NULL DEFAULT NOW()` where it has a
default (`users`, `albums`, `album_assets`, `search_presets`,
`refresh_tokens`, `user_preferences` all do); a `LocalDateTime` field (a wall
clock value with no zone) maps to `TIMESTAMP`. The `LocalDateTime` columns are
`assets.*_date_time` (file system times), `asset_exif.date_taken` (EXIF
`DateTimeOriginal` carries no zone, V7 and V33) and the pipeline's
`assets.*_completed_at` (V32). Spring Batch's `BATCH_*` tables (V16) use the
framework's own `TIMESTAMP(9)` and are not reviewed.

🟡 Flag a new column that records *when something happened* (created, updated,
issued, expired, completed) declared `TIMESTAMP` with a `LocalDateTime` field:
prefer `TIMESTAMPTZ` with an `Instant`, because the value is a moment and a
zone-less column cannot be compared across servers. Known cases to verify
rather than assume: the V32 `*_completed_at` columns are zone-less, and
`assets.deleted_at` is `TIMESTAMPTZ` (V10) while `AssetEntity.deletedAt` is a
`LocalDateTime`. Neither needs a data migration to fix retroactively; decide
the type before the next column of that kind, not after.

🟡 Flag `VARCHAR` without an explicit length (`VARCHAR(255)` etc.) when the
column has a genuine natural bound (usernames, enum-backed status columns,
theme names) — unbounded `TEXT` is fine for free-form content (`description`,
`path`, `token`) but an unbounded `VARCHAR` with no length is neither.

🟡 Flag a foreign key column whose type doesn't match its referenced PK's
type exactly (e.g. an `INTEGER` FK pointing at a `BIGSERIAL`/`BIGINT` PK, or a
`VARCHAR` FK pointing at a `UUID` PK).

🟡 Flag a new enum-like status/rotation/rating column stored as free-text
`VARCHAR` without a `CHECK` constraint **and** without a corresponding
`@Enumerated(EnumType.STRING)` domain enum on the entity — one of the two
must constrain the value set, or invalid strings will silently persist.

🟢 Flag `SERIAL`/`INTEGER` primary keys on a new table expected to grow large
(anything asset-scoped) — prefer `BIGSERIAL`/`BIGINT` to avoid a future
int32-overflow migration. (`tags.tag_id` is `SERIAL` — acceptable since the
tag vocabulary is inherently small and bounded, unlike `assets`.)

### 3. Indexes

#### 3.1 Coverage for foreign keys

🔴 Flag any new `FOREIGN KEY`/`REFERENCES` column that has no index. Postgres
does **not** auto-index FK columns (only the referenced side gets one via its
PK); every FK in this schema needs an explicit `CREATE INDEX` — see
`ix_assets_folder_id`, `ix_albums_user_id`, `ix_refresh_tokens_user_id`,
`ix_search_presets_user_id` for the existing pattern.

#### 3.2 Coverage for query predicates

🔴 Flag a column that appears in a repository's `WHERE`, `JOIN ... ON`, or
`GROUP BY` clause on a table with non-trivial expected row count (`assets`
above all — this is a photo catalog, rows are expected in the tens/hundreds
of thousands) and has no supporting index. Concretely check every `@Query` in
`JpaAssetRepository`/`JpaFolderRepository` against the indexes actually
created by migrations, not against what `@Table(indexes=...)` claims (§3.4).
Known example to verify hasn't regressed: `AssetEntity.hash` is queried by
`findByHashNotDeleted` (`WHERE a.hash = :hash` on non-deleted assets) and by the duplicate-detection queries
(`GROUP BY a.hash HAVING COUNT(a) > 1`, and the correlated subquery in
`countDuplicates`) but as of this schema has **no index** — flag this as 🔴 if
still true, since duplicate detection runs a full sequential scan of `assets`
on every call.

🟡 Flag a query that filters or joins on a column covered only by a
*composite* index where the queried column isn't the leftmost member (a
leftmost-prefix violation) — e.g. a hypothetical lookup by `tag_id` alone
against `asset_tags`, whose only index is the composite PK
`(asset_id, tag_id)` — the PK covers "find tags for an asset" but not "find
assets for a tag" without a sequential scan.

🟡 Flag a `LIKE`/`ILIKE` prefix query (`... LIKE :value%`, no leading `%`)
against an unindexed `TEXT`/`VARCHAR` column — a plain B-tree index still
helps a left-anchored pattern. Known example to verify: `JpaFolderRepository`
has both `findByPath(String path)` and a prefix query
(`WHERE f.path LIKE :parentPath%`) against `folders.path`, which has no index
beyond the `folder_id` primary key — flag as 🟡 (not 🔴, since the `folders`
table itself is small; escalate to 🔴 only if it's joined against a
large table in the same query).

🟢 Flag a `LIKE '%value%'` (leading wildcard) query and note that a plain
B-tree index cannot help it — a `pg_trgm` GIN index would be the fix, but
only worth flagging as a suggestion since it's a bigger infrastructure change
(new extension) than a routine index addition.

#### 3.3 Partial and covering indexes

🟢 Suggest a partial index (`WHERE` clause on the `CREATE INDEX` itself) when
a boolean/nullable column is queried almost exclusively for one value — the
existing `ix_albums_filter_json_not_null` (`V18`, indexing rows only `WHERE
filter_json IS NOT NULL`) and the implicit pattern behind
`ix_assets_deleted_at`/`ix_assets_processing_status` (queried mostly for
"active" or "pending" rows) are the reference examples. Don't insist on
converting an existing full index to partial unless a review specifically
asks for an index optimization pass — this is a suggestion, not a defect.

🟢 Flag redundant indexes: two indexes on the same table where one's column
list is a strict prefix of the other's — the shorter one adds write overhead
with no read benefit Postgres wouldn't get from the longer index alone.

#### 3.4 Entity/migration index drift

🟡 Flag any `@Index`/`@Table(indexes = ...)` entry on a JPA entity that has
**no** corresponding `CREATE INDEX` in any migration — since `ddl-auto` is
`validate`/`none` here, the annotation creates nothing; it's misleading
documentation at best, and a developer reading only the entity will believe
an index exists that doesn't.

🟡 Flag the reverse: a migration-created index on a column with no matching
`@Index` entry on the entity — not wrong, but worth surfacing so the entity's
index list stays a trustworthy mirror of the real schema. Known example to
verify hasn't regressed: `AssetEntity`'s `@Table(indexes = ...)` currently
lists `ix_assets_folder_id` and `ix_assets_processing_status` but omits
`ix_assets_deleted_at` and `ix_assets_rating`, both of which exist in
migrations (`V10`, `V11`) — flag as 🟡 if still missing.

### 4. Views & Functions

The schema currently has **no** `CREATE VIEW`/`CREATE OR REPLACE VIEW` or
`CREATE FUNCTION` in any migration — every derived/aggregated read (home
dashboard stats, duplicate grouping, storage-by-folder, format breakdown) is
done via JPQL projections in `JpaAssetRepository` (`FolderAssetCount`,
`FormatProjection`, `MonthlyCountProjection`, `RatingProjection`,
`FolderStorageProjection`) instead. If a migration introduces the first
view/function, apply:

🔴 Flag a `CREATE FUNCTION` written in `PL/pgSQL` when the same logic could be
a plain JPQL/native `@Query` — functions move logic out of version-controlled
Java into the database, which this project's existing style deliberately
avoids (see the projection interfaces above). Only justify a DB function for
something a query genuinely cannot express (e.g. recursive CTEs for a folder
tree, which the current `LIKE :parentPath%` approach in `JpaFolderRepository`
approximates without one).

🔴 Flag any function created `SECURITY DEFINER` without an explicit,
commented justification — it executes with the privileges of the function's
owner, not the caller.

🟡 Flag a `CREATE VIEW` that is not `CREATE OR REPLACE VIEW` — plain
`CREATE VIEW` fails on a rerun/rollback-and-reapply and can't be
version-bumped in place; every later migration touching it must re-issue
`CREATE OR REPLACE VIEW` with the full definition, since Postgres has no
`ALTER VIEW ... ADD COLUMN`.

🟡 Flag a materialized view added without a corresponding refresh strategy
(a scheduled job, or a trigger) — an unrefreshed materialized view silently
serves stale data forever.

🟢 Flag a view or function name that doesn't follow `snake_case` matching the
table naming convention in §2.1.

### 5. Migration Mechanics

🔴 Flag any modification to an **existing, already-applied** migration file
under `db/migration/` — once applied, a migration's content and checksum are
fixed; Flyway will refuse to start if a checksum mismatches. Any change goes
in a new `V{n+1}__*.sql` instead. (This mirrors `code-reviewer` §6 — restated
here because it's the single most consequential mistake in this area.)

🟡 Flag a migration filename that doesn't follow `V{n}__{Description}.sql`
(two underscores after the version, `Description` in `Snake_Case` or
`lower_snake_case` matching the existing files — e.g.
`V14__add_file_type_to_assets.sql`, not `V14_addFileTypeToAssets.sql`).

🟡 Flag a new migration whose version number is not one above the highest
existing `V{n}` in `db/migration/` — compare the numeric value, never the
filename sort (`V1` sorts after `V10`–`V19` alphabetically) — or that reuses a
number already present or one abandoned earlier. The version sequence has
documented gaps (`V20`–`V25` and `V28`–`V31`, see the directory's
`README.md`): they are migrations abandoned before merge, not findings, and
they are never filled or renumbered. A migration abandoned from now on should
leave a no-op placeholder (like `V5__no_op.sql`) or a line in that `README.md`
instead of a silent gap.

🔴 Flag a new `@Column(nullable = false)` on an entity with no matching
`NOT NULL` (or a `NOT NULL` added via a follow-up `ALTER TABLE ... SET NOT
NULL`) in a migration, or the reverse — entity nullability and the actual
column constraint must agree, or Hibernate will either reject valid nulls
the DB allows, or let invalid nulls through that the DB would have rejected
had `ddl-auto` been more than `validate`.

🟡 Flag a migration that adds a `NOT NULL` column to an existing populated
table without either a `DEFAULT` or a backfill `UPDATE` in the same
migration — this project's own `V32__add_asset_processing_status.sql` shows
the right shape (`ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT
'COMPLETED'`), avoiding a separate backfill step entirely.

🟡 Flag a single migration that both alters schema **and** performs a
large data backfill via `UPDATE` on a large table (`assets`) without
considering lock duration — prefer a `DEFAULT` (as above) over an
`UPDATE ... SET` touching every row when the two are equivalent in outcome.

🟢 Flag a migration that could have used `CREATE INDEX IF NOT EXISTS` (as
`V1__initial_schema.sql` does for `ix_assets_folder_id`) instead of a bare
`CREATE INDEX`, for migrations whose idempotency matters for re-run
resilience — not required project-wide, but consistent with the one
precedent that exists.

### 6. Foreign Key & Cascade Behavior

🔴 Flag a foreign key with no `ON DELETE` clause where the parent row can
legitimately be deleted while children exist — Postgres defaults to
`ON DELETE NO ACTION`, which will throw at delete time instead of failing
the migration review. Every FK in this schema currently uses
`ON DELETE CASCADE` explicitly (`folders`→`assets`, `users`→`albums`/
`refresh_tokens`/`search_presets`/`user_preferences`, `assets`/`tags`→
`asset_tags`, `albums`/`assets`→`album_assets`) — match that unless the
relationship genuinely needs `RESTRICT`/`SET NULL` instead, and say why in
the migration if it deviates.

🟡 Flag `ON DELETE CASCADE` on a relationship where cascading would silently
destroy data the user would expect to survive (e.g. cascading an asset
delete into audit/history data) — cross-check against
`java-developer`/`CLAUDE.md`'s note that MongoDB `asset_audit_log`
deletion is handled explicitly in application code
precisely *because* Postgres cascade can't reach them; a new Postgres-side
cascade that duplicates or conflicts with an existing explicit-delete code
path is worth flagging.

#### 6.3 Cross-Datastore Consistency

This project's data isn't confined to Postgres: `asset_audit_log` lives in
MongoDB, `refresh_tokens` is dual-written into Redis alongside its Postgres
table, and `asset_exif` has already migrated between Postgres and MongoDB
once (`mongodb-exif-store` → `revert-exif-postgres-jsonb`). A schema change
that touches a table with a presence in another store needs the same
scrutiny as an index or cascade change — a review that only checks the SQL
side can miss a consistency bug entirely.

🔴 Flag a migration or entity change to a table that is also mirrored into
another store (Redis, Mongo) when the change doesn't identify **which store
is authoritative for reads** post-change. `refresh_tokens` is explicit about
this today — Postgres remains authoritative for `findByToken`/
`deleteByUserId`/`deleteById` even though every write is mirrored into Redis
— a change that alters the write path without restating (or deliberately
changing) which side reads trust is a correctness risk, not just a
documentation gap.

🟡 Flag a new dual-write (a repository method that writes to Postgres and
also calls out to a Redis/Mongo store in the same method) that has no
comment or design-doc reference explaining why the mirror exists — every
existing one (`RedisRefreshTokenStore`, the Redis L2 thumbnail cache) is
tied to a named change (`redis-refresh-tokens`, `redis-thumbnail-cache`) with
a stated reason. An undocumented dual-write is hard for a future migration to
reason about — nobody can tell if dropping the Postgres column would break
the Redis mirror's contract, or vice versa.

🟡 Flag a schema change to a table whose data has previously lived in a
different store (per the migration history called out in
`JPPhotoManagerWeb/CLAUDE.md`) without checking whether the change should
also update that narrative — see the `docs-sync` skill. A reviewer
who only checks the migration file in isolation can miss that the change
continues or contradicts a documented prior migration between stores.

🟢 Suggest a `MongoIndexInitializer`-style startup-ensured index (compound
key + TTL, as used for `asset_audit_log`) when a new Mongo-backed collection
is introduced without one — Mongo has no Flyway-equivalent migration
tracking, so indexes need an explicit ensure-on-startup mechanism or they
simply won't exist outside whatever environment they were manually created
in.

### 8. Fix Workflow (project specifics)

#### 8.3 Fix loop

2. Apply the fix:
   - **A fix to an already-applied migration is never "edit the file"** —
     per §5, create a new `V{n+1}__*.sql` that performs the correction
     (e.g. `CREATE INDEX CONCURRENTLY` for a missing index, an `ALTER TABLE`
     for a data-type/nullability fix). Determine the next version number
     from the actual highest `V{n}` present, not a guess.
   - If the finding is purely on the JPA entity side (e.g. `@Table(indexes =
     ...)` drift from §3.4) and requires no schema change, editing the entity
     directly is fine — no new migration needed.
   - A finding that requires backfilling a large existing table
     (`assets`) needs a decision on batching/locking strategy; if it isn't
     obvious, stop and ask the user rather than picking one unilaterally
     (same rule as `code-reviewer` §17.3.3 / `security-reviewer` §9.3.3).
3. Verify the fix: run `mvn clean test-compile` at minimum; if the change
   affects schema applied to a Testcontainers-backed integration test, run
   the relevant integration test class (requires Docker) so Flyway actually
   applies the new migration against a real PostgreSQL instance rather than
   just compiling.
4. Update any repository tests whose expectations depended on the old
   schema/index shape.

#### 8.4 Continue until done

After finishing the selected scope, run the widest verification available
(`mvn clean test` at minimum; `mvn verify -Pintegration-tests` if any
migration file changed, since only the Testcontainers-backed integration
suite actually exercises Flyway against real PostgreSQL) once before asking
what's next.

[← Back to README](../README.md)
