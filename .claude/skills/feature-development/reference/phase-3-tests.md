# Feature Development — Phase 3 — Tests

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 3 — Tests (Subagent 3, optional)

**This phase is optional: skip it when the project has no automated tests.**
Before spawning anything, find out yourself (orchestrator, not a subagent)
what the project has: read its `CLAUDE.md`, `README` and build manifest
(`package.json` scripts, `Makefile`, `pyproject.toml`, `pom.xml`,
`Cargo.toml`, …) for the command that runs its tests — and for a faster
end-to-end "smoke" tier, if CI runs one. If a test command exists but you
cannot tell what it is, ask the user once (**AskUserQuestion**, in your own
context; offer "skip this phase" as an option). If the project has no tests,
record `TESTS: SKIPPED — no automated tests` for the Final Summary and go to
Phase 4.

This phase runs the project's local test suites — the ones that actually
determine whether the PR's CI goes green. A tier CI runs on every PR
unconditionally must be run here unconditionally too: do not skip it based on
`<change-name>`'s scope, the way Phase 4 skips based on touched areas — a
change that looks entirely unrelated (a shared component's translation
string, a relocated file) can still break a test that asserts against it.

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 4 cannot start until this subagent
reports `PASS` or `BLOCKED` — see the foreground guardrail in `../SKILL.md`), with the
following prompt (substitute the commands you found for `<TEST_COMMAND>` and
`<SMOKE_COMMAND>`; delete Step 2 if the project has no smoke tier):

> Run the project's tests — fast, local checks that together cover what CI
> actually gates this PR on.
> Change being validated: `<change-name>` (focus failure diagnosis here first).
>
> **Step 1 — Tests**
> 1. Run (using the Bash tool): `<TEST_COMMAND>`
> 2. If all tests pass, continue to Step 2.
> 3. If any tests fail:
>    a. Read the failure output carefully.
>    b. Identify the root cause (type error, missing stub, assertion
>    mismatch, etc.), checking files related to `<change-name>` first.
>    c. Fix the affected files. Prefer fixing test code over production
>    source code. If you must modify a production source file, note it with
>    `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `<TEST_COMMAND>`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>    without human input, then continue to Step 2.
>
> **Step 2 — Smoke tier (only if the project has one)**
> This is a distinct suite from Step 1 (for example end-to-end specs run
> against a dev server with the backend stubbed). Run it even if
> `<change-name>` looks unrelated to any spec in it; that's exactly the case
> most likely to be missed otherwise.
> 1. Run (using the Bash tool, allow up to 5 minutes): `<SMOKE_COMMAND>`
> 2. If all specs pass, report success with the total spec/test count.
> 3. If any spec fails:
>    a. Read the failure output carefully.
>    b. **Most failures here are a stale assertion in the spec itself** — a
>    hardcoded fixture value or expected UI string that no longer matches
>    what the component actually renders — rather than a genuine production
>    regression. Before changing any production file, check the actual
>    rendered output against the spec's assertion.
>    c. Fix the affected file(s), preferring the spec over production source,
>    mirroring Step 1's rule — if you must modify a production source file,
>    note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `<SMOKE_COMMAND>`.
>    e. Repeat until all specs pass or you reach a failure you cannot fix
>    without human input.
>
> End your response with one of:
>
> - `TESTS: PASS (<n> tests, <m> smoke tests)` if both steps pass (omit the
>   smoke count if there is no smoke tier).
> - `TESTS: BLOCKED — <brief reason, noting which tier>` if you encountered
>   a failure in either tier you cannot resolve.

If the subagent reports `BLOCKED`, stop the workflow and surface the
failure details to the user before continuing. Do not proceed to Phase 4
until it reports `PASS` (or the phase was skipped).

If the subagent's response contains one or more `PROD_CODE_FIXED:` lines,
surface them to the user with a note that those production source changes
were not covered by Phase 2's review. Ask the user whether to:

- **Proceed** — continue to Phase 4 without additional review. If chosen,
  record this (e.g. `UNREVIEWED_PROD_FIXES: <the PROD_CODE_FIXED lines>`) —
  the Final Summary in `../SKILL.md` must surface it.
- **Re-run Phase 2** — spawn a new Subagent 2 with the same prompt to review
  and code-review the production fixes, then re-run Phase 3 (spawn a new
  Subagent 3) to confirm all tests still pass before continuing to Phase 4.
  If this path is taken, there is nothing left to caveat in the Final
  Summary.

---
