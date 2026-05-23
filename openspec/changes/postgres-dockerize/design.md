## Context

The `docker-compose.yml` at `JPPhotoManagerWeb/` already runs three services — `db` (PostgreSQL 18), `backend` (Spring Boot), and `frontend` (Nginx). The stack is functional but has two problems:

1. **Wrong volume mount path.** The `pgdata` volume is mounted at `pgdata:/var/lib/postgresql`. PostgreSQL's actual data directory inside the container is `/var/lib/postgresql/data` (the default `PGDATA`). The current mount works — data ends up in a `data/` subdirectory inside the volume — but it means the volume root contains the `data/` directory rather than the data files directly, which is non-standard and can cause silent failures if a future PostgreSQL image version uses a different subdirectory or if `PGDATA` is ever overridden.

2. **Host PostgreSQL as the implied prerequisite.** The `README.md` describes a "Prerequisites" section that includes running PostgreSQL locally. Developers who have a local catalog must manually stop their host instance before starting the Compose stack (port conflict on 5432) and risk data loss if they forget to migrate first.

The fix is small — two lines in `docker-compose.yml` — but requires a data migration path for anyone with an existing host catalog.

## Goals / Non-Goals

**Goals:**

- Mount the `pgdata` volume directly at `/var/lib/postgresql/data` so PostgreSQL data files are at the volume root
- Set `PGDATA: /var/lib/postgresql/data` explicitly in the `db` service so the mount target and engine agree
- Expose a safe one-time migration script (`migrate-db.sh`) for users with an existing host PostgreSQL catalog
- Update `README.md` to make `docker compose up` the canonical start command and remove the host-PostgreSQL prerequisite

**Non-Goals:**

- Introducing Docker Compose profiles or multiple compose files
- Automating the host-to-container migration (the script is manual and one-time)
- Changing any backend Java code, Flyway migrations, or API contracts
- Supporting Windows hosts in the migration script (the app targets Linux/macOS)

## Decisions

### 1. Mount at `/var/lib/postgresql/data`, not `/var/lib/postgresql`

**Decision:** Change `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data` in the `db` service `volumes` block.

**Rationale:** PostgreSQL 18's default `PGDATA` is `/var/lib/postgresql/data`. Mounting the named volume at the parent path (`/var/lib/postgresql`) means the volume holds a `data/` subdirectory — not the data files themselves. This is wasteful (an extra directory layer) and fragile (any image that changes `PGDATA` breaks the layout without warning). Mounting at the actual `PGDATA` path is the documented, idiomatic pattern shown in the official PostgreSQL Docker Hub docs.

**Risk:** Existing deployments that already have data in the old volume layout will find an empty database after the change because the files are at `<volume>/data/` but PostgreSQL now expects them at `<volume>/`. This is handled by the migration script (see Decision 3).

**Alternative considered:** Leave the mount at `/var/lib/postgresql` and add `PGDATA: /var/lib/postgresql/data` without changing the volume path. Rejected because it perpetuates the non-standard layout and still differs from the official docs.

### 2. Explicitly set `PGDATA` in the `db` service environment

**Decision:** Add `PGDATA: /var/lib/postgresql/data` to the `db` service `environment` block alongside the existing `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

**Rationale:** Making `PGDATA` explicit in the Compose file documents intent — future maintainers can see at a glance where data lives without knowing PostgreSQL internals. It also guards against future PostgreSQL image changes that might alter the default.

### 3. Shell script for one-time host-to-container migration

**Decision:** Provide `JPPhotoManagerWeb/migrate-db.sh` — a Bash script the user runs once before switching to the Compose stack permanently. The script:
1. `pg_dump`s the host database to a `.dump` file
2. Starts only the `db` container (`docker compose up -d db`)
3. Waits for the health check to pass
4. `pg_restore`s the dump into the container using `docker compose exec db pg_restore`
5. Prints confirmation and instructions to stop the host instance

**Rationale:** Automating migration inside the Compose startup would risk data loss if something goes wrong. Keeping it as an explicit manual step lets the user verify each stage and abort if they see errors. The script is not part of the normal startup path — it is a one-time operation.

**Alternative considered:** Documenting manual `pg_dump` / `pg_restore` commands in the README without a script. Rejected because the sequence is long enough (start partial stack, wait for health, exec into container) that users are likely to make mistakes without automation.

### 4. `docker compose up` replaces host-PostgreSQL prerequisite in README

**Decision:** Update the "Prerequisites" section of `README.md` to remove the instruction to install and start PostgreSQL locally. The only prerequisites become Docker and Docker Compose. The "Running" section is updated to `docker compose up --build` as the single command to start everything.

**Rationale:** With the `db` container handling PostgreSQL, there is no reason to require a host installation. Removing the host prerequisite simplifies onboarding and eliminates the class of bugs caused by mismatched PostgreSQL versions between developer machines.

## Risks / Trade-offs

**Existing pgdata volume data becomes inaccessible after remounting**
→ Mitigation: The migration script handles this. Any user with an existing catalog must run `migrate-db.sh` before switching; the README update documents this step explicitly.

**Port 5432 conflict if host PostgreSQL is running**
→ Mitigation: The migration script checks for and warns about a running host PostgreSQL service. The README update tells users to stop it before running `docker compose up`.

**migrate-db.sh assumes the host database uses default connection settings**
→ Mitigation: The script accepts `PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE` environment variables so users with non-default host configs can override them.

## Migration Plan

For **new deployments** (no existing data): no migration needed. Run `docker compose up --build`.

For **existing deployments** (existing host PostgreSQL catalog):
1. Stop the running backend (`Ctrl+C` or `docker compose down` if already partially containerised)
2. Run `JPPhotoManagerWeb/migrate-db.sh` — it dumps the host DB and restores it into the container
3. Stop the host PostgreSQL service (`sudo systemctl stop postgresql` / `brew services stop postgresql`)
4. Run `docker compose up --build`

**Rollback:** If migration fails, the host PostgreSQL data is untouched (the script only reads from it, never writes). Simply re-start the host instance and backend without the Compose stack.

## Open Questions

_(none)_
