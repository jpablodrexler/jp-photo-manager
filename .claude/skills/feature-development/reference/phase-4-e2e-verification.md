# Feature Development — Phase 4 — E2E Verification

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 4 — E2E Verification (Subagent 4, conditional)

**This phase is optional: skip it when the project has no end-to-end or
browser suite, or when the change touches no user-facing flow.** Find out the
first yourself (orchestrator): the project's `CLAUDE.md`, `README` and build
manifest say whether there is an end-to-end suite and how it is run. If there
is none, record `E2E: SKIPPED — no end-to-end suite` and go to Phase 5.

**Before spawning Subagent 4 — resolve how the suite gets its test account, if
it needs one.** This is an orchestrator step, not something Subagent 4 does
itself: it needs `AskUserQuestion`, which a spawned subagent cannot reliably
access (the same reason Phase 0's `features-next` confirmation runs directly
in the orchestrator's own context rather than a spawned subagent — see that
phase in `../SKILL.md` and the guardrails there). Whatever context is
currently running this skill — the main conversation, or
`features-batch-development`'s loop, which invokes this skill inline rather
than as a background agent specifically so its own `AskUserQuestion` calls
reach the user — has that access; a spawned Subagent 4 would not.

1. Run `git status --porcelain` yourself (not via a subagent) and check
   whether any changed path falls under a user-facing flow the suite covers
   (the screens, routes or endpoints a user reaches, or a schema change that
   changes what they render). This is a lightweight pre-check purely to decide
   whether any question is even worth asking — Subagent 4's own Step 1 remains
   the authoritative, independently-recomputed check for whether Phase 4
   actually runs at all.
   - If none were touched: skip straight to spawning Subagent 4 as normal
     below — its own Step 1 will independently land on `E2E: SKIPPED`, and
     there is nothing to ask for a phase that isn't going to run.
2. If a user-facing flow was touched and the suite needs a test account or
   live services (a deployed backend, a hosted database): ask the user
   (**AskUserQuestion**) whether to run it, and how the account is provided
   (a credentials file they maintain, environment variables, or a fresh
   registration if the app allows it). **Never read a credentials file
   yourself, and never put credentials in a prompt** — Subagent 4 is told
   *which source to use*, not the values. Record the answer as
   `CREDENTIAL_SOURCE: <the user's answer>`; if they say not to run it,
   record `E2E: SKIPPED — user chose not to run the end-to-end suite` and go
   to Phase 5.
3. Carry `CREDENTIAL_SOURCE` into Subagent 4's prompt below, substituted
   the same way `<change-name>` is per the "Placeholder substitution"
   section in `../SKILL.md`.

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 5 cannot start until this subagent
reports its signal — see the foreground guardrail in `../SKILL.md`), with the
following prompt (substitute the suite's command for `<E2E_COMMAND>`):

> Perform these steps in sequence.
>
> **Step 1 — Determine whether E2E verification applies to this change**
> Run `git status --porcelain` and parse every line's file path — including
> untracked (`??`) entries — into a concrete list; call it `CHANGED_FILES`.
> This is the same recompute-fresh convention Phase 2 uses — never assume a
> list from an earlier phase is still accurate.
>
> Check whether any path in `CHANGED_FILES` falls under a user-facing flow
> the suite covers (screens, routes, endpoints, or a schema change that
> changes what they render).
>
> - If **none** were touched: end your response with exactly
>   `E2E: SKIPPED — no user-facing flow changed in this change` and stop. Do
>   not start any server or run the suite for a change that doesn't touch a
>   flow it covers.
> - If **any** were touched: continue to Step 2.
>
> **Step 2 — Confirm the environment is ready**
> Check that what the suite needs is reachable (the database or backend it
> talks to, required configuration files). If any check fails, end your
> response with `E2E_BLOCKED — <brief reason>` and stop — do not attempt to fix
> an environment/infrastructure problem yourself, with the two exceptions below.
>
> *Local container runtime.* If the project's tooling needs Docker (or a similar
> runtime) and it is not running, start it yourself — Docker Desktop on
> Windows/macOS (`Start-Process "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"`,
> `open -a Docker`) or the service on Linux — and poll `docker info` in the
> foreground for up to 3 minutes. Only if it never comes up, end with
> `E2E_BLOCKED — container runtime did not start`.
>
> *Pending migrations of this change.* If `CHANGED_FILES` contains a new
> migration, check read-only (the project's migration-list or dry-run command,
> named in its `CLAUDE.md`/docs) whether it is applied to the database the suite
> runs against. If it is not, apply it yourself — never stop to tell the user it
> is "blocked" and wait to be asked: first run the tool's **dry run** and
> compare the pending list with the migration files in `CHANGED_FILES`. If the
> pending list is exactly this change's migrations, run the real push/apply
> command and re-check that it now shows as applied. If the dry run lists
> anything else (another change's migration, a destructive-looking diff), do
> not push: end with `E2E_BLOCKED — unexpected pending migrations: <list>`.
> Report which migration you applied in your final signal line.
>
> **Step 3 — Start the dev server**
> If the suite needs the application running, kill any stale instance of it
> first, start a fresh one, and confirm it answers. If it never comes up, end
> your response with `E2E_BLOCKED — dev server did not start` and stop.
>
> **Step 4 — Run the browser checks**
> Credential source for this run: `<CREDENTIAL_SOURCE>` (substituted by the
> orchestrator before this prompt was sent — see the pre-step above). This was
> already decided; do not re-ask and do not decide it yourself even if
> `AskUserQuestion` happens to be available to you in this context. Never log,
> echo, or otherwise surface the credentials anywhere beyond using them to
> sign in. If the source cannot be used at this point (a file missing or
> malformed), end your response with
> `E2E_BLOCKED — <credential source> unusable at read time` rather than
> silently falling back to something else.
>
> 1. Run `<E2E_COMMAND>` — the narrowest command that covers the changed
>    flow, or the whole suite if the project has no narrower one.
> 2. If this change touched a specific flow (create/edit/complete/delete,
>    sign-in, a settings page), also exercise it once through the actual UI
>    (not just confirming the page loads) to confirm the new/changed behavior
>    works against the real backend, and that there are no console errors.
> 3. If the project's suite includes a check that one user cannot see another
>    user's data and this change touched data access, run it **only if** a
>    genuine second test account is available — otherwise skip that specific
>    check rather than registering accounts speculatively, and say so in the
>    final signal (see `PASS_PARTIAL` below) rather than silently omitting it.
> 4. Take a screenshot at the final state and read it back to confirm there's
>    no visible layout break.
>
> **Step 5 — Tear down**
> Stop any server you started.
>
> End your response with one of:
>
> - `E2E: PASS — <one-line summary of what was exercised>` if every check in
>   Step 4 that applied was run and passed.
> - `E2E: PASS_PARTIAL — <what passed> — NOT COVERED: <what was skipped and
>   why>` if everything that ran passed, but part of what the suite normally
>   covers (for example the two-account check) was skipped.
> - `E2E_BLOCKED — <brief reason>` if any check failed, the environment
>   wasn't ready, or a console error/visible layout break was observed.

Do not start Phase 5 until this subagent reports `E2E: PASS`,
`E2E: PASS_PARTIAL`, or `E2E: SKIPPED`. If it reports `E2E_BLOCKED`,
surface the details to the user and wait for guidance before continuing —
a failure here may equally be an environment/data problem as a code defect;
don't assume it belongs back in Phase 2's code review without the user
confirming that.

---
