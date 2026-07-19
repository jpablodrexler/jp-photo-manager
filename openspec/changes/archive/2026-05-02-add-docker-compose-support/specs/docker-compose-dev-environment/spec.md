## ADDED Requirements

### Requirement: Developer can start the full stack with Docker Compose
The system SHALL provide a `docker-compose.yml` at `JPPhotoManagerWeb/` that starts three services — `db` (PostgreSQL 18), `backend` (Spring Boot), and `frontend` (Angular via Nginx) — with a single `docker compose up --build` command.

#### Scenario: All services start successfully
- **WHEN** a developer runs `docker compose up --build` in `JPPhotoManagerWeb/`
- **THEN** the `db`, `backend`, and `frontend` containers start in dependency order, the backend connects to the database, Flyway migrations run, and the frontend is reachable at `http://localhost` (port 80)

#### Scenario: Backend is reachable through the frontend
- **WHEN** the stack is running
- **THEN** HTTP requests to `http://localhost/api/**` are reverse-proxied by Nginx to `http://backend:8080/api/**` and the API responds normally

---

### Requirement: PostgreSQL data persists across restarts
The system SHALL store PostgreSQL data in a named Docker volume (`pgdata`) so that all catalog entries, folder records, and asset metadata survive `docker compose down / up` cycles.

#### Scenario: Data survives a stop-start cycle
- **WHEN** a developer runs `docker compose down` followed by `docker compose up`
- **THEN** all previously catalogued assets and folder records are still present in the database

#### Scenario: Data can be fully reset
- **WHEN** a developer runs `docker compose down -v`
- **THEN** the `pgdata` volume is removed and the next `docker compose up` starts with an empty database

---

### Requirement: Host image directory is mounted read-write into the backend
The system SHALL mount the host image directory into the backend container as a read-write bind mount so that all write operations (catalog, sync, convert, move, copy, delete) work on the developer's actual files.

#### Scenario: Backend can read images from the host
- **WHEN** the stack is running and `HOST_IMAGE_DIR` is set in `.env` to a host path containing image files
- **THEN** the backend's catalog service discovers and indexes those images

#### Scenario: Backend can write to the host directory
- **WHEN** a write operation (delete duplicate, convert PNG→JPEG, move, copy) is performed via the API
- **THEN** the change is reflected on the host filesystem at `HOST_IMAGE_DIR`

---

### Requirement: Configuration via .env file
The system SHALL read all developer-specific configuration (database credentials, host image path, thumbnails path) from a `.env` file loaded automatically by Docker Compose.

#### Scenario: .env.example is committed to the repository
- **WHEN** a developer clones the repository
- **THEN** a `.env.example` file exists at `JPPhotoManagerWeb/` documenting every required variable with placeholder values

#### Scenario: Missing HOST_IMAGE_DIR causes an informative failure
- **WHEN** a developer runs `docker compose up` without setting `HOST_IMAGE_DIR` in `.env`
- **THEN** Docker Compose reports a variable substitution error referencing `HOST_IMAGE_DIR` before any container starts

---

### Requirement: Multi-stage Docker images are minimal
The system SHALL use multi-stage Docker builds so that the final runtime images contain only the compiled artifact and its runtime dependencies — not build tools, source code, or intermediate caches.

#### Scenario: Backend image excludes Maven and source code
- **WHEN** the backend image is built
- **THEN** the resulting image is based on a JRE (not JDK) base image and does not contain the Maven binary or `.java` source files

#### Scenario: Frontend image excludes Node.js and source code
- **WHEN** the frontend image is built
- **THEN** the resulting image is based on Nginx Alpine and does not contain Node.js, npm, or `.ts` source files
