# postgres-docker-deployment

Specifies how PostgreSQL is deployed inside the Docker Compose stack with a correctly configured data directory, and how existing host-based catalog data is migrated into the container on first deployment.

---

## ADDED Requirements

### Requirement: PostgreSQL data directory is mounted at the correct PGDATA path

The `db` service in `docker-compose.yml` SHALL mount the `pgdata` named volume at `/var/lib/postgresql/data` and SHALL set the `PGDATA` environment variable to `/var/lib/postgresql/data` so that the PostgreSQL engine and the volume mount target agree on the data directory location.

#### Scenario: Database starts with correct data directory

- **WHEN** a developer runs `docker compose up` for the first time
- **THEN** the `db` container initialises its data cluster at `/var/lib/postgresql/data`, the health check passes, and the backend connects successfully

#### Scenario: Data persists after volume remount

- **WHEN** a developer runs `docker compose down` followed by `docker compose up`
- **THEN** all previously catalogued assets are still present because the `pgdata` volume is mounted at the same path that PostgreSQL wrote to

#### Scenario: PGDATA is explicit in the compose file

- **WHEN** a developer inspects `docker-compose.yml`
- **THEN** both `PGDATA: /var/lib/postgresql/data` in the environment block and `pgdata:/var/lib/postgresql/data` in the volumes block are present and consistent

---

### Requirement: `docker compose up` is the canonical deployment command

The `README.md` SHALL document `docker compose up --build` as the single command to start the full application stack. It SHALL NOT require developers to install or start a local PostgreSQL instance as a prerequisite.

#### Scenario: README describes no host-PostgreSQL prerequisite

- **WHEN** a developer reads the README
- **THEN** the Prerequisites section lists only Docker and Docker Compose; no mention of a locally running PostgreSQL is present

#### Scenario: README describes the canonical start command

- **WHEN** a developer follows the README to start the application
- **THEN** running `docker compose up --build` in `JPPhotoManagerWeb/` starts `db`, `backend`, and `frontend` and the application is accessible at `http://localhost`

---

### Requirement: A migration script moves host catalog data into the container

The repository SHALL include a `migrate-db.sh` script at `JPPhotoManagerWeb/migrate-db.sh` that automates the one-time transfer of an existing host PostgreSQL catalog into the containerised database. The script SHALL accept `PGHOST`, `PGPORT`, `PGUSER`, and `PGDATABASE` environment variables to support non-default host configurations.

#### Scenario: Migration script dumps and restores catalog data

- **GIVEN** an existing catalog in a host PostgreSQL database
- **WHEN** a developer runs `migrate-db.sh`
- **THEN** the script creates a dump of the host database, starts only the `db` container, waits for the health check to pass, restores the dump into the container, and prints a success message with instructions to stop the host instance

#### Scenario: Migration script leaves host data untouched

- **GIVEN** an existing catalog in a host PostgreSQL database
- **WHEN** `migrate-db.sh` runs
- **THEN** the host database is read-only during the process; no data is deleted or modified on the host

#### Scenario: Migration script supports non-default host connection settings

- **GIVEN** a developer whose host PostgreSQL listens on a non-default port or uses a custom username
- **WHEN** the developer runs `PGPORT=5433 PGUSER=myuser migrate-db.sh`
- **THEN** the script uses those values instead of the defaults (`localhost`, `5432`, `postgres`, `photomanager`)
