---
name: feature-plan
description: Adds a new feature to the backlog's features-planned.md — drafts the row (Priority, Schema Change, Effort, Area, Summary), writes the feature's full brief to features/NNN-<name>.md as a user story with acceptance criteria, offers to write the project's Definition of Done when it has none, assigns the next feature number, inserts it into the Recommended implementation order, and confirms with the user before writing. Creates the backlog file (with header, legend, and empty table) on first use if it doesn't exist yet. TRIGGER when the user asks to plan a new feature, add a feature to the backlog, or file a feature idea/request.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

Add a new feature to the backlog — its full brief file `<backlog>/features/NNN-<change-name>.md` plus a row in `<backlog>/features-planned.md` — drafted from the user's description and confirmed before writing.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: A description of the feature (as much or as little detail as the user gives). May optionally include an explicit change name and/or explicit values for Priority/Schema Change/Effort/Area — use whatever is given directly and only draft the rest.

---

## Steps

### 1. Make sure there's enough to draft from

If the user's request is too vague to describe what the feature actually does (e.g. just a one- or two-word title with no hint at the mechanism), ask one clarifying question before drafting — a good row needs the same level of technical specificity as existing rows (endpoints, tables/columns, components, libraries), not a vague restatement of the title. If the user already gave enough to work with, proceed without asking.

### 2. Read both backlog files, creating them if they don't exist yet

Read:
- `<backlog>/features-planned.md`
- `<backlog>/features-implemented.md`

**If `<backlog>/features-planned.md` doesn't exist yet** (the normal case the first time this skill runs in this repo), create it with this skeleton before continuing — don't silently invent a different structure:

```markdown
# Planned Features

## Feature List

| # | Change name | Priority | Schema Change | Effort | Area | SDD Artifacts | Implementation | Summary | Brief |
| - | ------------ | -------- | -------------- | ------ | ---- | -------------- | ---------------- | ------- | ----- |

## Column legend

- **Priority**: `P0` (production-safety/security/data-loss gap) · `P1` (high-impact) · `P2` (scalability) · `P3` (operational convenience)
- **Schema Change**: `Yes` if the feature adds or alters a database migration, a schema definition or an access policy; `No` otherwise
- **Effort**: `S` / `M` / `L` / `XL` (`XL` is too large for one story: a candidate for splitting into several features)
- **Area**: `Backend` · `Frontend` · `Full-stack` (touches both) · `Infra` (deployment/provisioning-only, no application code)
- **SDD Artifacts**: (only meaningful in projects that use OpenSpec; otherwise it stays `⬜ Pending`) `⬜ Pending` until `openspec-propose` has generated proposal/specs/tasks for the change; `✅ Created` once it has
- **Implementation**: `⬜ Pending` until work starts; `🔶 In Progress` once `feature-development` has selected the feature and set up its branch, until shipped; `✅ Implemented` once `features-archive` has moved the row to `features-implemented.md`
- **Summary**: a short hand-written one- or two-sentence summary of the brief (at most 250 characters, no pipes) for humans and for `features-next`'s display only — never spec input
- **Brief**: link to the feature's full brief, `features/NNN-<change-name>.md` (`NNN` = zero-padded feature number), a user story with acceptance criteria — the verbatim text `feature-development` hands to `openspec-propose`; the file never moves, and planned vs. implemented is decided only by which table holds the row

## Dependencies

### Hard implementation dependencies

_None yet._

### Recommended implementation order

Priority tier first (P1 before P2 before P3), then hard dependencies, then feature number. Not a schedule — a reasoning aid for `features-next` and for whoever is picking up the next feature by hand. Re-derive this whenever priorities, dependencies, or the feature set change; don't let it silently drift. A plain bullet list — list position (not a printed number) is what encodes order, so inserting an entry never touches any other line.

_None yet._
```

**If `<backlog>/features-implemented.md` doesn't exist yet either**, create it with the matching skeleton (the implemented-table header shown, no legend needed since it's just an archive target, plus a `## Dependencies (Historical)` → `### Implementation notes` section and a `## Deployment (migration) dependencies (applied)` table) so `features-archive` has somewhere to write to later:

