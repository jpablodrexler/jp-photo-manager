---
name: feature-plan
description: Adds a new feature to JPPhotoManagerWeb/docs/backlog/features-planned.md — drafts the row (Priority, Schema Change, Effort, Area, Summary), writes the feature's full brief to JPPhotoManagerWeb/docs/backlog/features/NNN-<name>.md, assigns the next feature number, and confirms with the user before writing. TRIGGER when the user asks to plan a new feature, add a feature to the backlog, or file a feature idea/request.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.3"
---

Add a new feature to the backlog — its full brief file `JPPhotoManagerWeb/docs/backlog/features/NNN-<change-name>.md` plus a row in `JPPhotoManagerWeb/docs/backlog/features-planned.md` — drafted from the user's description and confirmed before writing.

**Layout**: `features-planned.md` and `features-implemented.md` hold only tables (plus their legend and dependency notes); each feature's long text lives in its own file under `JPPhotoManagerWeb/docs/backlog/features/`, linked from its row. The planned table's columns are `| # | Change name | Priority | Schema Change | Effort | Area | Summary | Brief | SDD Artifacts | Implementation |` — always address cells by header name, not position.

**Input**: A description of the feature (as much or as little detail as the user gives). May optionally include an explicit change name and/or explicit values for Priority/Schema Change/Effort/Area — use whatever is given directly and only draft the rest.

---

## Steps

### 1. Make sure there's enough to draft from

If the user's request is too vague to describe what the feature actually does (e.g. just a one- or two-word title with no hint at the mechanism), ask one clarifying question before drafting — a good row needs the same level of technical specificity as existing rows (endpoints, tables/columns, components, libraries), not a vague restatement of the title. If the user already gave enough to work with, proceed without asking.

### 2. Read both backlog files

Read:
- `JPPhotoManagerWeb/docs/backlog/features-planned.md`
- `JPPhotoManagerWeb/docs/backlog/features-implemented.md`

Both are needed to pick a non-colliding number and name (step 3) and to draft consistent attribute values (step 4).

### 3. Derive the change name and the next feature number

**Change name**: if the user gave an explicit one, validate it's kebab-case (lowercase letters, digits, hyphens only, e.g. `wallpaper-rotation-schedule`); if it isn't, convert it. If the user didn't give one, derive a concise kebab-case slug from the feature description, in the same style as existing names (`image-etag-cache`, `folder-watch-service`) — short, descriptive, no filler words.

Run `python3 .claude/skills/feature-plan/scripts/next_id.py <repo-root> --check-name <derived-name>` — it scans both files' `## Feature List` tables plus the filenames under `JPPhotoManagerWeb/docs/backlog/features/` and returns the next free `#` (`max + 1` across all of them, or `1` for a fresh backlog; numbers are a single global sequence, never reused even for a cancelled/reverted feature — see `#72`/`#84` in `features-implemented.md` for a precedent) plus whether the derived name already collides with an existing row. If `name_collision` is `true`, tell the user and ask for a different name or confirm they mean something else (do not silently rename or silently proceed with a duplicate). Use the returned `next_number` as the feature number.

### 4. Draft the four attribute columns

Using the **Column legend** immediately below the Feature List table in `features-planned.md` as the source of truth for the value vocabulary (don't hardcode a copy of the definitions here — read them fresh each run so this skill can't drift out of sync with the legend):

- **Priority** (`P0`–`P3`)
- **Schema Change** (`Yes`/`No`)
- **Effort** (`S`/`M`/`L`)
- **Area** (`Backend`/`Frontend`/`Full-stack`/`Infra`)

