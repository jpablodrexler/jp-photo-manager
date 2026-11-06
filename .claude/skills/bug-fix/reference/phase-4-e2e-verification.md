# Bug Fix — Phase 4 — E2E Verification

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 4 — E2E Verification (Subagent 4, optional, conditional)

Skip this phase — recording `E2E: SKIPPED — <reason>` — when the project has no
end-to-end or browser suite (its `CLAUDE.md`, `README` and build manifest say),
or when the fix touches no user-facing flow.

**Before spawning — resolve the test account** (orchestrator step; needs
`AskUserQuestion`):

1. Run `git status --porcelain` yourself. If no changed path falls under a
   user-facing flow the suite covers (screens, routes, endpoints, or a schema
   change that changes what they render) — skip straight to spawning Subagent
   4; its own Step 1 will land on `E2E: SKIPPED`.
2. If a flow was touched and the suite needs a test account or live services,
   ask the user (**AskUserQuestion**) whether to run it and how the account is
   provided (a credentials file they maintain, environment variables, or a
   fresh registration if the app allows it). Never read a credentials file
   yourself and never put credentials in a prompt; set
   `CREDENTIAL_SOURCE: <the answer>`. If they decline, record
   `E2E: SKIPPED — user chose not to run it` and go to Phase 5.
3. Substitute `<CREDENTIAL_SOURCE>` into the prompt.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the suite's command for `<E2E_COMMAND>`):

> **Step 1 — Does E2E verification apply?**
> `git status --porcelain` → parse all paths (incl. `??`) into
> `CHANGED_FILES`. If none fall under a user-facing flow the suite covers,
> end with `E2E: SKIPPED — no user-facing flow changed` and stop.
>
> **Step 2 — Environment** — confirm what the suite needs is reachable (the
> backend or database it talks to, required configuration, migrations applied
> if one was touched). On failure → `E2E_BLOCKED — <reason>` and stop.
>
> **Step 3 — Dev server** — if the suite needs the application running, kill
> any stale instance, start a fresh one, confirm it answers. On failure →
> `E2E_BLOCKED — dev server did not start`.
>
> **Step 4 — Browser checks** — credential source: `<CREDENTIAL_SOURCE>`
> (already decided; do not re-ask, do not decide yourself; never log or echo
> credentials; if the source is unusable now, `E2E_BLOCKED — <source> unusable
> at read time`, never silently fall back).
> 1. Run `<E2E_COMMAND>` — the narrowest command that covers the changed flow.
> 2. If the fix touched a specific flow, exercise the specific flow the bug was
>    about, once, through the real UI against the real backend — confirm the
>    bug is actually gone here, not just in the unit test — with no console
>    errors.
> 3. A cross-user data-scoping check — only if the fix touched data access
>    **and** a real second account is available; otherwise skip it and say so
>    via `PASS_PARTIAL`.
> 4. Screenshot the final state and read it back for layout breaks.
>
> **Step 5 — Tear down** — stop any server you started.
>
> End with one of:
> - `E2E: PASS — <one-line summary>`
> - `E2E: PASS_PARTIAL — <what passed> — NOT COVERED: <what was skipped and why>`
> - `E2E_BLOCKED — <reason>`

Do not start Phase 5 until `E2E: PASS`, `PASS_PARTIAL`, or `SKIPPED`. On
`E2E_BLOCKED`, surface and wait — a failure against a live environment may be
environment/data, not a code defect; don't assume it belongs back in Phase 2
without the user confirming.
