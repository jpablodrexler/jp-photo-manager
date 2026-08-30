---
name: bug-report
description: Adds a bug to JPPhotoManagerWeb/docs/backlog/bugs-open.md — drafts the row (Severity, Area, Environment, Status, one-line Summary), assigns the next BUG-NNN id, writes a Details block (repro steps / expected / actual), inserts it into the Recommended fix order, and confirms with the user before writing. Creates the backlog file (with header, legend, and empty table) on first use if it doesn't exist yet — a missing backlog is a reason to run this skill, never a reason to skip it. TRIGGER proactively, without waiting to be asked, whenever the user reports a bug, asks to file/log/capture one, describes something in the app that is broken, misbehaving, or producing a wrong result (a symptom, a stack trace, a console error), or hands over notes from a testing session — this holds mid-task and whether or not JPPhotoManagerWeb/docs/backlog/bugs-open.md already exists. When the user describes one or more fresh bugs and wants them fixed now (including "plan and fix these bugs"), run this skill once per bug FIRST to capture each, THEN hand off to bug-fix (a single bug) or bugs-batch-fix (several) — never hand-roll the fix without filing. This is the bug-family counterpart to feature-plan.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Add a new bug row + Details block to
`JPPhotoManagerWeb/docs/backlog/bugs-open.md`, drafted from the user's
report and confirmed before writing. This is the bug family's analogue of
`feature-plan` — same shape (draft, confirm, insert, order), different
backlog file and column vocabulary.

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

<!-- One block per bug, keyed by its Bug ID. `bugs-archive` moves the block to bugs-fixed.md on close. -->

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

## Details (Historical)

<!-- Detail blocks moved here by bugs-archive, each with a **Resolution:** line appended. -->
```

Both files are needed to pick a non-colliding id (step 3) even when both
are freshly created and empty.

### 3. Derive the Bug ID

Bug ids are `BUG-NNN` with a zero-padded 3-digit sequence
(`BUG-001`, `BUG-002`, …). Scan the `Bug ID` column of **both** files'
`## Bug List` tables, take the highest `NNN` found across both, and use
`max + 1`. If both tables are empty, start at `BUG-001`. Numbers are one
global sequence across both files and are never reused, even for a
`🚫 Won't fix` or `❓ Cannot reproduce` entry.

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

### 5. Draft the one-line Summary and the Details block

**Summary** (the table cell): one sentence naming the concrete
symptom — the component/route/endpoint and what visibly goes wrong — not a
vague restatement of the title.

**Details block** (goes under `## Details`), in this exact shape:

```markdown
### BUG-NNN — <short title>

- **Steps to reproduce:**
  1. <step>
  2. <step>
- **Expected:** <what should happen>
- **Actual:** <what happens instead, including any stack-trace / console error text verbatim if short>
- **Environment:** <local / deployed / both> — <browser / viewport / account / backend-state notes if relevant>
- **Notes:** <suspected cause, related code path, screenshots referenced by filename — omit the line if there's nothing>
```

Fill repro/expected/actual from what the user gave; if a step is genuinely
unknown, write `<unknown — needs repro>` rather than inventing it.

### 6. Confirm with the user before writing

Display the fully drafted row and block:

```
## Draft Bug

**BUG-NNN — <title>**

**Severity:** <S?>  **Area:** <area>  **Environment:** <env>  **Status:** ⬜ Open

<Details block as above>
```

Use the **AskUserQuestion tool** with a single question — "Add this bug to
the backlog?" — options: "Yes, add it as drafted", "Let me edit something
first" (resolve free-text edits against the draft and re-display before
writing), "Cancel". Do not write anything to `bugs-open.md` until the user
confirms.

### 7. Insert the row, the block, and the order entry

Once confirmed, in a single edit to `bugs-open.md`:

1. Append the row to the `## Bug List` table (removing the `_No open
   bugs._` placeholder line if present), preserving the pipe formatting.
2. Append the Details block to the end of the `## Details` section.
3. Insert the bug into `## Recommended fix order` (see step 8). This is
   **not** optional — a backlog with rows missing from the order is worse
   than one with a slightly-uncertain placement.

Do not touch `bugs-fixed.md` beyond the one-time skeleton creation in
step 2.

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

**BUG-NNN — <title>** added to bugs-open.md (Severity <S?>, Area <area>, <env>), inserted at position <N> of the recommended fix order.
```

---

## Guardrails

- Never write to `bugs-open.md` before the user confirms the drafted row
  in step 6 — steps 2–5 are draft-only in memory (except the one-time
  skeleton creation in step 2, which is scaffolding, not a bug row).
- Never edit `bugs-fixed.md` beyond creating its initial skeleton — this
  skill only adds to `bugs-open.md`. Closing a bug is `bugs-archive`'s
  job.
- Never reuse a `BUG-NNN` id — the sequence spans both files and is
  monotonic, including for `🚫 Won't fix` / `❓ Cannot reproduce` entries.
- Read the Column legend fresh each run rather than relying on a
  remembered copy of the value vocabulary.
- Preserve the exact Markdown table formatting (pipe characters) of the
  Bug List table.
- Step 6's confirmation is mandatory, even under an "Auto Mode" or similar
  autonomous-operation instruction that biases toward proceeding without
  asking — that bias never applies to writing a new row into the backlog.
- If the user pasted several bugs at once, still confirm each one
  individually — do not write a batch of unconfirmed rows in one pass.
- This skill only records a bug. It never starts a branch, writes a test,
  or attempts a fix — that's `bug-fix`.