If the user explicitly gave a value for any of these, use it as-is (still sanity-check it's one of the valid values from the legend). Otherwise infer it from the feature description using the same judgment already applied to the 37 rows added when these columns were introduced — e.g. a security/data-loss/observability gap is `P0`; a feature naming a new table/column is `Schema Change: Yes`; a change touching both an Angular component and a Spring controller is `Full-stack`.

Both `SDD Artifacts` and `Implementation` are always `⬜ Pending` for a brand-new row — this skill only plans a feature, it never creates SDD artifacts (that's `openspec-propose`, invoked later by `feature-development`) or implements it.

### 5. Draft the full brief and the Summary

**Full brief.** Write one dense paragraph in the same style as existing briefs (see any file under `JPPhotoManagerWeb/docs/backlog/features/`): name the concrete mechanism (new endpoint(s), table/column names, component names, libraries), not a vague restatement of the feature title. Reuse whatever technical detail the user already gave; fill gaps with reasonable, clearly-inferable implementation choices consistent with the rest of the codebase (see `JPPhotoManagerWeb/CLAUDE.md` conventions) rather than inventing an unrelated approach. This brief is the exact text `feature-development` later hands to `openspec-propose` as the description of what to build, so give it all the specificity the spec step will need; it goes in the feature's own file (step 7), not in the table.

**Summary.** Also hand-write a plain-text summary of that brief — at most 250 characters (target about 160), one or two sentences, no pipe characters, no Markdown links. It is only for humans skimming the table and for `features-next`'s display; it is never spec input, so it may drop detail the brief keeps.

### 6. Confirm with the user before writing

Display the fully drafted row:

```
## Draft Feature

**#<number> `<change-name>`**

**Priority:** <priority>  **Schema Change:** <schema_change>  **Effort:** <effort>  **Area:** <area>

**Summary:** <summary>

**Full brief** (saved to `JPPhotoManagerWeb/docs/backlog/features/<NNN>-<change-name>.md`):

<full brief>

**SDD Artifacts:** ⬜ Pending   **Implementation:** ⬜ Pending
```

Use the **AskUserQuestion tool** with a single question — "Add this feature to the backlog?" — options: "Yes, add it as drafted", "Let me edit something first" (resolve free-text edits against the draft and re-display for confirmation before writing), "Cancel". Do not write anything to `features-planned.md` or `JPPhotoManagerWeb/docs/backlog/features/` until the user confirms.

### 7. Write the brief file, then insert the row

Do these in order, file first, so a half-finished run leaves an orphan file (which `features-status` reports) rather than a row pointing at nothing:

1. Create `JPPhotoManagerWeb/docs/backlog/features/<NNN>-<change-name>.md` (create the `features/` folder if it doesn't exist; `NNN` = the feature number zero-padded to three digits, e.g. `054`). Content: the H1 `# Feature <number> — <change-name>` (an em dash; write "Feature 54", never "#54", since GitHub auto-links `#N`), one blank line, then the full brief from step 5 as a single paragraph. The file is never moved or renamed afterwards, whether the feature is planned or implemented.
2. Append the confirmed row to the end of the `## Feature List` table in `features-planned.md`, contiguous with the existing rows (no blank line inside the table), preserving the pipe-delimited formatting and the ten columns in the table header's order: `| <number> | `<change-name>` | <priority> | <schema_change> | <effort> | <area> | <summary> | [brief](features/<NNN>-<change-name>.md) | ⬜ Pending | ⬜ Pending |`.

Do not touch `features-implemented.md` — this skill only ever adds to the planned/backlog file.

### 8. Optional: record a hard dependency

If — and only if — the user's description names another feature this one depends on (by number or name, in either file), add an entry under `### Hard implementation dependencies` in `## Dependencies`, following the existing format:

```
**Feature <new#> → Feature <dep#>** (prerequisite already implemented | still pending)

`<new-name>` <one-sentence reason it depends on `<dep-name>`>.
```

Mark whether the prerequisite is already implemented (check `features-implemented.md`) or still pending. If the user didn't mention a dependency, skip this step entirely — do not go hunting for implicit dependencies the user didn't state.

### 9. Display confirmation

```
## Feature Added

**#<number> `<change-name>`** added to features-planned.md, brief written to `JPPhotoManagerWeb/docs/backlog/features/<NNN>-<change-name>.md` (Priority <priority>, Effort <effort>, Area <area>).
```

---

## Guardrails

- Never edit `features-implemented.md` — this skill only adds rows to `features-planned.md` (plus the new feature's brief file under `JPPhotoManagerWeb/docs/backlog/features/`).
- The row's `Summary` is at most 250 characters with no pipes, and its `Brief` cell always links the file written in step 7; never put the full brief in the table.
- Never write to `features-planned.md` or `features/` before the user has confirmed the drafted row in step 6 — all of steps 2–5 are draft-only, in memory.
- Never assign a feature number or change name that already exists in either file or in a `features/` filename (checked across all of them, not just the planned file).
- Do not add a row to the **Deployment (migration) dependencies** table even when `Schema Change` is `Yes` — per that column's own legend entry, migration *numbers* are deliberately not tracked at planning time since pending features are frequently reordered; the migration table only gets a new row once a feature is actually being implemented.
- Preserve the exact Markdown table formatting (pipe characters) of the Feature List table.
- Read the Column legend fresh each run rather than relying on a remembered copy of the value vocabulary — if the legend changes, this skill should follow without needing its own update.
- Step 8 (hard dependencies) is opt-in based on what the user actually said — never infer a dependency the user didn't mention.
- Step 6's confirmation is mandatory, even under an "Auto Mode" or similar autonomous-operation instruction that biases toward proceeding without stopping to ask — that bias never applies to writing a new row into the backlog. Never invoke this skill from inside a spawned/backgrounded subagent that lacks reliable `AskUserQuestion` access as a way to skip it.
