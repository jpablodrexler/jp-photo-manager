---
name: bug-report
description: >-
  Adds a bug to JPPhotoManagerWeb/docs/backlog/bugs-open.md: drafts the row
  (Severity, Area, Environment, Status, Summary), assigns the next BUG-NNN id,
  writes the details file JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md
  (repro / expected / actual), inserts it into the Recommended fix order, and
  confirms with the user before writing. Creates the backlog files on first
  use — a missing backlog is a reason to run this skill, never to skip it.
  TRIGGER proactively whenever the user reports a bug, asks to file/log one,
  describes something broken or producing a wrong result (symptom, stack
  trace, console error), or hands over testing-session notes, even mid-task.
  When fresh bugs should also be fixed now ("plan and fix these bugs"), run
  this once per bug FIRST, THEN hand off to bug-fix (one) or bugs-batch-fix
  (several). Bug-family counterpart to feature-plan.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.2"
---

Add a new bug row to `JPPhotoManagerWeb/docs/backlog/bugs-open.md` and
create its details file `JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md`,
drafted from the user's report and confirmed before writing. This is the
bug family's analogue of `feature-plan` — same shape (draft, confirm,
insert, order), different backlog file and column vocabulary.

**Layout**: the two backlog files (`bugs-open.md`, `bugs-fixed.md`) hold
only tables (plus the Column legend and Recommended fix order). Every
bug's write-up lives in its own file
`JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md` — a stable path that
never moves, even when the bug is closed. Severity, Area, Environment,
and Status live **only** in the table row (single source of truth), never
in the per-bug file. Each table row's first cell links to the file:
`[BUG-NNN](bugs/BUG-NNN.md)` (relative to
`JPPhotoManagerWeb/docs/backlog/`).

**Input**: A description of the bug — as much or as little as the user
gives (a one-liner, a paragraph, or raw notes from a manual testing
session). May optionally include explicit values for Severity / Area /
Environment, and explicit repro steps / expected / actual — use whatever
is given directly and only draft the rest.

If the user pasted **several** distinct bugs at once (common straight out
of a testing session), process them one at a time: draft the first, run
the confirmation, write it, then move to the next. Do not batch several
unconfirmed rows into a single write.

---

## Steps

### 1. Make sure there's enough to draft from

A useful bug row needs, at minimum: what the user did, what they expected,
and what actually happened. If the report is just a symptom with no
context ("the gallery is broken"), ask one clarifying question — the
route/action, and the observed vs expected behaviour — before drafting.
If the user already gave enough to reproduce it mentally, proceed without
asking.

### 2. Read both backlog files, creating them if they don't exist yet

Read:
- `JPPhotoManagerWeb/docs/backlog/bugs-open.md`
- `JPPhotoManagerWeb/docs/backlog/bugs-fixed.md`

**If `JPPhotoManagerWeb/docs/backlog/bugs-open.md` doesn't exist yet** (the
normal case the first time this skill runs), create it with this skeleton
before continuing — don't silently invent a different structure:

```markdown
# Open Bugs

## Bug List

| Bug ID | Severity | Area | Environment | Status | Fix | Summary |
| ------ | -------- | ---- | ----------- | ------ | --- | ------- |

_No open bugs._

## Column legend

- **Severity**: `S1` (blocker — app unusable, data loss, security hole, auth bypass) · `S2` (major — a feature is broken with no workaround) · `S3` (minor — a feature is impaired but has a workaround) · `S4` (trivial — cosmetic, copy, layout polish)
- **Area**: the affected route, component, or backend area — e.g. `/gallery`, `/albums`, `auth`, `nav`, `backend`, `db` (Flyway/JPA), `kafka`, `infra`. Use the route where the user hit it; use `db` for a migration/entity/query bug with no single owning route, `backend` for a controller/service bug.
- **Environment**: `local` (reproduces on a local dev run only) · `deployed` (reproduces on the deployed cluster/compose stack only) · `both`
- **Status**: `⬜ Open` · `🔶 In Progress` (a `bug-fix` / `bugs-batch-fix` run has started it) · `✅ Fixed` (transient — `bugs-archive` moves the row to `bugs-fixed.md` in the same pass it sets this) · `🚫 Won't fix` · `❓ Cannot reproduce`
- **Fix**: PR link or commit hash once closed; blank while open.

