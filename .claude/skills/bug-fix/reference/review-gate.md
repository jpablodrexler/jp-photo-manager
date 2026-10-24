<!-- VENDORED from shared/review-gate.md — do not edit here; edit the source and run `python3 tools/vendor.py`. -->
# Review gate

The review gate is shared by `feature-development` (Phase 2R) and `bug-fix` (Phase 2R). It runs right after the implementation subagent returns its `IMPLEMENT: DONE` / `FIX_IMPLEMENTED: DONE` signal, and **it is run by the orchestrator, not by the implementation subagent**.

Why it is separate: the reviewers (`code-reviewer`, `database-reviewer`, `security-reviewer`) used to run inside the implementation subagent, at the tail of a context already full of implementation work. That is where they got shortened, replaced by a self-review, or skipped on a wrong "not applicable" call, and nothing checked afterwards. So now:

1. **Each review runs in its own subagent** with a clean context and one job.
2. **The orchestrator decides which conditional reviews apply**, from its own G0 computation, never from the implementation subagent's word.
3. **The orchestrator verifies the artifacts** (report files and their checkboxes) before it accepts a signal. A signal without a matching report is not accepted.

`<work-id>` below is the change name (`feature-development`) or the Bug ID (`bug-fix`); substitute it, and `<reports>`, in every prompt before spawning. Append the standard **Hand-back** paragraph of the calling skill to every prompt, unchanged.

## Orchestrator procedure

