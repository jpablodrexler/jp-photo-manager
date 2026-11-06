# Gitflow — Tag a release or hotfix, and draft release notes

_Part of the `gitflow` skill — see `../SKILL.md` for the conventions, hosting access method, action mapping, working-tree check and guardrails. Read this file in full before running this action._

### 3e. Tag release / Tag hotfix

This step must run **after** the PR into `<production>` has actually been merged — it tags the resulting commit on `<production>`, matching Gitflow's convention of tagging production merges.

1. Verify the merge happened: check merge status per "Checking PR merge status" in `../SKILL.md`, with `base: <production>`, `head: <release/hotfix-branch>`. If no merged PR is found, stop and tell the user to merge the PR into `<production>` first — do not tag speculatively.
2. `git checkout <production>`
3. `git pull origin <production>`
4. Confirm the tag name with the user: `v<version>` (strip any `release/`/`hotfix/` prefix and leading `v` from the input, then re-add a single `v`).
5. Check the tag doesn't already exist: `git tag --list v<version>`. Then read how the project words its tag annotations: `git tag --sort=-v:refname -n1 | head -3`. The new message follows the latest one with the version replaced (for `backlog-kit 0.7.2` it is `backlog-kit 0.8.0`); with no earlier annotated tag, use `<Release|Hotfix> <version>`.
6. Ask the user to confirm before pushing the tag (this is a shared, hard-to-reverse action visible to everyone with repo access). Name the tag, the commit it will point at (`git rev-parse --short HEAD`) and the exact message.
7. `git tag -a v<version> -m "<message from step 5>"`
8. `git push origin v<version>`
9. Report the pushed tag.
10. Check whether `CHANGELOG.md` already documents this version — look for a `v<version>` heading (`grep`/read the file). This should normally already be true: 3d drafts and merges the entry as part of the release/hotfix PR, before this tagging step ever runs.
    - **If found**: nothing further to do — report that release notes for this version were already included in the merge. Do not draft a second entry.
    - **If not found** (3d's draft was declined at the time, or `<production>` was tagged without going through 3d at all): fall back to drafting it now per "Drafting release notes" below, called from this post-tag context — the resulting commit has no PR/review step in front of it, so treat it with the heavier care described there for this call site.
11. **Offer the release on the host.** A tag is not a release: GitHub and GitLab list releases separately, and many projects publish one per version. Check whether this one does: `gh release list --limit 3` (or `glab release list`, or `mcp__github__list_releases` when there is no `gh`). If there are earlier releases, offer to create this one, and never create it without the user's confirmation, since it is public:
    - **Title:** the tag name, `v<version>`, the same as the earlier releases (read the latest one's name to be sure). Not the tag message.
    - **Body:** the `CHANGELOG.md` section of this version without its heading. Write it to a file outside the repository (your scratchpad or the system temp directory).
    - **With `gh`:** `gh release create v<version> --title "v<version>" --notes-file <file> --verify-tag`. **With `glab`:** `glab release create v<version> --name "v<version>" --notes-file <file>`; if a flag is rejected, run `glab release create --help` and use the equivalent.
    - **Without a CLI** (the GitHub MCP tools cannot create a release): show the title and the body and tell the user to open Releases, "Draft a new release", pick the existing tag and paste them.
    - If the project has no releases at all, say so and ask whether to start publishing them; do not create the first one on your own.

### Drafting release notes

Draft a changelog entry for a release/hotfix version. This runs from two possible call sites, and which one you're in changes where the commit lands and how much confirmation weight that carries:

- **Normally, from 3d step 1**, on the `release/`/`hotfix/` branch itself, before its PR is opened — the commit is just another commit on a branch about to go through normal PR review.
- **As a fallback, from 3e step 10**, directly on `<production>`, only when no entry exists yet for the version being tagged — there is no PR/review step protecting `<production>`, so this path needs the heavier treatment noted in step 6 below.

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
   - **If it doesn't exist**: don't create it silently — this is a new top-level file and a one-time structural decision the user should make, not something to add as a side effect of finishing a release or tagging one. Show the drafted entry in chat instead and ask whether to create `CHANGELOG.md` now. If they decline, stop here — the drafted entry stays in the chat response, not in the repo (from 3d, this means the branch proceeds to its PR without a changelog commit; from 3e's fallback, it means `<production>` is left without one).
6. If writing to the file (existing or newly approved), stage and show the diff, then ask for explicit confirmation before committing:
   - **From 3d** (on the release/hotfix branch): confirm before committing, the same as any other commit this skill makes on a branch under review — `git add CHANGELOG.md && git commit -m "docs: add v<version> release notes"`. No separate push confirmation is needed here beyond 3d step 2's existing branch-push confirmation, since the changelog commit is just one of the commits that push carries.
   - **From 3e's fallback** (on `<production>`): treat this with the same care as the tag push in 3e — a commit here lands directly on `<production>` with no PR/review step in front of it. `git add CHANGELOG.md && git commit -m "docs: add v<version> release notes"`, then ask **separately** whether to push it (`git push origin main`) — don't bundle the commit and push confirmations into one yes, since a local commit is still easy to amend/undo but a push is not.
7. Report what was recorded (or, if the user declined, that the drafted notes were not saved anywhere).
