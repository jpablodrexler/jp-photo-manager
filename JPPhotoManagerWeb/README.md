# JP Photo Manager — Web Edition

A web rewrite of the JP Photo Manager desktop application. It replaces the original WPF/.NET application with a modern client–server architecture: a **Java 21 + Spring Boot 3** REST API backend and an **Angular 19** single-page application frontend.

---

## Documentation

This README is split into topic-specific files under [`docs/`](docs/):

| Doc | Covers |
|---|---|
| [Features](docs/features.md) | Gallery, albums, duplicate detection, sync, conversion, recycle bin, dashboard, analytics, audio playback, cataloging, real-time progress, authentication |
| [Architecture](docs/architecture.md) | System architecture diagram, backend hexagonal architecture, database schema, frontend component hierarchy, project structure |
| [Backend](docs/backend.md) | Technologies, internal architecture, key services, persistence, observability/custom metrics, REST API, configuration, running/testing the backend, CI/CD, logging |
| [Frontend](docs/frontend.md) | Technologies, application structure, gallery modes, real-time progress (SSE), running/building/testing the frontend, installing as a PWA |
| [Running with Docker Compose](docs/docker-compose.md) | Prerequisites, setup, first-time migration, services, monitoring (Grafana + Prometheus), common commands, running without Docker |
| [Running with Kubernetes](docs/kubernetes.md) | Manifests, architecture differences from Docker Compose, prerequisites, setup, accessing services, common commands, troubleshooting |
| [Catalog Process](docs/catalog-process.md) | The Spring Batch catalog job, lifecycle/triggers, job structure, Kafka messages, configuration |
| [Authentication](docs/authentication.md) | JWT flow, configuration, generating `JWT_SECRET`, multiple catalog root folders, default admin user, user administration |
| [curl Command Reference](docs/curl-reference.md) | Example `curl` commands for every REST endpoint, grouped by resource |

---

## Web Application (this project)

`JPPhotoManagerWeb/` is a Java 21 + Spring Boot 3 backend (`backend/`) and an Angular 19 frontend (`frontend/`). See [Architecture](docs/architecture.md) for the system diagram and [Backend](docs/backend.md) / [Frontend](docs/frontend.md) for how to run each half locally, or [Running with Docker Compose](docs/docker-compose.md) / [Running with Kubernetes](docs/kubernetes.md) to run the full stack.

---

## Quality Metrics

Each report below is a dated markdown snapshot written to `JPPhotoManagerWeb/docs/reports/<category>/` (gitignored, regenerated on demand — a fresh run adds a new dated file rather than overwriting the last one, so the directory accumulates a history you can diff over time). Frontend commands run from `frontend/`; backend commands run from `backend/`. A category with both a frontend and backend version writes separate `*_frontend.md` / `*_backend.md` files under the same directory.

| Metric | Report location | Regenerate |
|---|---|---|
| Type coverage (frontend) | `docs/reports/type-coverage/` | `npm run type-coverage:report` |
| Complexity / file size | `docs/reports/complexity/` | `npm run complexity:report` (frontend) / `bash scripts/complexity-report.sh` (backend) |
| Dead code | `docs/reports/dead-code/` | `npm run dead-code:report` (frontend) / `bash scripts/dead-code-report.sh` (backend) |
| Route coverage (frontend) | `docs/reports/route-coverage/` | `npm run route-coverage:report` |
| Auth coverage — Spring Security rules (backend) | `docs/reports/auth-coverage/` | `bash scripts/auth-coverage-report.sh` |
| Lighthouse (perf/a11y, frontend) | `docs/reports/lighthouse/` | `npm run lighthouse:report` |
| Code coverage trend | `docs/reports/code-coverage/` | `npm run coverage:trend-report` (frontend) / `bash scripts/coverage-report.sh` (backend) |
| Bundle size (frontend) | `docs/reports/bundle-size/` | `npm run bundle:report` |
| Dependency staleness | `docs/reports/dependency-staleness/` | `npm run deps:staleness` (frontend) / Maven equivalent (backend) |
| E2E run/flakiness | `docs/reports/e2e-run/` | `npm run test:e2e:mocked:report` |

`docs/reports/` also holds the same-shaped output from the review skills (`code-review/`, `security-review/`, `spec-compliance/`, etc.) — not quality metrics, but written the same way.
