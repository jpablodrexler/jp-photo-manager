## Why

Setting up the JPPhotoManager web application requires manually installing and configuring PostgreSQL, a Java 21 runtime, and Node.js — a friction-heavy process that slows onboarding and causes environment inconsistencies across developer machines. Docker Compose eliminates these prerequisites by defining the entire stack in a single file that any developer can start with one command.

## What Changes

- Add a multi-stage `Dockerfile` for the Spring Boot backend (Maven build stage → slim JRE runtime image).
- Add a multi-stage `Dockerfile` for the Angular frontend (Node build stage → Nginx serving the production bundle with a reverse-proxy to the backend).
- Add `docker-compose.yml` at `JPPhotoManagerWeb/` orchestrating three services: `db` (PostgreSQL 18), `backend`, and `frontend`.
- The `db` service uses a **named Docker volume** for PostgreSQL data so the database is persisted across `docker compose down / up` cycles.
- The `backend` service mounts a **host directory** as a read-write bind mount (path configured in `.env`) so images on the developer's machine are fully accessible for catalog, sync, convert, move, copy, and delete operations.
- Add `.env.example` documenting all required environment variables (database credentials, host image path, thumbnails path).
- Add `.dockerignore` files for both `backend/` and `frontend/` to exclude build artifacts and local config from image build contexts.

## Capabilities

### New Capabilities
- `docker-compose-dev-environment`: One-command local development stack (PostgreSQL 18 + Spring Boot backend + Angular/Nginx frontend) with a persistent database volume and a host-bind-mounted read-write image directory supporting all write features (catalog, sync, convert, move, copy, delete).

### Modified Capabilities
<!-- No existing spec-level requirements are changing. -->

## Impact

- **Backend:** Existing environment-variable configuration (`POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `photomanager.initial-directory`, `photomanager.thumbnails-directory`) already in `application.yml` is sufficient; no application logic changes are required.
- **Frontend:** Production build served by Nginx inside the container; the Angular dev-proxy (`/api → localhost:8080`) is replaced by an Nginx `location /api` reverse-proxy rule pointing to the `backend` service hostname.
- **Database:** No schema or migration changes; Flyway runs automatically on first backend startup. Data is retained across restarts via the named `pgdata` Docker volume.
- **Host image access:** Delivered via a read-write Docker bind mount; the host path is supplied by the developer in `.env` and injected into the backend container, overriding `photomanager.initial-directory` and `photomanager.root-catalog-folders`.
- **No changes** to domain logic, API contracts, or existing tests.
