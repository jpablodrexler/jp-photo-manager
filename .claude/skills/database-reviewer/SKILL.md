---
name: database-reviewer
description: >
  Database review skill: reviews schema migrations and the queries that use
  them for normalization, naming, data types, index coverage and migration
  safety, plus the project's own database checklists. TRIGGER when adding or
  reviewing a migration or any other schema change (table, column, index,
  constraint, view or function) — including when implementing planned tasks
  that touch the schema. Do not wait to be asked: review new/changed
  migrations proactively. Also TRIGGERS when asked to audit the entire
  migration history for schema drift or missing indexes. Also TRIGGERS when
  asked to fix, address, resolve, or work through findings from an existing
  dated DATABASE_REVIEW_FINDINGS report — see "Fix Workflow" below.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Database Reviewer Skill

Review the relational schema of the project: its migration files, and the
queries the application issues against them. The migrations are the source
of truth for what exists in the database; the stack-specific checks (the
migration tool, the data-access layer, access control inside the database)
live in the project's own checklist files — see Workflow step 4.

`<reports>` is `docs/reports` unless `.backlog-kit.toml` sets
`[paths] backlog_dir` (reports sit in the `reports` folder next to the
backlog's parent). The reference docs named below (`docs/architecture.md`, `docs/frontend.md`, …) sit in that same `docs` folder: the folder that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`).

**Never run `git commit`, `git push` or open a PR.**

This skill covers two distinct workflows:

- **Review** (below) — produce findings and a dated report. This is the
  default when reviewing a new/changed migration.
- **Fix** (§8) — work through the findings in an *existing* dated report,
  fixing them and checking them off. Use this when asked to fix, address, or
  resolve issues from a report rather than to review the schema.

## Workflow

1. Identify the **scope**: a full audit of every migration (and every query
   that reads those tables), or a scoped review (one new migration, one
   table, one query).
2. Read every migration file in scope **in the order the migration tool
   applies them** (the project checklist says how they are named and sorted)
   — schema history is cumulative, so a column added in one migration may
   be altered or have its policies or indexes replaced in a later one.
   Reviewing a migration in isolation misses that.
3. For each table touched, find every query in the application code that
   reads or writes it, and check whether the query's filter/sort columns
   are covered by an actual index (§3) — not just plausible-looking ones.
4. **Project checklists:** read the project's `docs/database.md` and, in it,
   the section named `Database review checks` (the kit convention: that doc
   holds the tables, access rules, migration history and database workflow,
   and what this review checks in the project). Work through the subsections
   that apply to what changed, one by one; the context and workflow
   subsection and the access-control one apply to every review. If the doc
   has no such section, review with the frame above plus what the project's
   CLAUDE.md and docs say, and state in the report that no project checklist
   was found. A `§N` inside the project's section refers to that section's
   own numbering; a `§N` in this file refers to this file.
5. Work through the checklist below, section by section.
6. Report findings grouped by **severity**, then by file.
7. Summarise with a short verdict and the top action items, then write the
   full report to a new, dated markdown file — see "Review Report Format &
   Output File" (§7) below. Do this on every run, not just full audits: a
   single-migration review still gets its own dated report.

Severity levels:

| Level             | Meaning                                                                                     |
| ----------------- | --------------------------------------------------------------------------------------------- |
| 🔴 **CRITICAL**   | Data integrity risk, an access-control gap that exposes or hides data incorrectly, or a query that will corrupt/silently drop data. Must be fixed before merging. |
| 🟡 **WARNING**    | Violates a project schema standard, causes avoidable query cost, or will cause maintainability/drift problems. Should be fixed. |
| 🟢 **SUGGESTION** | Style preference, minor normalization nit, or hardening idea. Fix if convenient.             |

---

## 1. Normalization

🔴 Flag a new column that duplicates data already derivable from a join
(storing a denormalized copy of a value reachable via an existing foreign
key) — the two can drift out of sync on update.

🟡 Flag a repeating group implemented as multiple similarly-named columns
(`tag_1`, `tag_2`, `tag_3`) instead of a join table.

🟢 Flag over-normalization: a lookup table for a fixed, small (<10), rarely
changing set of values that's only ever referenced from one table might be
better as a `CHECK` constraint on a text/enum-like column.

---

## 2. Column Naming & Data Types

### 2.1 Naming

Names follow the convention the project's checklist records for tables,
primary keys, foreign keys, booleans, timestamps, indexes and constraints.
With no such record, derive the convention from the existing schema and flag
only a deviation from what the schema already does consistently — never
impose a convention from outside it. Do flag a *foreign key* column that
isn't named after the table it references the way the schema's other foreign
keys are.

### 2.2 Data types

🟡 Flag a new timestamp column without time-zone information when the
schema's existing timestamp columns carry it; stay consistent.

🟡 Flag a foreign key column whose type doesn't match its referenced PK's
type exactly.

🟡 Flag a new enum-like status column stored as free-text without a
`CHECK` constraint (or an equivalent constraint) to constrain its value set
— invalid strings will silently persist.

---

## 3. Indexes

### 3.1 Coverage for foreign keys

🔴 Flag any new foreign-key column that has no index. Relational engines do
not generally index the referencing side of a foreign key for you, and a
column that every query filters or joins on without an index is a full table
scan. Treat "add a foreign-key column with no index" as a standing 🔴 for
any future table; the project checklist lists any known existing gap.

### 3.2 Coverage for query predicates

🔴 Flag a column that appears in a query's filter, join, grouping or sort
clause on a table with a non-trivial expected row count, with no supporting
index.

🟡 Flag a query that filters or sorts on a column covered only by a
*composite* index where the queried column isn't the leftmost member.

🟡 Flag a prefix query (`value%`, no leading `%`) against an unindexed
`text` column — a plain B-tree index still helps a left-anchored pattern.

🟢 Flag a query with a leading wildcard (`%value%`) and note that a plain
B-tree index cannot help it — a trigram or full-text index would be the
fix, but only worth flagging as a suggestion since it's a bigger
infrastructure change than a routine index addition.

### 3.3 Partial and covering indexes

🟢 Suggest a partial index (`WHERE` clause on the `CREATE INDEX` itself)
when a boolean/nullable column is queried almost exclusively for one value.

🟢 Flag redundant indexes: two indexes on the same table where one's column
list is a strict prefix of the other's.

---

## 4–5. Reserved for project checklists

The numbers 4 and 5 are left to the project's checklist files (typically
access control inside the database, and views and functions); they are read
in Workflow step 4. Without such files there is nothing to check under them.

