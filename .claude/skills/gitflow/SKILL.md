---
name: gitflow
description: >-
  Encapsulates the Gitflow workflow for this repo: `<integration>` is the
  integration branch, `<production>` the production branch. Manages topic branches —
  `feature/`, `fix/` (bug fixes), `doc/` (docs-only), `skill/` (Claude Code
  skills or CLAUDE.md) and `chore/` (dependency bumps, config, maintenance) —
  all created from `<integration>` and finished with a single PR into `<integration>`,
  plus `release/` and `hotfix/` branches, which PR into both `<production>` and
  `<integration>`. Also syncs topic branches with <integration>, tags releases after
  merge, and cleans up merged branches. On release/hotfix branches it drafts
  CHANGELOG.md notes before the PR opens, and optionally bumps the app version
  and runs the project's regression suite. A commit action makes one local
  commit on request (used by bug-fix and feature-development with --commit or when asked to commit as the work goes). TRIGGER when asked to start, sync,
  finish or merge any of those branches, create a release or hotfix, tag a
  release, or clean up merged branches; a bare request to merge/pull the production or
  integration branch into a topic branch is redirected to the Sync action.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Perform one Gitflow action: start a feature/fix/doc/skill/chore/release/hotfix branch, sync an existing topic branch (feature/fix/doc/skill/chore) with <integration>, finish (open a PR for) a topic branch, finish (open PRs for) a release/hotfix, tag main after a release/hotfix PR has merged, or clean up already-merged feature/fix/doc/skill/chore/release/hotfix branches.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`. `<integration>` is `develop` and `<production>` is `main` unless `.backlog-kit.toml` sets `[git] integration_branch` / `production_branch`. Read that file yourself when it exists; with none, the defaults hold.

**Input**: The action (`start feature`, `start fix`, `start doc`, `start skill`, `start chore`, `start release`, `start hotfix`, `sync feature`, `sync fix`, `sync doc`, `sync skill`, `sync chore`, `finish feature`, `finish fix`, `finish doc`, `finish skill`, `finish chore`, `finish release`, `finish hotfix`, `tag release`, `tag hotfix`, `commit`, `cleanup branches`) plus a name/version (not needed for `cleanup branches`).
If the action is ambiguous from the user's request, infer it from phrasing (see TRIGGER examples above) and confirm the target name/version with the user before acting if it wasn't given explicitly.

---

## Branch and tag conventions (defaults: `develop` and `main`)

| Branch type | Created from | Prefix       | Finishes into      |
|-------------|--------------|--------------|---------------------|
| feature     | `<integration>`    | `feature/`   | `<integration>` via PR |
| fix         | `<integration>`    | `fix/`       | `<integration>` via PR |
| doc         | `<integration>`    | `doc/`       | `<integration>` via PR |
| skill       | `<integration>`    | `skill/`     | `<integration>` via PR |
| chore       | `<integration>`    | `chore/`     | `<integration>` via PR |
| release     | `<integration>`    | `release/`   | `<production>` **and** `<integration>` via PR |
| hotfix      | `<production>`       | `hotfix/`    | `<production>` **and** `<integration>` via PR |

**Topic branches** — `feature`, `fix`, `doc`, `skill`, and `chore` — are mechanically identical: all created from `<integration>`, all finish with a single PR into `<integration>`, never touch `<production>`, never get a version tag.
They differ only in their prefix (which semantically categorizes the kind of change) and the resulting PR title (`"Feature <name>"`, `"Fix <name>"`, `"Doc <name>"`, `"Skill <name>"`, `"Chore <name>"`).
Every step below that says "topic branch" or lists all five prefixes together applies identically to each; `release`/`hotfix` remain structurally different (dual-branch finish, version bump, changelog, tag) and are covered separately in their own steps (3d/3e).

Use `fix/` for a bug fix, `doc/` for a documentation-only change (`docs/*.md`, `README.md`, `CLAUDE.md`'s own prose — not a change that also touches application code, which belongs under `feature/`, `fix/`, or `chore/` instead), `skill/` for a change scoped to `.claude/skills/**` or `CLAUDE.md` (the kind of work this skill's own maintenance, `decision-record`, or `docs-sync` produces), and `chore/` for anything else that doesn't fit the other four (a dependency bump, a config tweak, routine maintenance).

**Trunk-based projects.** When `integration_branch` and `production_branch` name the same branch (e.g. both `main`), the table holds with that one branch: topic branches are created from it and PR into it, 3a's bootstrap is skipped, and `finish release` / `finish hotfix` open a single PR (there is no second PR into `<integration>`).

Version tags on `<production>` use the `v<major>.<minor>.<patch>` format (e.g. `v2.1.0`), matching this repo's existing tag history.

---

## Hosting access method

This skill opens pull requests (merge requests on GitLab) and checks whether they merged. Before the first action that touches the host in a given invocation (before 3c, 3d, 3e or 3f; 3b/sync is local-only), find out the host and the access method:

1. **Host.** `[git] host` in `.backlog-kit.toml` is `auto`, `github`, `gitlab` or `other` (default `auto`). With `auto`, run `git remote get-url origin`: `github.com` means GitHub, a host whose name contains `gitlab` means GitLab, anything else is `other`. A self-hosted GitHub or GitLab needs `host` set explicitly.
2. **GitHub.** Run `gh --version`. If it succeeds, use the **gh CLI** for every operation below; it already knows the repo from the working directory.
   If it fails, use the **GitHub MCP tools** (`mcp__github__create_pull_request`, `mcp__github__list_pull_requests`, `mcp__github__pull_request_read`), the required path in Claude Code Remote / web sessions, which have no `gh` binary.
   These take explicit `owner`/`repo`: derive them once from `git remote get-url origin` (the `https://github.com/<owner>/<repo>.git` form, the `git@github.com:<owner>/<repo>.git` form, and the local proxy rewrite used in some sandboxed environments, `.../git/<owner>/<repo>`).
3. **GitLab.** Run `glab --version`. If it succeeds, use the **glab CLI**; it reads the project from the working directory. If a glab flag below is rejected, run `glab mr <command> --help` and use the equivalent; do not guess a different command.
4. **Anything else** (Bitbucket, Gitea, GitLab without `glab`, GitHub without `gh` or MCP): the skill cannot open or look up a PR. Say so, print the PR title, the description from "Writing the PR description" and the push command for the user to run, and for a step that needs a merge check ask the user whether it merged (see "Checking PR merge status"). Never fall back to raw REST/curl calls.

Wherever a step says "open a PR" or "check merge status", use the method selected here, per the equivalences below. "PR" means merge request on GitLab.

### Opening a PR

- **gh CLI**: `gh pr create --base <base> --head <branch> --title "<title>" --body "<description>"`. Capture the printed PR URL.
- **glab CLI**: `glab mr create --source-branch <branch> --target-branch <base> --title "<title>" --description "<description>" --yes`. Capture the printed MR URL.
- **Other hosts**: print the title and description and tell the user to open the PR from `<branch>` into `<base>`; ask for its URL afterwards if a later step needs it.
- **GitHub MCP**: `mcp__github__create_pull_request` with `owner`, `repo`, `head: <branch>`, `base: <base>`, `title: "<title>"`, `body: "<description>"`. The result includes the PR URL and number.

**Never write `#<digits>` immediately before a backlog feature number, in a
PR title or body** — GitHub and GitLab auto-link any `#<digits>` it finds in a title
or description as a cross-reference to an issue/PR in the same repo,
regardless of surrounding text or intent. A feature number from
`<backlog>/features-planned.md` (e.g. feature 32) is not a GitHub
issue/PR number and must never be written as `#32` — write `feature 32` or
`(feature 32)` instead, never `feature #32`/`(feature #32)`. This applies
even when a commit message on the branch already uses the `#N` form (see
"Writing the PR description" below, which pulls from those commit
messages) — reformat it when copying into the PR title/body rather than
carrying the `#` through verbatim. Reserve a literal `#<digits>` in PR text
exclusively for an actual, intentional cross-reference to another GitHub
PR or issue.

### Checking PR merge status

- **gh CLI**: `gh pr list --state merged --head <branch> --base <base> --json number,mergedAt`. A non-empty result means merged. For a single known PR number, `gh pr view <number> --json state,mergedAt` is equivalent.
- **glab CLI**: `glab mr list --merged --source-branch <branch> --target-branch <base> --output json`. A non-empty list means merged.
- **Other hosts**: ask the user whether the PR from `<branch>` into `<base>` has merged, and say that you are relying on their answer. As supporting evidence run `git fetch origin` and `git merge-base --is-ancestor origin/<branch> origin/<base>`: exit 0 supports the answer; a non-zero exit does not contradict it, because a squash or rebase merge leaves the branch commits out of `<base>`. If the user is not sure, treat it as not merged.
- **GitHub MCP**: `mcp__github__list_pull_requests` with `owner`, `repo`, `head: "<owner>:<branch>"` (the `owner:` prefix is required here — GitHub's `head` filter for listing PRs only accepts `owner:branch-name`, never a bare branch name;
  a bare branch silently matches nothing and would make every merge check falsely report "not merged", unlike `gh pr list --head`, which is a CLI-only convenience that accepts a bare branch name for same-repo PRs), `base: <base>`, `state: "closed"`, then check each returned PR's `merged_at` — a PR that's `closed` but not `merged` doesn't count (GitHub's API has no native "merged" state;
  `gh pr list --state merged` is itself just wrapping this closed-plus-merged_at check, so this reproduces it exactly).
  For a single known PR number, `mcp__github__pull_request_read` with `method: "get"`, `owner`, `repo`, `pullNumber` is equivalent to `gh pr view <number> --json state,mergedAt` — check the returned `merged`/`merged_at` field.

---

## Steps

### 1. Determine the action

Map the user's request to one of:
- **start `<topic>`** `<name>`, where `<topic>` is one of `feature`, `fix`, `doc`, `skill`, `chore`
- **start release** `<version>`
- **start hotfix** `<version>`
- **sync `<topic>`** (operates on the current topic branch, or one named explicitly — brings it up to date with `<integration>`)
- **finish `<topic>`** (operates on the current topic branch, or one named explicitly)
- **finish release** (operates on the current release branch, or one named explicitly)
- **finish hotfix** (operates on the current hotfix branch, or one named explicitly)
- **tag release** `<version>` / **tag hotfix** `<version>`
- **commit** `<message>` (commits the current changes on the current topic branch — see 3g)
- **cleanup branches** (no name/version — operates across all `feature/*`, `fix/*`, `doc/*`, `skill/*`, `chore/*`, `release/*`, `hotfix/*` branches in the repo)

If the name/version wasn't given and can't be inferred from the current branch, ask for it. For **sync `<topic>`** / **finish `<topic>`** specifically, if no name is given explicitly, infer it from the current branch name by stripping whichever of the five topic prefixes matches (e.g.
current branch `feature/skills-gitflow` → name `skills-gitflow`; `fix/login-redirect` → `login-redirect`) — no need to ask the user in this case.
If the current branch's prefix doesn't match the topic requested (e.g. `sync fix` invoked while on a `feature/*` branch, with no explicit name given), ask which branch to operate on rather than guessing.

For **sync `<topic>`** / **finish `<topic>`** / **finish release** / **finish hotfix** with an explicit name/version given (rather than inferred from the current branch), accept either form — the bare name (`skills-gitflow`) or the full branch name (`feature/skills-gitflow`) — and normalize to the full `<prefix>/<name-or-version>` form before using it as `<branch>` in the steps below: if the given value doesn't already start with `feature/`, `fix/`, `doc/`, `skill/`, `chore/`, `release/`, or `hotfix/`, prepend the prefix matching the action.

The same normalization applies to **start `<topic>`** / **start release** / **start hotfix**: if the given `<name>`/`<version>` already starts with `feature/`, `fix/`, `doc/`, `skill/`, `chore/`, `release/`, or `hotfix/` (e.g. the user says "start a fix called `fix/foo`"), strip that prefix before using the value in step 3a's `git checkout -b <prefix>/<name-or-version>` — otherwise the branch would end up double-prefixed (`fix/fix/foo`).

### 2. Check working tree state

Run `git status`. (Not for **commit**, whose whole point is the uncommitted changes — see 3g.) If there are uncommitted changes, stop and tell the user — do not stash or discard automatically.
One exception, for **start `<topic>`** only: changes confined to paths under `<backlog>/` (the row and brief `feature-plan` just wrote, or the entry `bug-report` just wrote) do not stop the action;
the new branch carries them along, and the report says so. (For **cleanup branches**, this only matters if the current branch is itself a deletion candidate — see 3f.
For **start `<topic>`** specifically, when the current branch already starts with a topic prefix (`feature/`, `fix/`, `doc/`, `skill/`, or `chore/`), resolve 3a's branch-continuation question first, before applying this check — it may turn out this check doesn't even apply, since continuing on the current branch involves no checkout and tolerates an uncommitted tree just fine.)

### Where each action lives

Starting, finishing, tagging and cleaning up are long, and a run needs only one of them, so each lives in its own file under `reference/`. **Before running an action, read its file in full**, then follow it exactly. The step numbers (3a to 3g) are stable: other skills and the guardrails below cite them. Sync (3b) and Commit (3g) are short and stay in this file.

| Action | Step | File |
| ------ | ---- | ---- |
| start `<topic>` / release / hotfix | 3a | `reference/start.md` |
| sync `<topic>` | 3b | below |
| finish `<topic>`; finish release / hotfix; "Writing the PR description" | 3c, 3d | `reference/finish.md` |
| tag release / hotfix, then the host release; "Drafting release notes" | 3e | `reference/tag-and-release-notes.md` |
| commit | 3g | below |
| cleanup branches | 3f | `reference/cleanup.md` |

### 3b. Sync a topic branch (update from <integration>)

Brings an existing topic branch (`feature/`, `fix/`, `doc/`, `skill/`, or `chore/`) up to date with `<integration>` mid-flight — e.g. after a release has merged into both `<production>` and `<integration>` and the branch needs those changes before continuing work. **`<integration>` is the only sanctioned source for this** — see the guardrail below on why `<production>` is never used here, even though right after a release merge `<production>` and `<integration>` are momentarily identical and it's tempting to treat either as "the latest."

Preconditions:
- Current branch (or the one named explicitly) must start with `feature/`, `fix/`, `doc/`, `skill/`, or `chore/`.

Steps:
1. `git fetch origin <integration>`
2. `git checkout <branch>` if not already on it.
3. `git merge origin/<integration>`. If it doesn't merge cleanly, stop and report the conflicting files — resolve conflicts together with the user, never auto-resolve or force through.
4. Report the result (already up to date / fast-forwarded / merge commit created / conflicts needing resolution).

This step never touches `<production>`. Do not push automatically — let the user decide when to push the merge commit.

### 3g. Commit

Makes one local commit of the work in the tree, on request only — for example when `bug-fix --commit` or `feature-development --commit` hands one closed item over, or after each phase when the user asked to commit as the work goes. Never pushes.

Preconditions:
- Current branch must start with `feature/`, `fix/`, `doc/`, `skill/`, or `chore/`. Refuse on `<production>`, `<integration>`, `release/*` and `hotfix/*`, and on a detached HEAD.
- `git status` shows changes. If it shows none, report "nothing to commit" and stop.

Steps:
1. Show `git status --short` and `git diff --stat` and say which paths will be committed. Stage the changed and new files with `git add -A`, but first list anything that looks like a secret, a credentials file, a generated report or build output, and ask rather than staging it (respect `.gitignore`).
2. Ask for confirmation of the message and the file list (**AskUserQuestion**). Use the message passed in, or draft one from the diff when none was given; never write `#<digits>` before a backlog item number (see "Opening a PR"). End the message with the attribution lines given in the session's system reminder, when there is one.
3. `git commit -m "<message>"`. Never `--no-verify`, never `--amend`. If a hook fails, show its output and stop; fix the cause and run the action again.
4. Report the short hash and the branch. Do not push.

---

## Guardrails

- Never force-push, force-create branches over existing ones, or force-delete a branch (`git branch -D`, `git push --force`). The only sanctioned branch deletion is the **Cleanup branches** action, and only for branches confirmed merged per its steps — never delete speculatively. Local deletes use `git branch -d`, never `-D`.
- **Never delete `main` or `develop` (or whichever branches `<production>` and `<integration>` name), under any circumstances, local or remote.** Cleanup branches only ever enumerates `feature/*`, `fix/*`, `doc/*`, `skill/*`, `chore/*`, `release/*`, and `hotfix/*` candidates (3f step 2), so `<production>`/`<integration>` should never reach the confirmation prompts in the first place — but treat that as defense-in-depth, not the only guard:
  3f step 8 also refuses the deletion outright if `<branch>` resolves to `<production>` or `<integration>`, even if a user instruction, a typo in a branch name, or a confirmation somehow points at one of them.
  No action in this skill ever deletes, force-resets, or overwrites these two branches.
- Cleanup branches never deletes anything without the user explicitly confirming: which branches (from the listed candidates), and for each, whether to delete local, remote, or both. Do not skip either confirmation step or bundle them into a single implicit approval.
- **Commit** is local only and happens only on request, on a topic branch, with the file list and message confirmed first. It never pushes, amends, or skips hooks.
- Never merge a PR automatically — "finish `<topic>`/release/hotfix" only creates the PR(s); merging is a human review step.
- **Never push a `release/`/`hotfix/` branch or open its PRs while its regression suite is failing.** Finish release/hotfix's step 3 gates on this: a failing run stops the action there — fix and re-run before continuing to push/PR. Skipping the suite is allowed only after telling the user and letting them choose, never silently.
- Never open a PR with a placeholder or generic body — always derive the description from the actual commits/diff on the branch, per "Writing the PR description" above.
- Never tag `<production>` without first confirming (via "Checking PR merge status" above) that the corresponding release/hotfix PR into `<production>` has actually merged.
- Never tag without explicit user confirmation immediately before the `git push origin <tag>` — pushing a tag is visible to the whole team and awkward to undo.
- Never create a release on the host (`gh release create`, `glab release create`) without the user's confirmation, and never before its tag is pushed. Its title is the tag name and its body the changelog section of that version.
- Never create `CHANGELOG.md` silently, on either call site — see "Drafting release notes". From 3d (the normal path, on the release/hotfix branch before its PR), a changelog commit is an ordinary commit under review, so a single commit confirmation is enough. From 3e's fallback (writing directly to `<production>`, only when 3d's draft was skipped or declined), there is no PR/review step protecting it, so commit and push need separate explicit confirmations, with the same care as the tag push.
- Never state whether a PR has or hasn't merged based on local git state (e.g. `git log`/`git diff` against a local branch, or `git pull` reporting "already up to date"). Local branches can be stale or a PR can be merged manually on GitHub outside of any local fetch. Always check directly per "Checking PR merge status" above before reporting merge status to the user.
- Never shell out to raw REST/curl calls as a substitute for `gh`, `glab` or the GitHub MCP tools — use whichever was selected under "Hosting access method" above, or the manual path for other hosts.
- More generally, never assert any repo-state claim (a branch contains/is missing a given change, two branches have diverged or are identical, a commit is or isn't an ancestor of another) from a narrative of prior actions or timing assumptions. Run the direct check in the moment: `git log A..B --oneline` / `git diff A..B --stat` to compare branches, `git branch --contains <sha>` to check ancestry. A fix scoped to one specific claim (e.g. PR-merged status) does not generalize on its own — re-verify every distinct kind of claim the same way.
- If the working tree has uncommitted changes at the start of any action, stop and report — never stash or discard automatically.
- If `git pull` on the base branch doesn't fast-forward cleanly, stop and report — never force-merge or rebase automatically.
- `feature/*`, `fix/*`, `doc/*`, `skill/*`, and `chore/*` branches only ever merge back into `<integration>`, never into `<production>` directly, and are never tagged — only `release/*` and `hotfix/*` branches that land on `<production>` get a version tag.
- **Never silently switch away from an already-checked-out topic branch to start a new one.** `start <topic>` always asks first when the current branch already starts with `feature/`, `fix/`, `doc/`, `skill/`, or `chore/` — per 3a's opening step — whether to continue on it or branch fresh from `<integration>`, and (only if the branch is unpushed) whether to rename it for the combined scope. Both are the user's call, never an automatic decision.
- **Never merge, rebase, or pull `<production>` into a topic branch, for any reason.** `<integration>` is the only sanctioned upstream for a topic branch's content — use the **Sync** action (3b), which merges `origin/<integration>` and nothing else.
  This holds even right after a release/hotfix PR has merged into both `<production>` and `<integration>`, when the two are momentarily identical and pulling from either looks equivalent in the moment — `<production>` is still the wrong branch to name in the command, because the next release may fork them apart again before the branch is finished, silently carrying content into it that never went through `<integration>`.
