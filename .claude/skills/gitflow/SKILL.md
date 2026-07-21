---
name: gitflow
description: Encapsulates the Gitflow branching workflow for this repo (develop as integration branch, main as production branch). TRIGGER when the user asks to start/create a new feature, release, or hotfix branch, when asking to merge/finish a feature, release, or hotfix, when asking to tag a release or hotfix after it has been merged, or when asking to clean up/delete already-merged branches. Phrases like "start a new feature", "create a release branch", "start a hotfix", "merge the feature", "finish the feature", "merge the release", "finish the hotfix", "tag the release", "clean up the branches", "delete merged branches" all trigger this skill.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Perform one Gitflow action: start a feature/release/hotfix branch, finish (open a PR for) a feature, finish (open PRs for) a release/hotfix, tag main after a release/hotfix PR has merged, or clean up already-merged feature/release/hotfix branches.

**Input**: The action (`start feature`, `start release`, `start hotfix`, `finish feature`, `finish release`, `finish hotfix`, `tag release`, `tag hotfix`, `cleanup branches`) plus a name/version (not needed for `cleanup branches`). If the action is ambiguous from the user's request, infer it from phrasing (see TRIGGER examples above) and confirm the target name/version with the user before acting if it wasn't given explicitly.

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

This skill needs to open pull requests and check PR merge status on GitHub. Before the first action that touches GitHub in a given invocation (i.e. before 3b, 3c, 3d, or 3e), determine which access method is available:

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
- **finish feature** (operates on the current feature branch, or one named explicitly)
- **finish release** (operates on the current release branch, or one named explicitly)
- **finish hotfix** (operates on the current hotfix branch, or one named explicitly)
- **tag release** `<version>` / **tag hotfix** `<version>`
- **cleanup branches** (no name/version — operates across all `feature/*`, `release/*`, `hotfix/*` branches in the repo)

If the name/version wasn't given and can't be inferred from the current branch, ask for it. For **finish feature** specifically, if no name is given explicitly, infer it from the current branch name by stripping the `feature/` prefix (e.g. current branch `feature/skills-gitflow` → name `skills-gitflow`) — no need to ask the user in this case.

For **finish feature** / **finish release** / **finish hotfix** with an explicit name/version given (rather than inferred from the current branch), accept either form — the bare name (`skills-gitflow`) or the full branch name (`feature/skills-gitflow`) — and normalize to the full `<prefix>/<name-or-version>` form before using it as `<branch>` in the steps below: if the given value doesn't already start with `feature/`, `release/`, or `hotfix/`, prepend the prefix matching the action.

The same normalization applies to **start feature** / **start release** / **start hotfix**: if the given `<name>`/`<version>` already starts with `feature/`, `release/`, or `hotfix/` (e.g. the user says "start a feature called `feature/foo`"), strip that prefix before using the value in step 3a's `git checkout -b <prefix>/<name-or-version>` — otherwise the branch would end up double-prefixed (`feature/feature/foo`).

### 2. Check working tree state

Run `git status`. If there are uncommitted changes, stop and tell the user — do not stash or discard automatically. (For **cleanup branches**, this only matters if the current branch is itself a deletion candidate — see 3e.)

### 3a. Start feature / release / hotfix

1. Determine the base branch: `develop` for feature/release, `main` for hotfix.
2. `git checkout <base>`
3. `git pull origin <base>` — fail loudly if this doesn't fast-forward cleanly (don't force).
4. `git checkout -b <prefix>/<name-or-version>` where prefix is `feature`, `release`, or `hotfix`.
5. Report the new branch name and its base. Do not push automatically — let the user decide when to push.

### 3b. Finish feature (open PR)

Preconditions:
- Current branch (or the one named explicitly) must start with `feature/`.
- Branch must exist on the remote — if not, push it first: `git push -u origin <branch>` (confirm with the user before pushing if this is the first push of the branch).

Steps:
1. Build the PR description (see "Writing the PR description" below).
2. Open a PR into `develop` per "Opening a PR" above, with `base: develop`, `head: <branch>`, title `"Feature <name>"`, and the description from step 1.
3. Report the PR URL to the user.

