---
name: code-reviewer
description: >
  Code review skill for the JPPhotoManager project (Spring Boot 3.5 / Java 21
  backend + Angular 22 frontend). TRIGGER after implementing any feature, fix,
  or refactor — including after completing an OpenSpec task or a set of tasks.
  Do not wait to be asked: review code proactively after writing it. Also
  triggers when explicitly asked to review a pull request, file, or change.
  Covers both sub-projects (backend and frontend) and all cross-cutting
  concerns: hexagonal architecture layering, naming, transactions, async,
  testing, and TypeScript/Java style rules. A full-codebase sweep of the whole
  web application produces one report per architecture layer instead of a
  single consolidated report — see "Full-Codebase Sweeps" below. Also TRIGGERS
  when asked to fix, address, resolve, or work through findings from an
  existing dated CODE_REVIEW_FINDINGS report — see the "Fix Workflow" section (§17).
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
  scope: [JPPhotoManagerWeb]
---

# Code Reviewer Skill

Review code in the JPPhotoManager project against its documented architecture,
conventions, and known pitfalls. The project has two sub-projects with distinct
stacks; apply the relevant checklist(s) based on which files are under review.

This skill reviews a *change*. For the health of the system's overall shape —
layering, state-management coupling, data-access patterns — use
`architecture-reviewer`; for a non-technical review of the feature scope, use
`product-scope-roaster`.

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing new/changed code.
- **Fix** (§17, in `reference/report-format-and-fix-workflow.md`) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review code.

## Workflow

1. Identify the **scope**: a full-codebase sweep of the whole web application,
   or a scoped review (single file, PR, feature, or one sub-project).
   - **Scoped review** → follow steps 2–6 below as a single pass, producing
     one report (unchanged from prior behavior).
   - **Full-codebase sweep** → follow "Full-Codebase Sweeps: Review by Layer"
     instead. It runs steps 2–6 once per layer, each producing its own report.
2. Identify which sub-project(s) are affected: **backend** (Java), **frontend**
   (Angular/TypeScript), or both.
3. Read every changed file before forming any opinion.
4. Load the reference file(s) from the index at the end of this file that
   match the affected sub-project(s) and what changed, then work through
   their checklist sections one by one. Always finish by scanning the
   known-pitfalls table (§15).
5. Report findings grouped by **severity**, then by file.
6. Summarise with a short verdict and the top action items.
7. Write the full report to a new, dated markdown file — see
   "Review Report Format & Output File" (§16, in `reference/report-format-and-fix-workflow.md`). Do this on every run, not just
   full-codebase sweeps: a single-file or single-PR review still gets its own
   dated report, scoped to whatever was actually reviewed.

### Full-Codebase Sweeps: Review by Layer

When asked to review all the code in the web application (or the entire
backend, or the entire frontend), don't produce one giant consolidated
report and don't review it inline in the main conversation. Instead, split
the sweep into one pass per **architecture layer**, each run by its own
subagent and producing its own dated report file (see "Process" below). This
keeps each pass's context small (a single layer's files, isolated in its own
subagent, not the whole codebase piled into the orchestrating conversation),
makes the sweep resumable across sessions, and lets the user later fix one
layer's findings at a time (§17) instead of wading through everything at
once.

**The layers:**

| Layer key                | Report suffix          | Directory scope                                                                                                    | Primary checklist sections                                    |
| ------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| Backend domain           | `backend-domain`         | `backend/.../domain/**`                                                                                                                   | §1.1, §5, §8 (domain-model rows)                                 |
| Backend application      | `backend-application`    | `backend/.../application/usecase/**`                                                                                                      | §1.1, §1.2, §2, §4, §7, §8 (use-case rows)                       |
| Backend api               | `backend-api`             | `backend/.../infrastructure/web/**` (controllers, request/response DTOs)                                                                  | §1.1 (controller delegation), §3.4, §6 (DTO/entity drift), §8 (DTO rows) |
| Backend infrastructure   | `backend-infrastructure` | `backend/.../infrastructure/persistence/**`, `infrastructure/service/**`, `infrastructure/kafka/**`, `infrastructure/batch/**`, `infrastructure/config/**` | §1.2, §3.1–3.3, §5, §6, §7                                       |
| Frontend core             | `frontend-core`           | `frontend/src/app/core/**`                                                                                                                | §10.2, §11, §12                                                  |
| Frontend features         | `frontend-features`       | `frontend/src/app/features/**`                                                                                                            | §10.1, §10.3, §11, §12                                           |
| Frontend shared           | `frontend-shared`         | `frontend/src/app/shared/**`                                                                                                              | §1.3, §10.1, §11, §12                                            |
| Cross-cutting             | `cross-cutting`           | Both sub-projects; no single directory                                                                                                    | §9, §13, §14, §18, §19, §20, §21, §22, delegate-only port/adapter or service pairs (§1.2/§10.2), dependency-direction violations, systemic naming patterns |

If only the backend (or only the frontend) is in scope, skip the layers that
don't apply — e.g. a "review the whole backend" request produces 4 reports
(domain, application, api, infrastructure) plus a backend-scoped
cross-cutting report, not all 8.

