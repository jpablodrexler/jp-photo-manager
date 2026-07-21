---
name: features-archive
description: Mark features as implemented in openspec/features.md and move them to openspec/features-implemented.md. Use when one or more features have been fully implemented and need to be archived.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Archive one or more implemented features from `openspec/features.md` into `openspec/features-implemented.md`.

**Input**: One or more feature numbers (e.g. `30 57`) or names (e.g. `image-rotation-viewer`). If omitted, auto-detect from archived SDD changes and prompt the user to confirm.

---

## Steps

### 1. Resolve which features to archive

**If the user provided numbers or names**, use them directly — skip to step 2.

**Otherwise, auto-detect from the archived SDD changes:**

This project follows a workflow where each feature in `features.md` is implemented via a corresponding OpenSpec (SDD) change. The full lifecycle is:
1. Feature added to `features.md` with `⬜ Pending`
2. SDD change created under `openspec/changes/<change-name>/` with proposal, specs, design, and tasks artifacts
3. SDD change applied (code implemented and committed)
4. SDD change archived to `openspec/changes/archive/YYYY-MM-DD-<change-name>/` via `/opsx:archive`
5. This skill archives the matching feature in `features.md`

To detect candidates at step 5:

a. **List archived SDD changes**: read all directory names under `openspec/changes/archive/`. Strip the leading `YYYY-MM-DD-` date prefix from each directory name to get the raw change names (e.g. `2026-06-21-accent-color-customization` → `accent-color-customization`).

b. **Cross-reference with features.md**: read `openspec/features.md`. For each archived change name, look for a row in the `## Feature List` table whose `Change name` column (the backtick-wrapped value, e.g. `` `accent-color-customization` ``) matches. Collect only the rows still showing `⬜ Pending` in the Implementation column — rows already showing `✅ Implemented` are excluded.

c. **Present candidates to the user**: if one or more matches are found, use the **AskUserQuestion tool** to show them and ask which to archive (multi-select). Pre-select all detected matches as the default recommendation.

d. **Fallback**: if no matches are found (all archived SDD changes either have no corresponding feature row or are already marked `✅ Implemented`), fall back to reading `openspec/features.md` and displaying all `⬜ Pending` features via **AskUserQuestion tool** so the user can pick manually.

### 2. Read both files

Read:
- `openspec/features.md` (source)
- `openspec/features-implemented.md` (destination)

### 3. For each selected feature — validate it exists and is pending

In the `features.md` table find the row whose `#` column matches. If the row already shows `✅ Implemented` warn the user and skip it.

### 4. Update the Implementation column in features.md

For each selected feature, change its table row from:

```
| ⬜ Pending      |
```

to:

```
| ✅ Implemented |
```

Save the change to `openspec/features.md`.

### 5. Extract the row from features.md

Copy the full table row (the `|` delimited line) for each selected feature.

### 6. Extract associated dependency / implementation notes

Scan the **Dependencies** and **Implementation notes** sections of `openspec/features.md` for any blocks that reference the selected feature numbers. A block is:

- A top-level heading or bold label that mentions the feature number (e.g. `**Feature 30 — …**`, `**Feature 30 → …**`)
- Everything after that heading up to (but not including) the **next top-level `**Feature N …**`-style heading** — not simply the next blank line.

Some blocks span many paragraphs separated by blank lines and use nested italic sub-headers (e.g. `*dev.samstevens.totp:totp (Maven, latest stable: 1.7.1)*`) as internal structure rather than starting a new block — stopping at the first blank line would truncate the block and strand its remaining paragraphs behind in `features.md` with no parent heading. Only a line matching the `**Feature N ...**` heading pattern ends the current block.

Collect these blocks; they will be appended to `openspec/features-implemented.md`.

### 7. Remove extracted content from features.md

Delete:
1. The table row(s) for the selected features from the `## Feature List` table.
2. The dependency / note blocks collected in step 6.

Also update the **Recommended implementation order** block if it lists the archived features — remove them from the ordered list.

Update the **Deployment (migration) dependencies** table — remove rows for migrations belonging to the archived features.

Also remove the archived features from the **no hard dependencies** list in the Dependencies section if they appear there.

Save the updated `openspec/features.md`.

### 8. Append to features-implemented.md

At the end of the `## Feature List` table in `openspec/features-implemented.md`, insert the extracted table row(s).

Then, under `## Dependencies (Historical)` → `### Implementation notes`, append the extracted dependency / note blocks.

If the feature has a Flyway migration, append its row to the **Deployment (migration) dependencies (applied)** table in `features-implemented.md`.

Save `openspec/features-implemented.md`.

### 9. Display summary

```
## Archive Complete

Archived N feature(s):
- #<n> `<name>` — moved to features-implemented.md
- …

Notes / dependency blocks moved: <count>
Migration rows moved: <count>
```

---

## Guardrails

- Never archive a feature that still shows `⬜ Pending` without first updating it to `✅ Implemented` in `features.md`.
- Never remove content from `features.md` without first confirming the writes to `features-implemented.md` succeeded.
- If a selected feature number does not exist in `features.md`, report an error for that entry and continue with the rest.
- Preserve the exact Markdown table formatting (column widths, pipe characters) in both files.
- Keep the section headers and structure of both files intact — only add/remove rows and note blocks, never rewrite entire sections.
- If the **Dependencies** section becomes empty after removals, leave the section header in place rather than deleting it.
- The auto-detection in step 1 is a convenience heuristic — the user always has the final say on which features to archive.
