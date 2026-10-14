---
name: docs-sync
description: >
  Keeps `CLAUDE.md`, the reference docs under `docs/`, and `README.md`'s
  documentation index in sync with the actual code. TRIGGER after
  implementing a feature or OpenSpec change that adds or changes anything
  the documentation describes (the `Kit conventions` table of `CLAUDE.md` says
  what each reference doc is checked against). Also TRIGGERS when explicitly asked to check,
  audit, or sync the project's documentation against the code, or as a
  standalone full sweep. Do not wait to be asked after a feature lands with
  any such change — none of this is re-derived automatically, so it drifts
  silently otherwise.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

# Docs Sync Skill

**Never run `git commit`, `git push` or open a PR.**

Paths: `<backlog>` is `docs/backlog` and `<reports>` is `docs/reports`,
unless `.backlog-kit.toml` sets `[paths] backlog_dir` (then each is the
folder of that name next to the backlog's parent). The reference docs, `CLAUDE.md` and `README.md` are found the same way: the `docs` folder is the one that holds the backlog folder (`app/docs` when `backlog_dir` is `app/docs/backlog`), and `CLAUDE.md` and `README.md` sit in its parent.

## The documentation tiers

Every project this skill serves keeps its documentation in three tiers, and
each needs active upkeep. Nothing else in the repo maintains them
automatically; that's this skill's job.

- **`CLAUDE.md`** — a lean quick-start for an agent working in the code:
  commands, repo layout and a few short, fixed-pointer subsections that each
  name the essential rule and link to the `docs/*.md` section holding the
  full detail. It should **never** carry a full table or API reference, a
  routing table, a long step-by-step runbook, or a per-feature narrative
  inline — those belong in `docs/`. If you find one growing back in
  `CLAUDE.md`, move it out: detail left inline drifts silently for a long
  time before anyone re-derives it from the code.
- **`docs/*.md`** — the actual reference material, indexed from `README.md`.
  This is comprehensive: diagrams, full tables, provenance notes. The kit
  has a convention for where each kind of content lives — the table in
  `reference/conventions.md` — so every project that uses it looks the same.
  A project keeps only the files it needs, and where it already uses other
  names its `Kit conventions` section or its notes (§1) say so and the skill
  follows them.
- **`README.md`** — a short intro plus the documentation index table
  pointing into `docs/`. It should stay an index, not regrow the runbook
  content that lives in `docs/`.

This skill has two modes:

- **Scoped sync** (default) — after implementing one feature/change, check
  only the doc sections that change could plausibly affect, and fix them.
- **Full sweep** — audit every doc, and every section with its own sources,
  against the current codebase from scratch. Use when asked to "audit,"
  "check for drift," or when it's been a while since the last scoped sync
  ran.

Both modes **edit the doc files directly** rather than writing a separate
findings report — for `CLAUDE.md`/`docs/*.md`/`README.md` this is
documentation, not application code, so the fix *is* the review output. Show
the user a short diff-style summary of what changed in chat.

---

## 1. What each doc is checked against

The `Kit conventions` section of the project's `CLAUDE.md` has a table with a
"Checked against" column: for each doc, in a phrase, the part of the code it
describes (the table and its defaults are in `reference/conventions.md`). Use
it as a pointer, not as a complete list: a doc also names the code it
describes in its own text. The workflows below call this "§1".

To find the docs and sections a changed file affects, combine both: the
"Checked against" column, and a search of `CLAUDE.md` and `docs/` for the
file's path, its folder and the names (classes, functions, routes, tables)
it defines. A doc that mentions the file, or something it defines, is
affected.

A doc whose row has no "Checked against" phrase (or a project whose
`CLAUDE.md` has no such table yet): infer it from what the doc describes,
work from that, and say so in the summary; the full sweep then proposes
completing the table (see §3).

---

## 2. Scoped Sync Workflow

1. Identify what changed: `git diff <base>..HEAD --stat`, where `<base>` is
   the branch's base (the branch the current feature branch was cut from),
   if run at the end of a feature; otherwise, whatever files the user points
   to.
2. Map each changed file to the docs and sections it affects (§1: the
   "Checked against" column plus a search of the docs for its path, folder
   and names), and to `CLAUDE.md` and `README.md` where their rows apply.
   Skip those that weren't touched — don't re-verify everything on
   a scoped sync.
3. For each mapped doc or section, read the current text and the current code side
   by side and determine what's missing, stale, or wrong.
4. Apply the fix directly with `Edit`, one section at a time, matching
   that file's existing style (heading level, list vs. table, diagram
   conventions if touching a diagram).
