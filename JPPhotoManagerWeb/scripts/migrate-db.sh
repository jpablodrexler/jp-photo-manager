#!/usr/bin/env bash
set -euo pipefail

# Usage: ./migrate-db.sh   (run from anywhere; the script cds to JPPhotoManagerWeb/,
# where docker-compose.yml lives, before running any docker compose command)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

PGHOST=${PGHOST:-localhost}
PGPORT=${PGPORT:-5432}
PGUSER=${PGUSER:-postgres}
PGDATABASE=${PGDATABASE:-photomanager}

DUMP_FILE="photomanager_backup.dump"
TIMEOUT=60

echo "==> Dumping host database '${PGDATABASE}' from ${PGHOST}:${PGPORT} ..."
pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -Fc "$PGDATABASE" -f "$DUMP_FILE"
echo "    Dump saved to ${DUMP_FILE}"

echo "==> Starting the db container ..."
docker compose up -d db

echo "==> Waiting for db container to be healthy (timeout: ${TIMEOUT}s) ..."
ELAPSED=0
until docker compose exec -T db pg_isready -U postgres -d photomanager -q 2>/dev/null; do
    if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
        echo "ERROR: db container did not become healthy within ${TIMEOUT} seconds." >&2
        exit 1
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done
echo "    db container is healthy."

echo "==> Restoring dump into the container ..."
docker compose exec -T db pg_restore -U postgres -d photomanager -c < "$DUMP_FILE"
echo "    Restore complete."

echo ""
echo "Migration successful!"
echo ""
echo "Next steps:"
echo "  1. Stop your host PostgreSQL service:"
echo "       sudo systemctl stop postgresql   (Linux)"
echo "       brew services stop postgresql    (macOS)"
echo "  2. Start the full application stack:"
echo "       docker compose up --build"
