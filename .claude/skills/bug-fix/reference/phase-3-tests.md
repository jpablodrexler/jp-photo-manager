# Bug Fix — Phase 3 — Tests

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 3 — Tests (Subagent 3, optional)

Skip this phase when the project has no automated tests: find out yourself
(orchestrator) from its `CLAUDE.md`, `README` and build manifest which command
runs them — and a faster end-to-end smoke tier, if CI runs one. If you cannot
tell, ask the user once (**AskUserQuestion**, with "skip this phase" as an
option); if there are no tests, record `TESTS: SKIPPED — no automated tests`
and go to Phase 4.

Runs unconditionally otherwise, regardless of what the fix touched — a tier CI
gates every PR on can still be broken by a fix that looks unrelated.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the commands you found; delete Step 2 if there is no smoke tier):

> Validate the fix for `<bug-id>`. Both suites are local-only.
>
> **Step 1 — Tests**
> `<TEST_COMMAND>`. On failure: diagnose (type error, missing stub,
> assertion mismatch, …), fix — preferring test code over production source;
> if you must touch production source note it `PROD_CODE_FIXED: <file> —
> <reason>` — re-run until green.
>
> **Step 2 — Smoke tier**
> `<SMOKE_COMMAND>` (allow ~5 min). Most failures here are a stale assertion in
> the spec itself (a hardcoded string / fixture that no longer matches what the
> component renders), not a real regression — check the rendered output before
> changing any production file. Fix, preferring the spec over production source
> (same `PROD_CODE_FIXED:` rule), re-run until green.
>
> End with one of:
> - `TESTS: PASS (<n> tests, <m> smoke tests)`
> - `TESTS: BLOCKED — <reason, noting which tier>`

On `BLOCKED`, stop and surface the failure. On one or more
`PROD_CODE_FIXED:` lines, surface them to the user (those production
changes weren't covered by the Phase 2R review gate) and ask: **Proceed** (record
`UNREVIEWED_PROD_FIXES:` for the Final Summary) or **Re-run the review gate** (Phase 2R over the fixes, then new Subagent 3).