This step **only opens the PR** — it does not merge it. Merging goes through normal GitHub review. Do not auto-merge. Feature branches never target `main` directly and are never tagged — only `release/*` and `hotfix/*` branches that land on `main` get a version tag.

### 3c. Finish release / Finish hotfix (open PRs)

Preconditions:
- Current branch (or the one named explicitly) must start with `release/` or `hotfix/` matching the action.
- Branch must exist on the remote — if not, push it first: `git push -u origin <branch>` (confirm with the user before pushing if this is the first push of the branch).

Steps:
1. Build the PR description (see "Writing the PR description" below) — the same description is used for both PRs since they carry the same commits.
2. Open a PR into `main` per "Opening a PR" above, with `base: main`, `head: <branch>`, and title `"<Release|Hotfix> <version>"`.
3. Open a PR into `develop` per "Opening a PR" above, with `base: develop`, `head: <branch>`, and the same title.
4. Report both PR URLs to the user.

This step **only opens the PRs** — it does not merge them. Merging goes through normal GitHub review. Do not auto-merge.

Note in the summary: once the PR into `main` is merged, run the **tag release/hotfix** action to tag `main` with the version.

### Writing the PR description

Every PR opened by this skill needs a description detailed enough for a reviewer to understand what's actually changing, not a placeholder:

1. Refresh the base branch first: `git fetch origin <base>`. Local `<base>` (`develop` or `main`) was only guaranteed fresh at branch-creation time (3a's `git pull`) — by the time you're finishing a feature, other PRs may have merged into it since, and diffing against a stale local branch skews what "new" means. Do not run `git pull origin <base>` here — that would switch/update the checked-out branch; a plain `fetch` is enough since the next step diffs against `origin/<base>` directly.
2. Inspect the actual content of the change: `git log origin/<base>..<branch> --oneline` for the commit list, and `git diff origin/<base>..<branch> --stat` for the files touched — against the freshly fetched `origin/<base>`, not local `<base>`. Read individual commit messages (and diffs, for anything non-obvious) rather than guessing from branch/file names alone.
3. Write the PR body using this structure:
   - **Summary** — 2-4 bullet points describing the substantive changes (what behavior changed and why, not a mechanical commit list).
   - **Changes** — grouped by area/component if the diff spans multiple concerns (e.g. domain, UI, migrations).
   - **Test plan** — how this was verified (tests run, manual checks) if that's discoverable from the commits; omit rather than fabricate if it isn't.
4. Do not use a generic placeholder body like `"..."`, `"Merge <branch>"`, or the PR title repeated — the description must reflect the real diff.

### 3d. Tag release / Tag hotfix

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

### 3e. Cleanup branches

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
- Never state whether a PR has or hasn't merged based on local git state (e.g. `git log`/`git diff` against a local branch, or `git pull` reporting "already up to date"). Local branches can be stale or a PR can be merged manually on GitHub outside of any local fetch. Always check directly per "Checking PR merge status" above before reporting merge status to the user.
- Never shell out to raw GitHub REST/curl calls as a substitute for `gh` or the GitHub MCP tools — use whichever of those two was selected under "GitHub access method" above.
- More generally, never assert any repo-state claim (a branch contains/is missing a given change, two branches have diverged or are identical, a commit is or isn't an ancestor of another) from a narrative of prior actions or timing assumptions. Run the direct check in the moment: `git log A..B --oneline` / `git diff A..B --stat` to compare branches, `git branch --contains <sha>` to check ancestry. A fix scoped to one specific claim (e.g. PR-merged status) does not generalize on its own — re-verify every distinct kind of claim the same way.
- If the working tree has uncommitted changes at the start of any action, stop and report — never stash or discard automatically.
- If `git pull` on the base branch doesn't fast-forward cleanly, stop and report — never force-merge or rebase automatically.
- `feature/*` branches only ever merge back into `develop`, never into `main` directly, and are never tagged — only `release/*` and `hotfix/*` branches that land on `main` get a version tag.