5. **If you find detail that belongs in `docs/` sitting inline in
   `CLAUDE.md` instead**, move it — don't just update it in place. Replace
   the inline content with a short pointer, matching `CLAUDE.md`'s existing
   "Documentation" section pattern.
6. If a `docs/*.md` file's content changed enough that its one-line
   description in `README.md`'s documentation index (or `CLAUDE.md`'s
   "Documentation" paragraph) no longer describes it accurately, update that
   index entry too — don't leave the pointer stale even if the target file
   is now correct.
7. Summarize in chat: which sections were checked, which were updated, and
   the one-line reason for each update.

---

## 3. Full Sweep Workflow

1. Read `CLAUDE.md`, `README.md`, and every file under `docs/` (excluding
   the paths named in Guardrails) in full.
2. For each doc, using its "Checked against" row and what it references,
   independently re-derive the current state from code rather than trusting
   the doc's existing content, and compare. For sources that are an ordered
   history of files (such as
   migrations), read every file directly in order — don't sample the most
   recent one or two and assume earlier narrative still holds; a later
   change can re-scope or drop what an earlier doc pass described.
3. Collect every discrepancy found before editing anything, so overlapping
   edits to the same file can be merged into one pass.
4. Apply all fixes with `Edit`, grouped by file.
5. **Check the `Kit conventions` section of `CLAUDE.md`.** The project's
   `CLAUDE.md` carries, under the heading `## Kit conventions`, the table of
   `reference/conventions.md`, so a person or any other skill reading it
   sees where everything lives and what each doc is checked against. Verify
   that the section exists, that it has the "Checked against" column filled
   in for every `docs/*.md` row, that it matches the table except where the
   project records a difference on purpose, and that every path it names
   exists. If the section is missing,
   propose adding it (copied from `reference/conventions.md`, with the
   project's differences) and add it only after the user says yes; when run
   unattended, list it in the summary as a suggestion and do not add it.
6. Summarize in chat: a short list of what was added/updated/removed,
   grouped by file, so the user can spot-check anything surprising (e.g. a
   removed row means something that no longer exists in code).

---

## Guardrails

- Never document behavior you haven't verified in the actual code — no
  inferring behavior from a name, and no guessing what exists from a
  folder name alone. Read the actual definitions (routes, policies,
  configuration) directly.
- Don't let `CLAUDE.md` regrow a full reference table, a routing table, or a
  long step-by-step runbook — that belongs in `docs/`. Moving misplaced
  detail out is itself a finding to fix, not just a staleness issue to
  patch in place.
- Don't create a dated report file the way `code-reviewer`/
  `security-reviewer`/`database-reviewer` do — the corrected `CLAUDE.md`/
  `docs/*.md`/`README.md` files *are* the artifact. The chat summary is
  enough of a record for this session.
- **Fixed pointers.** A subsection of `CLAUDE.md` that is a short pointer
  into a `docs/*.md` section (it names the essential rule and links to the
  detail) is a fixed pointer, by design, with no per-feature narrative of
  its own. A feature that changes what one describes updates the linked
  `docs/*.md` file only; do not add narrative detail back into `CLAUDE.md`,
  and do not treat a pointer's unchanged wording as something needing a
  sync on every feature. Touch a pointer again only when the `docs/*.md`
  section or heading it links to is renamed or moved, or when one of the one
  or two load-bearing facts it states directly changes (for example a
  required minimum check device, or the shape of a shared type).
- **Scope.** The skill's scope is `CLAUDE.md`, `README.md` and the reference
  docs of the kit conventions (`reference/conventions.md`: `docs/*.md`
  except the folders below). It does not touch `<backlog>/` files (owned by
  `feature-plan`/`features-archive`), `docs/decisions/*.md` (owned by
  `decision-record` — an append-only decision history, not current-state
  reference material this skill keeps synced against the code),
  `docs/explorations/`, or the report directories under `<reports>/` (owned
  by their respective skills) — those are tracked or managed elsewhere, not
  part of this skill's "keep the docs honest" job.
