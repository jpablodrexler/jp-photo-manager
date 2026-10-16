---
name: code-reviewer
description: >
  Code review skill. TRIGGER after implementing any feature, fix, or
  refactor — including after completing a task or a set of tasks. Do not
  wait to be asked: review code proactively after writing it. Also triggers
  when explicitly asked to review a pull request, file, or change. Covers
  architecture layering, naming, state handling, data-access usage, and
  testing conventions, as spelled out by the project's own checklists.
  Schema and migration changes are reviewed by the `database-reviewer`
  skill instead — invoke that one for migration files, not this one. A
  full-codebase sweep produces one report per architecture layer instead of
  a single consolidated report — see "Full-Codebase Sweeps" below. Also
  TRIGGERS when asked to fix, address, resolve, or work through findings
  from an existing dated CODE_REVIEW_FINDINGS report — see the "Fix Workflow" section
  (§12).
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Code Reviewer Skill

Review code in this project against its documented architecture,
conventions, and known pitfalls. The stack-specific checklists belong to the
project, not to this skill: they live in the `Code review checks`
section of the project's docs (see step 3). Schema and migration files are out
of scope here — use `database-reviewer` for those.

`<reports>` is the `reports` folder next to the backlog's parent (`docs/reports` by default, unless `.backlog-kit.toml` sets `[paths] backlog_dir`). The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Never run `git commit`, `git push` or open a PR.**

This skill reviews a *change*. For the health of the system's overall shape —
layering, state-management coupling, data-access patterns — use
`architecture-reviewer`; for a non-technical review of the feature scope, use
`product-scope-roaster`.

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing new/changed code.
- **Fix** (§12, in `reference/report-format-and-fix-workflow.md`) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review code.

## Workflow

1. Identify the **scope**: a full-codebase sweep of the whole codebase, or a
   scoped review (single file, PR, or feature, or a list of changed files
   given by the caller).
   - **Scoped review** → follow steps 2–5 below as a single pass, producing
     one report.
   - **Full-codebase sweep** → follow "Full-Codebase Sweeps: Review by
     Layer" instead. It runs steps 2–5 once per layer, each producing its
     own report.
2. Read every changed file before forming any opinion.
3. **Project checklists:** read the `Code review checks` section of the
   project's reference docs: `docs/frontend.md` and/or `docs/backend.md`,
   whichever sides the project has (the kit convention: each of those docs
   holds the structure, data flow and conventions of its side, and what this
   review checks there). Work through the subsections that apply to what
   changed, one by one; the known-pitfalls subsection applies to every review.
   If the docs have no such section, review with the frame above plus what
   the project's CLAUDE.md and docs say, and state in the report that no
   project checklist was found. Always finish by scanning the project's
   known-pitfalls subsection (§10), if it has one.
4. Report findings grouped by **severity**, then by file.
5. Summarise with a short verdict and the top action items, then write the
   full report to a new, dated markdown file — see "Review Report Format &
   Output File" (§11, in `reference/report-format-and-fix-workflow.md`). Do this on every run, not just full-codebase
   sweeps: a single-file or single-PR review still gets its own dated
   report, scoped to whatever was actually reviewed.

### Full-Codebase Sweeps: Review by Layer

When asked to review all the code, don't produce one giant
consolidated report and don't review it inline in the main conversation.
Instead, split the sweep into one pass per **architecture layer**, each run
by its own subagent and producing its own dated report file (see "Process"
below). This keeps each pass's context small, makes the sweep resumable
across sessions, and lets the user later fix one layer's findings at a time
(§12) instead of wading through everything at once.

**The layers:** the layer table (layer key, report suffix, directory scope,
primary checklist sections) comes from the `Full-codebase sweep layers`
subsection of the project's `Code review checks`. If there is none, the sweep is not
available — say so and ask the user for a scope instead.

**Process — one subagent per layer:**

Each layer's review runs in its own subagent (`Agent` tool,
`subagent_type: general-purpose`), not inline in the main conversation.

1. Before starting, check `<reports>/code-review/` for layer report files
   already dated today. If a sweep was interrupted in an earlier session,
   resume by only dispatching subagents for the layers that don't have a
   report yet for today's date.
2. The remaining applicable layers are independent of each other, so
   dispatch all of them together: one `Agent` call per layer, all issued
   in the **same message** so they run in parallel.
3. Each subagent's prompt must be self-contained — it has no memory of this
   conversation — and must include:
   - The layer key, its directory scope, and its primary checklist sections
     (copy the relevant row from the layer table into the prompt).
   - An instruction to first read the `code-reviewer` skill's `SKILL.md`
     (the severity legend and the project-checklists step), then the
     subsections of the project's `Code review checks` (in `docs/frontend.md`
     or `docs/backend.md`) that hold that layer's primary checklist sections, plus the "Review Report Format &
     Output File" section (§11, in `reference/report-format-and-fix-workflow.md`) — don't restate the checklist in the prompt.
   - The exact output path to write:
     `<reports>/code-review/CODE_REVIEW_FINDINGS_{today's date}_{layer suffix}.md`
     (apply the `-2`/`-3` collision rule from §11 itself if the file
     already exists).
   - An explicit instruction to only read/review files under that layer's
     own directory scope, and to write the report file itself rather than
     just returning findings as chat text.
   - A short return-message instruction: report back the Critical/Warning/
     Suggestion counts and the path it wrote, not the full findings text.
4. These are launched as background agents by default — don't poll or
   sleep waiting for them; you'll be notified as each one completes.
5. Once every dispatched subagent has completed, give a short in-chat
   summary: the list of report files written, and the Critical/Warning/
   Suggestion count per layer.

Severity levels used throughout:

| Level             | Meaning                                                                              |
| ----------------- | ------------------------------------------------------------------------------------ |
| 🔴 **CRITICAL**   | Breaks correctness or data integrity (including an access-control gap). Must be fixed before merging. |
| 🟡 **WARNING**    | Violates a project standard or will cause maintainability problems. Should be fixed. |
| 🟢 **SUGGESTION** | Style preference or minor improvement. Fix if convenient.                            |

---

## Reference

| Sections | Read when you're… | File |
| -------- | ----------------- | ---- |
| §11-12 Review report format & output file, fix workflow | writing the dated report (every run), or fixing findings from an existing report | `reference/report-format-and-fix-workflow.md` |

The checklist sections themselves (architecture, data access, state,
conventions, testing, pitfalls, metrics) are the project's, in the `Code review checks` section of its
`docs/frontend.md` and/or `docs/backend.md`. **Section numbers (§N) are
stable** — other skills, reports and ADRs cite them — and each one is in
exactly one place.