**G0 — Compute the scope yourself.** The rule depends on whether this run commits (`--commit`, or the user asked to commit as the work goes; the same fact `gitflow`'s commit action is called on):

- **Not committing (the default):** `CHANGED_FILES` is the uncommitted files. Run `git status --porcelain` and parse every path, untracked (`??`) entries included.
- **Committing:** `CHANGED_FILES` is the files that differ between the work branch and `develop`, plus the uncommitted files. Take the first with `git diff --name-only develop...HEAD` (three dots: from the merge base, so `develop`'s own newer commits are not counted; use `origin/develop` when there is no local `develop`, and `main` in a repository that has no `develop`), take the second as above, and merge the two lists without duplicates. A shared batch branch therefore includes the files of the batch's earlier committed work as well.

Never use `git diff --name-only HEAD` on its own (it misses new files), and never conclude from an empty list that nothing needs review before you have applied the rule for the mode you are in. Recompute `CHANGED_FILES` fresh before every gate below, and pass the list into each subagent prompt as an explicit argument, together with the rule that produced it, which replaces `<G0 MODE>` in the prompts below (`uncommitted files, untracked included` or `files differing from develop, plus uncommitted files, untracked included`). Also note whether each reviewer skill is installed (a folder `code-reviewer`, `database-reviewer` or `security-reviewer` under `.claude/skills/` or the plugin's skills).

**G1 — Code review.** Always applies. Spawn the code-review subagent.

**G2 — Database review.** Applies when `CHANGED_FILES` (fresh) holds a database migration or schema definition: a migrations folder, a schema/DDL file, an ORM model that generates migrations. If it applies, spawn the database-review subagent. If it does not, record `DATABASE_REVIEW: skipped — no migration or schema path in CHANGED_FILES`. The reason is yours, from your own list.

**G3 — Security review.** Applies when `CHANGED_FILES` (fresh) holds a path under authentication or authorization, access-control rules or policies, user-input handling, secrets or configuration, or a dependency manifest or lockfile. If it applies, spawn the security-review subagent; otherwise record `SECURITY_REVIEW: skipped — no security-sensitive path in CHANGED_FILES`.

**G4 — Viewport check.** Applies when `CHANGED_FILES` (fresh) holds a user-interface template, markup file or stylesheet. If it applies, spawn the viewport subagent; otherwise record `VIEWPORTS: skipped — no template, markup or stylesheet in CHANGED_FILES`.

**G5 — Follow-on code review.** If G2, G3 or G4 reported `FILES_CHANGED: yes` (they fixed something), recompute `CHANGED_FILES` and spawn the code-review subagent once more over the fixes. Repeat G2–G5 as needed, at most 3 rounds in all; a fourth round is `REVIEW_BLOCKED — repeated review cycles`.

**G6 — Verify before accepting.** After **every** review subagent returns, and before the next gate, check it yourself:

- The signal line is present and names a report path. A response without it is `BLOCKED`: surface the raw response and the working tree evidence, as the calling skill's missing-signal guardrail says.
- That report file exists (`ls` it) and sits under `<reports>/code-review/`, `<reports>/database-review/` or `<reports>/security-review/` with a dated `*_FINDINGS_*.md` name.
- `grep` the report for an unchecked `- [ ]` line carrying 🔴 or 🟡. Any hit means the subagent stopped short: re-spawn that same review subagent once with the unchecked lines quoted; a second miss is `REVIEW_BLOCKED — unresolved findings in <path>`.
- The report's first line is the stamp `<!-- backlog-kit: <skill> -->` of the reviewer skill that should have written it (`code-reviewer`, `database-reviewer`, `security-reviewer`). `SKILL_USED: <skill>` without that stamp, or with a stamp of another skill, is a failed run: re-spawn once. A report without a stamp is legitimate only for the fallback (skill not installed).
- The `SKILL_USED:` line says `<skill>` when the reviewer skill is installed. `SKILL_USED: none — fallback` while the skill **is** installed is a failed run: re-spawn once. When the skill is not installed, the fallback is legitimate; carry it into the Final Summary as `⚠ reviewed without the <skill> skill`.
- For a skipped review you recorded in G2–G4, nothing to verify, but the reason goes into the ledger.

**G7 — Ledger and signal.** Keep a ledger of one line per gate (`CODE_REVIEW: <path>`, `DATABASE_REVIEW: <path> | skipped — <reason>`, `SECURITY_REVIEW: <path> | skipped — <reason>`, `VIEWPORTS: <verified/skipped per configured viewport> | skipped — <reason>`). The Final Summary is built from it. When every gate that applies has passed G6 and no further `FILES_CHANGED: yes` is pending, the gate is `REVIEW_GATE: DONE`. Only then may the next phase start. Any `*_BLOCKED` from a gate stops the workflow: surface it and wait for the user.

## Subagent prompts

Spawn each as a **general-purpose subagent** via the Agent tool with `run_in_background: false`.

### G1 / G5 — Code review

> You are a reviewer who did not write this change. The scope is exactly these files (computed by the orchestrator by rule G0, untracked included):
>
> <CHANGED_FILES, one per line>
>
> **Step 1.** If the `code-reviewer` skill is installed, invoke it via the Skill tool using its **Review** workflow, with that list as the explicit scope. This subagent runs unattended: do not use the interactive Fix Workflow, and do not run a full-codebase sweep (one scoped review writes one dated `<reports>/code-review/CODE_REVIEW_FINDINGS_*.md`). If the skill is not installed, review the diff of those files yourself — correctness and edge cases, regressions in what calls the changed code, missing or weak tests, unintended changes, consistency with the project's conventions — and record the findings in that same dated report. Never skip the report.
>
> **Step 2.** Fix every 🔴 Critical and 🟡 Warning finding in the source. Check each off in the report (`- [ ]` → `- [x]`) with a `**Fixed:** <one sentence>` note on the same line. Recompute the file list by the same rule (`<G0 MODE>`), then review again (a fresh dated report) to confirm nothing new. Repeat until a re-check report is clean, at most 3 rounds; after that end with `REVIEW_BLOCKED — repeated review cycles`. A finding you cannot fix without human input ends the response with `REVIEW_BLOCKED — <reason>`.
>
> **Step 3.** End with exactly these lines:
> `SKILL_USED: code-reviewer` (or `SKILL_USED: none — fallback`)
> `FILES_CHANGED: yes` (or `no`; yes if Step 2 modified any file)
> `CODE_REVIEW: DONE <path of the last, clean report>`

### G2 — Database review

> You are a database reviewer. The scope is exactly these files (computed by the orchestrator by rule G0, untracked included):
>
> <CHANGED_FILES, one per line>
>
> **Step 1.** If the `database-reviewer` skill is installed, invoke it via the Skill tool using its **Review** workflow with that explicit scope (unattended: not the interactive Fix Workflow, and not a full migration-history audit). It writes a dated `<reports>/database-review/DATABASE_REVIEW_FINDINGS_*.md`. If it is not installed, review the schema files yourself for normalization, naming, data types, constraints, index coverage and migration safety, and record the findings in that same dated report.
>
> **Step 2.** Fix every 🔴 Critical and 🟡 Warning finding, checking each off in the report with a `**Fixed:**` note. A fix to an already-applied migration is never an edit to the existing file: it is a **new** migration created with the project's migration tool. Recompute the file list by the same rule (`<G0 MODE>`), re-review for a clean re-check report, at most 3 rounds (`DATABASE_BLOCKED — repeated review cycles`); a finding that needs a human decision (for instance a data-backfill or locking strategy) ends with `DATABASE_BLOCKED — <reason>`.
>
> **Step 3.** End with exactly these lines:
> `SKILL_USED: database-reviewer` (or `SKILL_USED: none — fallback`)
> `FILES_CHANGED: yes` (or `no`)
> `DATABASE_REVIEW: DONE <path of the last, clean report>`

### G3 — Security review

> You are a security reviewer. The scope is exactly these files (computed by the orchestrator by rule G0, untracked included):
>
> <CHANGED_FILES, one per line>
>
> **Step 1.** If the `security-reviewer` skill is installed, invoke it via the Skill tool using its **Review** workflow with that explicit scope (unattended: not the interactive Fix Workflow, and not a full-codebase sweep). It writes a dated `<reports>/security-review/SECURITY_REVIEW_FINDINGS_*.md`. If it is not installed, review those files yourself for missing authorization checks, injection, unsafe input handling, secrets or tokens in code or logs, and a vulnerable or unpinned dependency, and record the findings in that same dated report.
>
> **Step 2.** Fix every 🔴 Critical and 🟡 Warning finding (the safer fix for a security finding), checking each off in the report with a `**Fixed:**` note. Recompute the file list by the same rule (`<G0 MODE>`), re-review for a clean re-check report, at most 3 rounds (`SECURITY_BLOCKED — repeated review cycles`); a finding that needs a human decision ends with `SECURITY_BLOCKED — <reason>`. Never write a generated secret's raw value in a response, report or file.
>
> **Step 3.** End with exactly these lines:
> `SKILL_USED: security-reviewer` (or `SKILL_USED: none — fallback`)
> `FILES_CHANGED: yes` (or `no`)
> `SECURITY_REVIEW: DONE <path of the last, clean report>`

### G4 — Mobile and desktop viewport check

> The orchestrator found these user-interface files in the change (computed by rule G0):
>
> <the template, markup and stylesheet paths of CHANGED_FILES>
>
> Verify the affected view at **every phone viewport and the desktop viewport** from the `[viewports]` table of `.backlog-kit.toml` at the repository root (read the file yourself; with no file or no table, the defaults are phone `Galaxy S23 Ultra 384x824` and desktop `1920x1080`, entries written `Name WIDTHxHEIGHT` in CSS pixels) — with a browser tool, a browser's device emulation, or a component/E2E test that sets the viewport, whichever the project has. If the project has no way to render the view, say so and skip. At each phone size, confirm no text overlap between elements, no clipped/cropped content, and no forced horizontal scroll. At desktop, confirm no field/dialog/container collapses to a narrow, phone-sized width while leaving the rest of its space unused, and nothing correctly sized for the phone stayed wrongly pinned to that size once the wide layout applies. Fix any layout issue as a code-review finding would be fixed.
>
> Check **each** configured viewport in turn (a test or a device emulation per size); asserting only the smallest phone and the desktop is not enough. List every configured viewport in your hand-back as `verified` or `skipped — <reason>`; a viewport you did not check is never reported as verified.
>
> End with exactly these lines:
> `VIEWPORT <Name WIDTHxHEIGHT>: verified` (or `skipped — <reason>`), one line per configured viewport
> `FILES_CHANGED: yes` (or `no`)
> `VIEWPORTS: DONE`

The orchestrator's G6 for this gate: one `VIEWPORT` line per configured viewport, none missing.
