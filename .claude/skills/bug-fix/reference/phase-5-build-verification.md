# Bug Fix — Phase 5 — Build Verification

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 5 — Build Verification (Subagent 5, optional)

Skip this phase — recording `BUILD: SKIPPED — no build step` — when the project
has no build, compile or type-check step (find out yourself from its
`CLAUDE.md`, `README` and build manifest; if unsure, ask once with "skip" as an
option). This skill never deploys anything; the build runs locally only so a
failure is caught before CI or the host would.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the command for `<BUILD_COMMAND>`):

> **Step 1** — `<BUILD_COMMAND>` (allow up to 5 min; if it can take longer, poll
> with repeated foreground calls in this same turn — never end your response
> while it still runs in the background).
> **Step 2** — On failure: read the error (it names the file and cause), fix
> prioritising files related to the fix, re-run until it succeeds or you hit a
> blocker needing human input.
> **Step 3** — If the build produces an output directory or artifact, confirm
> it exists and is non-empty — empty or missing after a "successful" build is
> itself a failure.
>
> If you must modify a production source file (not a test or build
> configuration file), note it `PROD_CODE_FIXED: <file> — <reason>`.
>
> End with `BUILD: PASS` (after any `PROD_CODE_FIXED:` lines) or `BUILD: BLOCKED — <reason>`.

Do not start Phase 6 until `BUILD: PASS` (or the phase was skipped). On one or
more `PROD_CODE_FIXED:` lines, handle them as Phase 3's: ask the user to
**proceed** (record `UNREVIEWED_PROD_FIXES:` for the Final Summary) or to
**re-run the review gate** (Phase 2R over the fixes, then Phases 3 and 5 again).
