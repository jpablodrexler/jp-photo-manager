# Bug Fix — Phase 2 — Fix & Review gate

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 2 — Fix (Subagent 2) and Phase 2R — Review gate

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug `<bug-id>` has a failing regression test at `<repro-test-path>`.
> Recorded repro:
>
> <repro-details>
>
> **Step 1 — Implement the fix**
> Fix the root cause in the source. Prefer the
> narrowest change that addresses the cause, not the symptom. Then run the
> regression test at `<repro-test-path>` and confirm it now **passes**. If
> you hit a blocker needing user input, end with `FIX_BLOCKED — <reason>`
> and stop.
>
> Never materialize a generated secret's raw value anywhere in a response,
> report, or file (same rule as `feature-development` Phase 2) — leave any
> task that would require it unchecked with a note naming the manual
> command.
>
> **Step 2 — Signal completion**
> List what you changed in plain words (no verdict on whether any review applies; reviews are run by the orchestrator's review gate, each in its own subagent — do not run `code-reviewer`, `database-reviewer`, `security-reviewer` or the viewport check yourself). End with exactly:
> `FIX_IMPLEMENTED: DONE`

Do not start Phase 2R until this subagent returns `FIX_IMPLEMENTED: DONE`.

### Phase 2R — Review gate (orchestrator)

Read `reference/review-gate.md` in full and run it, with `<work-id>` = `<bug-id>`: the orchestrator computes `CHANGED_FILES` itself, spawns one subagent each for the code review, the database review, the security review and the viewport check (deciding itself, from `git status --porcelain`, which of the last three apply), verifies every report file and its checkboxes before accepting a signal, and ends with `REVIEW_GATE: DONE`. Its ledger lines feed the Final Summary.

Do not start Phase 3 until the gate returns `REVIEW_GATE: DONE`.
