# Gitflow — Start a branch

_Part of the `gitflow` skill — see `../SKILL.md` for the conventions, hosting access method, action mapping, working-tree check and guardrails. Read this file in full before running this action._

### 3a. Start feature / fix / doc / skill / chore / release / hotfix

**For `start <topic>` only (feature/fix/doc/skill/chore) — if already on a topic branch:** before anything else in this action (including the working-tree check in step 2 above — that check assumes a branch switch is about to happen, which may turn out not to be true here), check the current branch:
`git branch --show-current`. If it already starts with `feature/`, `fix/`, `doc/`, `skill/`, or `chore/` — any topic prefix, not only the one matching the requested action (e.g.
starting a `fix/` while already on a `feature/*` branch counts too) — this is a fork in the road for the user to resolve, not something to decide silently — jumping back to `<integration>` and spinning up a new branch could sideline in-progress work the user isn't ready to commit yet.
Ask the user (via **AskUserQuestion**) whether they want to:

- **Continue working on the current branch** instead of starting a separate one — appropriate when the new ask is really an extension of what's already in flight here.
  If chosen: skip the rest of this action entirely — no base checkout, no new branch, and step 2's working-tree check does not apply, since nothing is being switched.
  Then, if the current branch has **not** been pushed to the remote yet (`git rev-parse --verify --quiet origin/<current-branch>` returns nothing), ask a second, separate question: whether to **rename** it to better reflect the now-combined scope (e.g.
  `feature/task-filters` → `feature/task-filters-and-postpone`, or across topic types if the scope genuinely shifted, e.g. `fix/login-redirect` → `feature/login-redirect-and-remember-me`).
  Only offer this for an unpushed branch — renaming one that's already on the remote (and possibly backing an open PR) would orphan that PR's `head` ref, so leave a pushed branch's name alone even if asked, and say why.
  If the user wants the rename, confirm the exact new name with them, then run `git branch -m <old-name> <new-name>` (a local-only rename;
  nothing is pushed as a side effect). Report the final state — still on `<branch>`, or renamed to `<new-name>` — and stop; no further steps in this action run.
- **Start a brand-new branch as normal**, from `<integration>` — leaving the current branch untouched. If chosen, proceed with steps 1–6 below exactly as written, with step 2's working-tree check now applying in the usual way (the checkout to `<integration>` is about to happen for real).

Never pick either option without asking — always let the user decide.

1. Determine the base branch: `<integration>` for feature/fix/doc/skill/chore/release, `<production>` for hotfix.
2. **Bootstrap `<integration>` if it doesn't exist yet** (only relevant when the base is `<integration>` and it differs from `<production>`): run `git rev-parse --verify --quiet <integration>` and `git rev-parse --verify --quiet origin/<integration>`.
   If both fail, this repo hasn't adopted the `<integration>` integration branch yet — confirm with the user before creating it (a new long-lived branch other people's future PRs will target is worth a one-time explicit confirmation, unlike an ordinary topic branch).
   Once confirmed: `git checkout <production>`, `git pull origin <production>`, `git checkout -b <integration>`, `git push -u origin <integration>`.
   Proceed to step 3 with `<integration>` as the now-existing base. If `<integration>` already exists (locally or remotely), skip this step entirely.
3. `git checkout <base>`
4. `git pull origin <base>` — fail loudly if this doesn't fast-forward cleanly (don't force).
5. `git checkout -b <prefix>/<name-or-version>` where prefix is `feature`, `fix`, `doc`, `skill`, `chore`, `release`, or `hotfix`.
6. Report the new branch name and its base. Do not push automatically — let the user decide when to push.
