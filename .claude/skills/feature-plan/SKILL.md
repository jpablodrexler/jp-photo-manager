---
name: feature-plan
description: Adds a new feature to JPPhotoManagerWeb/docs/backlog/features-planned.md — drafts the row (Priority, Schema Change, Effort, Area, Brief description), assigns the next feature number, and confirms with the user before writing. TRIGGER when the user asks to plan a new feature, add a feature to the backlog, or file a feature idea/request.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Add a new feature row to `JPPhotoManagerWeb/docs/backlog/features-planned.md`, drafted from the user's description and confirmed before writing.

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

Check the derived name against the `Change name` column of **both** files' `## Feature List` tables (exact match on the backtick-wrapped value). If it already exists anywhere, tell the user and ask for a different name or confirm they mean something else (do not silently rename or silently proceed with a duplicate).

**Feature number**: scan the `#` column of both files' `## Feature List` tables, take the highest number found across both, and use `max + 1`. Numbers are a single global sequence across both files and are never reused, even for cancelled or reverted features (see `#72`/`#84` in `features-implemented.md` for a precedent).

### 4. Draft the four attribute columns

Using the **Column legend** immediately below the Feature List table in `features-planned.md` as the source of truth for the value vocabulary (don't hardcode a copy of the definitions here — read them fresh each run so this skill can't drift out of sync with the legend):

- **Priority** (`P0`–`P3`)
- **Schema Change** (`Yes`/`No`)
- **Effort** (`S`/`M`/`L`)
- **Area** (`Backend`/`Frontend`/`Full-stack`/`Infra`)

If the user explicitly gave a value for any of these, use it as-is (still sanity-check it's one of the valid values from the legend). Otherwise infer it from the feature description using the same judgment already applied to the 37 rows added when these columns were introduced — e.g. a security/data-loss/observability gap is `P0`; a feature naming a new table/column is `Schema Change: Yes`; a change touching both an Angular component and a Spring controller is `Full-stack`.

Both `SDD Artifacts` and `Implementation` are always `⬜ Pending` for a brand-new row — this skill only plans a feature, it never creates SDD artifacts (that's `openspec-propose`, invoked later by `feature-development`) or implements it.

### 5. Draft the brief description

Write one dense paragraph in the same style as existing rows: name the concrete mechanism (new endpoint(s), table/column names, component names, libraries), not a vague restatement of the feature title. Reuse whatever technical detail the user already gave; fill gaps with reasonable, clearly-inferable implementation choices consistent with the rest of the codebase (see `JPPhotoManagerWeb/CLAUDE.md` conventions) rather than inventing an unrelated approach.

### 6. Confirm with the user before writing

Display the fully drafted row:

```
## Draft Feature

**#<number> `<change-name>`**

**Priority:** <priority>  **Schema Change:** <schema_change>  **Effort:** <effort>  **Area:** <area>

<brief description>

**SDD Artifacts:** ⬜ Pending   **Implementation:** ⬜ Pending
```

Use the **AskUserQuestion tool** with a single question — "Add this feature to the backlog?" — options: "Yes, add it as drafted", "Let me edit something first" (resolve free-text edits against the draft and re-display for confirmation before writing), "Cancel". Do not write anything to `features-planned.md` until the user confirms.

### 7. Insert the row

Append the confirmed row to the end of the `## Feature List` table in `features-planned.md`, preserving the table's pipe-delimited formatting. Do not touch `features-implemented.md` — this skill only ever adds to the planned/backlog file.

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

**#<number> `<change-name>`** added to features-planned.md (Priority <priority>, Effort <effort>, Area <area>).
```

---

## Guardrails

- Never edit `features-implemented.md` — this skill only adds rows to `features-planned.md`.
- Never write to `features-planned.md` before the user has confirmed the drafted row in step 6 — all of steps 2–5 are draft-only, in memory.
- Never assign a feature number or change name that already exists in either file (checked across both, not just the planned file).
- Do not add a row to the **Deployment (migration) dependencies** table even when `Schema Change` is `Yes` — per that column's own legend entry, migration *numbers* are deliberately not tracked at planning time since pending features are frequently reordered; the migration table only gets a new row once a feature is actually being implemented.
- Preserve the exact Markdown table formatting (pipe characters) of the Feature List table.
- Read the Column legend fresh each run rather than relying on a remembered copy of the value vocabulary — if the legend changes, this skill should follow without needing its own update.
- Step 8 (hard dependencies) is opt-in based on what the user actually said — never infer a dependency the user didn't mention.
