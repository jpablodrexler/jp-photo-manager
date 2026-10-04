---
name: architecture-reviewer
description: >
  Whole-system architecture health check for the JPPhotoManagerWeb Angular
  frontend (`JPPhotoManagerWeb/frontend`). Assesses file/folder organization,
  the `core → features ← shared` layering and inter-layer communication
  (services wrapping the Spring REST API, never a direct `HttpClient` call
  from a component), API/data-access patterns (REST + the auth interceptor,
  HttpOnly-cookie JWT, Server-Sent Events progress streams, pagination),
  browser-storage (`localStorage`) usage, and state-management patterns —
  specifically hunting for a shared service or stream being reused past its
  own name as an undocumented cross-feature coordination bus — against
  Angular/industry best practice and the project's own documented
  conventions (`JPPhotoManagerWeb/CLAUDE.md`, `JPPhotoManagerWeb/docs/architecture.md`,
  `JPPhotoManagerWeb/docs/frontend.md`). A qualitative, whole-system structural
  review, distinct from `code-reviewer` (per-diff/per-PR correctness and
  convention checks) and `quality-metrics` (numeric automated sweeps) — run it
  periodically or whenever something about the app's overall shape is in
  question, not on every commit. TRIGGER when the user asks for an
  architecture review or assessment, asks whether the frontend follows
  good/recommended practice or a particular pattern, asks to "dig deeper"
  into a specific architectural dimension already raised (state management,
  data flow, layering, API calls, local storage, etc.), or asks to audit code
  organization/state management/API-call patterns. Also TRIGGERS when asked to
  fix, address, resolve, or work through findings from an existing dated
  ARCHITECTURE_REVIEW report — see "Follow-up Workflow" below.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
  scope: [frontend]
---

# Architecture Reviewer Skill

Assess whether the Angular frontend's overall structure — not a specific diff
or PR, but the shape of the system as a whole — still follows recommended
Angular practice and the project's own documented conventions. This skill
exists because the kind of issue it looks for rarely shows up in a single-PR
`code-reviewer` pass: it is implicit coupling that accretes gradually, one
individually-reasonable change at a time, across many separate reviews that
each only saw their own diff.

The motivating shape: a service that starts as one narrow concern (a
preference, a playback state, a sync status) and quietly becomes the
cross-feature "something changed, refetch" broadcast channel for several
unrelated features. Nothing about the service's name says so, and no single PR
that added one more call site would have looked wrong in isolation.

