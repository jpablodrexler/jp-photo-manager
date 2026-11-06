# Bug Fix — Phase 1 — Branch & Reproduce

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 1 — Branch & Reproduce (Subagent 1)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug to fix has already been selected and confirmed with the user:
> `<bug-id>`. Do not invoke `bugs-next` or re-select a bug. Its recorded
> repro:
>
> <repro-details>
>
> **Step 1 — Set up the fix branch**
>
> **Batch mode (`--skip-branch-setup` was passed):** run `git branch
> --show-current` once; if it is `<production>` or `<integration>`, end with
> `FIX_BLOCKED — batch mode requires an existing fix branch already
> checked out` and stop. Otherwise continue on the current
> branch. Do not check for uncommitted changes, and do not create, switch,
> or sync any branch — the caller owns branch state. Then go to Step 1.5.
>
> **Normal mode (no flag):** check resume status *before* checking for
> uncommitted changes (a resumed run is typically already on
> `fix/<slug>` with the in-progress fix sitting there uncommitted — that's
> expected, not a blocker).
>
> B0. `git fetch origin --prune`.
> B1. `git branch --show-current`.
>    - If it is already exactly `fix/<slug>`: this is a resume. Run
>      `git rev-list HEAD..origin/fix/<slug> --count` (treat a missing
>      `origin/fix/<slug>` as `0`). If `0`, skip to Step 1.5. If `>0`, end
>      with `FIX_BLOCKED — origin/fix/<slug> has <N> commit(s) not present
>      locally; reconcile manually` and stop.
>    - Otherwise continue to B2.
> B2. `git status --porcelain`. Ignore every entry whose path is under
>    `<backlog>/`: `bug-report` leaves the new row and details file
>    uncommitted on purpose, and the new branch carries them along (create
>    it with `git checkout -b` when `gitflow` stops on them, and say so in
>    your hand-back). If it prints anything else, end with
>    `FIX_BLOCKED — uncommitted changes present, cannot create fix branch`
>    and stop.
> B3. `git rev-parse --verify --quiet fix/<slug>`:
>    - Prints a hash → the branch exists locally but isn't checked out:
>      `git checkout fix/<slug>`, then if `origin/fix/<slug>` exists
>      `git merge origin/fix/<slug>` (already-pushed history of this same
>      fix — exempt from the no-commits rule, like a sync merge; if it
>      doesn't merge cleanly, end with `FIX_BLOCKED — fix branch diverged
>      from origin, needs manual resolution` and stop). Then invoke
>      `gitflow` with "sync fix `<slug>`" to bring it up to date with
>      `<integration>`. On an unresolvable conflict, end with `FIX_BLOCKED — fix
>      branch sync conflict with <integration>` and stop.
>    - Fails locally but `git rev-parse --verify --quiet origin/fix/<slug>`
>      succeeds → another device pushed it first: `git checkout -b
>      fix/<slug> origin/fix/<slug>`, then `gitflow` "sync fix `<slug>`",
>      same conflict handling.
>    - Fails both → invoke `gitflow` with "start fix `<slug>`". If it
>      reports a blocker, end with `FIX_BLOCKED — <the gitflow blocker>`
>      and stop.
> B4. `git branch --show-current` must now print exactly `fix/<slug>`. If
>    not, end with `FIX_BLOCKED — could not switch to fix branch` and stop.
>
> **Step 1.5 — Mark the bug In Progress**
> Open `<backlog>/bugs-open.md`, find the `<bug-id>` row, and if its
> **Status** is `⬜ Open` change it to `🔶 In Progress` and save. Leave it
> unchanged if it's already `🔶 In Progress`. If there's no matching row
> (bug tracked ad hoc), skip silently — not an error.
>
> **Step 2 — Reproduce the bug with a FAILING regression test**
> Write the smallest test that fails *because of this bug* and will pass
> once it's fixed. Pick the layer the bug actually lives at, following the
> project's test conventions (see its CLAUDE.md and existing tests), and
> prefer extending an existing test file over creating a new one.
>
> Run just that test and confirm it **fails**, and that the failure is the
> bug described in `<repro-details>` — not an unrelated error, a typo in
> the test, or a missing stub. **Quote the failing assertion or error
> output verbatim in your hand-back** (a compile error is acceptable only
> when the fix adds the API the test uses, and say so). A test you never
> saw fail is not a regression test; the hand-back without that quote is
> treated as blocked.
>
> **If you cannot make it fail** — the behaviour looks correct, or the
> recorded steps are too vague to reproduce — do not invent a fix. End
> with `FIX_BLOCKED — cannot reproduce <bug-id>: <what you tried and what
> you observed>` and stop. Do not change the bug's Status beyond the
> `🔶 In Progress` set in Step 1.5.
>
> **Step 3 — Hand back**
> End with exactly:
> `RED: <the verbatim failing assertion or error line>`
> `REPRO: DONE — test: <path to the test file>`
> `BUG_ID: <bug-id>`

After the subagent completes:
- Contains `FIX_BLOCKED — cannot reproduce`: surface it to the user and
  ask (**AskUserQuestion**, in the orchestrator's own context): "Mark
  `<bug-id>` as ❓ Cannot reproduce?" — options: "Yes — mark it Cannot
  reproduce" (invoke `bugs-archive <bug-id> cannot-repro "<subagent's
  note>"` and stop the workflow), "Keep it open — I'll add repro detail"
  (stop the workflow, leave the row `🔶 In Progress`), "Cancel". Do not
  proceed to Phase 2.
- Contains any other `FIX_BLOCKED`: surface the reason and stop.
- No `RED:` line quoting real failing output → treat as blocked; do not proceed to Phase 2.
- Otherwise: capture the `REPRO: DONE — test: <path>` and `RED:` lines and proceed.
