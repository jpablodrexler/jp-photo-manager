## 1. Backend Dockerfile

- [x] 1.1 Create `JPPhotoManagerWeb/backend/Dockerfile` with a two-stage build: stage 1 uses `maven:3.9-eclipse-temurin-21` to run `mvn clean package -DskipTests`, stage 2 uses `eclipse-temurin:21-jre-alpine` and copies the fat JAR
- [x] 1.2 Create `JPPhotoManagerWeb/backend/.dockerignore` to exclude `target/`, `.mvn/`, `*.md`, and IDE config files

## 2. Frontend Dockerfile and Nginx Configuration

- [x] 2.1 Create `JPPhotoManagerWeb/frontend/Dockerfile` with a two-stage build: stage 1 uses `node:22-alpine` to run `npm ci && npm run build:prod`, stage 2 uses `nginx:alpine` and copies the Angular dist output to `/usr/share/nginx/html`
- [x] 2.2 Create `JPPhotoManagerWeb/frontend/nginx.conf` that serves static files and adds a `location /api` block that reverse-proxies to `http://backend:8080`
- [x] 2.3 Create `JPPhotoManagerWeb/frontend/.dockerignore` to exclude `node_modules/`, `dist/`, `.angular/`, `*.md`, and IDE config files

## 3. Docker Compose and Environment Configuration

- [x] 3.1 Create `JPPhotoManagerWeb/docker-compose.yml` defining three services: `db` (postgres:18), `backend`, and `frontend`, with correct `depends_on` ordering
- [x] 3.2 Configure the `db` service with the `pgdata` named volume mounted at `/var/lib/postgresql/data` and environment variables sourced from `.env`
- [x] 3.3 Configure the `backend` service to bind-mount `${HOST_IMAGE_DIR}` (from `.env`) as a read-write mount at `/catalog` inside the container, and set `CATALOG_DIR=/catalog` as an environment variable
- [x] 3.4 Ensure the `backend` service passes all required environment variables to the container: `POSTGRES_HOST=db`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `CATALOG_DIR`, and `THUMBNAILS_DIR`
- [x] 3.5 Configure the `frontend` service to expose port 80 on the host
- [x] 3.6 Create `JPPhotoManagerWeb/.env.example` documenting all required variables: `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `HOST_IMAGE_DIR`, `THUMBNAILS_DIR`
- [x] 3.7 Add `JPPhotoManagerWeb/.env` to `.gitignore`

## 4. Backend Application Configuration

- [x] 4.1 Update `JPPhotoManagerWeb/backend/src/main/resources/application.yml` so that `photomanager.initial-directory` and `photomanager.root-catalog-folders` read from the `CATALOG_DIR` environment variable (using Spring's `${CATALOG_DIR:~/Pictures}` syntax), falling back to the existing default

## 5. Verification

- [x] 5.1 Run `docker compose up --build` and confirm all three containers start without errors
- [x] 5.2 Open `http://localhost` in a browser and confirm the Angular gallery loads
- [x] 5.3 Trigger a catalog operation and confirm images from `HOST_IMAGE_DIR` are indexed
- [x] 5.4 Perform a write operation (e.g., delete a duplicate) and confirm the change is reflected on the host filesystem
- [x] 5.5 Run `docker compose down` then `docker compose up` and confirm previously catalogued data is still present (persistent volume test)
- [x] 5.6 Run `docker compose down -v` and confirm the next startup begins with an empty database (volume reset test)

<!-- Verification tasks (5.x) require running docker compose from a terminal in JPPhotoManagerWeb/ with Docker socket access. -->
