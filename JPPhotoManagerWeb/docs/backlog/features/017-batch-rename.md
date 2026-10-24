# Feature 17 — batch-rename

**As a** photo library owner, **I want** to rename many selected photos with a pattern and preview the result, **so that** my files get consistent names without renaming each by hand.

## Acceptance Criteria
- AC1: Given a selection, when I open the gallery toolbar, then a Rename action is shown; it is absent with no selection and in the viewer or slideshow.
- AC2: Given the rename dialog, when it opens, then the pattern and the preview are empty; as I type a valid pattern, the preview fills live.
- AC3: Given the tokens, when a pattern is resolved, then {date:format} is the creation date, {index:03d} the padded 1-based position, {original} the base filename and {ext} the lowercase extension; a pattern with no tokens is valid; an invalid date format returns 400.
- AC4: Given a preview-only call, when it runs, then the resolved names are returned without renaming.
- AC5: Given a collision within the batch or with an existing asset in the folder, or an empty list, when I apply, then the request returns 400 and the Apply button is disabled.
- AC6: Given a valid preview, when I apply, then files are renamed and the database updated, and the gallery reloads; on failure an error message is shown.
- AC7: Given I cancel, when the dialog closes, then nothing is renamed.

## Technical notes

Pattern-based rename (e.g. `{date:yyyy-MM-dd}_{index:03d}_{original}`) for multi-selected assets; live preview table before applying; new `POST /api/assets/rename` endpoint

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
