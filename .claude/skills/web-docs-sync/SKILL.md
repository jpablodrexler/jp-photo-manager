---
name: web-docs-sync
description: >
  Keeps `JPPhotoManagerWeb/CLAUDE.md`, the reference docs under
  `JPPhotoManagerWeb/docs/`, and deploy-manifest/observability parity
  (`docker-compose.yml` ↔ `k8s/*.yaml`; custom Micrometer metrics ↔ the
  Grafana dashboard) in sync with the actual code. TRIGGER after
  implementing a feature or OpenSpec change that adds or changes a REST
  endpoint, a Spring `@Value`/`application.yml` config property, a named
  Redis cache, a Kafka topic or consumer group, a Flyway migration that
  changes which datastore owns a table, a frontend route, a Docker
  Compose/Kubernetes environment variable, or a custom `photomanager_*`
  metric. Also TRIGGERS when explicitly asked to check, audit, or sync the
  web app's documentation against the code, or as a standalone full sweep.
  Do not wait to be asked after a feature lands with any of the above
  changes — none of this is re-derived automatically, so it drifts
  silently otherwise.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Web Docs Sync Skill

`JPPhotoManagerWeb` documents itself in two tiers, and both need active
upkeep:

- **`CLAUDE.md`** — a lean quick-start for an agent working in the code:
  commands, package layout, coding conventions, and pointers to the tier
  below for anything detailed. It should **never** carry a REST API table,
  a full config table, or long narrative paragraphs inline — those belong
  in `docs/`. If you find one growing back in `CLAUDE.md`, move it out; this
  file was restructured once already (see its own "Documentation" section)
  specifically because unrestrained inline detail drifted silently for a
  long time before anyone re-derived it from the code.
- **`JPPhotoManagerWeb/docs/*.md`** — the actual reference material,
  indexed from `JPPhotoManagerWeb/README.md`. This is comprehensive and, as
  of the last full sweep, considerably more accurate and detailed than
  `CLAUDE.md` ever was — it includes mermaid diagrams, troubleshooting
  sections, and provenance notes. Nothing else in the repo maintains it
  automatically; that's this skill's job.

This skill has two modes:

- **Scoped sync** (default) — after implementing one feature/change, check
  only the doc sections that change could plausibly affect, and fix them.
- **Full sweep** — audit every table/section in §1 against the current
  codebase from scratch. Use when asked to "audit," "check for drift," or
  when it's been a while since the last scoped sync ran.

Both modes **edit the doc/config files directly** rather than writing a
separate findings report — for `CLAUDE.md`/`docs/*.md` this is documentation,
not application code, so the fix *is* the review output; for the manifest
parity and metrics rows (§1) the fix is a small, targeted config/dashboard
edit, still low-risk enough to apply directly rather than only report. Show
the user a short diff-style summary of what changed in chat.

---

## 1. Doc Sections & Their Source of Truth

