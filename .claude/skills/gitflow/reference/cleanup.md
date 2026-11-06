# Gitflow — Cleanup branches

_Part of the `gitflow` skill — see `../SKILL.md` for the conventions, hosting access method, action mapping, working-tree check and guardrails. Read this file in full before running this action._

### 3f. Cleanup branches

Finds `feature/*`, `fix/*`, `doc/*`, `skill/*`, `chore/*`, `release/*`, and `hotfix/*` branches that have already been merged, and deletes only the ones the user explicitly confirms — never speculatively, never with `-D`/`--force`.

1-2. Run `python3 scripts/list_merge_candidates.py <repo-root>` — it runs `git fetch origin --prune` (sync remote-tracking refs so branches already deleted on GitHub, e.g. auto-deleted on merge, aren't listed), enumerates every local/remote branch matching the 7 prefixes, and unions them by name with a `where: [local, remote]` flag per branch. This is local-git-only — it never calls `gh`/the GitHub API and never claims a branch is merged; that check is still step 3 below, per branch.
3. For each candidate branch the script returned, determine merged status with a direct check (per the repo-state guardrail below — never infer this from timing or narrative):
   - `feature/<name>`, `fix/<name>`, `doc/<name>`, `skill/<name>`, or `chore/<name>` (any topic branch): merged if checking merge status per "Checking PR merge status" in `../SKILL.md` (with `head: <branch>`, `base: <integration>`) returns at least one merged result.
   - `release/<version>` or `hotfix/<version>`: merged if **both** hold — checking merge status per "Checking PR merge status" in `../SKILL.md` (with `head: <branch>`, `base: <production>`) returns a merged result, **and** `git merge-base --is-ancestor origin/<branch> origin/<integration>` exits 0 (confirms `<integration>` also has the commits, whether via its own merged PR or because `<integration>` already contained them).
   - A branch with no merged PR found this way is **not** a candidate — leave it alone and don't list it, even if it looks stale. This only recognizes merges that went through a PR; branches merged some other way won't be flagged, which is the safe direction to be wrong in.
4. If no merged branches are found, report that and stop — nothing to clean up.
5. Print every merged candidate to the user as plain text (not through `AskUserQuestion`'s options — a repo that's accumulated several merged-but-undeleted branches can easily produce more candidates than the tool's 4-option-per-question limit allows):
   branch name, type (feature/fix/doc/skill/chore/release/hotfix), and where it exists (local/remote/both). Then, if there's more than one, use **AskUserQuestion** with a small fixed set of options — "Delete all listed", "Delete specific ones (I'll list the names)", "Cancel" — and resolve "specific ones" from the user's free-text follow-up matched against the printed list, rather than trying to enumerate every branch as its own option.
   If there's exactly one candidate, a plain yes/no/cancel confirmation is enough.
6. For whichever branches the user selected, ask whether to delete the **local** branch, the **remote** branch, or **both** — per branch if they exist in different places, or once for the whole batch if that's simpler for the user to answer.
7. Do one final explicit confirmation of exactly what's about to be deleted (branch names × local/remote) before running anything — deletion of a shared remote branch is hard to reverse.
8. Delete only what was confirmed:
   - Before deleting anything, verify `<branch>` is not `<production>` or `<integration>` (case-insensitive) — refuse and report an error if it is, even if somehow confirmed by the user. These two branches are never deletion candidates (see 3f step 2 and the guardrail below) and this check exists as a last-resort safety net against that invariant ever being violated upstream.
   - Local: `git branch -d <branch>` — never `-D`. If the branch to delete is the current branch, `git checkout` its base first (`<integration>` for a topic branch/release, `<production>` for a hotfix) before deleting.
   - Remote: `git push origin --delete <branch>`. If the ref is already gone (common — GitHub auto-deletes on merge), report that rather than treating it as an error.
9. Report exactly what was deleted (and what was already gone / skipped).
