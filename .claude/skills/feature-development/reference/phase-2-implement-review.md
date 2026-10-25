# Feature Development — Phase 2 — Implement & Review

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 2 — Implement (Subagent 2), then Phase 2R — Review gate

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 3 cannot start until this subagent
returns `IMPLEMENT: DONE` — see the foreground guardrail in `../SKILL.md`), with the
following prompt:

> Perform these steps in sequence:
>
> **Step 1 — Implement** (this subagent implements only: it does not run `code-reviewer`, `database-reviewer`, `security-reviewer` or the viewport check, and it does not decide whether they apply)
> **OpenSpec project** (an `openspec/` folder exists): use the Skill tool to
> invoke `openspec-apply-change <change-name>`. Work through all pending
> tasks until implementation is complete. Do not stop until all tasks show
> `[x]` or you encounter a blocker that requires user guidance (end with
> `IMPLEMENT_BLOCKED — <brief reason>` and stop). Run only the tests of
> the files you touched (the project's single-file form): the whole suite
> is Phase 3's job, and it can outlast the Bash tool's cap. Never end your
> response while a command you started is still running in the background. When
> `openspec-apply-change` completes, do NOT invoke `code-reviewer`
> or any other review — the orchestrator runs them in Phase 2R.
>
> **Otherwise**, the feature's brief file
> (`<backlog>/features/NNN-<change-name>.md`, everything after the H1; `NNN`
> is the feature number zero-padded to three digits, from the feature's row in
> `<backlog>/features-planned.md`) is the spec. Write down, for yourself, the
> short list of tasks it implies and what must be true when the feature is
> done, implement the tasks, and add or update tests so that what must be true
> is covered by tests where the project's test setup allows it. List those
> checks in your hand-back as `ACCEPTANCE: <check>` lines; Phase 6S uses them.
> When the brief lists acceptance criteria (`- AC1: …`), the tasks and the
> `ACCEPTANCE:` lines follow them: one line per criterion, named `AC1`, `AC2`, ….
> If you encounter a blocker during implementation, end your response with
> `IMPLEMENT_BLOCKED — <brief reason>` and stop.
>
> **Never materialize a generated secret's raw value anywhere in this
> response, a report file, a tasks.md checkbox note, or any implementation
> file.** If a task requires generating a credential (a private key, API
> token, password, signing key, or similar), you may generate it and
> use it only to complete the narrowly-required local action — e.g. writing
> its *public* half to a committed file, when the scheme has one. Do not
> write, log, or echo the secret half anywhere this session's transcript,
> a report, or a repo file would capture it. Treat actually supplying that
> value to a secrets store (a secrets-manager command, a Vault insert, an env
> var) as an out-of-band action only the user performs, in their own
> terminal, outside this workflow — never something you run with the value
> passed as a visible argument. Leave that task's checkbox unchecked with a
> note naming the required manual command (never the value) — e.g. "Store
> the private key as a deployment secret (`<secrets tool> set
> PRIVATE_KEY=...`) — run manually by the user, value never
> committed." Do this even under this workflow's general "keep going
> until done or blocked" instruction — a task that can only be finished by
> exposing a secret's raw value is, by definition, blocked. This applies
> equally to any skill invoked from within this step (e.g. `decision-record`
> writing an ADR that describes the credential) — a description of *which*
> secret and *how* it's stored is fine; the value itself is never fine.
>
>
> **Step 2 — Signal completion**
> List what you changed in plain words (no verdict on whether any review applies). Without OpenSpec, then list the `ACCEPTANCE: <check>` lines. The last line of your response is exactly:
> `IMPLEMENT: DONE`

Do not start Phase 2R until this subagent returns `IMPLEMENT: DONE`.

### Phase 2R — Review gate (orchestrator)

Read `reference/review-gate.md` in full and run it, with `<work-id>` = `<change-name>`: the orchestrator computes `CHANGED_FILES` itself, spawns one subagent each for the code review, the database review, the security review and the viewport check (deciding itself, from `git status --porcelain`, which of the last three apply), verifies every report file and its checkboxes before accepting a signal, and ends with `REVIEW_GATE: DONE`. Its ledger lines (`CODE_REVIEW:`, `DATABASE_REVIEW:`, `SECURITY_REVIEW:`, `VIEWPORTS:`) feed the Final Summary.

Do not start Phase 3 until the gate returns `REVIEW_GATE: DONE`. A `REVIEW_BLOCKED`, `DATABASE_BLOCKED` or `SECURITY_BLOCKED` stops the workflow: surface it and wait for the user.

---
