# Code Reviewer — Review Report Format & Fix Workflow

_Part of the `code-reviewer` skill — see `../SKILL.md` for the workflow, the severity legend and the project checklists. Load this file only when your review touches this topic._

## 11. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Review: <file or PR title>

### 🔴 Critical
- `path/to/file:42` — <issue description>

### 🟡 Warnings
- `path/to/file:15` — <issue description>

### 🟢 Suggestions
- `path/to/file:88` — <issue description>

### Verdict
<One or two sentences: overall quality, whether it is safe to merge, and the
single most important thing to fix first.>
```

If there are no findings in a severity category, omit that category
entirely.

### Write the report to a dated markdown file

Every time this skill runs — full-codebase sweep, single file, or PR
review — also write the findings to a new markdown file so work can be
resumed later without re-deriving context.

**Scoped review (single file, PR, or feature) — one file:**

- **Path:** `<reports>/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}.md` (today's
  date, ISO 8601). If a file for that date already exists,
  append `-2`, `-3`, etc. rather than overwriting the earlier run's report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:** `<reports>/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
  where `{layer}` is the report suffix from the layer table. Same `-2`,
  `-3` collision rule.
- Write each layer's file as soon as that layer's pass is done — don't hold
  all layers in memory to write at once.

**Both cases:**

- This directory is gitignored — reports are local working artifacts, not
  committed history. Create the directory if it doesn't exist yet.
- **Content:** the same Critical/Warnings/Suggestions grouping as the
  in-chat summary, using GitHub task-list checkboxes (`- [ ]`) per finding
  instead of plain bullets. Include a short header noting the scope
  reviewed and which commit/state the review was run against. Only include
  categories that have findings.
- **Scope of content:** write only what was actually found in *this* run —
  don't carry forward unresolved items from a previous dated report by
  default.
- Do not overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.

---

## 12. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new
review. It is interactive and incremental: fix a chunk, check it off, ask
what's next. **This workflow never commits** — all changes stay uncommitted
in the working tree for the user to review and commit themselves.

### 12.1 Locate the report(s) for a date

1. If the user names a specific report file, skip straight to §12.2 with
   that file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `<reports>/code-review/CODE_REVIEW_FINDINGS_*.md` file. If none exists, say
   so and stop.
2. List every report file for that date. For each, quickly check whether
   it has any unchecked (`- [ ]`) boxes left; drop fully-checked-off
   reports from the list.
3. **If exactly one report remains**, use it directly. **If more than one
   remains** (the normal case after a layer-split sweep, or several
   same-day scoped reviews), ask the user which report to work on first,
   showing the layer name (or scope) and a rough Critical/Warning/
   Suggestion count for each. Work on exactly one report at a time.

### 12.2 Read the report, then ask what to fix

1. Read the full report before asking anything.
2. Ask the user whether they want to fix an entire severity category, or a
   specific finding — only offer categories/findings with unchecked boxes
   remaining.

### 12.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected file(s) and understand the finding in full context
   before changing anything.
2. Apply the fix.
3. If a fix hinges on a real design decision rather than applying a known
   pattern, stop and ask the user before proceeding.
4. Verify the fix: run the narrowest relevant test scope
   (the affected test file or suite only).
5. Update tests for the new shape of the code — don't leave stale
   references or stale test names behind.
6. Mark the finding complete in the report file immediately: change
   `- [ ]` to `- [x]` and append a bolded outcome note:
   - `**Fixed:** <what changed and why, one or two sentences>.`
   - `**Evaluated, no change made:** <rationale>`
7. If fixing one finding incidentally resolves another still-unchecked one,
   check that one off too with a note explaining it was fixed as a
   byproduct.

### 12.4 Continue until done

After finishing the selected scope, run the widest verification available
(the full test suite and a production build) once before asking what's next. Then:

- If the current report still has unchecked findings, repeat from §12.2.
- If the current report is now fully checked off, go back to §12.1.

Stop when the user says to stop, or when every report for the date is
fully checked off.

### 12.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command
as part of this workflow. Leave all changes uncommitted so the user can
review the diff and commit it themselves.

---
