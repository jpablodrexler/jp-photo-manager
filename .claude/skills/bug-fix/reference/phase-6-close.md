# Bug Fix — Phase 6 — Close

_Part of the `bug-fix` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

## Phase 6 — Close (Subagent 6)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> **Step 1** — Invoke `docs-sync` in its scoped-sync mode for the
> current branch's changes (it diffs against the branch base itself).
> **Best-effort, not blocking**: a bug fix usually changes no documented
> behaviour, so "nothing to sync" is the common and fine outcome. Only
> stop and surface details if the skill errors out leaving files broken.
>
> **Step 2** — Invoke `bugs-archive <bug-id> fixed "<one-line resolution:
> what the root cause was and what changed; include the regression test
> path>"`. Wait for it to move the row to `<backlog>/bugs-fixed.md` and append the
> Resolution line to `<backlog>/bugs/<bug-id>.md`.
>
> End with exactly:
> `CLOSE: DONE`

Do not display the Final Summary until this subagent returns `CLOSE:
DONE`.
