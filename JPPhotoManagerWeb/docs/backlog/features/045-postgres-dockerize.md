# Feature 45 — postgres-dockerize

**As a** administrator, **I want** PostgreSQL to run in Docker by default, **so that** deployment needs no PostgreSQL installed on the host.

## Acceptance Criteria
- AC1: Given Docker Compose, when the database starts, then it uses the standard data directory with PGDATA set explicitly and the volume mounted there.
- AC2: Given the volume is remounted, when the container restarts, then the data persists.
- AC3: Given the README, when read, then it needs no host PostgreSQL and states docker-compose up as the canonical start command.
- AC4: Given the migration script, when run, then it dumps the host database and restores it into the container, leaves the host data untouched, and accepts non-default host connection settings.

## Technical notes

The application previously connected to a PostgreSQL instance running on the host (`POSTGRES_HOST` defaults to `localhost`); completed the transition by: (1) fixing the volume mount from `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data` to align with the PostgreSQL `PGDATA` default, (2) setting `PGDATA: /var/lib/postgresql/data` explicitly in the `db` service environment, (3) making `docker-compose up` the canonical deployment command and updating `README.md` accordingly, and (4) providing a one-time data migration script (`pg_dump` on host → `pg_restore` into the container) so existing data is not lost on first deployment

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
