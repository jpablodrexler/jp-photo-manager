---
name: gitflow
description: Encapsulates the Gitflow branching workflow for this repo (develop as integration branch, main as production branch). TRIGGER when the user asks to start/create a new feature, release, or hotfix branch, when asking to sync/update a feature branch with develop, when asking to merge/finish a feature, release, or hotfix, when asking to tag a release or hotfix after it has been merged, or when asking to clean up/delete already-merged branches. Phrases like "start a new feature", "create a release branch", "start a hotfix", "sync the feature branch", "update my feature branch", "bring the feature branch up to date", "merge the feature", "finish the feature", "merge the release", "finish the hotfix", "tag the release", "clean up the branches", "delete merged branches" all trigger this skill — including a bare request to merge/pull main or develop into a feature branch, which this skill redirects to the Sync feature action (3b) since main is never a valid source for that. Also drafts CHANGELOG.md release notes on the release/hotfix branch itself, before its PR is opened, so the entry ships through normal PR review instead of landing on main after the tag.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.5"
---

Perform one Gitflow action: start a feature/release/hotfix branch, sync an existing feature branch with develop, finish (open a PR for) a feature, finish (open PRs for) a release/hotfix, tag main after a release/hotfix PR has merged, or clean up already-merged feature/release/hotfix branches.

**Input**: The action (`start feature`, `start release`, `start hotfix`, `sync feature`, `finish feature`, `finish release`, `finish hotfix`, `tag release`, `tag hotfix`, `cleanup branches`) plus a name/version (not needed for `cleanup branches`). If the action is ambiguous from the user's request, infer it from phrasing (see TRIGGER examples above) and confirm the target name/version with the user before acting if it wasn't given explicitly.

---

## Branch and tag conventions (fixed for this repo)

| Branch type | Created from | Prefix       | Finishes into      |
|-------------|--------------|--------------|---------------------|
| feature     | `develop`    | `feature/`   | `develop` via PR |
| release     | `develop`    | `release/`   | `main` **and** `develop` via PR |
| hotfix      | `main`       | `hotfix/`    | `main` **and** `develop` via PR |

Version tags on `main` use the `v<major>.<minor>.<patch>` format (e.g. `v2.1.0`), matching this repo's existing tag history.

---

## GitHub access method

This skill needs to open pull requests and check PR merge status on GitHub. Before the first action that touches GitHub in a given invocation (i.e. before 3c, 3d, 3e, or 3f — 3b/sync feature is local-only and never touches GitHub), determine which access method is available:

1. Run `gh --version`. If it succeeds, use the **gh CLI** for every GitHub operation below — it already knows the repo from the current working directory.
2. If `gh --version` fails or `gh` is not found, use the **GitHub MCP tools** (`mcp__github__create_pull_request`, `mcp__github__list_pull_requests`, `mcp__github__pull_request_read`) instead. This is the required path in Claude Code Remote / web sessions, which have no `gh` binary. These tools take explicit `owner`/`repo` parameters — derive them once by running `git remote get-url origin` and parsing the `owner/repo` portion out of the URL (works for the `https://github.com/<owner>/<repo>.git` form, the `git@github.com:<owner>/<repo>.git` form, and the local proxy rewrite used in some sandboxed environments, `.../git/<owner>/<repo>`).
3. If neither is available, stop and tell the user GitHub access isn't configured for this session — do not fall back to raw REST/curl calls.

Wherever a step below says "open a PR" or "check merge status," use whichever method was selected here, per the equivalences below.

### Opening a PR

- **gh CLI**: `gh pr create --base <base> --head <branch> --title "<title>" --body "<description>"`. Capture the printed PR URL.
- **GitHub MCP**: `mcp__github__create_pull_request` with `owner`, `repo`, `head: <branch>`, `base: <base>`, `title: "<title>"`, `body: "<description>"`. The result includes the PR URL and number.

### Checking PR merge status