## Details

<!-- Per-bug details live in bugs/BUG-NNN.md (stable path), one file per bug, linked from its row here. `bugs-archive` moves only the table row to bugs-fixed.md and appends the Resolution line to that file. -->

Details for every bug live in `bugs/BUG-NNN.md`, linked from its row.

## Recommended fix order

Severity tier first (S1 before S2 before S3 before S4), then blast radius / how often it bites, then quick-win effort, then bug number. Not a schedule — a reasoning aid for `bugs-next` and for whoever picks up the next fix by hand. Re-derive whenever the bug set or severities change.

_No open bugs to order._
```

**If `JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` doesn't exist yet
either**, create it with the matching archive skeleton so `bugs-archive`
has somewhere to write later:

```markdown
# Fixed Bugs

## Bug List

| Bug ID | Severity | Area | Environment | Fixed | Fix | Summary |
| ------ | -------- | ---- | ----------- | ----- | --- | ------- |

## Details

<!-- Per-bug details live in bugs/BUG-NNN.md (stable path). `bugs-archive` moves only the table row between bugs-open.md and bugs-fixed.md and appends the Resolution line to that file. -->

Details for every bug live in `bugs/BUG-NNN.md`, linked from its row.
```

**If the `JPPhotoManagerWeb/docs/backlog/bugs/` folder doesn't exist yet**,
create it (the first per-bug file in step 7 does this implicitly).

All of these are needed to pick a non-colliding id (step 3) even when
freshly created and empty.

### 3. Derive the Bug ID

Bug ids are `BUG-NNN` with a zero-padded 3-digit sequence
(`BUG-001`, `BUG-002`, …). Run
`python3 .claude/skills/bug-report/scripts/next_id.py <repo-root>` — it
scans both table files and the filenames in
`JPPhotoManagerWeb/docs/backlog/bugs/` for every `BUG-NNN`, takes the
highest `NNN` found, and returns `BUG-<max+1>` zero-padded (or `BUG-001`
for a fresh backlog), printed as JSON: `{"next_id": "BUG-NNN"}`. Numbers
are one global sequence across both tables and the folder and are never
reused, even for a `🚫 Won't fix` or `❓ Cannot reproduce` entry.

### 4. Draft the attribute columns

