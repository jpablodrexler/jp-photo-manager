# Code Reviewer — Review Report Format & Fix Workflow

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 16. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Review: <file or PR title>

### 🔴 Critical
- `path/to/File.java:42` — <issue description>

### 🟡 Warnings
- `path/to/file.ts:15` — <issue description>

### 🟢 Suggestions
- `path/to/File.java:88` — <issue description>

### Verdict
<One or two sentences: overall quality, whether it is safe to merge, and the
single most important thing to fix first.>
```

If there are no findings in a severity category, omit that category entirely.

### Write the report to a dated markdown file

Every time this skill runs — full-codebase sweep, single file, or PR review —
also write the findings to a new markdown file so work can be resumed later
without re-deriving context.

**Scoped review (single file, PR, feature, or one sub-project) — one file:**

- **Path:** `docs/reports/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}.md` (repo
  root, today's date, ISO 8601). If a file for that date already exists (e.g.,
  a second review the same day), append `-2`, `-3`, etc. before `.md` rather
  than overwriting the earlier run's report.

**Full-codebase sweep (§"Full-Codebase Sweeps: Review by Layer") — one file
per layer:**

- **Path:** `docs/reports/code-review/CODE_REVIEW_FINDINGS_{YYYY-MM-DD}_{layer}.md`,
  where `{layer}` is the report suffix from the layer table (e.g.
  `backend-domain`, `frontend-features`, `cross-cutting`). Same `-2`, `-3`
  collision rule, applied per date+layer combination.
- Write each layer's file as soon as that layer's pass is done (see the
  "Process" steps above) — don't hold all layers in memory to write at once.

**Both cases:**

- This directory is gitignored — reports are local working artifacts, not
  committed history. Create the directory if it doesn't exist yet.
- **Content:** the same Critical/Warnings/Suggestions grouping as the in-chat
  summary, using GitHub task-list checkboxes (`- [ ]`) per finding instead of
  plain bullets, so items can be checked off as they're fixed. Include a short
  header noting the scope reviewed (full codebase sweep + layer name, vs. a
  specific file/PR) and which commit(s)/state the review was run against.
  Only include categories that have findings — omit empty ones.
- **Scope of content:** write only what was actually found in *this* run —
  don't carry forward unresolved items from a previous dated report by
  default. If asked to produce a combined or updated backlog, do that
  explicitly as its own step rather than silently merging.
- Do not overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.

---

## 17. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new review.
It is interactive and incremental: fix a chunk, check it off, ask what's next.
**This workflow never commits** — all changes stay uncommitted in the working
tree for the user to review and commit themselves.

### 17.1 Locate the report(s) for a date

1. If the user names a specific report file, skip straight to §17.2 with that
   file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `docs/reports/code-review/CODE_REVIEW_FINDINGS_*.md` file. If none exists, say so
   and stop — there is nothing to fix.
2. List every report file for that date (there may be several `-2`/`-3` reruns
   per layer — treat each filename, suffix included, as a distinct report).
   For each, quickly check whether it has any unchecked (`- [ ]`) boxes left;
   drop fully-checked-off reports from the list.
3. **If exactly one report remains**, use it directly — don't make the user
   pick from a list of one. **If more than one remains** (the normal case
   after a layer-split full-codebase sweep, or several same-day scoped
   reviews), ask the user which report to work on first, showing the layer
   name (or scope, for a pre-layer-split report) and a rough Critical/Warning/
   Suggestion count for each so they can prioritize. Work on exactly one
   report at a time — don't mix findings from two reports into a single fix
   pass.

### 17.2 Read the report, then ask what to fix

1. Read the full report before asking anything, so the menu of categories/
   findings you present next is accurate and reflects what's already checked
   off from prior sessions.
2. Do not silently pick a starting point or scope. Ask the user whether they
   want to fix:
   - An entire severity category (🔴 Critical, 🟡 Warning, or 🟢 Suggestion), or
   - A specific finding — let them name it, or list the still-unchecked
     findings in a category for them to choose from.

   Only offer categories/findings that still have unchecked (`- [ ]`) boxes; a
   category with everything already checked off isn't worth presenting again.

### 17.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected file(s) and understand the finding in the full context
   of the surrounding code before changing anything — the report is a
   pointer, not a substitute for reading the code.
2. Apply the fix. The report tells you what's wrong; the checklist sections
   above (1–15, 18) tell you what "right" looks like for that category of
   issue.
3. If a fix hinges on a real design decision rather than just applying a
   known pattern — e.g. a live-data/migration-compatibility risk, a public
   API/contract change, or several equally valid approaches — stop and ask
   the user before proceeding instead of picking one unilaterally.
4. Verify the fix before moving on: compile/build, then run the narrowest
   relevant test scope (single test class/spec). For Java, prefer a **clean**
   compile/test (`mvn clean test-compile` or `mvn clean test`) after changing
   any method or type signature — incremental builds can silently skip
   recompiling dependent test files and report a false green.
5. Update tests for the new shape of the code (renamed types/methods, changed
   signatures, moved files) — don't leave stale references or stale test
   names behind.
6. Mark the finding complete in the report file immediately, not batched at
   the end: change `- [ ]` to `- [x]` and append a bolded outcome note to the
   same line, in the same style as prior fix sessions:
   - `**Fixed:** <what changed and why, one or two sentences>.`
   - `**Evaluated, no change made:** <rationale>` — for findings where, after
     investigation, the right call is to leave the code as-is. Document why
     so the item isn't silently dropped.
   Updating the report per-finding (not at the end of the whole scope) means
   an interrupted session still leaves an accurate resume point.
7. If fixing one finding incidentally resolves another still-unchecked one
   (e.g. moving a domain model also fixes a naming-convention Warning on the
   same class), check that one off too with a note explaining it was fixed as
   a byproduct — don't leave it unchecked just because it wasn't the primary
   target.

### 17.4 Continue until done

After finishing the selected scope, run the widest verification available
(full backend suite, full frontend Cypress suite, lint, production build)
once before asking what's next — don't let per-finding narrow tests substitute
for a full-suite check when a scope is done. Then:

- If the current report still has unchecked findings, repeat from §17.2: ask
  what to fix next **within the same report**, offering only what's still
  unchecked.
- If the current report is now fully checked off, go back to §17.1 — re-list
  the remaining reports for the date (if this was a layer-split sweep, there
  are likely others) and ask the user whether to move to another one or stop.

Stop when the user says to stop, or when every report for the date is fully
checked off.

### 17.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command as
part of this workflow, not even implicitly. Leave all changes uncommitted so
the user can review the diff and commit it themselves.

---
