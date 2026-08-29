---
name: bug-fix
description: >
  Orchestrates the full lifecycle of fixing one bug from the backlog:
  selects the bug, creates a fix/ branch, reproduces the bug with a
  failing regression test FIRST, implements the fix until that test
  passes, runs code / database (conditional) / security (conditional)
  reviews with findings fixed, runs backend and frontend tests, runs
  e2e-testing's browser checks when the fix touches auth / the gallery /
  a migration, runs a local build, syncs docs if behaviour changed, then
  closes the bug via bugs-archive. No git commits at any point — hands off
  to the user for review and PR. TRIGGER when the user asks to fix a bug,
  work through the bug backlog one at a time, or address a specific
  BUG-NNN. This is the bug-family counterpart to feature-development.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Orchestrate one bug fix from selection to close, using dedicated subagents
per phase. This is a leaner cousin of `feature-development`: no SDD
artifacts, no `openspec-*`, no `spec-compliance-check` — but the same
branch discipline, the same review/test/build gates, and the same
never-commit rule. The one thing it adds that `feature-development`
doesn't have: **a failing regression test is written before the fix**, and
it's the durable artifact the fix leaves behind.

**Input**: Optional Bug ID (e.g. `BUG-004`), optionally followed by
`--skip-branch-setup` (e.g. `BUG-004 --skip-branch-setup`). The Bug ID, if
given, is passed to `bugs-next` to skip recommendation and jump straight
to confirmation. `--skip-branch-setup` is for orchestrators — the
`bugs-batch-fix` skill — that have already checked out a shared branch and
run multiple fixes on it without committing between them; omit it for
normal single-bug use.

---

## Overview

Seven phases: Phase 0 runs in the orchestrator's own context (never a
subagent — the confirmation must reliably reach the user); Phases 1–6 each
run as a `run_in_background: false` subagent, each gated on the prior
phase's signal.

