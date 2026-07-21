---
name: gitflow
description: Encapsulates the Gitflow branching workflow for this repo (develop as integration branch, main as production branch). TRIGGER when the user asks to start/create a new feature, release, or hotfix branch, when asking to merge/finish a release or hotfix, or when asking to tag a release or hotfix after it has been merged. Phrases like "start a new feature", "create a release branch", "start a hotfix", "merge the release", "finish the hotfix", "tag the release" all trigger this skill.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Perform one Gitflow action: start a feature/release/hotfix branch, finish (open PRs for) a release/hotfix, or tag main after a release/hotfix PR has merged.

**Input**: The action (`start feature`, `start release`, `start hotfix`, `finish release`, `finish hotfix`, `tag release`, `tag hotfix`) plus a name/version. If the action is ambiguous from the user's request, infer it from phrasing (see TRIGGER examples above) and confirm the target name/version with the user before acting if it wasn't given explicitly.

---

## Branch and tag conventions (fixed for this repo)

| Branch type | Created from | Prefix       | Finishes into      |
|-------------|--------------|--------------|---------------------|
| feature     | `develop`    | `feature/`   | `develop` (not automated by this skill) |
| release     | `develop`    | `release/`   | `main` **and** `develop` via PR |
| hotfix      | `main`       | `hotfix/`    | `main` **and** `develop` via PR |

Version tags on `main` use the `v<major>.<minor>.<patch>` format (e.g. `v2.1.0`), matching this repo's existing tag history.

---

## Steps

### 1. Determine the action

Map the user's request to one of:
- **start feature** `<name>`
- **start release** `<version>`
- **start hotfix** `<version>`
- **finish release** (operates on the current release branch, or one named explicitly)
- **finish hotfix** (operates on the current hotfix branch, or one named explicitly)
- **tag release** `<version>` / **tag hotfix** `<version>`

If the name/version wasn't given and can't be inferred from the current branch, ask for it.

### 2. Check working tree state

Run `git status`. If there are uncommitted changes, stop and tell the user — do not stash or discard automatically.

### 3a. Start feature / release / hotfix

1. Determine the base branch: `develop` for feature/release, `main` for hotfix.
2. `git checkout <base>`
3. `git pull origin <base>` — fail loudly if this doesn't fast-forward cleanly (don't force).
4. `git checkout -b <prefix>/<name-or-version>` where prefix is `feature`, `release`, or `hotfix`.
5. Report the new branch name and its base. Do not push automatically — let the user decide when to push.

### 3b. Finish release / Finish hotfix (open PRs)

Preconditions:
- Current branch (or the one named explicitly) must start with `release/` or `hotfix/` matching the action.
- Branch must exist on the remote — if not, push it first: `git push -u origin <branch>` (confirm with the user before pushing if this is the first push of the branch).

Steps:
1. `gh pr create --base main --head <branch> --title "<Release|Hotfix> <version>" --body "..."` — PR into `main`.
2. `gh pr create --base develop --head <branch> --title "<Release|Hotfix> <version>" --body "..."` — PR into `develop`.
3. Report both PR URLs to the user.

This step **only opens the PRs** — it does not merge them. Merging goes through normal GitHub review. Do not auto-merge.

Note in the summary: once the PR into `main` is merged, run the **tag release/hotfix** action to tag `main` with the version.

### 3c. Tag release / Tag hotfix

This step must run **after** the PR into `main` has actually been merged — it tags the resulting commit on `main`, matching Gitflow's convention of tagging production merges.

1. Verify the merge happened: `gh pr list --state merged --base main --head <release/hotfix-branch> --json number,mergedAt`. If no merged PR is found, stop and tell the user to merge the PR into `main` first — do not tag speculatively.
2. `git checkout main`
3. `git pull origin main`
4. Confirm the tag name with the user: `v<version>` (strip any `release/`/`hotfix/` prefix and leading `v` from the input, then re-add a single `v`).
5. Check the tag doesn't already exist: `git tag --list v<version>`.
6. Ask the user to confirm before pushing the tag (this is a shared, hard-to-reverse action visible to everyone with repo access).
7. `git tag -a v<version> -m "<Release|Hotfix> <version>"`
8. `git push origin v<version>`
9. Report the pushed tag.

---

## Guardrails

- Never force-push, force-create branches over existing ones, or delete branches as part of this skill.
- Never merge a PR automatically — "finish release/hotfix" only creates the PRs; merging is a human review step.
- Never tag `main` without first confirming (via `gh pr list --state merged`) that the corresponding release/hotfix PR into `main` has actually merged.
- Never tag without explicit user confirmation immediately before the `git push origin <tag>` — pushing a tag is visible to the whole team and awkward to undo.
- If the working tree has uncommitted changes at the start of any action, stop and report — never stash or discard automatically.
- If `git pull` on the base branch doesn't fast-forward cleanly, stop and report — never force-merge or rebase automatically.
- `feature/*` branches only ever merge back into `develop`, never into `main` directly, and this skill does not automate that merge — feature integration typically happens via normal PR review into `develop`, outside this skill's "finish" actions.
