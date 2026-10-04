# Feature Development — Phase 3 — Test

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 3 — Test (Subagents 3 & 4 — launch in parallel)

Spawn **two general-purpose subagents in a single message** (both Agent tool
calls in the same response) so they run in parallel. Both calls must set
`run_in_background: false` — Phase 4 cannot start until both report `PASS`
or `BLOCKED` (see the foreground guardrail in `../SKILL.md`), and `false` still runs
them concurrently when issued together in one message; it only means this
skill waits for both results before continuing rather than defaulting to
background execution.

### Subagent 3 — Backend tests

Prompt:

> Run backend unit tests for the JPPhotoManager web application.
> Change being validated: `<change-name>` (focus failure diagnosis here first).
> Only modify files under `JPPhotoManagerWeb/backend/` — do not touch frontend files.
>
> 1. Run (using the Bash tool): `cd JPPhotoManagerWeb/backend && mvn test`
> 2. If all tests pass, report success with the total test count.
> 3. If any tests fail:
>    a. Read the failure output carefully.
>    b. Identify the root cause (compilation error, assertion mismatch, missing
>    mock setup, etc.), checking files related to `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/backend/`. Prefer fixing
>    test code over production source code. If you must modify a production
>    source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/backend && mvn test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>    without human input.
>
> End your response with one of:
>
> - `BACKEND_TESTS: PASS (<n> tests)` if all tests pass.
> - `BACKEND_TESTS: BLOCKED — <brief reason>` if you encountered a failure
>   you cannot resolve.

### Subagent 4 — Frontend tests

Prompt:

> Run frontend component tests for the JPPhotoManager web application.
> Change being validated: `<change-name>` (focus failure diagnosis here first).
> Only modify files under `JPPhotoManagerWeb/frontend/` — do not touch backend files.
>
> 1. Run (using the Bash tool): `cd JPPhotoManagerWeb/frontend && npm test`
> 2. If all tests pass, report success with the total test count.
> 3. If any tests fail:
>    a. Read the failure output carefully.
>    b. Identify the root cause (type error, missing stub, assertion mismatch,
>    missing `provideNoopAnimations()`, etc.), checking files related to
>    `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/frontend/`. Prefer fixing
>    test code over production source code. If you must modify a production
>    source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/frontend && npm test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>    without human input.
>
> End your response with one of:
>
> - `FRONTEND_TESTS: PASS (<n> tests)` if all tests pass.
> - `FRONTEND_TESTS: BLOCKED — <brief reason>` if you encountered a failure
>   you cannot resolve.

Wait for **both** subagents to complete before proceeding to Phase 4.

If either subagent reports `BLOCKED`, stop the workflow and surface the failure
details to the user before continuing. Do not proceed to Phase 4 until both
subagents report `PASS`.

If either subagent's response contains one or more `PROD_CODE_FIXED:` lines,
surface them to the user with a note that those production source changes were
not covered by Phase 2's review. Ask the user whether to:

- **Proceed** — continue to Phase 4 without additional review. If chosen,
  record this (e.g. `UNREVIEWED_PROD_FIXES: <the PROD_CODE_FIXED lines>`) —
  the Final Summary in `../SKILL.md` must surface it, since those files shipped without
  going through code-review.
- **Re-run Phase 2** — spawn a new Subagent 2 with the same prompt to review
  and code-review the production fixes, then re-run Phase 3 (spawn new
  Subagents 3 and 4) to confirm all tests still pass before continuing to
  Phase 4. If this path is taken, there is nothing left to caveat in the
  Final Summary — the fixes did go through review.

---
