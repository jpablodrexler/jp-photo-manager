---
name: decision-record
description: >
  Records a lightweight Architecture Decision Record (ADR) in
  JPPhotoManagerWeb/docs/decisions/ whenever a real architectural or
  technical decision is made — a pattern chosen over an alternative, a
  library evaluated and rejected, a constraint discovered the hard way
  (e.g. "Cypress component mounting stays on `cypress/angular`, not
  `cypress/angular-zoneless`, even after the app itself migrated to
  zoneless change detection — the Angular Component Testing preset ships
  no zoneless-specific mount helper for this app's Cypress version").
  Decisions like these currently only survive as prose footnotes scattered
  through CLAUDE.md/docs/*.md, discoverable only by reading the whole
  file — this skill gives each one its own durable, numbered, append-only
  record instead. TRIGGER when the user asks to record/log a decision,
  write an ADR, or capture "why did we do X"-style context worth
  preserving. Also TRIGGER proactively, without being asked, immediately
  after making or reversing a non-obvious architectural choice during a
  feature or fix (a library swapped, a pattern rejected, a workaround
  adopted for a specific bug) — don't wait for the user to ask before the
  reasoning is lost. Does not replace docs/*.md reference material — see
  "Relationship to web-docs-sync" below.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Record a short, numbered Architecture Decision Record (ADR) under
`JPPhotoManagerWeb/docs/decisions/`, capturing a real decision, the
alternatives it rejected, and its consequences — not a description of
what the code does.

**Input**: A description of the decision (what was decided, and ideally
what alternative(s) were considered and why they lost). As much or as
little detail as the user gives; ask a clarifying question only if there's
no rejected alternative or no discoverable reason at all to work from (see
step 1).

---

## Steps

### 1. Confirm this is actually decision-worthy

Not every implementation choice needs a record. A good heuristic: **would a
future engineer or agent plausibly ask "why not X instead?" about this?** If
yes — record it. If it's just "how Y was implemented" with no real fork in
the road (no alternative seriously considered, no non-obvious constraint
that forced the outcome), it belongs in a code comment or the relevant
`docs/*.md` file, not an ADR — say so and skip rather than padding
`JPPhotoManagerWeb/docs/decisions/` with routine choices.

If the user's request doesn't make clear what was rejected or why, ask one
clarifying question before drafting rather than inventing a plausible-
sounding alternative.

### 2. Read the existing records and determine the next number

Read every file in `JPPhotoManagerWeb/docs/decisions/` (and its
`README.md` index, if present). **If the directory or index doesn't exist
yet**, this is the first record — create
`JPPhotoManagerWeb/docs/decisions/README.md` with this skeleton before
continuing:

```markdown
# Architecture Decision Records

Numbered, append-only records of non-obvious technical decisions for
JPPhotoManagerWeb — what was decided, what alternatives were rejected and
why, and what it costs going forward. This is a decision *history*, not
current-state reference material (that's `docs/architecture.md`,
`docs/backend.md`, etc.); a decision recorded here stays as-written even
after a later record supersedes it.

## Index

| # | Title | Status | Date |
| - | ----- | ------ | ---- |
```

Scan existing filenames for the `NNNN-` prefix (4-digit, zero-padded), take
the highest number found, and use `max + 1`. If none exist yet, start at
`0001`. Numbers are a single sequence and are never reused, even for a
decision later superseded or reversed.

### 3. Derive a short kebab-case title slug

Short, descriptive, no filler words — e.g. `keep-zone-based-cypress-mount`,
`kafka-per-instance-consumer-groups`. This becomes
`JPPhotoManagerWeb/docs/decisions/NNNN-title-slug.md`.

### 4. Draft the record

```markdown
# NNNN. <Title, a plain declarative sentence>

**Date:** <YYYY-MM-DD>
**Status:** Accepted

## Context

<The problem or question that forced a decision — 2-4 sentences. What was
about to go wrong, or what genuinely had more than one reasonable answer?>

## Decision

<What was decided, stated plainly — one or two sentences, not a summary of
the whole discussion.>

## Alternatives considered

- **<Alternative A>** — <why it was rejected>
- **<Alternative B>** — <why it was rejected>

## Consequences

- <What this makes easier, or what it protects against>
- <What this makes harder, or what future work needs to watch for>
```

Reuse whatever technical detail the user already gave; fill gaps with
reasonable, clearly-inferable detail consistent with the actual code/repo
state — don't invent an alternative that wasn't genuinely considered just
to fill the template.

### 5. Confirm with the user before writing

Display the fully drafted record and use the **AskUserQuestion tool** with
a single question — "Record this decision?" — options: "Yes, record it as
drafted", "Let me edit something first" (resolve free-text edits and
re-display before writing), "Cancel". Do not write anything until the user
confirms.

### 6. Write the record and update the index

1. Write `JPPhotoManagerWeb/docs/decisions/NNNN-title-slug.md`.
2. Append a row to the `## Index` table in
   `JPPhotoManagerWeb/docs/decisions/README.md`:
   `| NNNN | <Title> | Accepted | <YYYY-MM-DD> |`.

### 7. If this decision supersedes an earlier one

If the user's description reverses or replaces a decision already recorded
(e.g. re-evaluating a past rejection), find that earlier record and change
**only its `Status:` line** to `Superseded by NNNN` (using the new record's
number). Never edit or delete its `Context`/`Decision`/`Alternatives`/
`Consequences` sections — the record of what was believed and decided at
the time stays intact; only its current standing changes. Update its row in
the index table's `Status` column to match.

### 8. Display confirmation

```
## Decision Recorded

**#NNNN** `JPPhotoManagerWeb/docs/decisions/NNNN-title-slug.md` — <Title>
[If superseding:] Marked **#MMMM** as superseded by this record.

If this decision changes what CLAUDE.md/docs/*.md should currently say
about the topic, sync those files separately — this skill only records
*why* the decision was made, not the current-state narrative.
```

---

## Relationship to reference docs

`JPPhotoManagerWeb/docs/decisions/` is out of scope for the reference docs
under `JPPhotoManagerWeb/docs/*.md` — it's append-only decision history,
not current-state reference material those files are kept synced against
the code. The two complement each other but never overlap:

- **This skill** captures *why* a decision was made, once, at the time it
  was made. It never edits `CLAUDE.md`/`docs/*.md`.
- **The reference docs** (`docs/architecture.md`, `docs/backend.md`,
  `docs/frontend.md`, etc.) keep the *current-state* narrative accurate
  against the code as it exists today. If recording a new decision means
  that narrative needs updating, this skill says so in step 8's
  confirmation but doesn't do it — update those files separately.

---

## Guardrails

- **Records are append-only.** Never rewrite or delete an existing record's
  `Context`/`Decision`/`Alternatives`/`Consequences` — only a `Status` line
  changes, and only per step 7.
- **Not every choice is decision-worthy.** Don't create a record for a
  routine implementation detail with no rejected alternative — see step 1.
  When in doubt, ask rather than pad the log.
- **Never touch `CLAUDE.md`/`docs/*.md` directly.** If a decision affects
  what those files should currently say, update them separately — don't
  edit them from this skill.
- **Never write anything before the user confirms the draft in step 5.**
- **Never assign a record number that already exists** — numbers are a
  single global sequence, scanned fresh from
  `JPPhotoManagerWeb/docs/decisions/` each run, never reused even for a
  later-superseded decision.
- Preserve the exact Markdown table formatting of the `## Index` table when
  appending a row.
