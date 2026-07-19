[← Back to README](../README.md)

# Running with Docker Compose

The fastest way to run the full stack. No local Java, Maven, Node.js, or PostgreSQL installation required.

## Prerequisites

- Docker 24+
- Docker Compose v2 (`docker compose` — not the legacy `docker-compose`)

## Setup

1. Copy the environment template and fill in your values:
   ```bash
   cd JPPhotoManagerWeb
   cp .env.example .env
   ```

2. Edit `.env` — the only required change is `HOST_IMAGE_DIR`:

   | Variable | Description |
   |---|---|
   | `HOST_IMAGE_DIR` | **Required.** Absolute path on your machine to the directory containing images to catalogue (e.g. `/home/yourname/Pictures`). Mounted read-write so all write features work on your actual files. |
   | `HOST_IMAGE_DIR_2` … `HOST_IMAGE_DIR_N` | *Optional.* Additional directories to catalogue. See [Configuring multiple catalog root folders](authentication.md#configuring-multiple-catalog-root-folders). |
   | `JWT_SECRET` | **Required.** HS256 signing secret. See [Generating JWT_SECRET](authentication.md#generating-jwt_secret) below. |
   | `POSTGRES_DB` | Database name (default: `photomanager`). |
   | `POSTGRES_USERNAME` | Database user (default: `postgres`). |
   | `POSTGRES_PASSWORD` | Database password (default: `postgres`). |

3. Build and start all three services:
   ```bash
   docker compose up --build
   ```

4. Open `http://localhost` in your browser.

## First-time migration (existing catalog)

If you have an existing catalog in a **host PostgreSQL instance** and want to move it into the Docker Compose stack, run the migration script **once** before switching over.

**When to run:** only if you previously ran the backend against a host PostgreSQL installation (not the Compose stack) and want to preserve your catalog data.

**Steps:**

1. Make sure your host PostgreSQL is running and the backend is stopped.

2. From the `JPPhotoManagerWeb/` directory, run the script:
   ```bash
   cd JPPhotoManagerWeb
   ./scripts/migrate-db.sh
   ```
   The script dumps your host database, starts only the `db` container, waits for it to be ready, and restores the dump. Pass environment variables to override the defaults:
   ```bash
   PGHOST=localhost PGPORT=5432 PGUSER=postgres PGDATABASE=photomanager ./scripts/migrate-db.sh
   ```

3. Once the script prints "Migration successful!", stop your host PostgreSQL service:
   ```bash
   # Linux (systemd)
   sudo systemctl stop postgresql

   # macOS (Homebrew)
   brew services stop postgresql
   ```

4. Start the full stack:
   ```bash
   docker compose up --build
   ```

> **Rollback:** if anything goes wrong, the host database is untouched — the script only reads from it. Simply restart your host PostgreSQL and backend without the Compose stack.

## Services

| Service | Container | Host port | Description |
|---|---|---|---|
| `db` | `postgres:18` | `5433` | PostgreSQL 18; data persisted in the `pgdata` named volume |
| `kafka` | `apache/kafka:3.9.0` | `9092` (internal) `9094` (host) | Apache Kafka in KRaft mode (no ZooKeeper); pub/sub backbone for catalog/sync/convert progress events. Port 9092 is for inter-container traffic; port 9094 exposes the broker to the host machine. |
| `mongo` | `mongo:8` | `27017` | MongoDB 8; stores the `asset_audit_log` collection; data persisted in the `mongodata` named volume; no authentication configured |
| `redis` | `redis:7-alpine` | `6379` | Redis 7 (`allkeys-lru` eviction, 256 MB cap); backs the thumbnail cache, `assets`/`tags`/`home-stats`/`sub-folders`/`asset-exif` query caches, refresh-token mirror, and rate limiting (Bucket4j); no persistent volume — cache-only, safe to lose |
| `backend` | JRE 21 Alpine | `8080` | Spring Boot REST API; `HOST_IMAGE_DIR` bind-mounted at `/catalog`; connects to Kafka via `kafka:9092` and Redis via `redis:6379` |
| `frontend` | Nginx Alpine | `80` | Angular SPA; reverse-proxies `/api` to the backend |
| `prometheus` | `prom/prometheus` | `9090` | Scrapes backend metrics from `/actuator/prometheus` every 15 s |
| `grafana` | `grafana/grafana` | `3000` | Dashboard UI backed by Prometheus |

## Accessing services from the host

After `docker compose up`, all services are reachable from the host machine:

| Service | URL / address | Default credentials |
|---|---|---|
| Frontend (Angular SPA) | `http://localhost` | `admin` / `admin` — change after first login |
| Backend REST API | `http://localhost:8080/api` | JWT cookie set on login |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | — |
| PostgreSQL | `localhost:5433` | see table below |
| MongoDB | `localhost:27017` | no auth — see below |
| Redis | `localhost:6379` | no auth — cache only, safe to flush |
| Kafka | `localhost:9094` | no auth — see below |
| Prometheus | `http://localhost:9090` | — |
| Grafana | `http://localhost:3000` | `admin` / value of `GRAFANA_ADMIN_PASSWORD` (default: `admin`) |

### Connecting DBeaver to the database

Create a new **PostgreSQL** connection in DBeaver with the following settings:

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `photomanager` (or the value of `POSTGRES_DB` in your `.env`) |
| Username | `postgres` (or `POSTGRES_USERNAME`) |
| Password | `postgres` (or `POSTGRES_PASSWORD`) |
| SSL | disabled |

Steps:
1. Open DBeaver → **Database** menu → **New Database Connection**.
2. Select **PostgreSQL** and click **Next**.
3. Fill in the fields from the table above and click **Test Connection** to verify.
4. Click **Finish**.

### Installing a MongoDB client on Windows and connecting to the database

[MongoDB Compass](https://www.mongodb.com/try/download/compass) is the official GUI client — the MongoDB equivalent of DBeaver above.

**Install (pick one):**
- **winget** (recommended):
  ```powershell
  winget install MongoDB.Compass.Full
  ```
- **Manual download**: get the installer from the [MongoDB Compass download page](https://www.mongodb.com/try/download/compass) and run it.

Prefer a CLI instead of a GUI? Install `mongosh` the same way:
```powershell
winget install MongoDB.Shell
```

**Connection settings:**

| Field | Value |
|---|---|
| Connection string | `mongodb://localhost:27017/photomanager` |
| Authentication | None — the `mongo` container runs without credentials |

Steps (Compass):
1. Open MongoDB Compass.
2. Paste the connection string above into the **URI** field on the welcome screen.
3. Click **Connect**.
4. Expand the `photomanager` database in the sidebar to browse the `asset_audit_log` collection.

Or from `mongosh`:
```bash
mongosh "mongodb://localhost:27017/photomanager"
```

This same connection string works unchanged against a Kubernetes deployment — see [Accessing services](kubernetes.md#accessing-services) for the `kubectl port-forward` command that maps `svc/mongo` to the same local port.

### Installing a Kafka client/admin tool on Windows and connecting to the broker

[KafkIO](https://kafkio.com/) is a free, native Kafka GUI for Windows/macOS/Linux — the Kafka equivalent of DBeaver/MongoDB Compass above, for browsing topics, producing/consuming messages, and managing consumer groups.

**Install (pick one):**
- **Scoop**:
  ```powershell
  scoop bucket add extras
  scoop install kafkio
  ```
- **MSI installer**: download `KafkIO-win-<version>-x64.msi` from the [KafkIO download page](https://kafkio.com/download) and run it.
- **Portable ZIP**: download the portable `.zip` from the same page — no installation required.

Prefer the official CLI instead of a GUI? The same `apache/kafka:3.9.0` distribution the project already runs works locally too, with no risk of a mismatched client/broker protocol version:
1. Download and extract the matching binary release: [`kafka_2.13-3.9.0.tgz`](https://downloads.apache.org/kafka/3.9.0/kafka_2.13-3.9.0.tgz).
2. Requires a JDK on `PATH` — the JDK 21 already installed for backend development (see [Running the backend](backend.md#running-the-backend)) works fine.
3. Run the `.bat` scripts under `bin\windows\` from a command prompt inside the extracted folder, e.g.:
   ```powershell
   .\bin\windows\kafka-topics.bat --bootstrap-server localhost:9094 --list
   ```

**Connection settings:**

| Field | Value |
|---|---|
| Bootstrap server(s) | `localhost:9094` |
| Security protocol | `Plaintext` (no authentication) |

In KafkIO: add a new cluster connection, set **Bootstrap servers** to `localhost:9094` and **Security protocol** to **Plaintext**, then use the built-in connection test before saving.

This works as-is against Docker Compose, whose `EXTERNAL` listener is advertised as `localhost:9094` directly. Against Kubernetes it needs one extra step beyond a plain port-forward — see [Accessing services](kubernetes.md#accessing-services) below, which covers it in detail.

### Calling the backend API directly from the host

With port 8080 exposed you can hit the REST API directly — useful for testing with curl or tools like Insomnia:

```bash
# Log in and capture the jwt cookie
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Use the cookie to call a protected endpoint
curl -b cookies.txt http://localhost:8080/api/folders
```

## Monitoring (Grafana + Prometheus)

After `docker compose up`, Grafana is available at **`http://localhost:3000`**.

**First-time login:**

| Field | Value |
|---|---|
| Username | `admin` |
| Password | value of `GRAFANA_ADMIN_PASSWORD` in your `.env` (default: `admin`) |

Set `GRAFANA_ADMIN_PASSWORD` in `.env` before the first run. Changing it afterwards has no effect because Grafana stores the password in its persistent volume — update the password via the UI instead, or delete the `grafana_data` volume to reset.

**Persistence:** Grafana stores all configuration (dashboards, data sources, users) in a named Docker volume (`grafana_data`) so nothing is lost across container restarts or `docker compose down` (without `--volumes`).

**Pre-configured data source and dashboards:** The Prometheus data source (`http://prometheus:9090`) and the following dashboards are provisioned automatically from `grafana/provisioning/` — no manual setup required.

| Dashboard | Grafana ID | What it covers |
|---|---|---|
| JP Photo Manager | (custom) | HTTP rate, latency, JVM heap, CPU, Spring Batch catalog job |
| JVM (Micrometer) | 4701 | GC pauses, memory pools, threads, classloading, buffer pools |
| Spring Boot 3.x Statistics | 19004 | Basic stats, CPU, load average, JVM memory/GC, HikariCP pool, HTTP server stats, Logback |

**Explore metrics:**

The backend exposes Spring Boot Actuator metrics at `/actuator/prometheus`. Key metric families:

| Metric prefix | Description |
|---|---|
| `http_server_requests_*` | HTTP request counts, error rates, and latencies |
| `jvm_memory_*` | JVM heap and non-heap memory usage |
| `jvm_gc_*` | Garbage collection pause times and counts |
| `process_cpu_*` | JVM process CPU usage |
| `hikaricp_*` | Database connection pool utilisation |

You can also query Prometheus directly at **`http://localhost:9090`**.

**Create a dashboard manually:**

1. Go to **Dashboards → New → New dashboard → Add visualization**.
2. Select your Prometheus data source.
3. In the query editor, switch to **Code** mode and enter a PromQL expression. Useful starting points:

| What you want to see | PromQL |
|---|---|
| HTTP request rate (req/s) | `rate(http_server_requests_seconds_count[1m])` |
| HTTP error rate (5xx) | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| P99 request latency | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` |
| JVM heap used | `jvm_memory_used_bytes{area="heap"}` |
| GC pause time rate | `rate(jvm_gc_pause_seconds_sum[1m])` |
| DB connection pool active | `hikaricp_connections_active` |

4. Choose a visualization type (Time series, Gauge, Stat, …), set a title, and click **Apply**.
5. Repeat for each metric, then **Save dashboard**.

**Troubleshooting:**

*Prometheus target shows "Error scraping target: server returned HTTP status 500"*

The backend `GlobalExceptionHandler` has a catch-all `Exception` handler that intercepts `NoResourceFoundException` thrown when the `/actuator/prometheus` endpoint is not registered. Check the backend logs:

```bash
docker compose logs backend | grep -i "error\|exception\|actuator"
```

If you see `NoResourceFoundException: No static resource actuator/prometheus`, the `micrometer-registry-prometheus` JAR is missing from the running fat JAR — the container is using a stale image built before that dependency was added to `pom.xml`. Verify:

```bash
docker compose exec backend sh -c "unzip -l app.jar | grep micrometer"
```

If `micrometer-registry-prometheus-*.jar` does not appear, rebuild the backend image from scratch and force the container to use it:

```bash
docker compose build --no-cache backend
docker compose up -d --force-recreate backend
```

Note: `docker compose up --build` reuses Docker layer cache for the `mvn dependency:go-offline` step if `pom.xml` has not changed on disk. If the dependency is still missing after that, `--no-cache` + `--force-recreate` guarantees a clean build and a new container.

Confirm the endpoint is now registered:

```bash
docker compose exec backend wget -qO- http://localhost:8080/actuator
```

`prometheus` must appear in the `_links` object before Prometheus can scrape it.

*Grafana panels show "No data" even though the Prometheus target is UP*

Check that the Prometheus data source URL in Grafana is `http://prometheus:9090`, not `http://localhost:9090`. From inside the Grafana container, `localhost` resolves to Grafana itself, not to Prometheus. The Docker service name `prometheus` is the correct hostname.

Verify end-to-end connectivity with this minimal query in any Grafana panel:

```promql
up{job="photomanager-backend"}
```

A result of `1` means the full pipeline — Grafana → Prometheus → backend — is working.

## Volume behaviour

| Volume | Type | Description |
|---|---|---|
| `pgdata` | Named Docker volume | PostgreSQL data — survives `docker compose down`, removed by `docker compose down -v` |
| `thumbnails` | Named Docker volume | Generated thumbnail files — survives `docker compose down`, removed by `docker compose down -v` |
| `HOST_IMAGE_DIR` | Bind mount (read-write) | Your photos directory — changes made by the app are reflected on your host filesystem |

## Common commands

```bash
# Start (build images on first run or after code changes)
docker compose up --build

# Start without rebuilding
docker compose up

# Rebuild and restart a single service (e.g. after editing frontend or backend source)
docker compose up --build frontend
docker compose up --build backend

# Stop (keeps volumes — data preserved)
docker compose down

# Stop and wipe all volumes (full reset — deletes DB and thumbnails)
docker compose down -v

# View logs for a specific service
docker compose logs -f backend
```

## Linux file permission note

If write operations (delete, move, convert) fail with `AccessDeniedException`, the container user's UID doesn't match the owner of `HOST_IMAGE_DIR`. Fix by adding `user` to the `backend` service in `docker-compose.yml`:

```yaml
backend:
  user: "${UID}:${GID}"
```

Then start with:
```bash
UID=$(id -u) GID=$(id -g) docker compose up
```


---

## Running the full application (without Docker)

1. Start the backend:
   ```bash
   cd JPPhotoManagerWeb/backend
   mvn spring-boot:run
   ```

2. In a separate terminal, start the frontend dev server:
   ```bash
   cd JPPhotoManagerWeb/frontend
   npm install
   npm start
   ```

3. Open `http://localhost:4200` in your browser.

[← Back to README](../README.md)