```markdown
# Implemented Features

## Feature List

| # | Change name | Priority | Schema Change | Effort | Area | Details |
| - | ------------ | -------- | -------------- | ------ | ---- | -------------------- |

## Dependencies (Historical)

### Implementation notes

_None yet._

## Deployment (migration) dependencies (applied)

| Migration | Feature |
| ---------- | --------- |
```

Both files are needed to pick a non-colliding number and name (step 3) and to draft consistent attribute values (step 4), even when both are freshly created and empty.

**Definition of Done.** Check whether `<backlog>/definition-of-done.md` exists. If it does, read it: the brief's own `## Definition of Done` section (step 5) lists only what is extra for this feature. If it does not, ask one question with the **AskUserQuestion tool**, every time this skill runs while the file is missing — "This project has no Definition of Done yet. Write one now?" — options: "Write it now" (ask the user for the checklist items, or take them from what they paste), "Draft a starter for me to edit" (propose a checklist drawn from the project's own docs and the phases its workflow already runs, show it, and write it only after the user confirms), "Not now". Whatever the answer, **never block planning**: "Not now" (or no usable answer) continues to step 3 with no side effects, and a missing file is never an error. The file is a plain markdown checklist (`- [ ] …` items) that applies to every feature; it is written only when the user chose one of the first two options.

### 3. Derive the change name and the next feature number

**Change name**: if the user gave an explicit one, validate it's kebab-case (lowercase letters, digits, hyphens only, e.g. `task-due-dates`); if it isn't, convert it. If the user didn't give one, derive a concise kebab-case slug from the feature description — short, descriptive, no filler words (e.g. `task-search`, `task-completion-toggle`).

