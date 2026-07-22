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