| Section | File(s) | Source of truth to check against |
|---|---|---|
| Commands, package layout, conventions | `CLAUDE.md` | `pom.xml`/`package.json` scripts, actual package structure, existing code style |
| REST API table | `docs/backend.md` (REST API section) | Every `@RestController` in `backend/.../infrastructure/web/controller/` — one row per `@GetMapping`/`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`, auth column cross-checked against `SecurityConfig`'s `permitAll()`/`hasRole()` rules and any method-level `@PreAuthorize` |
| `curl` examples | `docs/curl-reference.md` | Same controllers as above — one example per endpoint, kept in sync with the REST API table's endpoint list |
| Configuration table | `docs/backend.md` (Configuration section) | Every `@Value("${...}")` usage plus `application.yml` defaults |
| Persistence narrative | `docs/backend.md` (Persistence), `docs/architecture.md` (DB diagram) | `@Entity` classes (Postgres), Mongo repository classes, Redis dual-writes/mirrors — grep for a second store's write inside a method that already writes to the primary store |
| Authentication flow | `docs/authentication.md` | `SecurityConfig`, `AuthController`, `JwtTokenServiceAdapter`, `RefreshTokenServiceAdapter`, cookie names/flags |
| Catalog pipeline & Kafka topics | `docs/catalog-process.md` | `infrastructure/batch/**`, `infrastructure/kafka/**`, `config/KafkaTopicConfig.java` — cross-check with `kafka-events-conventions` |
| Server-side caching | `docs/backend.md`/`docs/architecture.md` | `AppConfig.cacheManager()`'s `perCacheConfigurations` — cross-check with `redis-caching-conventions` |
| System/backend/database diagrams | `docs/architecture.md` | Overall package structure, Flyway migration history for the ERD, `app.routes.ts` for the frontend component diagram |
| Frontend structure & routes | `docs/frontend.md` | `frontend/src/app/app.routes.ts`, `features/` directory listing |
| Feature list | `docs/features.md` | What's actually reachable in the UI — cross-check against routes, not aspirational/planned features |
| Docker Compose reference | `docs/docker-compose.md` | `docker-compose.yml`, `.env.example` |
| Kubernetes reference | `docs/kubernetes.md` | `k8s/*.yaml`, `kustomization.yaml`, `scripts/*.sh` |
| **Deploy manifest parity** | `docker-compose.yml` ↔ `k8s/configmap.yaml` + `k8s/backend.yaml`/`k8s/frontend.yaml` env blocks | See §2 |
| **Custom metrics ↔ dashboard** | `grafana/provisioning/dashboards/photomanager.json` | Every `Counter.builder("photomanager_...")`/`Timer.builder(...)`/`Gauge.builder(...)` registration — see §3 |

---

## 2. Deploy Manifest Parity

`k8s/configmap.yaml`'s own header comment states the requirement directly:
"Mirrors the plain environment variables set in `../docker-compose.yml`'s
`backend` service" — but nothing checks that this stays true after either
file changes.

**Check:** every non-secret environment variable in `docker-compose.yml`'s
`backend` service block has a matching key in `k8s/configmap.yaml` (or, for
values that must differ between the two — e.g. `KAFKA_BOOTSTRAP: kafka:9092`
in both, since both use the in-cluster/in-network service name `kafka` — a
deliberate, in-cluster-appropriate value). Do the same check in the other
direction: every `k8s/configmap.yaml` key should trace back to a
`docker-compose.yml` env var or an explicit Kubernetes-only reason for its
existence (there shouldn't be many — the whole point of the ConfigMap is to
mirror Compose).

**Secrets are out of scope for this check.** `JWT_SECRET`,
`POSTGRES_PASSWORD`, and `GRAFANA_ADMIN_PASSWORD` live in `.env` /
`k8s/secret.yaml` respectively — **never read `k8s/secret.yaml` or
`k8s/catalog-volumes.yaml`** (per `CLAUDE.md`'s "Never read these files").
Confirming a secret *key* is present in both `.env.example` and
`k8s/secret.yaml.example` (the safe-to-read templates) is fine; confirming
*values* match is not something this skill does or needs to do.

🔴 Flag a new environment variable added to `docker-compose.yml`'s `backend`
service with no corresponding `k8s/configmap.yaml` entry (or vice versa) —
this is a live bug, not just a doc gap: the Kubernetes deployment will
silently run with a stale/default value for that property until someone
notices.

**Fix:** add the missing key directly to whichever manifest is missing it,
matching the existing formatting/comment style in that file. This is a
config file edit, not a markdown edit — still covered by this skill's
"apply directly" workflow (§ above), but flag it clearly in the chat summary
since it's a higher-stakes file than a doc.

---

## 3. Custom Metrics ↔ Grafana Dashboard

Every custom application metric follows the pattern documented in
`docs/architecture.md`'s "Observability" section (also see `java-developer`
if a similar convention gets added there): `photomanager_<name>` via an
injected `MeterRegistry`, with `.description(...)`. As of the last full
sweep the existing metrics are `photomanager_catalog_assets_total`
(Counter), `photomanager_thumbnail_generation_seconds` (Timer), and
`photomanager_active_sse_connections` (Gauge) — but this list drifts the
moment a new one is added, so re-derive it rather than trusting this list.