**Input**: either "review the architecture" (full review, all dimensions
below) or a specific dimension to review or dig into further (e.g. "just state
management", "how does it handle local storage", "look deeper at layering") —
a deep-dive on one dimension, at higher depth than the full review gives each.

---

## The dimensions

1. **File/folder organization & naming** — the `core → features ← shared`
   structure (`JPPhotoManagerWeb/docs/frontend.md`'s "Application structure"),
   whether feature folders stay self-contained, whether `shared/` holds
   genuinely multi-consumer code or single-consumer code kept there out of
   habit.
2. **Layering & inter-layer communication** — the service pattern (data access
   in an injectable `core/services/` service wrapping the backend REST API,
   never a direct `HttpClient` call from a component), and how components talk
   to each other absent a direct parent/child relationship (an
   `@Input`/`@Output`, a shared service, or — see dimension 5 — an implicit
   shared stream or signal).
3. **API / data-access patterns** — `HttpClient` + `auth.interceptor.ts`
   (401 refresh-and-retry, error snackbar), the HttpOnly-cookie JWT model (no
   token handling in components), Server-Sent Events for catalog/sync/convert
   progress (subscription lifecycle: is every `EventSource`/SSE stream closed
   on destroy or completion?), and pagination (`PaginatedData` models and
   whether list endpoints are consumed page-wise rather than fetched whole).
4. **Browser-storage (`localStorage`) usage** — enumerate every
   `localStorage` read/write under `JPPhotoManagerWeb/frontend/src/app/`
   (`Grep -rn "localStorage\." JPPhotoManagerWeb/frontend/src/app`, excluding
   `*.cy.ts`); confirm each is a genuine per-browser preference (theme, accent
   colour) rather than something that should be synced server-side, and
   confirm what the stored session value actually contains — an HttpOnly
   cookie model should not leave bearer tokens readable from JavaScript.
5. **State management** — RxJS and/or signals in `core/services/`, no
   NgRx/Akita (appropriate for this app's actual complexity — don't recommend
   one unless evidence genuinely shows a need). The specific technique this
   skill applies here, always, on every run that touches this dimension: for
   every `core/services/` service that exposes a signal, a
   `BehaviorSubject`/`Subject`, an `effect()` dependency, or an event-like
   method (`refresh()`, `notify()`, a `version`/`tick`-style counter), `Grep`
   every consumer across the whole `frontend/src/app` tree and check whether
   those consumers span features unrelated to the service's own
   name/purpose. A service whose consumers span two or more features with no
   parent/child relationship to each other, and whose name doesn't describe a
   broadcast role, is the exact shape this skill exists to catch — flag it
   with the full consumer count and the list of features it spans. Start with
   services that already look cross-cutting (`background-sync.service.ts`,
   `media-player.service.ts`, `theme.service.ts`) but check every service.
6. **Routing & guards** — lazy-loading coverage in `app.routes.ts`,
   functional guard usage (`authGuard`), whether route-level concerns (auth
   gating) stay out of feature components.
7. **Error handling & logging** — `core/error-handler/global-error-handler.ts`
   and the interceptor's error snackbar coverage; whether a feature has grown
   its own ad hoc error handling instead of relying on the global pipeline.
8. **Testing architecture** — a light pointer only (component-test ratio,
   whether new features get co-located `.cy.ts` files). Don't duplicate
   `cypress-unit-test-developer`/`e2e-suite`/`quality-metrics` — they own this
   in depth; flag it only if a structural pattern (e.g. a whole feature with no
   tests at all) shows up incidentally while reviewing another dimension.

---

## Steps

### 1. Determine scope

- **Full review**: all 8 dimensions, one pass each, at survey depth (2-4
  concrete evidence points per dimension).
- **Single-dimension deep-dive**: the user names one dimension (or one
  already flagged in a previous report — see "Follow-up Workflow"). Go deep:
  read the actual service/component source in full, `Grep` every consumer,
  quantify (counts, which features/components), and cite `file:line` for every
  claim.

### 2. Read the documented baseline first

Read `JPPhotoManagerWeb/CLAUDE.md` and the relevant sections of
`JPPhotoManagerWeb/docs/architecture.md`/`JPPhotoManagerWeb/docs/frontend.md`
for what the project *claims* its architecture does. This is the starting
hypothesis, not the answer — docs can drift from the code. Verify every claim
against the actual source in step 3 rather than reporting the docs' own prose
as a finding.

### 3. Ground every claim in real code

For each in-scope dimension, `Grep`/`Read` the actual files — don't assess from
documentation alone. If the docs and the code disagree, that mismatch is its
own finding, tagged for `web-docs-sync` (a docs-accuracy issue), separate from
and never conflated with an actual architecture finding (a code-shape issue).
For dimension 5, always run the "hunt the broadcast" technique described there
— it is the concrete mechanism that catches what a per-diff review misses, so
don't skip it even on a full-review pass; a quick `Grep` per service is cheap.

### 4. Classify each finding

- **✅ Strength** — a pattern worth explicitly keeping. Cite it with the same
  rigor as a risk (file/pattern named) so it doesn't get accidentally "fixed"
  away by someone who didn't know it was deliberate.
- **🟡 Worth addressing** — a real deviation, growing coupling, or friction
  that isn't urgent or user-blocking today.
- **🔴 Structural risk** — actively causing, or clearly about to cause, a
  correctness or maintainability problem (e.g. a component calling
  `HttpClient` directly, bypassing its `core/services/` service; an SSE stream
  that is never closed).

Every 🟡/🔴 finding needs concrete evidence: `file:line`, and for a
coupling/coordination finding, the consumer count and which
features/components it spans — never a bare "this could be an issue" without
the grep behind it.

### 5. Present in chat, then write the dated report

In-chat summary groups findings under `### ✅ Strengths`, `### 🟡 Worth
addressing`, `### 🔴 Structural risks` (omit a heading with nothing in it),
each a bullet with `file:line` evidence — mirroring `code-reviewer`'s
severity-grouped format. Close with a short verdict: overall health, and the
single most important thing to address first if anything is 🔴 or
accumulating in 🟡.

**Write the report to a dated file** (every run, full or single-dimension):

- **Path:** `JPPhotoManagerWeb/docs/reports/architecture-review/ARCHITECTURE_REVIEW_{YYYY-MM-DD}.md`
  for a full review, or
  `.../ARCHITECTURE_REVIEW_{YYYY-MM-DD}_{dimension-slug}.md` for a
  single-dimension deep-dive (e.g. `_state-management`). If a file for that
  date/scope already exists, append `-2`, `-3`, etc. rather than overwriting.
- This directory is gitignored (`JPPhotoManagerWeb/docs/reports/*` catch-all in
  `.gitignore`, not individually un-ignored like the `quality-metrics`
  categories) — reports are local working artifacts, not committed history.
  Create the directory if it doesn't exist yet.
- Content: the same ✅/🟡/🔴 grouping as the in-chat summary, using GitHub
  task-list checkboxes (`- [ ]`) per 🟡/🔴 finding (✅ strengths stay plain
  bullets). Include a short header noting the scope (full review, or which
  dimension) and which commit/state the review was run against.
- Never overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.

### 6. Candidate follow-ups — never auto-file

End the report with a `### Candidate follow-ups` list: for each 🟡/🔴 finding,
name which skill would record it —

- **`bug-report`** for a concrete tech-debt item or an actual defect (the
  normal case).
- **`decision-record`** if the review reaffirms a decision already made (worth
  an ADR if none exists yet) or surfaces one that should be reversed.

List the candidates; do not invoke either skill automatically. Ask the user
which findings (if any) they want filed now — the review happens first,
entirely in chat, and filing is a separate, explicit follow-up request.

---

## Follow-up Workflow

Use this when asked to revisit an **existing** dated report instead of running
a fresh review.

1. Resolve which report: one the user names directly, or (default) the most
   recent `ARCHITECTURE_REVIEW_*.md` for the dimension in question (or the most
   recent full review if none is named). If none exists, say so and suggest
   running a fresh review instead.
2. Read it in full, then ask whether the user wants to: dig deeper into one
   specific still-open (`- [ ]`) finding (→ re-enter this skill in
   single-dimension deep-dive mode, scoped to that finding), or file one or
   more of its `Candidate follow-ups` now (→ hand off to `bug-report`/
   `decision-record` per §6, one at a time, each with its own confirmation).
3. When a deep-dive on a previously-flagged finding turns up that it's
   unchanged, worse, or resolved since the earlier report, say so explicitly
   and cite the earlier report's own numbers (e.g. "still 9 consumers,
   unchanged since the {date} report") rather than silently re-deriving the
   count as if this were the first look.

---

## Guardrails

- **Report-only — never edit application code.** This skill assesses and
  writes a report; it never refactors a service, moves a file, or changes
  state-management code itself. A finding becomes an actual code change only
  through a separate, explicit follow-up (most often `bug-fix`).
- **Never auto-file a finding.** §6 lists candidates and asks; it never invokes
  `bug-report`/`decision-record` on its own initiative mid-run.
- **Ground every claim in current, real code — never assess from documentation
  prose alone.** A docs/code mismatch is its own (separate) finding for
  `web-docs-sync`, not evidence for an architecture finding.
- **Scope is the frontend.** The Spring backend, Docker Compose and Kubernetes
  layout are out of scope; mention a backend-contract concern only when it
  shows up as a frontend coupling problem.
- **Don't duplicate `code-reviewer`'s job.** Per-file naming, signals usage
  within one component, and other per-diff/per-PR conventions stay
  `code-reviewer`'s territory; this skill looks at the system's overall shape.
- **Don't duplicate `quality-metrics`' job.** No numeric sweep, no trend table
  across dated reports — this skill's output is qualitative findings.
- **Don't reach for a state-management library by default.** The default fix
  for a dimension-5 coordination finding is a small, honestly-named
  service/stream split (a typed-channel broadcast service, or giving each
  feature's own service its own state) — recommend NgRx/Akita/etc. only if the
  evidence genuinely shows a complex derived-state graph or a
  time-travel/undo/cross-tab-sync need.
- **Never commit.** Write the dated report to the gitignored
  `JPPhotoManagerWeb/docs/reports/architecture-review/` folder only; no git
  command runs as part of this skill.
- Every 🟡/🔴 finding needs `file:line` evidence, and a coupling finding needs
  its consumer count — a claim without a grep behind it doesn't belong in the
  report.
