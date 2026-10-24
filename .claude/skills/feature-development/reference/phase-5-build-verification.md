# Feature Development — Phase 5 — Build Verification

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 5 — Build Verification (Subagent 5, optional)

**This phase is optional: skip it when the project has no build, compile or
type-check step.** Find out yourself (orchestrator) from the project's
`CLAUDE.md`, `README` and build manifest which command that is; if there is
none, record `BUILD: SKIPPED — no build step` and go to Phase 6S. If a build
command probably exists but you cannot tell which, ask the user once
(**AskUserQuestion**, with "skip this phase" as an option).

This skill never deploys anything. What this phase *does* do is run the exact
build command the project's host or CI runs, locally first, so a build failure
is caught before it ever reaches an actual deployment.

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 6S cannot start until this subagent
completes — see the foreground guardrail in `../SKILL.md`), with the following
prompt (substitute the command for `<BUILD_COMMAND>`):

> **Step 1 — Run the build**
> Run (using the Bash tool, from the repo root): `<BUILD_COMMAND>`
> Allow up to 5 minutes before treating it as a failure. If it can take
> longer, poll for completion with repeated foreground calls in this same
> turn — never end your response while it still runs in the background.
>
> **Step 2 — Diagnose a failure**
> If the build fails:
> a. Read the build output — it names the specific file and error (a type
>    error, an unused import flagged by a strict compiler option, a
>    reference to something that doesn't exist).
> b. Fix the affected file(s), prioritizing files related to `<change-name>`.
>    If you must modify a production source file (not a test or a build
>    configuration file), note it with `PROD_CODE_FIXED: <filename> — <reason>`.
> c. Re-run the build command.
> d. Repeat until it succeeds or you reach a failure you cannot resolve
>    without human input.
>
> **Step 3 — Verify the output**
> If the build produces an output directory or artifact, confirm it exists and
> isn't empty. An empty or missing output after a reported-successful build is
> itself a failure — a host that serves it would fail on every page.
>
> End your response with one of:
>
> - `BUILD: PASS` if the build succeeded and the output is populated (list
>   any `PROD_CODE_FIXED:` lines before it).
> - `BUILD: BLOCKED — <brief reason>` if the build failed and you cannot
>   resolve it without human input.

Do not start Phase 6S until this subagent completes (or the phase was
skipped). If it reports `BUILD: BLOCKED`, surface the details to the user and
wait for guidance before continuing.

If the response contains `PROD_CODE_FIXED:` lines, handle them exactly like
Phase 3's (see `reference/phase-3-tests.md`): ask the user to **proceed**
(record `UNREVIEWED_PROD_FIXES`, which the Final Summary must surface) or to
**re-run the Phase 2R review gate** over the fixes and then re-run Phases 3
and 5.

---