- **gh CLI**: `gh pr list --state merged --head <branch> --base <base> --json number,mergedAt`. A non-empty result means merged. For a single known PR number, `gh pr view <number> --json state,mergedAt` is equivalent.
- **GitHub MCP**: `mcp__github__list_pull_requests` with `owner`, `repo`, `head: "<owner>:<branch>"` (the `owner:` prefix is required here — GitHub's `head` filter for listing PRs only accepts `owner:branch-name`, never a bare branch name; a bare branch silently matches nothing and would make every merge check falsely report "not merged", unlike `gh pr list --head`, which is a CLI-only convenience that accepts a bare branch name for same-repo PRs), `base: <base>`, `state: "closed"`, then check each returned PR's `merged_at` — a PR that's `closed` but not `merged` doesn't count (GitHub's API has no native "merged" state; `gh pr list --state merged` is itself just wrapping this closed-plus-merged_at check, so this reproduces it exactly). For a single known PR number, `mcp__github__pull_request_read` with `method: "get"`, `owner`, `repo`, `pullNumber` is equivalent to `gh pr view <number> --json state,mergedAt` — check the returned `merged`/`merged_at` field.

---

## Steps

### 1. Determine the action

Map the user's request to one of:
- **start feature** `<name>`
- **start release** `<version>`
- **start hotfix** `<version>`
- **sync feature** (operates on the current feature branch, or one named explicitly — brings it up to date with `develop`)
- **finish feature** (operates on the current feature branch, or one named explicitly)
- **finish release** (operates on the current release branch, or one named explicitly)
- **finish hotfix** (operates on the current hotfix branch, or one named explicitly)
- **tag release** `<version>` / **tag hotfix** `<version>`
- **cleanup branches** (no name/version — operates across all `feature/*`, `release/*`, `hotfix/*` branches in the repo)

If the name/version wasn't given and can't be inferred from the current branch, ask for it. For **sync feature** / **finish feature** specifically, if no name is given explicitly, infer it from the current branch name by stripping the `feature/` prefix (e.g. current branch `feature/skills-gitflow` → name `skills-gitflow`) — no need to ask the user in this case.

For **sync feature** / **finish feature** / **finish release** / **finish hotfix** with an explicit name/version given (rather than inferred from the current branch), accept either form — the bare name (`skills-gitflow`) or the full branch name (`feature/skills-gitflow`) — and normalize to the full `<prefix>/<name-or-version>` form before using it as `<branch>` in the steps below: if the given value doesn't already start with `feature/`, `release/`, or `hotfix/`, prepend the prefix matching the action.

The same normalization applies to **start feature** / **start release** / **start hotfix**: if the given `<name>`/`<version>` already starts with `feature/`, `release/`, or `hotfix/` (e.g. the user says "start a feature called `feature/foo`"), strip that prefix before using the value in step 3a's `git checkout -b <prefix>/<name-or-version>` — otherwise the branch would end up double-prefixed (`feature/feature/foo`).

### 2. Check working tree state

Run `git status`. If there are uncommitted changes, stop and tell the user — do not stash or discard automatically. (For **cleanup branches**, this only matters if the current branch is itself a deletion candidate — see 3f. For **start feature** specifically, when the current branch already starts with `feature/`, resolve 3a's branch-continuation question first, before applying this check — it may turn out this check doesn't even apply, since continuing on the current branch involves no checkout and tolerates an uncommitted tree just fine.)

### 3a. Start feature / release / hotfix

**For `start feature` only — if already on a feature branch:** before anything else in this action (including the working-tree check in step 2 above — that check assumes a branch switch is about to happen, which may turn out not to be true here), check the current branch: `git branch --show-current`. If it already starts with `feature/`, this is a fork in the road for the user to resolve, not something to decide silently — jumping back to `develop` and spinning up a new branch could sideline in-progress work the user isn't ready to commit yet. Ask the user (via **AskUserQuestion**) whether they want to:

- **Continue working on the current feature branch** instead of starting a separate one — appropriate when the new ask is really an extension of what's already in flight here. If chosen: skip the rest of this action entirely — no base checkout, no new branch, and step 2's working-tree check does not apply, since nothing is being switched. Then, if the current branch has **not** been pushed to the remote yet (`git rev-parse --verify --quiet origin/<current-branch>` returns nothing), ask a second, separate question: whether to **rename** it to better reflect the now-combined scope (e.g. `feature/albums-sort` → `feature/albums-sort-and-filter`). Only offer this for an unpushed branch — renaming one that's already on the remote (and possibly backing an open PR) would orphan that PR's `head` ref, so leave a pushed branch's name alone even if asked, and say why. If the user wants the rename, confirm the exact new name with them, then run `git branch -m <old-name> <new-name>` (a local-only rename; nothing is pushed as a side effect). Report the final state — still on `<branch>`, or renamed to `<new-name>` — and stop; no further steps in this action run.
- **Start a brand-new feature branch as normal**, from `develop` — leaving the current feature branch untouched. If chosen, proceed with steps 1–5 below exactly as written, with step 2's working-tree check now applying in the usual way (the checkout to `develop` is about to happen for real).

Never pick either option without asking — always let the user decide.

1. Determine the base branch: `develop` for feature/release, `main` for hotfix.
2. `git checkout <base>`
3. `git pull origin <base>` — fail loudly if this doesn't fast-forward cleanly (don't force).
4. `git checkout -b <prefix>/<name-or-version>` where prefix is `feature`, `release`, or `hotfix`.
5. Report the new branch name and its base. Do not push automatically — let the user decide when to push.

### 3b. Sync feature (update from develop)

Brings an existing feature branch up to date with `develop` mid-flight — e.g. after a release has merged into both `main` and `develop` and the feature branch needs those changes before continuing work. **`develop` is the only sanctioned source for this** — see the guardrail below on why `main` is never used here, even though right after a release merge `main` and `develop` are momentarily identical and it's tempting to treat either as "the latest."

Preconditions:
- Current branch (or the one named explicitly) must start with `feature/`.

Steps:
1. `git fetch origin develop`
2. `git checkout <branch>` if not already on it.
3. `git merge origin/develop`. If it doesn't merge cleanly, stop and report the conflicting files — resolve conflicts together with the user, never auto-resolve or force through.
4. Report the result (already up to date / fast-forwarded / merge commit created / conflicts needing resolution).

This step never touches `main`. Do not push automatically — let the user decide when to push the merge commit.

### 3c. Finish feature (open PR)

Preconditions:
- Current branch (or the one named explicitly) must start with `feature/`.
- Branch must exist on the remote — if not, push it first: `git push -u origin <branch>` (confirm with the user before pushing if this is the first push of the branch).

Steps:
1. Build the PR description (see "Writing the PR description" below).
2. Open a PR into `develop` per "Opening a PR" above, with `base: develop`, `head: <branch>`, title `"Feature <name>"`, and the description from step 1.
3. Report the PR URL to the user.

This step **only opens the PR** — it does not merge it. Merging goes through normal GitHub review. Do not auto-merge. Feature branches never target `main` directly and are never tagged — only `release/*` and `hotfix/*` branches that land on `main` get a version tag.

### 3d. Finish release / Finish hotfix (open PRs)

Preconditions:
- Current branch (or the one named explicitly) must start with `release/` or `hotfix/` matching the action.

Steps:
1. Draft or update the `CHANGELOG.md` entry for this release/hotfix on the current `release/`/`hotfix/` branch itself, per "Drafting release notes" below, using the version embedded in the branch name (e.g. `release/v2.3.0` → `v2.3.0`). Do this **before** the branch is pushed and before either PR is opened. This is the one thing this step changed about the old flow: the changelog entry used to get drafted after the tag, which meant committing it directly to `main` with no PR/review in front of it. Drafting it here instead means it ships as an ordinary commit on the branch under review, like everything else in the release/hotfix — no more out-of-band `main` commits.
2. **Run the maintained E2E suite** (see the `e2e-suite` skill) before pushing — a release/hotfix is exactly the point where full-app regression coverage matters most, and this is the one point in this whole workflow that gates on it. Check whether the full stack is up (Postgres/MongoDB/Redis/Kafka, backend, frontend — `e2e-testing` skill §1–§3):
   - **If it's up**: invoke the `e2e-suite` skill (or run `cd JPPhotoManagerWeb/frontend && npm run test:e2e` directly) and wait for it to finish. If every test passes, continue to step 3. If any test fails, **stop here** — do not push the branch or open either PR with known-failing E2E coverage. Report the failure(s) to the user; once they're fixed (by you or the user), re-run the suite before continuing. A flaky-looking single failure should be re-run once before concluding it's real (watch specifically for the suite's own login-rate-limit note in `e2e-suite` §1 — a `429` from back-to-back runs isn't a real regression), not dismissed outright.
   - **If it isn't**: the suite can't run without a live backend. Tell the user plainly that E2E verification is being skipped for this reason (never skip it silently) and ask (**AskUserQuestion**) whether to bring the stack up first (`docker compose up -d db kafka redis mongo`, then start the backend/frontend) or proceed without it.
3. Push the branch (including the changelog commit from step 1, if the user chose to write one): `git push -u origin <branch>` if the branch doesn't exist on the remote yet, or a plain `git push` if it does (e.g. this branch was already pushed earlier in the session and step 1 just added a new commit on top) — confirm with the user before the first push of the branch either way.
4. Build the PR description (see "Writing the PR description" below) — the same description is used for both PRs since they carry the same commits. The description's diff/commit inspection will naturally include the changelog commit from step 1; that's fine, it's a real part of what the branch changed. If step 2 ran the E2E suite, note that it passed in the PR's Test plan section.
5. Open a PR into `main` per "Opening a PR" above, with `base: main`, `head: <branch>`, and title `"<Release|Hotfix> <version>"`.
6. Open a PR into `develop` per "Opening a PR" above, with `base: develop`, `head: <branch>`, and the same title.
7. Report both PR URLs to the user.

This step **only opens the PRs** — it does not merge them. Merging goes through normal GitHub review. Do not auto-merge.

Note in the summary: once the PR into `main` is merged, run the **tag release/hotfix** action to tag `main` with the version. The changelog entry is already merged at that point via step 1 above — 3e below only tags; it drafts release notes itself solely as a fallback for the rare case where step 1 was declined or the branch was tagged without ever going through this step.

### Writing the PR description

Every PR opened by this skill needs a description detailed enough for a reviewer to understand what's actually changing, not a placeholder:

1. Refresh the base branch first: `git fetch origin <base>`. Local `<base>` (`develop` or `main`) was only guaranteed fresh at branch-creation time (3a's `git pull`) — by the time you're finishing a feature, other PRs may have merged into it since, and diffing against a stale local branch skews what "new" means. Do not run `git pull origin <base>` here — that would switch/update the checked-out branch; a plain `fetch` is enough since the next step diffs against `origin/<base>` directly.
2. Inspect the actual content of the change: `git log origin/<base>..<branch> --oneline` for the commit list, and `git diff origin/<base>..<branch> --stat` for the files touched — against the freshly fetched `origin/<base>`, not local `<base>`. Read individual commit messages (and diffs, for anything non-obvious) rather than guessing from branch/file names alone.
3. Write the PR body using this structure:
   - **Summary** — 2-4 bullet points describing the substantive changes (what behavior changed and why, not a mechanical commit list).
   - **Changes** — grouped by area/component if the diff spans multiple concerns (e.g. domain, UI, migrations).
   - **Test plan** — how this was verified (tests run, manual checks) if that's discoverable from the commits; omit rather than fabricate if it isn't.
4. Do not use a generic placeholder body like `"..."`, `"Merge <branch>"`, or the PR title repeated — the description must reflect the real diff.

### 3e. Tag release / Tag hotfix

This step must run **after** the PR into `main` has actually been merged — it tags the resulting commit on `main`, matching Gitflow's convention of tagging production merges.

1. Verify the merge happened: check merge status per "Checking PR merge status" above, with `base: main`, `head: <release/hotfix-branch>`. If no merged PR is found, stop and tell the user to merge the PR into `main` first — do not tag speculatively.
2. `git checkout main`
3. `git pull origin main`
4. Confirm the tag name with the user: `v<version>` (strip any `release/`/`hotfix/` prefix and leading `v` from the input, then re-add a single `v`).
5. Check the tag doesn't already exist: `git tag --list v<version>`.
6. Ask the user to confirm before pushing the tag (this is a shared, hard-to-reverse action visible to everyone with repo access).
7. `git tag -a v<version> -m "<Release|Hotfix> <version>"`
8. `git push origin v<version>`
9. Report the pushed tag.
10. Check whether `CHANGELOG.md` already documents this version — look for a `v<version>` heading (`grep`/read the file). This should normally already be true: 3d drafts and merges the entry as part of the release/hotfix PR, before this tagging step ever runs.
    - **If found**: nothing further to do — report that release notes for this version were already included in the merge. Do not draft a second entry.
    - **If not found** (3d's draft was declined at the time, or `main` was tagged without going through 3d at all): fall back to drafting it now per "Drafting release notes" below, called from this post-tag context — the resulting commit has no PR/review step in front of it, so treat it with the heavier care described there for this call site.

### Drafting release notes

Draft a changelog entry for a release/hotfix version. This runs from two possible call sites, and which one you're in changes where the commit lands and how much confirmation weight that carries:

- **Normally, from 3d step 1**, on the `release/`/`hotfix/` branch itself, before its PR is opened — the commit is just another commit on a branch about to go through normal PR review.
- **As a fallback, from 3e step 10**, directly on `main`, only when no entry exists yet for the version being tagged — there is no PR/review step protecting `main`, so this path needs the heavier treatment noted in step 6 below.

Steps (the same either way except where noted):

1. Determine the version and the commit range to draft from:
   - **From 3d**: the version is embedded in the current branch name (`release/v2.3.0`/`hotfix/v2.3.0` → `v2.3.0`); the tag for it doesn't exist yet.
   - **From 3e's fallback**: the version is the `<version>` already confirmed and tagged in step 4 of that section.
2. Find the previous tag: `git tag --sort=-v:refname`.
   - **From 3d** (tag doesn't exist yet): take the first (most recent) entry in that list as the previous tag.
   - **From 3e's fallback** (tag already exists, since it was just pushed in step 8): take the entry immediately after `v<version>` in the list (i.e. the one before it chronologically).
   - Either way, skip this step and treat everything as new if there is no usable previous tag (this is the first release ever).
3. List the commits it contains:
   - **From 3d**: `git log <previous-tag>..HEAD --oneline`, where `HEAD` is the tip of the current release/hotfix branch.
   - **From 3e's fallback**: `git log <previous-tag>..v<version> --oneline`.
   - Read individual commit messages — don't just dump the oneline list verbatim, since commit subjects vary in how self-explanatory they are.
4. Group the entries into **Added**, **Changed**, **Fixed**, and **Removed** (omit any category with nothing in it) based on what each commit actually did, not its raw message text — a commit titled "update X" might be a fix, a feature, or a removal depending on the diff; check the diff for anything ambiguous rather than guessing from the subject line.
5. Check whether `CHANGELOG.md` exists at the repo root.
   - **If it exists**: read its existing format (heading style, section ordering) and match it. Insert the new version's section in the same position newer entries already occupy (top-of-file "Keep a Changelog" style is the common convention — follow whatever this file already does, don't impose a different structure).
   - **If it doesn't exist**: don't create it silently — this is a new top-level file and a one-time structural decision the user should make, not something to add as a side effect of finishing a release or tagging one. Show the drafted entry in chat instead and ask whether to create `CHANGELOG.md` now. If they decline, stop here — the drafted entry stays in the chat response, not in the repo (from 3d, this means the branch proceeds to its PR without a changelog commit; from 3e's fallback, it means `main` is left without one).
6. If writing to the file (existing or newly approved), stage and show the diff, then ask for explicit confirmation before committing:
   - **From 3d** (on the release/hotfix branch): confirm before committing, the same as any other commit this skill makes on a branch under review — `git add CHANGELOG.md && git commit -m "docs: add v<version> release notes"`. No separate push confirmation is needed here beyond 3d step 2's existing branch-push confirmation, since the changelog commit is just one of the commits that push carries.
   - **From 3e's fallback** (on `main`): treat this with the same care as the tag push in 3e — a commit here lands directly on `main` with no PR/review step in front of it. `git add CHANGELOG.md && git commit -m "docs: add v<version> release notes"`, then ask **separately** whether to push it (`git push origin main`) — don't bundle the commit and push confirmations into one yes, since a local commit is still easy to amend/undo but a push is not.
7. Report what was recorded (or, if the user declined, that the drafted notes were not saved anywhere).

### 3f. Cleanup branches

Finds `feature/*`, `release/*`, and `hotfix/*` branches that have already been merged, and deletes only the ones the user explicitly confirms — never speculatively, never with `-D`/`--force`.

1. `git fetch origin --prune` — sync remote-tracking refs so branches already deleted on GitHub (e.g. auto-deleted on merge) aren't listed as candidates.
2. Enumerate candidates: local branches via `git branch --list 'feature/*' 'release/*' 'hotfix/*' --format='%(refname:short)'`, remote via `git branch -r --list 'origin/feature/*' 'origin/release/*' 'origin/hotfix/*' --format='%(refname:short)'` (strip the `origin/` prefix). Union the two lists by name — note whether each exists locally, remotely, or both.
3. For each candidate branch, determine merged status with a direct check (per the repo-state guardrail below — never infer this from timing or narrative):
   - `feature/<name>`: merged if checking merge status per "Checking PR merge status" above (with `head: <branch>`, `base: develop`) returns at least one merged result.
   - `release/<version>` or `hotfix/<version>`: merged if **both** hold — checking merge status per "Checking PR merge status" above (with `head: <branch>`, `base: main`) returns a merged result, **and** `git merge-base --is-ancestor origin/<branch> origin/develop` exits 0 (confirms `develop` also has the commits, whether via its own merged PR or because `develop` already contained them).
   - A branch with no merged PR found this way is **not** a candidate — leave it alone and don't list it, even if it looks stale. This only recognizes merges that went through a PR; branches merged some other way won't be flagged, which is the safe direction to be wrong in.
4. If no merged branches are found, report that and stop — nothing to clean up.
5. Print every merged candidate to the user as plain text (not through `AskUserQuestion`'s options — a repo that's accumulated several merged-but-undeleted branches can easily produce more candidates than the tool's 4-option-per-question limit allows): branch name, type (feature/release/hotfix), and where it exists (local/remote/both). Then, if there's more than one, use **AskUserQuestion** with a small fixed set of options — "Delete all listed", "Delete specific ones (I'll list the names)", "Cancel" — and resolve "specific ones" from the user's free-text follow-up matched against the printed list, rather than trying to enumerate every branch as its own option. If there's exactly one candidate, a plain yes/no/cancel confirmation is enough.
6. For whichever branches the user selected, ask whether to delete the **local** branch, the **remote** branch, or **both** — per branch if they exist in different places, or once for the whole batch if that's simpler for the user to answer.
7. Do one final explicit confirmation of exactly what's about to be deleted (branch names × local/remote) before running anything — deletion of a shared remote branch is hard to reverse.
8. Delete only what was confirmed:
   - Local: `git branch -d <branch>` — never `-D`. If the branch to delete is the current branch, `git checkout` its base first (`develop` for feature/release, `main` for hotfix) before deleting.
   - Remote: `git push origin --delete <branch>`. If the ref is already gone (common — GitHub auto-deletes on merge), report that rather than treating it as an error.
9. Report exactly what was deleted (and what was already gone / skipped).

---

## Guardrails

- Never force-push, force-create branches over existing ones, or force-delete a branch (`git branch -D`, `git push --force`). The only sanctioned branch deletion is the **Cleanup branches** action, and only for branches confirmed merged per its steps — never delete speculatively.
- Cleanup branches never deletes anything without the user explicitly confirming: which branches (from the listed candidates), and for each, whether to delete local, remote, or both. Do not skip either confirmation step or bundle them into a single implicit approval.
- Never merge a PR automatically — "finish feature/release/hotfix" only creates the PR(s); merging is a human review step.
- Never open a PR with a placeholder or generic body — always derive the description from the actual commits/diff on the branch, per "Writing the PR description" above.
- Never tag `main` without first confirming (via "Checking PR merge status" above) that the corresponding release/hotfix PR into `main` has actually merged.
- Never tag without explicit user confirmation immediately before the `git push origin <tag>` — pushing a tag is visible to the whole team and awkward to undo.
- Never create `CHANGELOG.md` silently, on either call site — see "Drafting release notes". From 3d (the normal path, on the release/hotfix branch before its PR), a changelog commit is an ordinary commit under review, so a single commit confirmation is enough. From 3e's fallback (writing directly to `main`, only when 3d's draft was skipped or declined), there is no PR/review step protecting it, so commit and push need separate explicit confirmations, with the same care as the tag push.
- Never state whether a PR has or hasn't merged based on local git state (e.g. `git log`/`git diff` against a local branch, or `git pull` reporting "already up to date"). Local branches can be stale or a PR can be merged manually on GitHub outside of any local fetch. Always check directly per "Checking PR merge status" above before reporting merge status to the user.
- Never shell out to raw GitHub REST/curl calls as a substitute for `gh` or the GitHub MCP tools — use whichever of those two was selected under "GitHub access method" above.
- More generally, never assert any repo-state claim (a branch contains/is missing a given change, two branches have diverged or are identical, a commit is or isn't an ancestor of another) from a narrative of prior actions or timing assumptions. Run the direct check in the moment: `git log A..B --oneline` / `git diff A..B --stat` to compare branches, `git branch --contains <sha>` to check ancestry. A fix scoped to one specific claim (e.g. PR-merged status) does not generalize on its own — re-verify every distinct kind of claim the same way.
- If the working tree has uncommitted changes at the start of any action, stop and report — never stash or discard automatically.
- If `git pull` on the base branch doesn't fast-forward cleanly, stop and report — never force-merge or rebase automatically.
- `feature/*` branches only ever merge back into `develop`, never into `main` directly, and are never tagged — only `release/*` and `hotfix/*` branches that land on `main` get a version tag.
- **Never silently switch away from an already-checked-out `feature/*` branch to start a new one.** `start feature` always asks first when the current branch already starts with `feature/` — per 3a's opening step — whether to continue on it or branch fresh from `develop`, and (only if the branch is unpushed) whether to rename it for the combined scope. Both are the user's call, never an automatic decision.
- **Never push a `release/`/`hotfix/` branch or open its PRs while the E2E suite has a known failure.** Finish release/hotfix's step 2 gates on this: a failing run stops the action there — fix and re-run before continuing to push/PR. Skipping the suite entirely (stack not up) is allowed, but only after telling the user and letting them choose, never silently.
- **Never merge, rebase, or pull `main` into a `feature/*` branch, for any reason.** `develop` is the only sanctioned upstream for a feature branch's content — use the **Sync feature** action (3b), which merges `origin/develop` and nothing else. This holds even right after a release/hotfix PR has merged into both `main` and `develop`, when the two are momentarily identical and pulling from either looks equivalent in the moment — `main` is still the wrong branch to name in the command, because the next release may fork them apart again before the feature branch is finished, silently carrying content into the feature branch that never went through `develop`.