Using the **Column legend** in `bugs-open.md` as the source of truth for
the value vocabulary (read it fresh each run — don't hardcode a copy here,
so this skill can't drift out of sync with the legend):

- **Severity** (`S1`–`S4`) — if the user gave one, use it (sanity-check
  it's valid). Otherwise infer: anything touching auth, a schema
  migration, data loss, or "can't use the app" is `S1`; a broken feature
  with no workaround is `S2`; an annoyance with a workaround is `S3`;
  cosmetic is `S4`.
- **Area** — the route/component/backend area where it was hit.
- **Environment** — `local` / `deployed` / `both`. Default to `local` if
  the user was running a local dev stack and didn't say; use `both` only
  if they confirmed it on the deployed stack too.

### 5. Draft the one-line Summary and the per-bug file content

**Summary** (the table cell): one sentence naming the concrete
symptom — the component/route/endpoint and what visibly goes wrong — not a
vague restatement of the title.

**Per-bug file** (`JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md`), in
this exact shape — first line an H1 `# BUG-NNN — <short title>`, one
blank line, then the bullets:

```markdown
# BUG-NNN — <short title>

- **Steps to reproduce:**
  1. <step>
  2. <step>
- **Expected:** <what should happen>
- **Actual:** <what happens instead, including any stack-trace / console error text verbatim if short>
- **Environment:** <local / deployed / both> — <browser / viewport / account / backend-state notes if relevant>
- **Notes:** <suspected cause, related code path, screenshots referenced by filename — omit the line if there's nothing>
```

Do not put Severity, Area, or Status in the file — they live only in the
table row. Fill repro/expected/actual from what the user gave; if a step
is genuinely unknown, write `<unknown — needs repro>` rather than
inventing it.

### 6. Confirm with the user before writing

Display the fully drafted row and file content:

```
## Draft Bug

**BUG-NNN — <title>**

**Severity:** <S?>  **Area:** <area>  **Environment:** <env>  **Status:** ⬜ Open

<per-bug file content as above>
```

Use the **AskUserQuestion tool** with a single question — "Add this bug to
the backlog?" — options: "Yes, add it as drafted", "Let me edit something
first" (resolve free-text edits against the draft and re-display before
writing), "Cancel". Do not write anything under
`JPPhotoManagerWeb/docs/backlog/` (other than the step-2 scaffolding)
until the user confirms.

### 7. Insert the row, create the file, and add the order entry

Once confirmed:

1. Create `JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md` with the step-5
   content (create the `bugs/` folder if needed). The file name is exactly
   the Bug ID — it never changes afterwards.
2. In a single edit to `bugs-open.md`: append the row to the `## Bug List`
   table (removing the `_No open bugs._` placeholder line if present),
   preserving the pipe formatting, with the first cell as the link —
   `| [BUG-NNN](bugs/BUG-NNN.md) | <S?> | <area> | <env> | ⬜ Open | | <summary> |`.
   The new row goes directly under the last table row (on a fresh table,
   directly under the `| ------ |` separator row) — no blank line inside
   the table.
3. In the same edit, insert the bug into `## Recommended fix order` (see
   step 8). This is **not** optional — a backlog with rows missing from
   the order is worse than one with a slightly-uncertain placement.

Do not append anything to the `## Details` section — it holds only the
pointer sentence. Do not touch `bugs-fixed.md` beyond the one-time
skeleton creation in step 2.

### 8. Update the recommended fix order

The `## Recommended fix order` section is a numbered list of
`` BUG-NNN — <title> `` entries, ordered severity tier first (S1 before S2
before S3 before S4), then blast radius, then quick-win effort, then bug
number.

1. Find the tier boundary: the last existing entry whose Severity is the
   same or higher than the new bug's, and the first whose Severity is
   strictly lower. The new bug goes between them (same-severity entries
   keep their existing relative order; the new one lands after all of
   them, unless the user's description makes it obviously higher blast
   radius than a same-tier peer — then place it ahead of that peer and say
   why in the entry line).
2. If the list still reads `_No open bugs to order._`, the new bug becomes
   entry `1.`.
3. Renumber `1.`, `2.`, `3.`, … after inserting.

Write the entry line in the same style as its neighbours — severity, the
one-phrase reason for its placement (blast radius / quick win / blocks
testing of X), and environment where relevant.

### 9. Display confirmation

```
## Bug Added

**BUG-NNN — <title>** added to bugs-open.md (Severity <S?>, Area <area>, <env>), details in JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md, inserted at position <N> of the recommended fix order.
```

---

## Guardrails

- Never write a bug row or a `bugs/BUG-NNN.md` file before the user
  confirms the draft in step 6 — steps 2–5 are draft-only in memory
  (except the one-time skeleton creation in step 2, which is scaffolding,
  not a bug).
- Never edit `bugs-fixed.md` beyond creating its initial skeleton — this
  skill only adds to `bugs-open.md` and creates the new per-bug file.
  Closing a bug is `bugs-archive`'s job.
- Never append a details block to `bugs-open.md` / `bugs-fixed.md` — the
  details live only in `bugs/BUG-NNN.md`.
- Never reuse a `BUG-NNN` id — the sequence spans both tables and the
  `bugs/` folder and is monotonic, including for `🚫 Won't fix` /
  `❓ Cannot reproduce` entries.
- Read the Column legend fresh each run rather than relying on a
  remembered copy of the value vocabulary.
- Preserve the exact Markdown table formatting (pipe characters) of the
  Bug List table, including the `[BUG-NNN](bugs/BUG-NNN.md)` link in the
  first cell.
- Step 6's confirmation is mandatory, even under an "Auto Mode" or similar
  autonomous-operation instruction that biases toward proceeding without
  asking — that bias never applies to writing a new row into the backlog.
- If the user pasted several bugs at once, still confirm each one
  individually — do not write a batch of unconfirmed rows in one pass.
- This skill only records a bug. It never starts a branch, writes a test,
  or attempts a fix — that's `bug-fix`.
