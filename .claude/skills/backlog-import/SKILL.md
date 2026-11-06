---
name: backlog-import
description: >-
  Imports open issues from a tracker (GitHub Issues or GitLab issues) into the
  backlog: reads them, classifies each as a bug or a feature, picks up
  priority, severity, story points and area from its labels, skips pull
  requests, closed issues and issues already imported, shows the plan, and
  after the user approves hands each issue to feature-plan or bug-report to
  draft, estimate and write. Nothing is closed or edited in the tracker.
  TRIGGER when asked to import, bring in, migrate or sync issues into the
  backlog, to start a backlog from the existing issues, or to turn issue #N
  into a feature or bug. Not for creating a new item from a description: use
  feature-plan or bug-report.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Import open tracker issues into the markdown backlog, one reviewed item at a time. The tracker is only read; the backlog is written by `feature-plan` and `bug-report`, so ids, estimation questions and confirmations stay theirs.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: Optionally a filter ("only the label `ready`", "issue 21", "the 5 oldest"). With none, the open issues of the repository the current directory belongs to.

---

## Steps

### 1. Fetch the issues (read-only)

Find the host the way `gitflow` does (do not invoke it): `[git] host` in `.backlog-kit.toml`, else the origin URL (`git remote get-url origin`).

- **GitHub**: `gh issue list --state open --limit 100 --json number,title,body,labels,url,state` (add `--label <name>` for a filter); without `gh`, `mcp__github__list_issues` with the `owner`/`repo` derived from `git remote get-url origin`.
- **GitLab**: `glab issue list --output json` (add `--label <name>`).
- **Anything else**: ask the user to paste or export the issues as JSON.

Write the list to a temporary file **outside the repository** (your scratchpad or the system temp directory) and never commit it. If the tracker is not reachable, say so and stop; do not use raw REST/curl calls.

### 2. Build the plan

```
python3 scripts/plan_import.py <repo-root> <issues.json> [--limit 10] [--json]
```

Run it from this skill's base directory and show the `## Import plan` as it is. It lists, per open issue, bug or feature, the priority (`P0`-`P3`), severity (`S1`-`S4`), story points and area that its labels state, and what it skipped (pull requests, closed issues, issues whose URL already appears in a brief under `<backlog>/features/` or `<backlog>/bugs/`).
Label conventions it understands: `bug`/`defect`/`regression`, `priority: high` / `P1`, `severity: major` / `S2`, `points: 5` (1, 2, 3, 5, 8, 13), `area: <name>`.
A bug without a severity label takes one from its priority; anything else stays empty, for the next step to ask.

### 3. Ask which to import

Print the plan, then ask with **AskUserQuestion** which issues to import: "All listed", "Specific ones (I'll list the numbers)", "None". Resolve a specific list against the printed one. Importing is capped by `--limit` (default 10) per run: tell the user how many more are waiting.

### 4. Hand each approved issue to the owning skill

For every approved issue, in order:

- **Feature** — invoke `feature-plan` with the issue title and body as the description, and pass along the label-derived priority, area and points as suggestions. It drafts the user story and acceptance criteria, asks for the story points, confirms and writes.
- **Bug** — invoke `bug-report` with the title, the body (steps, expected, actual as far as the issue states them; unknown parts stay `<unknown — needs repro>`) and the severity and area as suggestions. It confirms and writes.
- In both, put the issue URL in the brief (the `Technical notes` of a feature, the `Notes` bullet of a bug) as `Imported from <url>`. This is what stops the next run from importing it again.

Do not translate a label into a value the owning skill's own rules reject (a points label outside 1, 2, 3, 5, 8, 13, a priority outside P0-P3); leave it for the skill to ask.

### 5. Report

List what was imported (`#N` of the issue, the new feature number or Bug ID) and what was left, and remind the user that the tracker was not touched: issues stay open until the user closes them, for example after `features-archive` or `bugs-archive`.

---

## Guardrails

- The tracker is read-only here: never comment on, label, close or edit an issue.
- Never write a backlog file directly. Rows, briefs and ids come from `feature-plan` and `bug-report`.
- Never import without the user's approval of the plan, and never more than the plan showed.
- Do not write the issue number as `#<digits>` in a PR title or body later; write `issue 21`. In a brief, the URL is enough.
- **Never run `git commit`, `git push` or open a PR.**
