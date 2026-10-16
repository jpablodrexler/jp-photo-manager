# Feature Development — Phase 2 — Implement & Review

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 2 — Implement & Review (Subagent 2)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 3 cannot start until this subagent
returns `IMPLEMENT: DONE` — see the foreground guardrail in `../SKILL.md`), with the
following prompt:

> Perform these steps in sequence:
>
> **Step 1 — Implement**
> **OpenSpec project** (an `openspec/` folder exists): use the Skill tool to
> invoke `openspec-apply-change <change-name>`. Work through all pending
> tasks until implementation is complete. Do not stop until all tasks show
> `[x]` or you encounter a blocker that requires user guidance. When
> `openspec-apply-change` completes, do NOT invoke `code-reviewer`
> proactively — it is handled explicitly in Step 2.
>
> **Otherwise**, the feature's brief file
> (`<backlog>/features/NNN-<change-name>.md`, everything after the H1; `NNN`
> is the feature number zero-padded to three digits, from the feature's row in
> `<backlog>/features-planned.md`) is the spec. Write down, for yourself, the
> short list of tasks it implies and what must be true when the feature is
> done, implement the tasks, and add or update tests so that what must be true
> is covered by tests where the project's test setup allows it. List those
> checks in your hand-back as `ACCEPTANCE: <check>` lines; Phase 6 uses them.
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
> **Determining "the files changed by `<change-name>`" — recompute fresh immediately before every single use, never cache one snapshot**
> Run `git status --porcelain` and parse every line's file path —
> including untracked (`??`) entries, not just modified/staged ones — into
> a concrete list; call it `CHANGED_FILES`. Do not use
> `git diff --name-only HEAD` for this, alone or as an alternative: it only
> reports changes to already-tracked files, so it silently misses any
> brand-new file — and a brand-new migration or configuration file is exactly the case
> a later review most needs to catch. Never substitute a branch-to-branch
> diff like `git diff main..HEAD` either: per this skill's "No git commits
> at any point" guardrail nothing gets committed during this workflow, so
> the branch's committed history stays identical to its base branch
> throughout — a branch-to-branch diff would always show zero files.
>
> **Do not compute this once and reuse it across Steps 2–5.** The working
> tree keeps changing throughout Phase 2 — Step 2's fix-and-re-review loop
> (up to 3 rounds), Step 3's, and Step 4's each modify or create files, and
> a fix can easily be exactly the kind of file the *next* step's trigger
> check needs to see
> (e.g. a Step 2 fix that adds a new migration must be visible to Step 3's
> database-review trigger). A `CHANGED_FILES` snapshot taken once
> before Step 2 and reused afterward would silently miss all of that. Instead, re-run `git status --porcelain` and recompute
> `CHANGED_FILES` fresh immediately before **every** point below marked
> "(recompute `CHANGED_FILES`)", and pass that freshly computed list as an
> explicit argument each time. Never just describe scope in prose as "the
> files changed by `<change-name>`" and let the invoked skill re-derive it
> itself.
>
> **Step 2 — Code review**
> (recompute `CHANGED_FILES`) If the `code-reviewer` skill is not installed,
> review the diff of `CHANGED_FILES` yourself as a reviewer who did not write
> it — correctness and edge cases, regressions in what calls the changed code,
> missing or weak tests, unintended changes, consistency with the project's
> conventions — fix every real finding and review the fixes again, up to 3
> rounds, then continue to Step 3. If it is installed, invoke `code-reviewer` via the Skill tool
> using its **Review** workflow, passing the freshly recomputed
> `CHANGED_FILES` as the explicit scope (this subagent runs unattended — do
> not invoke the skill's interactive Fix Workflow, which is designed for a
> human to steer turn-by-turn across a conversation). This is a scoped
> review of one change, not a full-codebase sweep — do not request or
> expect multiple reports or additional subagents here. Every invocation
> writes a single dated `<reports>/code-review/CODE_REVIEW_FINDINGS_*.md` report. After the review completes, examine the findings:
>
> - If the report contains **no 🔴 Critical or 🟡 Warning findings**: proceed.
> - If the report contains any 🔴 Critical or 🟡 Warning findings: fix every
>   one of them in the source files. As you fix each finding, check it off in
>   that report file (`- [ ]` → `- [x]`) with a `**Fixed:** <one-sentence
summary of what changed>` note appended to the same line. Then (recompute
>   `CHANGED_FILES` — the fixes just applied changed the working tree)
>   re-invoke `code-reviewer`, passing the freshly recomputed list, to
>   confirm no new findings were introduced; this writes a fresh report for
>   the re-check. Repeat until the re-check report is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `REVIEW_BLOCKED — repeated review cycles` and stop.
> - If you encounter a finding you cannot fix without human input: end your
>   response with `REVIEW_BLOCKED — <brief reason>` and stop.
>
> Do not proceed to Step 3 until all Critical and Warning findings are resolved.
>
> **Step 3 — Database review (conditional)**
> (recompute `CHANGED_FILES` — Step 2's fix rounds have modified the
> working tree since it was last computed) Check whether any path in the
> freshly recomputed `CHANGED_FILES` is a database migration or schema
> definition (a migrations folder, a schema/DDL file, an ORM model that
> generates migrations).
>
> - If **no** such path was touched: skip this step.
> - If **any** such path was touched: invoke `database-reviewer` via the
>   Skill tool using its **Review** workflow, passing that same
>   `CHANGED_FILES` as the explicit scope (this subagent runs unattended —
>   do not invoke the skill's interactive Fix Workflow). This is a scoped
>   review of one change, not a full migration-history audit. If the
>   `database-reviewer` skill is not installed, review those files yourself
>   for the same concerns (normalization, naming, data types, constraints,
>   index coverage, migration safety) and record the findings in a dated
>   `<reports>/database-review/DATABASE_REVIEW_FINDINGS_*.md` report; with
>   the skill, it writes that report itself. After the review completes, fix
>   every 🔴 Critical and 🟡 Warning finding in the source files — check each
>   off in that report file (`- [ ]` → `- [x]`) with a `**Fixed:**` note as
>   you go — then (recompute `CHANGED_FILES`) re-review, passing the freshly
>   recomputed list, to confirm no new findings were introduced; this writes
>   a fresh report for the re-check. Repeat until the re-check report is
>   clean.
>   Remember that a fix to an already-applied migration is never an edit to
>   the existing file: it must be a new migration created with the project's
>   migration tool.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `DATABASE_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input (e.g. it
>   requires a data-backfill/locking strategy decision): end your response
>   with `DATABASE_BLOCKED — <brief reason>` and stop.
>   Once the database report is clean, (recompute `CHANGED_FILES` — the
>   database fixes just applied changed the working tree again) review the
>   changed files once more as in Step 2, passing the freshly recomputed list
>   as scope, for a final pass covering the database fixes — apply any 🔴
>   Critical or 🟡 Warning findings, checking each off in that pass's report
>   file with a `**Fixed:**` note as in Step 2, before proceeding.
>
> **Step 4 — Security review (conditional)**
> (recompute `CHANGED_FILES` — Step 3's fixes, if any, have modified the
> working tree since it was last computed) Check whether any path in the
> freshly recomputed `CHANGED_FILES` falls under any of these
> security-sensitive areas: authentication/authorization, access-control
> rules or policies, user input handling, secrets or configuration, or a
> dependency change (a package manifest or lockfile).
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `security-reviewer` via the Skill tool
>   using its **Review** workflow, passing that same `CHANGED_FILES` as the
>   explicit scope (this subagent runs unattended — do not invoke the
>   skill's interactive Fix Workflow). This is a scoped review of one
>   change, not a full-codebase sweep — do not request or expect multiple
>   reports or additional subagents here. If the `security-reviewer` skill is
>   not installed, review those files yourself for the matching risks
>   (missing authorization checks, injection, unsafe input handling, secrets
>   or tokens in code or logs, a vulnerable or unpinned dependency) and record
>   the findings in a dated
>   `<reports>/security-review/SECURITY_REVIEW_FINDINGS_*.md` report. After
>   the review completes, fix every 🔴 Critical and 🟡 Warning finding in
>   the source files — check each off in that report file (`- [ ]` → `- [x]`)
>   with a `**Fixed:**` note as you go — then (recompute `CHANGED_FILES`)
>   re-review, passing the freshly recomputed list, to confirm no new
>   findings were introduced; this writes a fresh report for the re-check.
>   Repeat until the re-check report is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `SECURITY_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input: end your
>   response with `SECURITY_BLOCKED — <brief reason>` and stop.
>   Once the security report is clean, (recompute `CHANGED_FILES` — the
>   security fixes just applied changed the working tree again) review the
>   changed files once more as in Step 2, passing the freshly recomputed list
>   as scope, for a final pass covering the security fixes — apply any 🔴
>   Critical or 🟡 Warning findings, checking each off in that pass's report
>   file with a `**Fixed:**` note as in Step 2, before proceeding.
>
> Do not proceed to Step 5 until all Critical and Warning code-review,
> database-review, and security-review findings (including any follow-on
> code-review findings from the database and security sub-steps) are resolved.
>
> **Step 5 — Mobile and desktop viewport check (conditional)**
> (recompute `CHANGED_FILES`) Check whether any path in the freshly
> recomputed `CHANGED_FILES` is a user-interface template, markup file or
> stylesheet.
>
> - If **none**: skip this step.
> - If **any**: verify the affected view at **every phone viewport and the desktop
>   viewport** from the `[viewports]` table of `.backlog-kit.toml` at the
>   repository root (read the file yourself; with no file or no table, the
>   defaults are phone `Galaxy S23 Ultra 384x824` and desktop `1920x1080`,
>   entries written `Name WIDTHxHEIGHT` in CSS pixels) — with a browser
>   tool, a browser's device emulation, or a component/E2E test that sets the
>   viewport, whichever the project has. If the project has no way to render
>   the view, skip this step and say so in the Final Summary. At each phone
>   size, confirm no text overlap between elements, no clipped/cropped
>   content, and no forced horizontal scroll. At desktop, confirm no
>   field/dialog/container collapses to a narrow, phone-sized width while
>   leaving the rest of its space unused, and nothing correctly sized for the
>   phone stayed wrongly pinned to that size once the wide layout applies. Fix
>   any layout issue the same way as a code-review finding (recompute
>   `CHANGED_FILES`, review the fix once more as in Step 2). Note what was
>   checked at each viewport, and anything caught, for the Final Summary.
>   Check **each** configured viewport in turn (a test or a device emulation per size); asserting only the smallest phone and the desktop is not enough. List every configured viewport in your hand-back as `verified` or `skipped — <reason>`; a viewport you did not check is never reported as verified.
>
> **Step 6 — Signal completion**
> End your response with exactly this line:
> `IMPLEMENT: DONE`

Do not start this phase until Phase 1 subagent has confirmed artifacts are
ready. Do not start Phase 3 until this subagent returns `IMPLEMENT: DONE`.

---