**What goes in the cross-cutting report:** findings that don't belong to one
file's home layer — backend testing conventions (§9) and Cypress conventions
(§13), comment/code-style patterns (§14) that recur across many files, and
anything whose root cause spans two layers at once (e.g. a delegate-only
port/adapter pair, where the finding is really about the port *and* the
adapter *and* the callers, not just one of them). A naming or architecture
violation that's local to a single file still goes in that file's own layer
report, even if the rule itself is defined in a "cross-cutting" section like
§8/§12.

**Process — one subagent per layer:**

Each layer's review runs in its own subagent (`Agent` tool,
`subagent_type: general-purpose`), not inline in the main conversation. This
is what actually keeps the sweep within session limits: each subagent starts
cold, reads only its own layer's files, writes its own report, and never
touches the orchestrating conversation's context.

1. Before starting, check `docs/reports/code-review/` for layer report files already
   dated today. If a sweep was interrupted in an earlier session, resume by
   only dispatching subagents for the layers that don't have a report yet for
   today's date — don't redo layers already completed.
2. The remaining applicable layers are independent of each other (no layer's
   review depends on another's findings), so dispatch all of them together:
   one `Agent` call per layer, all issued in the **same message** so they run
   in parallel. Don't dispatch them one at a time and wait in between.
3. Each subagent's prompt must be self-contained — it has no memory of this
   conversation — and must include:
   - The layer key, its directory scope, and its primary checklist sections
     (copy the relevant row from the layer table above into the prompt).
   - An instruction to first read
     `.claude/skills/code-reviewer/SKILL.md` in the repo (the severity legend
     and the reference index), then the reference files the index lists for
     that layer's primary checklist sections, plus the "Review Report Format &
     Output File" section (§16, in `reference/report-format-and-fix-workflow.md`) — don't restate the checklist
     in the prompt, point the subagent at the files.
   - The exact output path to write:
     `docs/reports/code-review/CODE_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
     (apply the `-2`/`-3` collision rule from §16 itself if the file already
     exists).
   - An explicit instruction to only read/review files under that layer's own
     directory scope — not the whole repo — and to write the report file
     itself rather than just returning findings as chat text.
   - A short return-message instruction: report back the Critical/Warning/
     Suggestion counts and the path it wrote, not the full findings text —
     the orchestrating conversation only needs the summary, the report file
     is the full record.
4. These are launched as background agents by default — don't poll or sleep
   waiting for them; you'll be notified as each one completes.
5. Once every dispatched subagent has completed, give a short in-chat
   summary: the list of report files written, and the Critical/Warning/
   Suggestion count per layer (from each subagent's return message), so the
   user can see at a glance which layer needs attention first.

Severity levels used throughout:

| Level             | Meaning                                                                              |
| ----------------- | ------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Breaks correctness, data integrity, or security. Must be fixed before merging.       |
| 🟡 **WARNING**    | Violates a project standard or will cause maintainability problems. Should be fixed. |
| 🟢 **SUGGESTION** | Style preference or minor improvement. Fix if convenient.                            |

---

## Reference index

The checklist sections live under `reference/`, split so a review loads only
what the change touches instead of the whole checklist every time. **Section
numbers (§N) are stable** — other skills, reports and ADRs cite them — and each
one is in exactly one file below. Read the file(s) matching what changed, as
you reach that part of the review; for a full-codebase sweep, each layer's
subagent reads the files holding that layer's primary checklist sections (the
layer table above gives the §N, this index gives the file).

| Sections | Read when you're reviewing… | File |
| -------- | ---------------------------- | ---- |
| §1 Architecture & layering (backend and frontend) | new/moved files or layer boundaries on either side | `reference/architecture-layering.md` |
| §2-6 Backend: Spring proxy pitfalls, annotations & boilerplate, transactions, JPA entities, database migrations | a use case, controller, entity, repository, `@Transactional`/`@Async` boundary or Flyway migration | `reference/backend-spring.md` |
| §7-9 Backend: async & SSE, naming conventions, testing | `@Async`/SSE code, backend class naming, or a backend test | `reference/backend-async-naming-testing.md` |
| §10 Frontend: Angular conventions (including Material layout gotchas) | a component, template, route, guard, service or stylesheet under `frontend/` | `reference/frontend-angular.md` |
| §11-13 Frontend: TypeScript conventions, naming, Cypress tests | frontend TypeScript style, file/class naming, or a `*.cy.ts` file | `reference/frontend-typescript-naming-cypress.md` |
| §14-15 Cross-cutting comments & code style, known project-specific pitfalls | **every review** — scan the pitfalls table as the final pass before reporting | `reference/crosscutting-and-pitfalls.md` |
| §16-17 Review report format & output file, fix workflow | writing the dated report (every run), or fixing findings from an existing report | `reference/report-format-and-fix-workflow.md` |
| §18-19 Cyclomatic complexity, code coverage (both sub-projects) | a metrics sweep, complexity thresholds, or coverage thresholds | `reference/metrics-complexity-coverage.md` |
| §20-23 Type coverage, dead code, performance & accessibility (Lighthouse), deep accessibility audit (cypress-axe) | a metrics sweep touching `any` usage, unused code, route performance or accessibility | `reference/metrics-types-deadcode-lighthouse-a11y.md` |
| §24-25 Mutation testing, secrets scanning, license compliance, dependency vulnerabilities | a metrics sweep, or a change touching dependencies or committed secrets | `reference/metrics-mutation-security.md` |
