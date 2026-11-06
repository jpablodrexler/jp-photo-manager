# Gitflow — Finish a topic branch, release or hotfix

_Part of the `gitflow` skill — see `../SKILL.md` for the conventions, hosting access method, action mapping, working-tree check and guardrails. Read this file in full before running this action._

### 3c. Finish a topic branch (open PR)

Preconditions:
- Current branch (or the one named explicitly) must start with `feature/`, `fix/`, `doc/`, `skill/`, or `chore/`.
- Branch must exist on the remote — if not, push it first: `git push -u origin <branch>` (confirm with the user before pushing if this is the first push of the branch).

Steps:
1. Build the PR description (see "Writing the PR description" below).
2. Open a PR into `<integration>` per "Opening a PR" in `../SKILL.md`, with `base: <integration>`, `head: <branch>`, title `"<Type> <name>"` — `"Feature <name>"`, `"Fix <name>"`, `"Doc <name>"`, `"Skill <name>"`, or `"Chore <name>"`, matching the branch's own prefix — and the description from step 1.
3. Report the PR URL to the user.

This step **only opens the PR** — it does not merge it. Merging goes through normal GitHub review. Do not auto-merge. Topic branches never target `<production>` directly and are never tagged — only `release/*` and `hotfix/*` branches that land on `<production>` get a version tag.

### 3d. Finish release / Finish hotfix (open PRs)

Preconditions:
- Current branch (or the one named explicitly) must start with `release/` or `hotfix/` matching the action.

Steps:
1. Draft or update the `CHANGELOG.md` entry for this release/hotfix on the current `release/`/`hotfix/` branch itself, per "Drafting release notes" below, using the version embedded in the branch name (e.g.
   `release/v2.3.0` → `v2.3.0`). Do this **before** the branch is pushed and before either PR is opened. This is the one thing this step changed about the old flow: the changelog entry used to get drafted after the tag, which meant committing it directly to `<production>` with no PR/review in front of it.
   Drafting it here instead means it ships as an ordinary commit on the branch under review, like everything else in the release/hotfix — no more out-of-band `<production>` commits.
2. **Optional — bump the app version.** Only if the project keeps a version in a manifest (for example `package.json`, `pyproject.toml`, `Cargo.toml`);
   otherwise skip this step. Using the version embedded in the branch name (e.g. `release/v2.3.0` → `2.3.0`, stripped of the `v` prefix), update the manifest's `version` field with the ecosystem's own command or by editing the file, **without** creating a tag or commit through it (for npm:
   `npm version 2.3.0 --no-git-tag-version`) — this skill's own commit and tag (3e) supersede it. Stage and show the diff, then ask for confirmation before committing: `git commit -m "chore: bump version to 2.3.0"`.
   Skipping it leaves any version the app displays stale.
3. **Optional — run the project's regression suite** (end-to-end or full test suite) before pushing, if it has one — a release/hotfix is exactly the point where full-app regression coverage matters most.
   Ask the user (**AskUserQuestion**) for the command to run, and if the suite needs a test account, how the credentials are provided;
   never read a credentials file yourself and never put credentials in a prompt. If every test passes, continue. If any test fails, **stop here** — do not push the branch or open either PR with known-failing coverage;
   report the failure(s), and once fixed (by you or the user) re-run the suite before continuing. A flaky-looking single failure should be re-run once before concluding it's real.
   If the project has no such suite, or it can't run (no credentials, say), tell the user plainly that the check is being skipped and why (never skip it silently), and ask whether to proceed without it or pause.
4. Push the branch (including the changelog commit from step 1, if the user chose to write one, and the version-bump commit from step 2): `git push -u origin <branch>` if the branch doesn't exist on the remote yet, or a plain `git push` if it does (e.g. this branch was already pushed earlier in the session and the steps above just added commits on top) — confirm with the user before the first push of the branch either way.
5. Build the PR description (see "Writing the PR description" below) — the same description is used for both PRs since they carry the same commits. The description's diff/commit inspection will naturally include the changelog commit from step 1 and the version-bump commit; that's fine, they're a real part of what the branch changed. If step 3 ran the regression suite, note that it passed in the PR's Test plan section.
6. Open a PR into `<production>` per "Opening a PR" in `../SKILL.md`, with `base: <production>`, `head: <branch>`, and title `"<Release|Hotfix> <version>"`.
7. Unless `<integration>` and `<production>` are the same branch (then step 6 was the only PR), open a PR into `<integration>` per "Opening a PR" in `../SKILL.md`, with `base: <integration>`, `head: <branch>`, and the same title.
8. Report both PR URLs to the user.

This step **only opens the PRs** — it does not merge them. Merging goes through normal GitHub review. Do not auto-merge.

Note in the summary: once the PR into `<production>` is merged, run the **tag release/hotfix** action to tag `<production>` with the version. The changelog entry and the version bump (if any) are already merged at that point via steps 1–2 above — 3e below only tags; it drafts release notes itself solely as a fallback for the rare case where step 1 was declined or the branch was tagged without ever going through this step.

### Writing the PR description

Every PR opened by this skill needs a description detailed enough for a reviewer to understand what's actually changing, not a placeholder:

1. Refresh the base branch first: `git fetch origin <base>`. Local `<base>` (`<integration>` or `<production>`) was only guaranteed fresh at branch-creation time (3a's `git pull`) — by the time you're finishing a branch, other PRs may have merged into it since, and diffing against a stale local branch skews what "new" means. Do not run `git pull origin <base>` here — that would switch/update the checked-out branch; a plain `fetch` is enough since the next step diffs against `origin/<base>` directly.
2. Inspect the actual content of the change: `git log origin/<base>..<branch> --oneline` for the commit list, and `git diff origin/<base>..<branch> --stat` for the files touched — against the freshly fetched `origin/<base>`, not local `<base>`. Read individual commit messages (and diffs, for anything non-obvious) rather than guessing from branch/file names alone.
3. Write the PR body using this structure:
   - **Summary** — 2-4 bullet points describing the substantive changes (what behavior changed and why, not a mechanical commit list).
   - **Changes** — grouped by area/component if the diff spans multiple concerns (e.g. domain, UI, migrations).
   - **Test plan** — how this was verified (tests run, manual checks) if that's discoverable from the commits; omit rather than fabricate if it isn't.
4. Do not use a generic placeholder body like `"..."`, `"Merge <branch>"`, or the PR title repeated — the description must reflect the real diff.
5. If a commit message read in step 2 references a backlog feature number as `feature #N`/`(feature #N)`, reformat it to `feature N`/`(feature N)` when it appears in the PR body — never carry the `#` through into GitHub-rendered PR text, per "Opening a PR" in `../SKILL.md`'s guardrail.
