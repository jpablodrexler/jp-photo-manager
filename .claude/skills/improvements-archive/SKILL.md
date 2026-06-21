---
name: improvements-archive
description: Mark improvements as implemented in openspec/improvements.md and move them to openspec/improvements-implemented.md. Use when one or more improvements have been fully implemented and need to be archived.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Archive one or more implemented improvements from `openspec/improvements.md` into `openspec/improvements-implemented.md`.

**Input**: One or more improvement numbers (e.g. `30 57`) or names (e.g. `image-rotation-viewer`). If omitted, auto-detect from archived SDD changes and prompt the user to confirm.

---

## Steps

### 1. Resolve which improvements to archive

**If the user provided numbers or names**, use them directly — skip to step 2.

**Otherwise, auto-detect from the archived SDD changes:**

This project follows a workflow where each improvement in `improvements.md` is implemented via a corresponding OpenSpec (SDD) change. The full lifecycle is:
1. Improvement added to `improvements.md` with `⬜ Pending`
2. SDD change created under `openspec/changes/<change-name>/` with proposal, specs, design, and tasks artifacts
3. SDD change applied (code implemented and committed)
4. SDD change archived to `openspec/changes/archive/YYYY-MM-DD-<change-name>/` via `/opsx:archive`
5. This skill archives the matching improvement in `improvements.md`

To detect candidates at step 5:

a. **List archived SDD changes**: read all directory names under `openspec/changes/archive/`. Strip the leading `YYYY-MM-DD-` date prefix from each directory name to get the raw change names (e.g. `2026-06-21-accent-color-customization` → `accent-color-customization`).

b. **Cross-reference with improvements.md**: read `openspec/improvements.md`. For each archived change name, look for a row in the `## Improvement List` table whose `Change name` column (the backtick-wrapped value, e.g. `` `accent-color-customization` ``) matches. Collect only the rows still showing `⬜ Pending` in the Implementation column — rows already showing `✅ Implemented` are excluded.

c. **Present candidates to the user**: if one or more matches are found, use the **AskUserQuestion tool** to show them and ask which to archive (multi-select). Pre-select all detected matches as the default recommendation.

d. **Fallback**: if no matches are found (all archived SDD changes either have no corresponding improvement row or are already marked `✅ Implemented`), fall back to reading `openspec/improvements.md` and displaying all `⬜ Pending` improvements via **AskUserQuestion tool** so the user can pick manually.

### 2. Read both files

Read:
- `openspec/improvements.md` (source)
- `openspec/improvements-implemented.md` (destination)

### 3. For each selected improvement — validate it exists and is pending

In the `improvements.md` table find the row whose `#` column matches. If the row already shows `✅ Implemented` warn the user and skip it.

### 4. Update the Implementation column in improvements.md

For each selected improvement, change its table row from:

```
| ⬜ Pending      |
```

to:

```
| ✅ Implemented |
```

Save the change to `openspec/improvements.md`.

### 5. Extract the row from improvements.md

Copy the full table row (the `|` delimited line) for each selected improvement.

### 6. Extract associated dependency / implementation notes

Scan the **Dependencies** and **Implementation notes** sections of `openspec/improvements.md` for any blocks that reference the selected improvement numbers. A block is:

- A heading or bold label that mentions the improvement number (e.g. `**Improvement 30 — …**`, `**Improvement 30 → …**`)
- The paragraph(s) immediately following that heading until the next heading or blank separator

Collect these blocks; they will be appended to `openspec/improvements-implemented.md`.

### 7. Remove extracted content from improvements.md

Delete:
1. The table row(s) for the selected improvements from the `## Improvement List` table.
2. The dependency / note blocks collected in step 6.

Also update the **Recommended implementation order** block if it lists the archived improvements — remove them from the ordered list.

Update the **Deployment (migration) dependencies** table — remove rows for migrations belonging to the archived improvements.

Also remove the archived improvements from the **no hard dependencies** list in the Dependencies section if they appear there.

Save the updated `openspec/improvements.md`.

### 8. Append to improvements-implemented.md

At the end of the `## Improvement List` table in `openspec/improvements-implemented.md`, insert the extracted table row(s).

Then, under `## Dependencies (Historical)` → `### Implementation notes`, append the extracted dependency / note blocks.

If the improvement has a Flyway migration, append its row to the **Deployment (migration) dependencies (applied)** table in `improvements-implemented.md`.

Save `openspec/improvements-implemented.md`.

### 9. Display summary

```
## Archive Complete

Archived N improvement(s):
- #<n> `<name>` — moved to improvements-implemented.md
- …

Notes / dependency blocks moved: <count>
Migration rows moved: <count>
```

---

## Guardrails

- Never archive an improvement that still shows `⬜ Pending` without first updating it to `✅ Implemented` in `improvements.md`.
- Never remove content from `improvements.md` without first confirming the writes to `improvements-implemented.md` succeeded.
- If a selected improvement number does not exist in `improvements.md`, report an error for that entry and continue with the rest.
- Preserve the exact Markdown table formatting (column widths, pipe characters) in both files.
- Keep the section headers and structure of both files intact — only add/remove rows and note blocks, never rewrite entire sections.
- If the **Dependencies** section becomes empty after removals, leave the section header in place rather than deleting it.
- The auto-detection in step 1 is a convenience heuristic — the user always has the final say on which improvements to archive.
