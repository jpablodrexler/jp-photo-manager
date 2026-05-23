## 1. Docker Compose — Fix PostgreSQL data directory

- [ ] 1.1 In `JPPhotoManagerWeb/docker-compose.yml`, change the `db` service volume mount from `pgdata:/var/lib/postgresql` to `pgdata:/var/lib/postgresql/data`
- [ ] 1.2 Add `PGDATA: /var/lib/postgresql/data` to the `db` service `environment` block (alongside `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)
- [ ] 1.3 Verify the stack starts cleanly on a fresh run: `docker compose down -v && docker compose up --build`; confirm the `db` health check passes and the backend connects

## 2. Migration Script

- [ ] 2.1 Create `JPPhotoManagerWeb/migrate-db.sh` with a shebang (`#!/usr/bin/env bash`) and `set -euo pipefail`
- [ ] 2.2 Read connection settings from environment variables with defaults: `PGHOST=${PGHOST:-localhost}`, `PGPORT=${PGPORT:-5432}`, `PGUSER=${PGUSER:-postgres}`, `PGDATABASE=${PGDATABASE:-photomanager}`
- [ ] 2.3 Dump the host database: `pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -Fc "$PGDATABASE" -f photomanager_backup.dump`
- [ ] 2.4 Start only the `db` container: `docker compose up -d db`
- [ ] 2.5 Wait for the container health check to pass (poll `docker compose ps db` or use `pg_isready` inside the container) with a timeout
- [ ] 2.6 Restore the dump: `docker compose exec -T db pg_restore -U postgres -d photomanager -c photomanager_backup.dump` (pass the dump file via stdin or a bind mount)
- [ ] 2.7 Print a success message instructing the user to stop the host PostgreSQL service and run `docker compose up --build`
- [ ] 2.8 Make the script executable: `chmod +x migrate-db.sh`
- [ ] 2.9 Test the script against a local PostgreSQL instance to confirm dump + restore completes without errors

## 3. README Update

- [ ] 3.1 In `JPPhotoManagerWeb/README.md`, update the "Prerequisites" section: replace "PostgreSQL 18+ running locally" with "Docker and Docker Compose"
- [ ] 3.2 Update the "Running" / "Getting Started" section to show `docker compose up --build` as the single start command
- [ ] 3.3 Add a "First-time migration (existing catalog)" subsection that explains when to run `migrate-db.sh` and the steps to stop the host PostgreSQL instance afterward
- [ ] 3.4 Verify the README is accurate by doing a clean setup following only its instructions
