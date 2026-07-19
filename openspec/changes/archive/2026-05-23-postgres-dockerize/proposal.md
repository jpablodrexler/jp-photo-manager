## Why

The `docker-compose.yml` already declares a `db` service and a `pgdata` named volume but the PostgreSQL data volume is mounted at the wrong path (`/var/lib/postgresql` instead of `/var/lib/postgresql/data`), meaning data persists but in a subtly incorrect layout that can cause silent failures on fresh deployments or image upgrades. Additionally, `docker compose up` is not yet the documented or canonical way to run the application — developers are expected to have PostgreSQL running on the host, which creates an uncontrolled dependency and makes onboarding harder.

## What Changes

- Fix the `pgdata` volume mount from `pgdata:/var/lib/postgresql` → `pgdata:/var/lib/postgresql/data` to match PostgreSQL's default `PGDATA`
- Add `PGDATA: /var/lib/postgresql/data` explicitly to the `db` service environment so the mount target and the database engine agree on where data lives
- Update `README.md` to make `docker compose up` the canonical single-command way to start the full stack (replacing the "run PostgreSQL on the host" prerequisite)
- Add a one-time `migrate-db.sh` script that `pg_dump`s data from a host PostgreSQL instance and `pg_restore`s it into the container so existing users don't lose their catalog on first containerised deployment

## Capabilities

### New Capabilities

- `postgres-docker-deployment`: PostgreSQL runs exclusively inside the Docker Compose stack with a correctly configured data directory; `docker compose up` is the canonical deployment command; a migration script moves existing host data into the container on first deploy

### Modified Capabilities

_(none — no existing spec-level requirements change; existing data-persistence scenarios in `docker-compose-dev-environment` continue to hold with the corrected volume mount)_

## Impact

- `JPPhotoManagerWeb/docker-compose.yml` — two-line change to `db` service (`volumes` and `environment`)
- `JPPhotoManagerWeb/README.md` — "Prerequisites" and "Running" sections updated to remove host-PostgreSQL requirement
- `JPPhotoManagerWeb/migrate-db.sh` — new shell script (one-time, not part of the regular startup path)
- No Java code changes, no Flyway migrations, no frontend changes
