# Feature Development — Phase 1 — Propose

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 1 — Propose (Subagent 1)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (this phase's result gates every later phase —
see the foreground guardrail in `../SKILL.md`), with the following prompt (substitute
`<change-name>` with the value confirmed in Phase 0):

> Perform these steps in sequence. Do NOT skip any step.
>
> The change name has already been selected and confirmed with the user by
> the orchestrator, before you were spawned: `<change-name>`. Do not invoke
> `features-next` yourself, and do not attempt to select, change, or
> re-confirm a feature on your own — that decision is final and out of
> scope for you. Proceed directly to Step 1.5 using this change name.
>
> **Step 1.5 — Create the feature branch**
>
> **Batch mode (`--skip-branch-setup` was passed):** skip everything else in
> this step. Run `git branch --show-current` once and confirm it is not
> `main` or `develop` — if it is, end your response with `PROPOSE_BLOCKED —
> batch mode requires an existing feature branch already checked out, not
> main/develop` and stop. Otherwise proceed to Step 1.6 below (not Step 2
> directly — marking the feature In Progress applies in batch mode too)
> using whatever branch is currently checked out. Do not check for
> uncommitted changes here — an orchestrator running several features on
> one branch without committing between them will always have a dirty tree
> by the second feature onward, and that's expected, not a blocker. Do not
> create, switch, or sync any branch in this mode; the caller is
> responsible for branch state.
>
> **Normal mode (no flag passed):** before creating or modifying any file in
> the repository (including SDD artifacts in Step 3 below), make sure work
> happens on a dedicated branch cut from `develop`. Check resume status
> *before* checking for uncommitted changes — not the other way around: a
> resumed session (e.g. this skill being re-run after an interruption
> mid-Phase-2) is typically already checked out on `feature/<change-name>`
> with the in-progress implementation sitting there uncommitted, since this
> workflow never commits until a human reviews it. That's expected,
> resumable state, not a blocker — but if the uncommitted-changes check ran
> first, it would misfire on exactly that state and block the resume before
> the already-on-the-right-branch check ever got a chance to short-circuit
> it.
>
> 0. Run: `git fetch origin --prune`. This refreshes remote-tracking refs
>    (`origin/feature/*`) *before* any of the checks below reason about
>    branch existence — this repo is worked from more than one device, and
>    without a fresh fetch a branch (or new commits on a branch) pushed
>    from another device since this device's last fetch would be invisible
>    to steps 1 and 3 below, risking either a second, divergent
>    `feature/<change-name>` branch getting created, or a stale local copy
>    silently shadowing newer work that already exists on the remote.
>    `--prune` also drops remote-tracking refs for branches already deleted
>    on the remote, matching `gitflow`'s own cleanup-branches convention.
> 1. Run: `git branch --show-current`.
>    - If the output is already exactly `feature/<change-name>`: we're
>      already on the target branch — this is a resume. Uncommitted
>      changes here are expected. Before skipping ahead, check whether
>      another device has pushed commits to this same branch that this
>      local checkout doesn't have yet: run
>      `git rev-list HEAD..origin/feature/<change-name> --count` (if
>      `origin/feature/<change-name>` doesn't exist — i.e. this branch was
>      never pushed — treat the count as `0`, nothing to reconcile).
>      - If the count is `0`: nothing to reconcile — skip steps 2 and 3
>        below entirely and proceed straight to Step 1.6 of this prompt.
>      - If the count is greater than `0`: another device has pushed work
>        to this branch that isn't reflected in this local, uncommitted
>        checkout. Merging it now risks colliding with those uncommitted
>        local changes, which is not something to resolve silently. End
>        your response with `PROPOSE_BLOCKED — origin/feature/<change-name>
>        has <N> commit(s) not present locally (likely pushed from another
>        device); commit or stash local changes, then merge/rebase
>        manually before continuing` and stop.
>    - Otherwise, continue to step 2 below.
> 2. Run: `git status --porcelain`
>    If this prints anything (uncommitted changes present), end your
>    response with `PROPOSE_BLOCKED — uncommitted changes present, cannot
>    create feature branch` and stop. This check only applies here — we're
>    about to check out `develop` and either cut a new branch from it or
>    switch to an existing one, both of which need a clean tree; it does
>    not apply once step 1 has already confirmed we're on the target branch.
> 3. Run: `git rev-parse --verify --quiet feature/<change-name>`
>    - If it prints a commit hash, the branch already exists locally but
>      isn't currently checked out — this is a resume of a change that's
>      been sitting idle, possibly while other work (including a release,
>      or further commits pushed to this same branch from another device)
>      landed in the meantime: run `git checkout feature/<change-name>`.
>      Then, if `git rev-parse --verify --quiet origin/feature/<change-name>`
>      succeeds, run `git merge origin/feature/<change-name>` to pick up
>      any commits pushed from another device before this local branch was
>      checked out — this merges already-pushed history of this same
>      change, not new uncommitted implementation, so — like a sync-feature
>      merge from `develop` — it's exempt from this workflow's "no git
>      commits" guardrail (see the guardrails section). If this merge
>      doesn't complete cleanly, end your response with
>      `PROPOSE_BLOCKED — feature branch has diverged from
>      origin/feature/<change-name>, needs manual resolution` and stop.
>      Then use the Skill tool to invoke `gitflow` with the action "sync
>      feature `<change-name>`" to bring it up to date with `develop`
>      before continuing. `develop` is the *only* branch this may ever be
>      synced from — never `main`, even though right after a release merges
>      into both, they're momentarily identical and it's tempting to treat
>      either as equivalent; see `gitflow`'s guardrails for why that's
>      still wrong. If `gitflow` reports a merge conflict it can't resolve
>      automatically, end your response with `PROPOSE_BLOCKED — feature
>      branch sync conflict with develop, needs manual resolution` and
>      stop. Otherwise, if it reports any other blocker, end your response
>      with `PROPOSE_BLOCKED — <the gitflow blocker>` and stop.
>    - If it fails locally but
>      `git rev-parse --verify --quiet origin/feature/<change-name>`
>      succeeds: the branch has never been checked out on this device but
>      already exists on the remote — this is the case where another
>      device started (and pushed) this same feature branch first. Do
>      **not** invoke `gitflow`'s "start feature" action here — that always
>      branches fresh from `develop` and would silently orphan the work
>      already sitting on the remote branch. Instead, check it out tracking
>      the remote directly: `git checkout -b feature/<change-name>
>      origin/feature/<change-name>`. Then use the Skill tool to invoke
>      `gitflow` with the action "sync feature `<change-name>`" to bring it
>      up to date with `develop`, exactly as the "already exists locally"
>      case above does, applying the same conflict handling.
>    - If it fails both locally and on the remote (the branch doesn't exist
>      anywhere yet — the common case, no other device has started this
>      change): use the Skill tool to invoke `gitflow` with the action
>      "start feature `<change-name>`". It checks out `develop`, pulls the
>      latest, and creates `feature/<change-name>` from it. If it reports a
>      blocker (e.g. `develop` doesn't fast-forward cleanly), end your
>      response with `PROPOSE_BLOCKED — <the gitflow blocker>` and stop.
> 4. Run `git branch --show-current` and confirm the output is exactly
>    `feature/<change-name>` before proceeding. If it is not, end your
>    response with `PROPOSE_BLOCKED — could not switch to feature branch`
>    and stop.
>
> **Step 1.6 — Mark the feature In Progress**
> Both the batch-mode and normal-mode paths above reach this point once the
> branch to work on is ready. Before doing anything else, reflect that
> development has actually started: open
> `JPPhotoManagerWeb/docs/backlog/features-planned.md`, find the row in the
> `## Feature List` table whose `Change name` column (backtick-wrapped)
> matches `<change-name>` (columns are always addressed by header name,
> never by position — the planned table's column order is `# | Change name |
> Priority | Schema Change | Effort | Area | Summary | Brief | SDD Artifacts |
> Implementation`), and if its `Implementation` column shows
> `⬜ Pending`, change it to `🔶 In Progress` and save the file. If the row
> already shows `🔶 In Progress` (e.g. this is a resume of a previously
> interrupted run) or `✅ Implemented`, leave it unchanged — do not
> overwrite `✅ Implemented`. If no matching row exists (e.g. the change was
> proposed ad hoc, outside the tracked backlog), skip this silently — it is
> not an error. This runs before Step 2's artifact check so a change that
> still needs `openspec-propose` is marked in progress too, not only one
> that already has artifacts.
>
> **Step 2 — Check whether SDD artifacts exist**
> Run:
>
> ```
> openspec status --change "<change-name>" --json
> ```
>
> Parse the JSON. Find the `applyRequires` array and check whether every
> artifact ID in that array has `"status": "done"` in the `artifacts` list.
>
> - If **all** `applyRequires` artifacts are `done`: artifacts are ready —
>   skip Step 3.
> - If **any** are not `done`: artifacts are missing — proceed to Step 3.
>
> **Step 3 — Create missing artifacts (if needed)**
> The feature's full brief is the input `openspec-propose` turns into the
> SDD spec, so hand it over explicitly instead of letting `openspec-propose`
> ask for a description:
>
> 1. Open `JPPhotoManagerWeb/docs/backlog/features-planned.md` and find the
>    row in the `## Feature List` table whose `Change name` column
>    (backtick-wrapped) matches `<change-name>`. Columns are read by header
>    name, never by position.
> 2. **If a planned row exists**: take its `#` and build the brief file path
>    `JPPhotoManagerWeb/docs/backlog/features/NNN-<change-name>.md` (`NNN` =
>    the number zero-padded to three digits, e.g. `054`). Read that file;
>    everything after the `# Feature N — <change-name>` H1 line, with
>    surrounding blank lines trimmed, is the brief. If the file is missing
>    or the brief is empty, end your response with
>    `PROPOSE_BLOCKED — brief file JPPhotoManagerWeb/docs/backlog/features/NNN-<name>.md missing or empty`
>    (substituting the real number and name) and stop.
> 3. Use the Skill tool to invoke `openspec-propose <change-name>`, giving it
>    the brief verbatim and in full as the description of what to build
>    (e.g. "Description of what to build for `<change-name>`:" followed by
>    the brief text), so it never has to ask the user for one. The brief
>    file is the spec input; never pass the row's one-line `Summary` cell,
>    which exists only for human skimming and `features-next`'s display.
> 4. **If no planned row exists** (an ad hoc change proposed outside the
>    tracked backlog, or one already moved to `features-implemented.md`):
>    there is no brief to read — skip the file lookup in step 2 and invoke
>    `openspec-propose <change-name>` exactly as before, with no
>    description.
>
> Wait for it to complete. If it fails or reports an error, end your
> response with `PROPOSE_BLOCKED — <brief reason>` and stop.
> After it completes, re-run `openspec status --change "<change-name>" --json`
> and confirm every artifact ID in `applyRequires` now has `"status": "done"`.
> If any are still missing, end your response with
> `PROPOSE_BLOCKED — artifacts incomplete after propose` and stop.
>
> Now that artifacts are confirmed `done`, reflect that in the backlog: open
> `JPPhotoManagerWeb/docs/backlog/features-planned.md`, find the row in the
> `## Feature List` table whose `Change name` column (backtick-wrapped)
> matches `<change-name>`, and if its `SDD Artifacts` column shows `⬜ Pending`,
> change it to `✅ Created` and save the file (again addressing the cell by
> its header name). If the row already shows `✅ Created`, leave it
> unchanged. If no matching row exists (e.g. the change was proposed ad hoc,
> outside the tracked backlog), skip this silently — it is not an error. The
> brief file never changes; only the row's `SDD Artifacts` cell does.
>
> **Step 3.5 — Check for a newly discovered hard dependency**
> The artifacts just confirmed `done` (proposal.md, and design.md if
> present) may reveal, only now that the change has actually been designed,
> that `<change-name>` needs something another *not-yet-implemented* backlog
> feature provides (a table/column, an endpoint, a service method, a Kafka
> topic) — a dependency nobody could have known about back when this
> feature was recommended, because it only became apparent once the design
> was written. Left undetected, this surfaces later — mid-Phase-2
> implementation, or worse, after review — as a surprise blocker. Catch it
> here instead, before any implementation work starts.
>
> 1. Read proposal.md (and design.md if it exists) for `<change-name>`.
> 2. Read `JPPhotoManagerWeb/docs/backlog/features-planned.md`'s
>    `## Feature List` table and its `### Hard implementation dependencies`
>    subsection.
> 3. Determine whether the design depends on something owned by a
>    *different* feature row that is still `⬜ Pending` or `🔶 In Progress`
>    (not `<change-name>` itself, and not something already shipped —
>    shipped prerequisites are fine and not what this step is checking for).
> 4. **If no such dependency exists, or it's already recorded** in the
>    `### Hard implementation dependencies` subsection: nothing to do,
>    continue to Step 4.
> 5. **If a genuine, not-yet-recorded dependency is found**: add a new
>    `**Feature <this feature's #> → Feature <prerequisite's #>**
>    (prerequisite still pending)` block to the
>    `### Hard implementation dependencies` subsection, mirroring the exact
>    format of the existing entries there (one short sentence naming what's
>    needed and why), and save the file. Then end your response with
>    `PROPOSE_NEEDS_DEPENDENCY_DECISION — <change-name> (#<this feature's #>)
>    depends on <prerequisite-name> (#<prerequisite's #>), which is not yet
>    implemented; dependency recorded in
>    JPPhotoManagerWeb/docs/backlog/features-planned.md` and stop — do not
>    proceed to Step 4. This is not a `PROPOSE_BLOCKED` failure (nothing
>    went wrong — proposing surfaced real information); the orchestrator
>    handles it as a decision point, below.
>
> **Step 4 — Return the change name**
> End your response with exactly this line so the orchestrator can extract it:
> `CHANGE_NAME: <change-name>`

After the subagent completes:

- If the response contains `PROPOSE_BLOCKED`: surface the reason to the user and stop.
- If the response contains `PROPOSE_NEEDS_DEPENDENCY_DECISION`: this is a
  decision point, not a failure — handle it here in the orchestrator's own
  context (never delegate this confirmation to a subagent, for the same
  reason Phase 0's feature selection never is: reliable `AskUserQuestion`
  access is only guaranteed in the foreground). Surface the dependency
  found (which feature, which prerequisite, why) and use
  **AskUserQuestion** with:
  - **Question**: "`<change-name>` turned out to depend on
    `<prerequisite-name>` (#<prerequisite's #>), which isn't implemented
    yet. How do you want to proceed?"
  - **Options**:
    1. "Implement `<prerequisite-name>` first (recommended)" — stop this
       workflow run here (the dependency is already recorded in
       `features-planned.md`, so a future `features-next` run will
       correctly treat `<change-name>` as blocked and can recommend the
       prerequisite instead); tell the user to re-run `feature-development`
       once ready, either unnamed or naming the prerequisite directly.
    2. "Proceed with `<change-name>` anyway" — only if the user judges the
       dependency isn't actually a hard blocker for a usable first version;
       continue to Phase 2 and the rest of this workflow as normal. The
       dependency note stays recorded regardless — it was real, even if
       not blocking today.
    3. "Cancel" — stop the entire workflow here.
  - If the user picks option 1 or 3, stop the whole `feature-development`
    run now — do not proceed to Phase 2. Do not revert the dependency note
    just written to `features-planned.md`; it's accurate information
    regardless of what happens next.
- Otherwise: confirm the `CHANGE_NAME:` line matches the value Phase 0
  already confirmed with the user (it should always match, since Subagent 1
  was given that exact name and never re-selects one — this is a sanity
  check, not a new decision point). If it somehow doesn't match, treat that
  as a bug in the workflow: stop and surface both values to the user rather
  than silently trusting either one.

---