**Check:** grep the backend for `Counter.builder("photomanager_`,
`Timer.builder("photomanager_`, and `Gauge.builder("photomanager_` to get
the current, real list. For each one, confirm
`grafana/provisioning/dashboards/photomanager.json` has a panel querying
that metric name (search the dashboard JSON's `"expr"` fields).

🟡 Flag any `photomanager_*` metric registered in code with no corresponding
dashboard panel — it's being collected and scraped but nobody can see it
without writing an ad-hoc PromQL query first.

🟢 Flag a dashboard panel querying a `photomanager_*` metric name that no
longer exists in code (the reverse drift — a removed/renamed metric leaving
a dead panel).

**Fix:** add a panel to `photomanager.json` following the existing panels'
structure (same `datasource`, similar `gridPos` placement, a PromQL `expr`
appropriate to the metric type — `rate(...)` for a counter, a raw gauge
value, `histogram_quantile`/`_sum`/`_count` for a timer, matching the
existing `HTTP p95/p99 Latency` panels' shape).

---

## 4. Scoped Sync Workflow

1. Identify what changed: `git diff origin/develop..HEAD --stat` (or the
   equivalent for whatever base the current feature branch was cut from) if
   run at the end of a feature; otherwise, whatever files the user points to.
2. Map each changed file to the row(s) in §1 (and §2/§3 if relevant) whose
   source of truth it touches. Skip rows whose source of truth wasn't
   touched — don't re-verify everything on a scoped sync.
3. For each mapped row, read the current doc text and the current code side
   by side and determine what's missing, stale, or wrong.
4. Apply the fix directly with `Edit`, one section at a time, matching that
   file's existing style (headings, table columns, mermaid diagram
   conventions if touching `docs/architecture.md`).
5. **If you find detail that belongs in `docs/` sitting inline in
   `CLAUDE.md` instead** (the exact failure mode this skill was created to
   stop), move it — don't just update it in place. Replace the inline
   content with a short pointer, matching `CLAUDE.md`'s existing
   "Documentation" section pattern.
6. Summarize in chat: which sections were checked, which were updated, and
   the one-line reason for each update.

---

## 5. Full Sweep Workflow

1. Read `JPPhotoManagerWeb/CLAUDE.md` and `JPPhotoManagerWeb/README.md` in
   full so you have the current doc index.
2. For each row of §1 (plus §2 and §3), independently re-derive the current
   state from code rather than trusting the doc's existing content, and
   compare. For the REST API / config / curl-reference rows in particular,
   grep every controller / every `@Value` usage directly — don't sample a
   few and assume the rest are current (this is exactly how `CLAUDE.md`'s
   REST API table ended up covering 6 of 15 controllers before the doc
   restructuring that created this skill).
3. Collect every discrepancy found before editing anything, so overlapping
   edits to the same file can be merged into one pass.
4. Apply all fixes with `Edit`, grouped by file.
5. Summarize in chat: a short list of what was added/updated/removed,
   grouped by file, so the user can spot-check anything surprising (e.g. a
   removed row means an endpoint/property/metric that no longer exists in
   code).

---

## Guardrails

- Never document behavior you haven't verified in the actual code — no
  inferring a config default from a variable name, no guessing an
  endpoint's auth requirement from the URL shape. Read `SecurityConfig`'s
  rule list (and any method-level `@PreAuthorize`) directly for auth
  columns.
- Never read `JPPhotoManagerWeb/k8s/secret.yaml` or
  `JPPhotoManagerWeb/k8s/catalog-volumes.yaml` while doing this — same rule
  as `security-reviewer`/`database-reviewer`/`incident-response`. Secret
  *keys* (not values) can be checked via the `.example` templates.
- Don't let `CLAUDE.md` regrow the content this skill's first run extracted
  out of it — a REST API table, a full config table, or a multi-paragraph
  persistence/caching narrative appearing there again is itself a finding
  to fix (move it to `docs/`), not just a staleness issue to patch in place.
- Don't create a dated report file the way `code-reviewer`/`security-reviewer`/
  `database-reviewer` do — the corrected docs/config/dashboard files *are*
  the artifact. The chat summary is enough of a record for this session.
- Manifest-parity and dashboard-panel fixes (§2, §3) are config edits, not
  prose — call them out distinctly in the chat summary rather than folding
  them silently into the same list as a typo fix in `docs/features.md`.
