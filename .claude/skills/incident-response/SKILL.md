---
name: incident-response
description: >
  Production incident triage and rollback playbook for the JPPhotoManager
  web application (Kubernetes/Docker Compose deploy, Postgres/Mongo/Redis/
  Kafka backing services). TRIGGER when asked to diagnose a production
  issue, a failed or bad deploy, a stuck catalog/sync/convert/upload job, a
  crash-looping pod, or whether to roll back a recent release/hotfix. Also
  TRIGGERS when asked to investigate why a feature that worked in E2E
  testing is broken after deployment. Do not perform any rollback or
  data-affecting action without explicit user confirmation — this skill
  triages and recommends; it does not execute destructive recovery steps
  unsupervised.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Incident Response Skill

Triage a live problem in the deployed application, decide whether the right
recovery is roll back or forward-fix, and — only with explicit confirmation
at each hard-to-reverse step — carry it out. This project has already lived
through one real incident of this shape: `asset_exif` was moved to MongoDB
(`mongodb-exif-store`, #72) and later reverted back to PostgreSQL
(`revert-exif-postgres-jsonb`, #84) after the feature that motivated the
move was cancelled — via a **new forward migration**, not by deleting the
one that introduced the change. That precedent is the model for how schema
issues get "rolled back" in this codebase (see §3).

## Workflow

1. **Establish the signal** (§1) — what's actually observed, and since when.
2. **Gather evidence** (§2) — logs, pod/container status, recent deploys,
   recent migrations, dependency health.
3. **Classify** (§3) — infra-level (won't start), application-level (bad
   code path or bad migration), or dependency-level (Postgres/Mongo/Redis/
   Kafka unreachable or degraded).
4. **Decide: rollback vs. forward-fix** (§4).
5. **Execute — with confirmation gates** (§5).
6. **Verify recovery** (§6) — reuse `feature-development` Phase 4's
   post-deploy smoke test pattern; don't declare the incident resolved on
   "pods are Running" alone.
7. **Write an incident note** (§7).

---

## 1. Establish the Signal

Get specific before doing anything else:

- What's the observed symptom — errors returned to users, a stuck SSE
  progress stream, elevated latency, a crash-looping pod, missing data?
- Since when — correlate against the deploy/tag history
  (`git log --oneline --all --decorate -20`, `kubectl rollout history
  deployment/backend -n photomanager`) to find what changed recently.
- Scope — one user, one endpoint, everyone, one Kafka consumer group, one
  pod out of N replicas?

A vague "something's wrong" report should be narrowed to at least one of
the above before moving to §2 — the evidence to gather differs a lot
depending on whether this is "the backend won't start" vs. "uploads
silently never finish."

---

## 2. Gather Evidence

Pull from whichever deploy target is live (check `kubectl config
current-context` / `docker compose ps` the same way `feature-development`
Phase 4 does):

**Kubernetes:**
```bash
kubectl get pods -n photomanager -l 'app in (backend,frontend)'
kubectl describe pod <pod-name> -n photomanager   # events, restart count, probe failures
kubectl logs <pod-name> -n photomanager --tail=200
kubectl logs <pod-name> -n photomanager --previous  # if it restarted, the log before the restart
kubectl rollout history deployment/backend -n photomanager
```

**Docker Compose:**
```bash
docker compose ps
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
```

**Cross-cutting, either target:**
- Recent Flyway migrations: `ls JPPhotoManagerWeb/backend/src/main/resources/db/migration/` — did the most recent one apply cleanly, or is the backend refusing to start on a checksum/validation error (Flyway fails startup outright on this — see `database-reviewer` §5)?
- Kafka consumer lag/health, if the deploy has monitoring — this app ships Prometheus + Grafana (`docker-compose.yml` / `k8s/prometheus.yaml`, `k8s/grafana.yaml`); check dashboards there before falling back to raw `kubectl logs` grepping if they're reachable.
- Redis reachability — **remember every Redis-backed path in this app fails open** (`redis-caching-conventions` §4): a down Redis does *not* crash the backend or show up as an obvious error. Symptoms are subtler (slow responses, thumbnails always regenerating, rate limiting not enforced) — check Redis connectivity explicitly rather than ruling it out because nothing crashed.
- Mongo reachability — `asset_audit_log` writes will fail (logged `WARN`, not fatal — see `AuditLogKafkaListener`'s catch-and-log pattern) if Mongo is down; this degrades audit history, not core functionality, so it's a lower-severity finding on its own but worth ruling in/out.

---

## 3. Classify

| Class | Signature | Example |
|---|---|---|
| **Infra-level** | Pod never reaches `Running`/`Ready`, or crash-loops | `startupProbe` timing out — see `k8s/backend.yaml`'s own comment on Spring context startup taking 5+ min under CPU contention before assuming this is a code regression |
| **Application-level** | Pod is healthy but a specific feature is broken or a job never completes | A bad code change in the most recent deploy; a stuck Kafka consumer (wrong consumer-group shape — cross-check `kafka-events-conventions` §2) |
| **Migration-level** | Backend refuses to start with a Flyway validation/checksum error, or starts but a query now fails | A migration that doesn't match what's actually applied, or a schema change the deployed code doesn't expect |
| **Dependency-level** | The app itself is fine but Postgres/Mongo/Redis/Kafka is unreachable or misconfigured | Wrong `POSTGRES_HOST`/`KAFKA_BOOTSTRAP`/`REDIS_HOST`/`MONGO_URI` in the deploy's config/secret |

An incident can be more than one of these at once (e.g. a bad migration
that also crash-loops the pod) — don't stop classifying at the first match
if the evidence from §2 points to more than one.

---

## 4. Decide: Rollback vs. Forward-Fix

- **Rollback** when the previous deployed state was known-good and the
  fastest safe recovery is reverting to it — appropriate for infra-level and
  most application-level incidents where the regression is isolated to the
  most recent deploy.
- **Forward-fix** when rolling back would lose data, when the bad state
  isn't purely "the last deploy" (e.g. a migration already partially
  applied against production data), or when the issue predates the most
  recent deploy. **Migration-level incidents are forward-fix by default** —
  this project's convention (§5.5, and the `mongodb-exif-store` →
  `revert-exif-postgres-jsonb` precedent) is a new corrective migration,
  never editing or deleting an applied one.

If it's ambiguous which is safer (e.g. the last deploy included both a code
fix and a migration, and only the migration is suspect), say so explicitly
and ask the user rather than picking one unilaterally — same rule
`database-reviewer`/`security-reviewer` use for design-level fix decisions.

