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
| Deep accessibility audit (per-route, axe-core, frontend) | `docs/reports/a11y/` | `npm run a11y:report` |
| Code coverage trend | `docs/reports/code-coverage/` | `npm run coverage:trend-report` (frontend) / `bash scripts/coverage-report.sh` (backend) |
| Bundle size (frontend) | `docs/reports/bundle-size/` | `npm run bundle:report` |
| Dependency staleness | `docs/reports/dependency-staleness/` | `npm run deps:staleness` (frontend) / Maven equivalent (backend) |
| E2E run/flakiness | `docs/reports/e2e-run/` | `npm run test:e2e:mocked:report` |
| Mutation testing | `docs/reports/mutation/` | `npm run mutation:report` (frontend) / `bash scripts/mutation-report.sh` (backend) |
| Secrets scanning | `docs/reports/secrets-scan/` | `npm run secrets:report` |
| License compliance | `docs/reports/license-compliance/` | `npm run license:report` (frontend) / `bash scripts/license-report.sh` (backend) |
| Dependency vulnerabilities (SCA) | `docs/reports/dependency-vulnerabilities/` | `npm run sca:report` (frontend) / `bash scripts/sca-report.sh` (backend) |

`docs/reports/` also holds the same-shaped output from the review skills (`code-review/`, `security-review/`, `spec-compliance/`, etc.) — not quality metrics, but written the same way.

### Where each metric is calculated, and whether it's automatic

Every metric is documented inside a skill under `.claude/skills/`, but "documented in a skill" and "runs automatically when you use `feature-development`" are not the same thing. `feature-development` invokes `code-reviewer` (and, conditionally, `security-reviewer`) in their **scoped Review workflow** at the end of a feature — not a full-codebase sweep, and not every report script that skill's `SKILL.md` documents. Only a metric whose checklist section says outright "run this every time" actually executes as part of that scoped review; everything else is documented (with a flagging rule for if you happen to have a report in hand) but is on-demand only, run by hand or scheduled separately.

| Metric | Owning skill | Runs automatically during `feature-development`? |
|---|---|---|
| Type coverage (frontend) | `code-reviewer` §20 | **Yes** — every scoped review |
| Complexity / file size | `code-reviewer` §18 (frontend §18.1, backend §18.2) | **Yes** — every scoped review |
| Code coverage (80% gate) | `code-reviewer` §19 (frontend §19.1, backend §19.2) | **Yes** — every scoped review; a sub-80% scope is a 🟡 Warning that blocks the review from being "clean" |
| Dead code | `code-reviewer` §21 (frontend §21.1, backend §21.2) | No — full-codebase sweeps only |
| Route coverage (frontend) | `e2e-suite` §8 | No — `feature-development` never invokes `e2e-suite`; run by hand when a new route ships |
| Auth coverage — Spring Security rules (backend) | `security-reviewer` | **Conditional** — auto-runs when the reviewed change is security-sensitive (auth/dependency/RLS-equivalent/input-handling files touched); otherwise not invoked |
| Lighthouse (perf/a11y, frontend) | `code-reviewer` §22 | No |
| Deep accessibility audit (axe-core, frontend) | `code-reviewer` §23 | No |
| Mutation testing | `code-reviewer` §24 (frontend §24.1, backend §24.2) | No — explicitly report-only, no CI/gate equivalent |
| Secrets scanning | `code-reviewer` §25 | No |
| License compliance | `code-reviewer` §25 | No |
| Dependency vulnerabilities (SCA) | `code-reviewer` §25 (dated report) + `security-reviewer` (raw `npm audit`) | **Conditional** — the raw `npm audit --audit-level=high` check auto-runs when the reviewed change touches dependencies/auth/input handling; the dated trend report (`sca:report`/`sca-report.sh`) is always on-demand |
| Code coverage trend (dated snapshot) | `code-reviewer` §19 | No — the §19 *gate* auto-runs (see above); the dated trend file itself is a full-sweep/on-demand extra |
| Bundle size (frontend) | *(not yet referenced by any skill)* | No |
| Dependency staleness | *(not yet referenced by any skill — `dependency-upgrade` runs raw `npm outdated`/`mvn versions:display-dependency-updates` instead of this report)* | No |
| E2E run/flakiness | *(not yet referenced by any skill)* | No |

### Running every metric manually

There's no single "run everything" script. From `frontend/`:

```bash
npm run type-coverage:report
npm run complexity:report
npm run dead-code:report
npm run route-coverage:report
npm run lighthouse:report
npm run a11y:report
npm run coverage:trend-report
npm run bundle:report
npm run deps:staleness
npm run test:e2e:report          # real backend — needs the full app deployed to k8s first, see the e2e-suite skill §1
npm run test:e2e:mocked:report   # no backend needed
npm run secrets:report           # scans the whole repo, not just frontend/
npm run license:report
npm run sca:report
npm run mutation:report          # by far the slowest — a full Stryker run, expect it to take significantly longer than every other report combined
```

From `backend/`:

```bash
bash scripts/complexity-report.sh
bash scripts/dead-code-report.sh
bash scripts/auth-coverage-report.sh
bash scripts/coverage-report.sh
bash scripts/dependency-staleness-report.sh
bash scripts/license-report.sh
bash scripts/sca-report.sh
bash scripts/mutation-report.sh   # slowest of the backend scripts — a full PIT run
```

Each writes its own dated file under `docs/reports/<category>/` and is safe to re-run — nothing here mutates source, and nothing is a CI gate except the code-coverage/complexity/type-coverage checks that already run inside `code-reviewer`.
