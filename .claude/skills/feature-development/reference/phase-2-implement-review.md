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
> Use the Skill tool to invoke `openspec-apply-change <change-name>`.
> Work through all pending tasks until implementation is complete.
> Do not stop until all tasks show `[x]` or you encounter a blocker that
> requires user guidance.
> When `openspec-apply-change` completes, do NOT invoke `code-reviewer` or
> `security-reviewer` proactively — both are handled explicitly in Steps 2
> and 3.
> If you encounter a blocker during implementation, end your response with
> `IMPLEMENT_BLOCKED — <brief reason>` and stop.
>
> **Never materialize a generated secret's raw value anywhere in this
> response, a report file, a tasks.md checkbox note, or any implementation
> file.** If a task requires generating a credential (a private key, API
> token, password, signing key, or similar), you may generate it and use it
> only to complete the narrowly-required local action — e.g. writing its
> *public* half to a committed file, when the scheme has one. Do not write,
> log, or echo the secret half anywhere this session's transcript, a
> report, or a repo file would capture it. Treat actually supplying that
> value to a secrets store (`k8s/secret.yaml`, a CI secret, an env var) as
> an out-of-band action only the user performs, in their own terminal,
> outside this workflow — never something you run with the value passed as
> a visible argument. Leave that task's checkbox unchecked with a note
> naming the required manual command (never the value). Do this even under
> this workflow's general "keep going until done or blocked" instruction —
> a task that can only be finished by exposing a secret's raw value is, by
> definition, blocked. This applies equally to any skill invoked from
> within this step (e.g. `decision-record` writing an ADR that describes
> the credential) — a description of *which* secret and *how* it's stored
> is fine; the value itself is never fine.
>
> **Determining "the files changed by `<change-name>`" — recompute fresh immediately before every single use, never cache one snapshot**
> Run `git status --porcelain` and parse every line's file path —
> including untracked (`??`) entries, not just modified/staged ones — into
> a concrete list; call it `CHANGED_FILES`. Do not use
> `git diff --name-only HEAD` for this, alone or as an alternative: it only
> reports changes to already-tracked files, so it silently misses any
> brand-new file — and a brand-new Flyway migration or a brand-new JPA
> entity (untracked until ever committed) is exactly the case Step 3 below
> most needs to catch. Never substitute a branch-to-branch diff like
> `git diff develop..HEAD` either: this branch was cut from `develop` and
> per this skill's "No git commits at any point" guardrail nothing gets
> committed during this workflow, so the branch's committed history stays
> identical to `develop` throughout — a branch-to-branch diff would always
> show zero files.
>
> **Do not compute this once and reuse it across Steps 2–4.** The working
> tree keeps changing throughout Phase 2 — Step 2's fix-and-re-review loop
> (up to 3 rounds), Step 3's, and Step 4's each modify or create files, and
> a fix can easily be exactly the kind of file the *next* step's trigger
> check needs to see (e.g. a Step 2 fix that adds a new entity or
> migration must be visible to Step 3's database-review trigger). A
> `CHANGED_FILES` snapshot taken once before Step 2 and reused afterward
> would silently miss all of that. Instead, re-run `git status --porcelain`
> and recompute `CHANGED_FILES` fresh immediately before **every** point
> below marked "(recompute `CHANGED_FILES`)" — every trigger check, every
> initial reviewer invocation, every re-check after a fix round, and every
> "final pass" after a conditional step's fixes — and pass that freshly
> computed list as an explicit argument each time. Never just describe
> scope in prose as "the files changed by `<change-name>`" and let the
> invoked skill re-derive it itself: a skill that re-derives scope might
> reach for its own branch-diff logic and reintroduce the exact zero-files
> failure mode this computation exists to avoid — one layer down, invisibly
> to this skill, which would otherwise believe the problem was already
> solved.
>
> **Step 2 — Code review**
> (recompute `CHANGED_FILES`) Invoke `code-reviewer` via the Skill tool
> using its **Review** workflow, passing the freshly recomputed
> `CHANGED_FILES` as the explicit scope — do not describe the scope in
> prose and let the skill re-derive it (this subagent runs
> unattended — do not invoke the skill's interactive Fix Workflow, which is
> designed for a human to steer turn-by-turn across a conversation). This is
> a scoped review of one change, not a full-codebase sweep of the whole web
> application — the code-reviewer skill's per-layer report split and
> subagent dispatch only apply to whole-app sweeps, so do not request or
> expect multiple reports or additional subagents here. Every invocation
> writes a single dated `docs/reports/code-review/CODE_REVIEW_FINDINGS_*.md` report
> (repo root, gitignored). After the review completes, examine the findings:
>
> - If the report contains **no 🔴 Critical or 🟡 Warning findings**: proceed.
> - If the report contains any 🔴 Critical or 🟡 Warning findings: fix every
>   one of them in the source files. As you fix each finding, check it off in
>   that report file (`- [ ]` → `- [x]`) with a `**Fixed:** <one-sentence
summary of what changed>` note appended to the same line — the same
>   convention the code-reviewer skill's Fix Workflow uses — so the report is
>   left as an accurate record of what this session resolved rather than a
>   stale "nothing done" snapshot. Then (recompute `CHANGED_FILES` — the
>   fixes just applied changed the working tree) re-invoke `code-reviewer`,
>   passing the freshly recomputed list, to confirm no new findings were
>   introduced; this writes a fresh report for the re-check (the original
>   report already reflects what was fixed). Repeat until the re-check
>   report is clean.
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
> freshly recomputed `CHANGED_FILES` falls under any of these
> database-schema areas: a Flyway migration under `db/migration/`, a JPA
> entity under `infrastructure/persistence/entity/`, or a repository query
> (`@Query`, derived query method, or native query) under
> `infrastructure/persistence/jpa/` or
> `infrastructure/persistence/adapter/`.
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `database-reviewer` via the Skill tool
>   using its **Review** workflow, passing that same `CHANGED_FILES` as the
>   explicit scope — do not describe the scope in prose and let the skill
>   re-derive it (this subagent runs unattended — do not invoke the
>   skill's interactive Fix Workflow, which is designed for a human to steer
>   turn-by-turn across a conversation). This is a scoped review of one
>   change, not a full migration-history audit — do not request or expect
>   additional subagents here. Every invocation writes a single dated
>   `docs/reports/database-review/DATABASE_REVIEW_FINDINGS_*.md` report (repo root,
>   gitignored). After the review completes, fix every 🔴 Critical and 🟡
>   Warning finding in the source files — check each off in that report file
>   (`- [ ]` → `- [x]`) with a `**Fixed:**` note as you go, the same
>   convention `code-reviewer`'s Fix Workflow uses — then (recompute
>   `CHANGED_FILES`) re-invoke `database-reviewer`, passing the freshly
>   recomputed list, to confirm no new findings were introduced; this
>   writes a fresh report for the re-check. Repeat until the re-check report
>   is clean.
>   Remember that a fix to an already-applied Flyway migration is never an
>   edit to the existing file — it must be a new `V{n+1}__*.sql` migration
>   (per the skill's own §5); only entity-only findings (e.g. `@Table(indexes
= ...)` drift) may be fixed by editing the entity directly.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `DATABASE_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input (e.g. it
>   requires a data-backfill/locking strategy decision): end your response
>   with `DATABASE_BLOCKED — <brief reason>` and stop.
>   Once the database report is clean, (recompute `CHANGED_FILES` — the
>   database fixes just applied changed the working tree again) re-invoke
>   `code-reviewer` (Review workflow, same non-interactive and
>   scoped-not-full-sweep caveats as Step 2), passing the freshly
>   recomputed list as scope, for a final pass covering the database fixes
>   — apply any 🔴 Critical or 🟡 Warning findings, checking each off in
>   that pass's report file with a `**Fixed:**` note as in Step 2, before
>   proceeding.
>
> **Step 4 — Security review (conditional)**
> (recompute `CHANGED_FILES` — Step 3's fixes, if any, have modified the
> working tree since it was last computed) Check whether any path in the
> freshly recomputed `CHANGED_FILES` falls under any of these
> security-sensitive areas: authentication, authorization, file I/O, user
> input handling, dependency changes (`pom.xml` / `package.json`), or data
> persistence.
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `security-reviewer` via the Skill tool
>   using its **Review** workflow, passing that same `CHANGED_FILES` as the
>   explicit scope — do not describe the scope in prose and let the skill
>   re-derive it (this subagent runs unattended — do not invoke the
>   skill's interactive Fix Workflow, which is designed for a human to steer
>   turn-by-turn across a conversation). This is a scoped review of one
>   change, not a full-codebase sweep of the whole web application — the
>   security-reviewer skill's per-layer report split and subagent dispatch
>   only apply to whole-app sweeps, so do not request or expect multiple
>   reports or additional subagents here. Every invocation writes a single
>   dated `docs/reports/security-review/SECURITY_REVIEW_FINDINGS_*.md` report (repo
>   root, gitignored). After the review completes, fix every 🔴 Critical and
>   🟡 Warning finding in the source files — check each off in that report
>   file (`- [ ]` → `- [x]`) with a `**Fixed:**` note as you go, the same
>   convention `code-reviewer`'s Fix Workflow uses — then (recompute
>   `CHANGED_FILES`) re-invoke `security-reviewer`, passing the freshly
>   recomputed list, to confirm no new findings were introduced; this
>   writes a fresh report for the re-check. Repeat until the re-check report
>   is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `SECURITY_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input: end your
>   response with `SECURITY_BLOCKED — <brief reason>` and stop.
>   Once the security report is clean, (recompute `CHANGED_FILES` — the
>   security fixes just applied changed the working tree again) re-invoke
>   `code-reviewer` (Review workflow, same non-interactive and
>   scoped-not-full-sweep caveats as Step 2), passing the freshly
>   recomputed list as scope, for a final pass covering the security fixes
>   — apply any 🔴 Critical or 🟡 Warning findings, checking each off in that pass's report
>   file with a `**Fixed:**` note as in Step 2, before proceeding.
>
> Do not proceed to Step 5 until all Critical and Warning code-review,
> database-review, and security-review findings (including any follow-on
> code-review findings from the database and security sub-steps) are resolved.
>
> **Step 5 — Mobile viewport check (conditional)**
> (recompute `CHANGED_FILES`) Check whether any path in the freshly
> recomputed `CHANGED_FILES` is a component template (`*.html`) or
> stylesheet (`*.css`/`*.scss`) under `JPPhotoManagerWeb/frontend/src/app/`.
>
> - If **none**: skip this step.
> - If **any**: verify the affected view at the project's standard mobile
>   check device — Samsung Galaxy S23 Ultra, CSS viewport 384×824 @3.75x
>   DPR — per `angular-developer`'s standard mobile check device (Chrome
>   DevTools custom device, or `cy.viewport(384, 824)` in a Cypress
>   spec/screenshot). Confirm no text overlap between elements, no
>   clipped/cropped content, and no forced horizontal scroll at that
>   width. Fix any layout issue the same way as a code-review finding
>   (recompute `CHANGED_FILES`, re-invoke `code-reviewer` once more over
>   the fix). Note what was checked, and anything caught, for the Final
>   Summary.
>
> **Step 6 — Signal completion**
> End your response with exactly this line:
> `IMPLEMENT: DONE`

Do not start this phase until Phase 1 subagent has confirmed artifacts are
ready. Do not start Phase 3 until this subagent returns `IMPLEMENT: DONE`.

---