Run (from this skill's base directory) `python3 scripts/next_id.py <repo-root> --check-name <derived-name>` — it scans both files' `## Feature List` tables plus the filenames under `<backlog>/features/` and returns the next free `#` (`max + 1` across both, or `1` for a fresh backlog; numbers are a single global sequence, never reused even for a cancelled/reverted feature) plus whether the derived name already collides with an existing row. If `name_collision` is `true`, tell the user and ask for a different name or confirm they mean something else (do not silently rename or silently proceed with a duplicate). Use the returned `next_number` as the feature number.

### 4. Draft the four attribute columns

Using the **Column legend** immediately below the Feature List table in `features-planned.md` as the source of truth for the value vocabulary (don't hardcode a copy of the definitions here — read them fresh each run so this skill can't drift out of sync with the legend):

- **Priority** (`P0`–`P3`)
- **Schema Change** (`Yes`/`No`)
- **Effort** (`S`/`M`/`L`/`XL`)
- **Area** (one of the values the legend defines)

If the user explicitly gave a value for any of these, use it as-is (still sanity-check it's one of the valid values from the legend). Otherwise infer it from the feature description — e.g. a security/data-loss gap is `P0`; a feature naming a new table/column/policy is `Schema Change: Yes`; a change touching both backend and frontend code is `Full-stack`.

If the Effort comes out `XL` (given or inferred), the feature is a candidate for splitting: say so in one sentence and, in step 6's question, offer "Split into several features" alongside the other options. Splitting means drafting one feature per independently valuable story (each with its own row, brief and number, and a hard dependency between them where one needs the other), shown together for one confirmation. Keeping the `XL` is allowed; never force the split.

Both `SDD Artifacts` and `Implementation` are always `⬜ Pending` for a brand-new row — this skill only plans a feature, it never creates SDD artifacts (that's `openspec-propose`, invoked later by `feature-development`) or implements it.

### 5. Draft the full brief and the Summary

**Full brief.** Write the brief as one user story, in this shape (the file's H1 is added in step 7):

```markdown
**As a** <specific role>, **I want** <goal>, **so that** <benefit>.

## Acceptance Criteria
- AC1: Given <context>, when <action>, then <observable outcome>.
- AC2: …

## Technical notes
<the concrete mechanism: new endpoint(s), table/column names, component names, libraries>

## Out of scope
<optional: what this story deliberately leaves out>

## Definition of Done
Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- [ ] <feature-specific item, or "None">
```

Rules for drafting it:
- **Role**: a concrete persona taken from the product (never the bare word "user"); the sentence is one line, with the same punctuation every time.
- **Acceptance criteria**: numbered `AC1`…`ACn`, each in Given/When/Then form, each binary and testable and describing behaviour, never implementation. Cover the main path, the main failure or empty case, and any permission or data-ownership rule the user mentioned. They are the checklist `spec-compliance-check` later verifies, and the scenarios `openspec-propose` turns into WHEN/THEN requirements.
- **Technical notes**: keep the specificity the old one-paragraph brief carried (names a spec step needs); reuse whatever technical detail the user gave and fill gaps with reasonable, clearly-inferable choices consistent with the project's documented conventions (`CLAUDE.md`, the README, decision records) rather than inventing an unrelated approach.
- **Definition of Done**: if `<backlog>/definition-of-done.md` does not exist, write "No project-level Definition of Done defined yet." instead of the first sentence. Never copy the project-level items into the brief.
- **One story per brief.** If the description holds several independently valuable behaviours, say so and offer to split (the same offer as for an `XL`).

This brief is the exact text `feature-development` later hands to `openspec-propose` as the description of what to build, so give it all the specificity the spec step will need; it goes in the feature's own file (step 7), not in the table.

**Summary.** Also hand-write a plain-text summary of that brief, built from its "I want … so that …" clause — at most 250 characters (target about 160), one or two sentences, no pipe characters, no Markdown links. It is only for humans skimming the table and for `features-next`'s display; it is never spec input, so it may drop detail the brief keeps.

### 6. Confirm with the user before writing

Display the fully drafted row:

```
## Draft Feature

**#<number> `<change-name>`**

**Priority:** <priority>  **Schema Change:** <schema_change>  **Effort:** <effort>  **Area:** <area>

**Summary:** <summary>

**Full brief** (saved to `<backlog>/features/<NNN>-<change-name>.md`):

<full brief>

**SDD Artifacts:** ⬜ Pending   **Implementation:** ⬜ Pending
```

Use the **AskUserQuestion tool** with a single question — "Add this feature to the backlog?" — options: "Yes, add it as drafted", "Split into several features" (only when the Effort is `XL` or the description holds several stories; see step 4), "Let me edit something first" (resolve free-text edits against the draft and re-display for confirmation before writing), "Cancel". Do not write anything to `features-planned.md` or `<backlog>/features/` until the user confirms.

### 7. Write the brief file, then insert the row

Do these in order, file first, so a half-finished run leaves an orphan file (which `features-status` reports) rather than a row pointing at nothing:

1. Create `<backlog>/features/<NNN>-<change-name>.md` (create the `features/` folder if it doesn't exist; `NNN` = the feature number zero-padded to three digits, e.g. `054`). Content: the H1 `# Feature <number> — <change-name>` (an em dash; write "Feature 54", never "#54", since GitHub auto-links `#N`), one blank line, then the full brief from step 5 (the user story, its acceptance criteria and the other sections). The file is never moved or renamed afterwards, whether the feature is planned or implemented.
2. Append the confirmed row to the end of the `## Feature List` table in `features-planned.md`, contiguous with the existing rows (no blank line inside the table), preserving the pipe-delimited formatting and the ten columns in header order: `| <number> | `<change-name>` | <priority> | <schema_change> | <effort> | <area> | ⬜ Pending | ⬜ Pending | <summary> | [brief](features/<NNN>-<change-name>.md) |`.

Do not touch `features-implemented.md` — this skill only ever adds to the planned/backlog file.

### 8. Optional: record a hard dependency

If — and only if — the user's description names another feature this one depends on (by number or name, in either file), add an entry under `### Hard implementation dependencies` in `## Dependencies`, following the existing format:

```
**Feature <new#> → Feature <dep#>** (prerequisite already implemented | still pending)

`<new-name>` <one-sentence reason it depends on `<dep-name>`>.
```

Mark whether the prerequisite is already implemented (check `features-implemented.md`) or still pending. If the user didn't mention a dependency, skip this step entirely — do not go hunting for implicit dependencies the user didn't state.

### 9. Update the recommended implementation order

Read the `### Recommended implementation order` subsection of `## Dependencies` (immediately below `### Hard implementation dependencies`). It's a bullet list of `` `change-name` (#number) `` entries — list position, not a printed number, encodes order — ordered priority tier first (P1 before P2 before P3 before untiered), then hard dependencies, then feature number — see that section's own intro line for the exact rule.

Insert the newly-added feature into that list, don't just append it:

1. Find the tier boundary: the last existing entry whose Priority is the same or higher than the new feature's, and the first existing entry whose Priority is strictly lower. The new feature goes between them (same-priority entries keep their existing relative order; the new feature lands after all of them).
2. If step 8 recorded a hard dependency for this feature, make sure it's positioned after its dependency regardless of what tier placement alone would suggest — move it later in the list if needed.
3. If the list still reads `_None yet._` (a fresh backlog with no order yet), the new feature becomes the list's only entry.
4. Every other bullet stays exactly as it was — inserting a bullet never requires touching any line but the new one.

Write one line for the new entry in the same style as its neighbors — priority, unblocked-or-blocked-on-what, and independent/small/etc. where relevant — mirroring the phrasing of adjacent entries rather than inventing a new format.

The existing list is not guaranteed to be strictly tier-sorted (a P1 may sit after P2/P3 entries); apply the rule literally — land after the last entry that has the same or higher priority, which may be the final bullet — and never reorder the existing bullets to fix that.

If genuinely ambiguous where a same-priority feature should land relative to its peers (no dependency, no clear reason to prefer earlier or later), place it last within its tier — don't guess at a rationale that isn't there.

### 10. Display confirmation

```
## Feature Added

**#<number> `<change-name>`** added to features-planned.md, brief written to `<backlog>/features/<NNN>-<change-name>.md` (Priority <priority>, Effort <effort>, Area <area>), inserted into the recommended implementation order <right after `<preceding-entry>` (#N) | as the first entry>.
```

---

## Guardrails

- Never edit `features-implemented.md` beyond creating its initial skeleton (step 2) if it doesn't exist — this skill only adds rows to `features-planned.md` (plus the new feature's brief file under `<backlog>/features/`).
- The row's `Summary` is at most 250 characters with no pipes, and its `Brief` cell always links the file written in step 7; never put the full brief in the table.
- Never write to `features-planned.md` or `<backlog>/features/` before the user has confirmed the drafted row in step 6 — all of steps 2–5 are draft-only, in memory (except the one-time skeleton creation in step 2, which is scaffolding, not a feature row).
- Never assign a feature number or change name that already exists in either file or in a `features/` filename (checked across all of them, not just the planned file).
- Do not add a row to the **Deployment (migration) dependencies** table even when `Schema Change` is `Yes` — migration *numbers* (i.e. which migration implements it) are deliberately not tracked at planning time since pending features are frequently reordered; that table only gets a new row once a feature is actually being implemented.
- A missing `<backlog>/definition-of-done.md` is asked about every run (step 2) but never blocks planning, and the file is written only after the user chose to; skipping the question is always one keypress.
- Existing one-paragraph briefs stay valid and are never rewritten by this skill; only the brief of the feature being planned is in the story shape.
- Preserve the exact Markdown table formatting (pipe characters) of the Feature List table.
- Read the Column legend fresh each run rather than relying on a remembered copy of the value vocabulary — if the legend changes, this skill should follow without needing its own update.
- Step 8 (hard dependencies) is opt-in based on what the user actually said — never infer a dependency the user didn't mention.
- Step 9 (recommended implementation order) is **not** opt-in — unlike step 8, always insert the new feature into that list; a backlog with rows missing from the order is worse than one with a slightly-uncertain placement. If the list doesn't exist yet under `## Dependencies` (an older backlog file predating this section), add it with the intro line shown in step 9 before inserting the new feature.
- Step 6's confirmation is mandatory, even under an "Auto Mode" or similar autonomous-operation instruction that biases toward proceeding without stopping to ask — that bias never applies to writing a new row into the backlog. Never invoke this skill from inside a spawned/backgrounded subagent that lacks reliable `AskUserQuestion` access as a way to skip it.
- **Never run `git commit`, `git push` or open a PR.**