| Phase | Skills / actions |
| ----- | ---------------- |
| 0 — Select & Confirm *(orchestrator's own context)* | `bugs-next` (recommend + mandatory `AskUserQuestion`) |
| 1 — Branch & Reproduce | `git fetch --prune` → `gitflow` (start/resume/sync `fix/<slug>` from `develop`) → mark bug `🔶 In Progress` → write a **failing** regression test that reproduces the bug |
| 2 — Fix & Review | implement the fix until the regression test passes → `code-reviewer` + `database-reviewer` (conditional) + `security-reviewer` (conditional), findings fixed |
| 3 — Tests | `cd JPPhotoManagerWeb/backend && mvn test` and `cd JPPhotoManagerWeb/frontend && npm test` in parallel until both pass, unconditionally |
| 4 — E2E verification *(conditional)* | `e2e-testing` browser checks when the fix touches auth / the gallery / a Flyway migration |
| 5 — Build verification | `cd JPPhotoManagerWeb/backend && mvn -q -DskipTests package` and `cd JPPhotoManagerWeb/frontend && npm run build` |
| 6 — Close | `web-docs-sync` (scoped, best-effort, only if behaviour changed) → `bugs-archive <BUG-ID> fixed "<resolution>"` |

---

## Phase 0 — Select & Confirm (orchestrator's own context — never a subagent)

**Never delegate this to a spawned subagent**, including under an "Auto
Mode" or similar autonomous-operation instruction. Which bug gets fixed is
the user's decision; a spawned/backgrounded subagent can't reliably
surface `AskUserQuestion`.

1. Use the Skill tool to invoke `bugs-next` directly (not wrapped in an
   Agent call), passing the Bug ID given to this skill, if any. Let it
   present its recommendation and ask for confirmation in this same
   context.
2. If `bugs-next` returns without a `BUG_ID:` line (user cancelled, or it
   couldn't obtain confirmation), stop the whole workflow here and tell
   the user. Never proceed to Phase 1 without a confirmed Bug ID.
3. Otherwise capture the confirmed Bug ID from the `BUG_ID:` line. This is
   `<bug-id>` in every phase below. Read that bug's row and Details block
   from `JPPhotoManagerWeb/docs/backlog/bugs-open.md` now, in the
   orchestrator context, so the repro steps / expected / actual can be
   pasted verbatim into Subagent 1's prompt (a subagent shouldn't have to
   re-hunt for them).

**Derive `<slug>`** for the branch: `<bug-id lowercased>-<2–4 kebab words
from the bug title>`, e.g. `bug-004-gallery-thumbnail-404`. The branch
will be `fix/<slug>`.

---

## Placeholder substitution (Phases 1–6)

Before spawning any subagent, replace every `<bug-id>`, `<slug>`, and
`<repro-details>` (the pasted Steps / Expected / Actual / Notes from Phase
0) in its prompt with the actual values. From Phase 2 onward, also
substitute `<repro-test-path>` with the path captured from Phase 1's
`REPRO: DONE — test: <path>` line.

---

## Phase 1 — Branch & Reproduce (Subagent 1)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug to fix has already been selected and confirmed with the user:
> `<bug-id>`. Do not invoke `bugs-next` or re-select a bug. Its recorded
> repro:
>
> <repro-details>
>
> **Step 1 — Set up the fix branch**
>
> **Batch mode (`--skip-branch-setup` was passed):** run `git branch
> --show-current` once; if it is `main` or `develop`, end with
> `FIX_BLOCKED — batch mode requires an existing fix branch already
> checked out` and stop. Otherwise proceed to Step 2 on the current
> branch. Do not check for uncommitted changes, and do not create, switch,
> or sync any branch — the caller owns branch state. Then go to Step 1.5.
>
> **Normal mode (no flag):** check resume status *before* checking for
> uncommitted changes (a resumed run is typically already on `fix/<slug>`
> with the in-progress fix sitting there uncommitted — that's expected,
> not a blocker).
>
> 0. `git fetch origin --prune`.
> 1. `git branch --show-current`.
>    - If it is already exactly `fix/<slug>`: this is a resume. Run
>      `git rev-list HEAD..origin/fix/<slug> --count` (treat a missing
>      `origin/fix/<slug>` as `0`). If `0`, skip to Step 1.5. If `>0`, end
>      with `FIX_BLOCKED — origin/fix/<slug> has <N> commit(s) not present
>      locally; reconcile manually` and stop.
>    - Otherwise continue to step 2.
> 2. `git status --porcelain`. If it prints anything, end with
>    `FIX_BLOCKED — uncommitted changes present, cannot create fix branch`
>    and stop.
> 3. `git rev-parse --verify --quiet fix/<slug>`:
>    - Prints a hash → the branch exists locally but isn't checked out:
>      `git checkout fix/<slug>`, then if `origin/fix/<slug>` exists
>      `git merge origin/fix/<slug>` (already-pushed history of this same
>      fix — exempt from the no-commits rule, like a sync merge; if it
>      doesn't merge cleanly, end with `FIX_BLOCKED — fix branch diverged
>      from origin, needs manual resolution` and stop). Then invoke
>      `gitflow` with "sync fix `<slug>`" to bring it up to date with
>      `develop`. On an unresolvable conflict, end with `FIX_BLOCKED — fix
>      branch sync conflict with develop` and stop.
>    - Fails → invoke `gitflow` with "start fix `<slug>`" (branches from
>      `develop`). If it reports a blocker, end with `FIX_BLOCKED — <the
>      gitflow blocker>` and stop.
> 4. `git branch --show-current` must now print exactly `fix/<slug>`. If
>    not, end with `FIX_BLOCKED — could not switch to fix branch` and stop.
>
> **Step 1.5 — Mark the bug In Progress**
> Open `JPPhotoManagerWeb/docs/backlog/bugs-open.md`, find the `<bug-id>`
> row, and if its **Status** is `⬜ Open` change it to `🔶 In Progress` and
> save. Leave it unchanged if it's already `🔶 In Progress`. If there's no
> matching row (bug tracked ad hoc), skip silently — not an error.
>
> **Step 2 — Reproduce the bug with a FAILING regression test**
> Write the smallest test that fails *because of this bug* and will pass
> once it's fixed. Pick the layer the bug actually lives at:
> - a backend logic / controller / service bug → a JUnit (or
>   `@SpringBootTest` / `@WebMvcTest`) test under
>   `JPPhotoManagerWeb/backend/src/test/` (see `java-unit-test-developer`);
> - a JPA query / entity / schema bug → a `@DataJpaTest` or a repository
>   test, or a migration test against a Flyway-migrated schema (see
>   `database-reviewer` for the migration conventions);
> - a UI / component bug → a Cypress component test or assertion,
>   extending the relevant existing `*.cy.ts` spec (preferred over a new
>   file — see `cypress-unit-test-developer`);
> - a bug only visible through a full page flow → a Cypress e2e spec (see
>   `e2e-suite`).
>
> Run just that test and confirm it **fails**, and that the failure is the
> bug described in `<repro-details>` — not an unrelated error, a typo in
> the test, or a missing fixture. Quote the failing assertion in your
> hand-back.
>
> **If you cannot make it fail** — the behaviour looks correct, or the
> recorded steps are too vague to reproduce — do not invent a fix. End
> with `FIX_BLOCKED — cannot reproduce <bug-id>: <what you tried and what
> you observed>` and stop. Do not change the bug's Status beyond the
> `🔶 In Progress` set in Step 1.5.
>
> **Step 3 — Hand back**
> End with exactly:
> `REPRO: DONE — test: <path to the test file>`
> `BUG_ID: <bug-id>`

After the subagent completes:
- Contains `FIX_BLOCKED — cannot reproduce`: surface it to the user and
  ask (**AskUserQuestion**, in the orchestrator's own context): "Mark
  `<bug-id>` as ❓ Cannot reproduce?" — options: "Yes — mark it Cannot
  reproduce" (invoke `bugs-archive <bug-id> cannot-repro "<subagent's
  note>"` and stop the workflow), "Keep it open — I'll add repro detail"
  (stop the workflow, leave the row `🔶 In Progress`), "Cancel". Do not
  proceed to Phase 2.
- Contains any other `FIX_BLOCKED`: surface the reason and stop.
- Otherwise: capture the `REPRO: DONE — test: <path>` line and proceed.

---

## Phase 2 — Fix & Review (Subagent 2)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug `<bug-id>` has a failing regression test at `<repro-test-path>`.
> Recorded repro:
>
> <repro-details>
>
> **Step 1 — Implement the fix**
> Fix the root cause in the source under `JPPhotoManagerWeb/backend/` or
> `JPPhotoManagerWeb/frontend/` (or a **new** Flyway migration under
> `JPPhotoManagerWeb/backend/src/main/resources/db/migration/` for a
> schema bug — never edit an already-applied migration file; see
> `database-reviewer` §5). Prefer the narrowest change that addresses the
> cause, not the symptom. Then run the regression test at
> `<repro-test-path>` and confirm it now **passes**. If you hit a blocker
> needing user input, end with `FIX_BLOCKED — <reason>` and stop.
>
> Never materialize a generated secret's raw value anywhere in a response,
> report, or file (same rule as `feature-development` Phase 2) — leave any
> task that would require it unchecked with a note naming the manual
> command.
>
> **Determining `CHANGED_FILES`** — run `git status --porcelain` and parse
> every path, including untracked (`??`) entries; recompute it fresh
> immediately before every review step below, never cache one snapshot
> (Step 2's fix rounds keep changing the tree). Do not use `git diff
> --name-only HEAD` (misses new files) or a branch-to-branch diff (nothing
> is committed, so it shows zero).
>
> **Step 2 — Code review**
> (recompute `CHANGED_FILES`) Invoke `code-reviewer` via the Skill tool
> using its **Review** workflow, passing `CHANGED_FILES` as the explicit
> scope — a scoped review of one change, not a full-codebase sweep, and
> not the interactive Fix Workflow. It writes one dated
> `JPPhotoManagerWeb/docs/reports/code-review/CODE_REVIEW_FINDINGS_*.md`.
> Then:
> - No 🔴 Critical / 🟡 Warning findings → proceed.
> - Any → fix each in source, check it off in the report (`- [ ]` →
>   `- [x]` with a `**Fixed:**` note), then (recompute `CHANGED_FILES`)
>   re-invoke `code-reviewer` to confirm nothing new. Repeat until clean.
>   After 3 rounds still not clean → end with `REVIEW_BLOCKED — repeated
>   review cycles` and stop. A finding you can't fix without human input →
>   `REVIEW_BLOCKED — <reason>` and stop.
>
> **Step 3 — Database review (conditional)**
> (recompute `CHANGED_FILES`) If any path is under
> `JPPhotoManagerWeb/backend/src/main/resources/db/migration/`, or touches
> a JPA entity or a repository `@Query`: invoke `database-reviewer`
> (Review workflow, scoped, non-interactive) — dated report at
> `JPPhotoManagerWeb/docs/reports/database-review/DATABASE_REVIEW_FINDINGS_*.md`.
> Fix every 🔴/🟡 finding (a fix to an already-applied migration is a
> **new** `V{n}__*.sql` file, never an edit), check each off with a
> `**Fixed:**` note, re-invoke to confirm clean, 3-round cap →
> `DATABASE_BLOCKED — …`. Then (recompute `CHANGED_FILES`) re-invoke
> `code-reviewer` once more over the database fixes.
>
> **Step 4 — Security review (conditional)**
> (recompute `CHANGED_FILES`) If any path touches auth
> (`core/guards/auth.guard.ts`, `core/interceptors/auth.interceptor.ts`,
> the login component, or backend `SecurityConfig.java` /
> `@PreAuthorize`), a Flyway migration, user-input handling, or a
> dependency manifest (`backend/pom.xml`, `frontend/package.json`): invoke
> `security-reviewer` (Review workflow, scoped, non-interactive) — dated
> report at
> `JPPhotoManagerWeb/docs/reports/security-review/SECURITY_REVIEW_FINDINGS_*.md`.
> Same fix / re-check / 3-round-cap loop → `SECURITY_BLOCKED — …`. Then
> re-invoke `code-reviewer` once more over the security fixes.
>
> Do not proceed until all Critical and Warning findings (including
> follow-on code-review findings from Steps 3–4) are resolved.
>
> **Step 5 — Signal completion**
> End with exactly:
> `FIX_IMPLEMENTED: DONE`

Do not start Phase 3 until this subagent returns `FIX_IMPLEMENTED: DONE`.
On any `*_BLOCKED`, surface the details and wait for user guidance.

---

## Phase 3 — Backend & Frontend Tests (Subagents 3 & 4, parallel)

Runs unconditionally, regardless of what the fix touched. Spawn **two
general-purpose subagents in a single message** (both Agent tool calls in
the same response), both `run_in_background: false`.

**Subagent 3 — Backend tests:**

> Validate the fix for `<bug-id>`. Only modify files under
> `JPPhotoManagerWeb/backend/`.
> 1. `cd JPPhotoManagerWeb/backend && mvn test`.
> 2. On failure: diagnose (compilation error, assertion mismatch, missing
>    mock setup), fix — preferring test code over production source; if
>    you must touch production source note it `PROD_CODE_FIXED: <file> —
>    <reason>` — re-run until green.
> End with `BACKEND_TESTS: PASS (<n> tests)` or `BACKEND_TESTS: BLOCKED —
> <reason>`.

**Subagent 4 — Frontend tests:**

> Validate the fix for `<bug-id>`. Only modify files under
> `JPPhotoManagerWeb/frontend/`.
> 1. `cd JPPhotoManagerWeb/frontend && npm test`.
> 2. On failure: diagnose (type error, missing stub, assertion mismatch,
>    missing `provideNoopAnimations()` — see `cypress-unit-test-developer`),
>    fix — preferring test code over production source (same
>    `PROD_CODE_FIXED:` rule) — re-run until green.
> End with `FRONTEND_TESTS: PASS (<n> tests)` or `FRONTEND_TESTS: BLOCKED
> — <reason>`.

Wait for **both**. On any `BLOCKED`, stop and surface the failure. On one
or more `PROD_CODE_FIXED:` lines, surface them to the user (those
production changes weren't covered by Phase 2's review) and ask:
**Proceed** (record `UNREVIEWED_PROD_FIXES:` for the Final Summary) or
**Re-run Phase 2** (new Subagent 2 over the fixes, then new Subagents 3
& 4).

---

## Phase 4 — E2E Verification (Subagent 5, conditional)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> **Step 1 — Does E2E verification apply?**
> `git status --porcelain` → parse all paths (incl. `??`) into
> `CHANGED_FILES`. Check whether any falls under: authentication
> (`core/guards/auth.guard.ts`, `core/interceptors/auth.interceptor.ts`,
> the login component, backend `SecurityConfig.java`), the gallery
> (`features/gallery/`, the app's primary photo-browsing flow), or a
> Flyway migration under `db/migration/`. If none, end with `E2E: SKIPPED
> — no auth/gallery/migration changes` and stop.
>
> **Step 2 — Prerequisites** — follow `e2e-testing` §1 (PostgreSQL,
> MongoDB, Redis reachable; real data present). On failure → `E2E_BLOCKED
> — <reason>` and stop.
>
> **Step 3 — Start backend + frontend** — per `e2e-testing` §2. On
> failure → `E2E_BLOCKED — services did not start`.
>
> **Step 4 — Browser checks** — sign in with `e2e-testing`'s standard
> local test account. Confirm the app shell renders, no console errors.
> If the fix touched the gallery, exercise the specific browse/view/edit
> flow the bug was about, once, through the real UI — confirm the bug is
> actually gone here, not just in the unit test. If the fix touched auth
> or a migration, run `e2e-testing`'s auth / RLS-equivalent scoping checks.
> Screenshot the final state and read it back for layout breaks.
>
> **Step 5 — Tear down** — per `e2e-testing` §6.
>
> End with one of:
> - `E2E: PASS — <one-line summary>`
> - `E2E: PASS_PARTIAL — <what passed> — NOT COVERED: <what was skipped and why>`
> - `E2E_BLOCKED — <reason>`

Do not start Phase 5 until `E2E: PASS`, `PASS_PARTIAL`, or `SKIPPED`. On
`E2E_BLOCKED`, surface and wait — a failure against a live local stack may
be environment/data, not a code defect; don't assume it belongs back in
Phase 2 without the user confirming.

---

## Phase 5 — Build Verification (Subagent 6)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> **Step 1** — `cd JPPhotoManagerWeb/backend && mvn -q -DskipTests
> package` (allow up to 10 min) and `cd JPPhotoManagerWeb/frontend && npm
> run build` (allow up to 5 min).
> **Step 2** — On failure: read the compiler / esbuild error (it names the
> file and cause), fix under the relevant module prioritising files
> related to the fix, re-run until it succeeds or you hit a blocker
> needing human input.
> **Step 3** — Confirm each build produced its expected artifact (backend
> `target/*.jar`, frontend `dist/`), non-empty.
>
> End with `BUILD: PASS` or `BUILD: BLOCKED — <reason>`.

Do not start Phase 6 until `BUILD: PASS`. This phase never deploys — it
only confirms the fix compiles and packages.

---

## Phase 6 — Close (Subagent 7)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> **Step 1** — Invoke `web-docs-sync` in its scoped-sync mode for the
> current branch's changes (it diffs against the branch base itself).
> **Best-effort, not blocking**: a bug fix usually changes no documented
> behaviour, so "nothing to sync" is the common and fine outcome. Only
> stop and surface details if the skill errors out leaving files broken.
>
> **Step 2** — Invoke `bugs-archive <bug-id> fixed "<one-line resolution:
> what the root cause was and what changed; include the regression test
> path>"`. Wait for it to move the row to
> `JPPhotoManagerWeb/docs/backlog/bugs-fixed.md`.
>
> End with exactly:
> `CLOSE: DONE`

Do not display the Final Summary until this subagent returns `CLOSE:
DONE`.

---

## Final Summary

```
## Bug Fix Complete

**Bug:** <bug-id> — <title>
**Regression test:** <path> (failed before the fix, passes now)
**Root cause / fix:** <one or two sentences>
**Code review:** ✓ All findings resolved
**Database review:** ✓ All findings resolved (or N/A)
**Security review:** ✓ All findings resolved (or N/A)
**Backend tests:** ✓ passing
**Frontend tests:** ✓ passing
[if UNREVIEWED_PROD_FIXES was recorded in Phase 3:]
**⚠ Unreviewed production fixes:** <the PROD_CODE_FIXED lines> — fixed while chasing test failures in Phase 3; the user chose to proceed without routing them back through Phase 2.
**E2E verification:** ✓ <Phase 4 summary> (or N/A, or ⚠ Partial: <what wasn't covered>)
**Build:** ✓ backend package + frontend build succeeded locally
**Docs sync:** ✓ <one-line summary, or "Nothing to sync">
**Bug:** ✓ Closed — moved to bugs-fixed.md

Nothing was committed. The fix (and its regression test) is on `fix/<slug>`, entirely uncommitted. Review with `git status` / `git diff`, then use `gitflow`'s finish-fix action to commit and open a PR.
```

---

## Guardrails

- Always capture the Bug ID confirmed in Phase 0 and propagate it to every
  later phase. Substitute `<bug-id>` / `<slug>` / `<repro-details>` /
  `<repro-test-path>` before spawning each subagent.
- **Cancellation**: if `bugs-next` returns without a `BUG_ID:` line, stop
  immediately, before Subagent 1 is spawned.
- **The regression test is written before the fix, in Phase 1, and must
  fail for the right reason before Phase 2 starts.** A fix with no failing
  test to justify it is not this skill's workflow — if Phase 1 can't
  reproduce the bug, that's a `FIX_BLOCKED — cannot reproduce`, resolved
  through the AskUserQuestion decision point, never worked around by
  skipping straight to a speculative fix.
- Do not start a phase until the prior phase's subagent(s) return their
  expected signal. On any `*_BLOCKED`, surface details and wait for user
  guidance — never retry automatically.
- **Missing signal fallback**: a subagent that returns without its
  expected signal is treated as `BLOCKED`; surface its raw response and
  wait.
- **Phase 3 always runs**, on every invocation — never conditional on what
  the fix touched, unlike Phase 4.
- **`code-reviewer` / `database-reviewer` / `security-reviewer` always use
  their Review workflow, scoped to `CHANGED_FILES`, never the interactive
  Fix Workflow and never a full-codebase sweep.** `database-reviewer`
  only runs when a `db/migration/**` file, a JPA entity, or a repository
  `@Query` changed; a fix to an already-applied migration is a **new**
  `V{n}__*.sql`, never an edit.
- **No git commits at any point.** Neither this skill nor any subagent may
  run `git commit` / `git push` / any git write. The sole exceptions are
  Phase 1 Step 1's branch creation and sync (`git fetch`, `git checkout`,
  `git checkout -b`, `git merge origin/develop` via `gitflow`'s
  start-fix/sync-fix, and `git merge origin/fix/<slug>` to pick up
  another device's already-pushed commits on this same fix).
- **The fix branch is synced from `develop` only, never `main`** — via
  `gitflow`'s sync-fix action, which merges `origin/develop` and nothing
  else. This holds even right after a release merges into both.
- **`--skip-branch-setup` (batch mode) only affects Phase 1 Step 1.** No
  other phase changes — Phases 2–6 already operate on "whatever branch is
  checked out". This flag is only ever passed by `bugs-batch-fix`.
- **Phase 5 never deploys** — it runs `mvn package` / `npm run build` as a
  local compile check only; it never invokes `build-and-deploy-k8s.sh`,
  Docker Compose, or `kubectl`.
- **Auto Mode never overrides a required confirmation** — Phase 0's bug
  selection, the cannot-reproduce decision point, Phase 3's
  `PROD_CODE_FIXED` choice, and any post-`*_BLOCKED` guidance prompt all
  still stop and wait for a real answer.
- **Never let a subagent print a generated secret's raw value** anywhere
  in a response, report, or file (same standard as `feature-development`).