---

## 5. Execute — With Confirmation Gates

**Every action in this section is hard-to-reverse, visible, or both. Stop
and get explicit confirmation immediately before each one — not once at the
start for the whole plan.**

### 5.1 Kubernetes rollback

```bash
kubectl rollout undo deployment/backend -n photomanager
kubectl rollout undo deployment/frontend -n photomanager
kubectl rollout status deployment/backend -n photomanager --timeout=12m
kubectl rollout status deployment/frontend -n photomanager --timeout=2m
```

Confirm with the user before running `rollout undo` — it changes what every
user of the deployed app sees. Note: `k8s/backend.yaml`'s `startupProbe`
allows up to 10 minutes for Spring Boot startup under contention (see its
comment) — a rollback isn't stuck just because it's slow; give it the same
runway `feature-development` Phase 4 does before treating it as failed.

### 5.2 Docker Compose rollback

**This project does not currently retain versioned images** —
`docker-compose.yml` builds `photomanager-backend:latest`/
`photomanager-frontend:latest` with no version tag, so there is no
previous image to fast-swap back to. A Compose rollback means: confirm
with the user, `git checkout <previous-known-good-ref>` for the affected
service's source, then rebuild and redeploy via the same
`docker compose up --build -d <services>` sequence `feature-development`
Phase 4 uses. This is slower than a Kubernetes rollback and worth flagging
to the user as a gap if instant rollback matters — retaining a tagged image
per release (e.g. `photomanager-backend:v2.3.0`) alongside `:latest` would
close it, but that's a deploy-pipeline change outside this skill's scope to
make unilaterally.

### 5.3 Forward-fix via hotfix branch

For an application-level bug needing a code fix rather than a rollback, use
the `gitflow` skill's **start hotfix** action to cut a `hotfix/<name>`
branch from `main`, fix the issue, then **finish hotfix** to open the PRs
per that skill's normal flow. Don't hand-edit code directly against a
running deployment's checked-out state outside of a proper branch.

### 5.4 Restarting a stuck consumer/job

A stuck catalog/sync/convert/upload job (no progress events, no terminal
event — see `e2e-testing` §6.4 for the SSE-side symptoms) may just need the
backend restarted to rejoin its Kafka consumer group, rather than a full
rollback:

```bash
kubectl rollout restart deployment/backend -n photomanager   # Kubernetes
docker compose restart backend                                # Compose
```

Confirm with the user first — this drops any in-flight requests, not just
the stuck job.

### 5.5 Migration-level fixes

**Never edit or delete an already-applied migration file** (same rule as
`database-reviewer` §5). Write a new `V{n+1}__*.sql` that corrects the
problem — a corrective `ALTER TABLE`, a data backfill, or (for the
"wrong store entirely" case this project has precedent for) a migration
that moves data back to its previous store. If the fix requires a decision
about data loss or a large backfill's locking behavior, stop and ask the
user rather than picking a strategy unilaterally (same rule as
`database-reviewer` §8.3).

---

## 6. Verify Recovery

Don't declare the incident resolved on pod/container status alone — reuse
`feature-development` Phase 4's post-deploy smoke test (an authenticated
API call through the real deploy path, not just `/actuator/health`) and, if
the original symptom involved an SSE-driven feature, `e2e-testing` §6's
stream-verification steps. The goal is confirming the **original symptom**
is gone, not just that the process is running.

---

## 7. Write an Incident Note

After resolution, write a short note to
`docs/incident-response/INCIDENT_{YYYY-MM-DD}_{short-slug}.md` (this
directory is gitignored, same convention as `docs/database-review/` and
`docs/security-review/` — a local working record, not committed history
unless the user explicitly asks to commit it):

- **Signal** — what was observed and since when (§1).
- **Root cause** — the classification from §3 and what evidence supported it.
- **Resolution** — rollback or forward-fix, and exactly what was run.
- **Follow-up** — anything that should become a real fix later (e.g. "add
  versioned image tags to close the Compose-rollback gap noted in §5.2") so
  it isn't lost once the immediate fire is out.

Do not commit this file or anything else as part of this skill — leave it
for the user to review, same as every other review/fix skill in this repo.