---

## 6. Migration Mechanics

🔴 Flag any modification to an **existing, already-applied** migration file.
Once applied to a shared environment, a migration's content is fixed — the
migration tool tracks applied migrations by name or checksum, and editing a
historical file desyncs local state from what's actually on the database.
Any change goes in a new migration file instead.

🟡 Flag a migration that adds a `NOT NULL` column to an existing populated
table without either a `DEFAULT` or a backfill step in the same migration —
the right shape is to add the column nullable with a `DEFAULT`,
delete/backfill the rows that would otherwise violate the constraint, *then*
`ALTER COLUMN ... SET NOT NULL` in the same migration, all before anything
starts relying on the column being non-null.

🟡 Flag a migration that both alters schema **and** performs a large data
backfill via `UPDATE`/`DELETE` on a large table without considering lock
duration or whether the delete is actually safe — a blanket `DELETE` that is
fine on a handful of rows would need a batching/backfill strategy discussion
against a large populated table.

🟢 Flag a migration missing a header comment explaining *why* a
non-obvious step is being taken.

---

## 7. Review Report Format & Output File

Structure the in-chat summary as follows:

```
## Database Review: <migration/table/PR title>

### 🔴 Critical
- `<migration file>:5` — <issue description>

### 🟡 Warnings
- `<query or service file>:16` — <issue description>

### 🟢 Suggestions
- `<migration file>:18` — <issue description>

### Verdict
<One or two sentences: overall schema health, whether it is safe to
merge, and the single most important thing to fix first.>
```

If there are no findings in a severity category, omit that category
entirely.

### Write the report to a dated markdown file

