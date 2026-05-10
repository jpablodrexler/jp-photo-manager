## Context

The JPPhotoManager web application consists of three runtime components: a Spring Boot 3.4 / Java 21 backend, an Angular 19 frontend, and a PostgreSQL database. Today a developer must install all three locally before running any part of the stack. This change introduces Docker Compose to replace that manual setup with a single command, while preserving direct access to the developer's photo library on the host filesystem.

Key constraints:
- The backend must be able to read and write the host image directory (catalog, sync, convert, move, copy, delete).
- PostgreSQL data must survive `docker compose down / up` cycles.
- The frontend communicates with the backend through `/api` — this proxy must work inside the container network without changing any Angular source code.

## Goals / Non-Goals

**Goals:**
- `docker compose up --build` starts all three services (db, backend, frontend).
- PostgreSQL data is persisted via a named Docker volume (`pgdata`).
- The host image directory is mounted read-write into the backend container.
- All configuration (DB credentials, host paths) is provided via a `.env` file.
- Nginx serves the Angular production build and reverse-proxies `/api` to the backend service.

**Non-Goals:**
- Production-grade deployment or Kubernetes manifests.
- Hot-reload / live development mode inside Docker (Angular's `ng serve` or Spring Boot DevTools).
- Running the test suite inside Docker.
- CI/CD pipeline integration.

## Decisions

### Multi-stage Docker builds

Both the backend and frontend use multi-stage builds to keep the final image lean:

- **Backend:** Stage 1 uses `maven:3.9-eclipse-temurin-21` to compile and package the JAR (`mvn clean package -DskipTests`). Stage 2 uses `eclipse-temurin:21-jre-alpine` and copies only the fat JAR. This avoids shipping Maven, source code, and build caches in the runtime image.
- **Frontend:** Stage 1 uses `node:22-alpine` to run `npm ci && npm run build:prod`. Stage 2 uses `nginx:alpine`, copies the build output to `/usr/share/nginx/html`, and adds a custom `nginx.conf` that reverse-proxies `/api` to `http://backend:8080`.

**Alternatives considered:**
- Single-stage build (simple but ships a ~600 MB image with JDK + sources).
- Buildpack / Jib for the backend (less transparent, adds a Jib plugin dependency; multi-stage is well-understood and dependency-free).

### Nginx as frontend web server + API reverse-proxy

The Angular app makes all backend calls to `/api`. In local development, `proxy.conf.json` rewrites these to `localhost:8080`. Inside Docker, the two containers are on the same network and the backend is reachable as `http://backend:8080`. Nginx handles this with a `location /api` block — no changes to Angular source code are needed.

**Alternative considered:** Expose the backend port directly to the host and point the frontend build at the host IP. This would require a build-time environment variable and break portability.

### PostgreSQL 18 with a named volume

The `db` service uses the official `postgres:18` image. Data is stored in a named volume `pgdata` mounted at `/var/lib/postgresql/data`. Named volumes survive `docker compose down` (but not `docker compose down -v`), which matches the expected developer workflow.

**Alternative considered:** A bind-mount to a host directory for the DB data. This causes file-permission problems on Linux (PostgreSQL requires the data dir owned by UID 999) and offers no practical benefit over a named volume for local development.

### Read-write bind mount for the host image directory

The host image path (defined by `HOST_IMAGE_DIR` in `.env`) is mounted as a read-write bind mount into the backend container at a fixed container path (e.g., `/catalog`). The backend receives this path via the `CATALOG_DIR` environment variable, which overrides `photomanager.initial-directory` and `photomanager.root-catalog-folders` in `application.yml`.

Read-write (not read-only) is required because features such as delete duplicates, convert PNG→JPEG, sync, move, and copy all write to the filesystem.

**Risk — file permissions:** On Linux the backend container runs as a non-root user. If the UID inside the container does not match the owning UID on the host, writes will fail. **Mitigation:** The `Dockerfile` creates a dedicated `appuser` and the `docker-compose.yml` documents that `HOST_IMAGE_DIR` must be owned or group-writable by the host user; alternatively, developers can set `user: "${UID}:${GID}"` in the compose file using shell expansion.

### Configuration via `.env` file

Docker Compose natively reads a `.env` file in the same directory. All developer-specific values (DB password, host image path, thumbnails path) live there. An `.env.example` is committed; `.env` is gitignored. This avoids hardcoded paths in `docker-compose.yml` while keeping the compose file portable.

## Risks / Trade-offs

- **Long first build time** → Mitigation: Maven and npm dependency layers are ordered before source-copy steps so Docker layer caching speeds up rebuilds on code-only changes.
- **No hot-reload in Docker** → This setup targets "run the app" workflows, not "active development" workflows. Developers writing code should still use local `mvn spring-boot:run` and `ng serve`.
- **PostgreSQL 18 image availability** → The official `postgres:18` image is available on Docker Hub as of PostgreSQL 18's GA release; verify tag availability before finalising.
- **Bind mount UID/GID mismatch on Linux** → Documented in `.env.example`; advanced users can set `user:` in compose. Windows and macOS users are unaffected (Docker Desktop handles UID mapping transparently).

## Migration Plan

1. Developer copies `.env.example` to `.env` and fills in `HOST_IMAGE_DIR`.
2. Run `docker compose up --build` — images are built and all three services start.
3. Flyway migrations run automatically on backend startup; the DB schema is created fresh.
4. To stop without losing data: `docker compose down` (volume is preserved).
5. To reset the database: `docker compose down -v` (removes the `pgdata` volume).

No rollback is needed — the Docker files are purely additive; existing local workflows are unaffected.

## Open Questions

- Should a `thumbnails` named volume be added for generated thumbnails, or is the default `~/.photomanager/thumbnails` path (bind-mounted via `THUMBNAILS_DIR`) sufficient? Currently designed as a configurable bind mount; can be changed to a named volume if portability is preferred.