Every time this skill runs — full audit or scoped review — also write the
findings to a new markdown file so work can be resumed later without
re-deriving context.

- **Path:** `<reports>/database-review/DATABASE_REVIEW_FINDINGS_{YYYY-MM-DD}.md`
  (today's date, ISO 8601). If a file for that date already
  exists, append `-2`, `-3`, etc. rather than overwriting the earlier run's
  report.
- This directory is gitignored — reports are local working artifacts, not
  committed history. Create the directory if it doesn't exist yet.
- **Content:** the same Critical/Warnings/Suggestions grouping as the
  in-chat summary, using GitHub task-list checkboxes (`- [ ]`) per finding
  instead of plain bullets. Include a short header noting the scope
  reviewed (full migration audit vs. a specific migration/table/PR) and
  which migration (name or version) the review was run against. When no
  project checklist was found, say so in the header. Only include
  categories that have findings.
- **Scope of content:** write only what was actually found in *this* run —
  don't carry forward unresolved items from a previous dated report by
  default.
- Do not overwrite or delete a previous dated report — each run's file is a
  point-in-time snapshot.

---

## 8. Fix Workflow

Use this workflow when asked to fix, address, resolve, or work through
findings from an **existing** dated report, instead of running a new
review. It is interactive and incremental: fix a chunk, check it off, ask
what's next. **This workflow never commits** — all changes stay uncommitted
in the working tree for the user to review and commit themselves.

### 8.1 Locate the report

1. If the user names a specific report file, skip straight to §8.2 with
   that file. Otherwise resolve a **date**: the date the user asked for, or
   (default) the most recent date that has any
   `<reports>/database-review/DATABASE_REVIEW_FINDINGS_*.md` file. If none
   exists, say so and stop.
2. If multiple `-2`/`-3` reruns exist for that date, drop any fully checked
   off. If more than one remains, ask the user which to work on first,
   showing a rough Critical/Warning/Suggestion count for each.

### 8.2 Read the report, then ask what to fix

1. Read the full report before asking anything.
2. Ask the user whether they want to fix an entire severity category, or a
   specific finding — only offer categories/findings with unchecked boxes
   remaining.

### 8.3 Fix loop

For the selected scope, work through each unchecked finding one at a time:

1. Read the affected migration/query file(s) in full context before
   changing anything.
2. Apply the fix:
   - **A fix to an already-applied migration is never "edit the file"** —
     per §6, create a new migration with the project's migration tool (the
     project checklist says how, so the filename is generated correctly)
     that performs the correction (a new constraint, an `ALTER TABLE
     ... ADD COLUMN`, a `CREATE INDEX`, etc.).
   - A finding that requires backfilling a large existing table needs a
     decision on batching strategy; if it isn't obvious, stop and ask the
     user rather than picking one unilaterally.
3. Apply and verify the fix the way the project checklist describes (how a
   new migration is applied, which command checks it). If that needs an
   environment that is not set up (for example a remote database that is not
   linked), stop and tell the user rather than setting it up on their
   behalf. With no project checklist, ask the user which command applies and
   verifies a migration.
4. Update any code or tests whose expectations depended on the old schema
   shape.
5. Mark the finding complete in the report file immediately: change
   `- [ ]` to `- [x]` and append a bolded outcome note:
   - `**Fixed:** <what changed and why, one or two sentences>.`
   - `**Evaluated, no change made:** <rationale>` — for findings where the
     right call, after investigation, is to leave the schema as-is.
6. If fixing one finding incidentally resolves another still-unchecked one
   (e.g. adding a missing index also fixes a related "known gap" finding),
   check that one off too with a note explaining it was fixed as a
   byproduct.

### 8.4 Continue until done

After finishing the selected scope, run the widest verification available
(the project checklist names it) once before asking what's next. Then:

- If the report still has unchecked findings, repeat from §8.2.
- If fully checked off, say so and stop.

Stop when the user says to stop, or when the report is fully checked off.

### 8.5 Never commit

Do not run `git add`, `git commit`, or any other state-changing git command
as part of this workflow. Leave all changes uncommitted so the user can
review the diff and commit it themselves.
