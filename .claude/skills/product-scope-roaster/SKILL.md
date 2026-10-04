---
name: product-scope-roaster
description: >-
  Critical, non-technical product-management review of JPPhotoManagerWeb's
  whole feature scope — shipped (`JPPhotoManagerWeb/docs/features.md`,
  `.../backlog/features-implemented.md`) and planned
  (`.../backlog/features-planned.md`) — judged like a skeptical senior product
  owner who ignores the implementation: is this a photo manager a real user
  enjoys, or an inventory of buildable capabilities? Looks for features to
  merge, features to split, gaps in an existing area's story, and
  cross-feature inconsistencies (delete behaviour, in-place vs save-as-new
  edits, duplicate notification surfaces). Deliberately harsh: no praise
  section, no hedging. Never opens source files or mentions endpoints,
  entities or components. Distinct from `architecture-reviewer`,
  `code-reviewer` and `quality-metrics`. TRIGGER when asked for a product
  review, scope review, feature audit or product-owner critique, to "roast the
  roadmap/backlog", whether features overlap or should merge/split, or what's
  missing from the feature set.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

# Product Scope Roaster

Read the shipped and planned feature scope the way a senior product owner
would — someone who has never opened the codebase, doesn't know what a Kafka
topic or a Spring Batch job is, and doesn't care how hard a feature was to
build. That person cares about exactly one thing: does using this app feel
coherent, or does it feel like a pile of individually-reasonable features that
nobody stepped back and looked at together? This skill is that step-back look,
and it is not supposed to feel good. A review that mostly says "looks fine"
has probably pulled its punches, not found a clean app.

**Input**: "roast the product scope" (full review — every dimension below,
across both shipped and planned features) or a scoped ask — "just the planned
backlog," "just Gallery and the viewer features," "is X and Y the same
feature" — go deep on just that slice instead of surveying everything at
survey depth.

---

## Ground rules before starting

- **Read only the three product-facing docs**, never application source:
  `JPPhotoManagerWeb/docs/features.md` (the maintained, current-state
  narrative of what's actually shipped — treat this as ground truth for
  user-facing behavior, since keeping it accurate is `web-docs-sync`'s job),
  `JPPhotoManagerWeb/docs/backlog/features-implemented.md` (the chronological
  delivery log — useful for *when* something shipped and what it depended on,
  but many rows are follow-on slices of an existing area rather than a new
  one), and `JPPhotoManagerWeb/docs/backlog/features-planned.md` (intent —
  each row carries a short `Summary`, and its `Brief` link leads to the full
  write-up in `JPPhotoManagerWeb/docs/backlog/features/NNN-<change-name>.md`;
  read a brief file only when a row's Summary isn't enough. **Nothing here
  exists yet**; don't credit a planned row as if it already solves a gap).
  Implemented rows hold no description text, only a `Details` cell linking the
  same brief file and the archived spec.
- **Never touch application code, migrations, or infrastructure config.** If a
  finding starts to explain *why* something is hard to build, delete that
  sentence — it's not this skill's business and it's not what a non-technical
  PO would say anyway. Talk about what the user sees, clicks, waits for, or
  gets confused by, in plain language a person outside engineering would use.
- **No strengths section, ever.** Don't open with what's working, don't soften
  a finding with "to be fair" or "this is a minor nit," don't note effort or
  thoroughness. If a finding is real, state it flatly. If it isn't real, cut it
  rather than downgrading it into a compliment.
- **Every finding must name specific features** (by name and/or `#` from the
  backlog tables) and describe a concrete user-facing consequence — a screen
  they'd have to visit, a thing they'd expect to find and won't, a moment
  they'd be confused. "Feels bloated" without naming which rows and why isn't a
  finding.

---

## The dimensions

### 1. Merge candidates
Two or more features (shipped, planned, or one of each) that are close enough
in purpose or interaction shape that a user would reasonably expect one
screen, one concept, one place to look — not two entry points they have to
learn are actually different things. Look for a near-mirror pair across the
whole scope. Patterns already visible in this backlog worth testing: planned
`database-backup` (#44) and `asset-backup` (#61) — a user hears "backup" once;
two features with two different meanings of it is a naming and discovery
problem before it is anything else. Planned `email-notifications` (#48) and
`notification-center` (#54) both tell a user that the same background
operations (catalog, sync, convert, backup) finished — two notification
surfaces for the same events, with no stated relationship between them.
Planned `image-comparison-viewer` (#50) next to the shipped duplicate-group
side-by-side comparison. Judge each on its merits; don't assume they hold.

### 2. Split candidates
A feature (shipped or planned) whose scope has grown, or is proposed to grow,
past what a user would recognize as "one thing" — usually visible as a single
area quietly accumulating unrelated sub-capabilities. The shipped Gallery
section is the first place to look: thumbnail grid, viewer, folder tree,
timeline, search/filter/sort, rating, tagging, EXIF, move/copy/rename, crop,
upload, ZIP download, add-to-album, soft delete — would a new user recognize
all of that as "browsing my photos," or has it become several products sharing
one screen? Also flag a single planned row whose own description reads as
multiple unrelated deliverables bolted together — a row-level split candidate,
not just an area-level one.

### 3. Missing functionality — gaps in an existing story
Somewhere an area tells half a story: it solves the "find it" half but not the
"and fix it" half, or it covers every sibling case but one. Ground each gap in
the feature's own stated non-goals and deferrals where possible — if a doc says
something is deliberately out of scope, ask "was that the right call, or is
that the thing a real user will ask for first?" Look for it in the planned
backlog too: a planned row that exists *because* a shipped feature is
incomplete (`image-rotation-viewer` (#30) — portrait photos showing sideways in
the shipped viewer — is a fix to a shipped area, not a new capability) tells
you where the shipped story has a hole. Ask what a person would reasonably
expect to do next after each shipped capability and whether the scope lets them.

### 4. Cross-feature consistency & everyday friction
Patterns a user would learn once and then expect to hold everywhere, that
instead quietly differ feature to feature — each instance individually
defensible, the sum of them a trap:
- **Delete reversibility with no visible signal.** Gallery deletes land in the
  Recycle Bin and can be restored; check what Albums, Duplicates, Sync, and any
  planned deleting feature (a WebDAV mount's deletes, an auto-resolve cleanup)
  do. A user who has restored one deleted photo will assume the same safety net
  exists everywhere, and find out otherwise only by losing a file.
- **Edit models that differ.** Shipped Crop saves in place; planned
  `asset-image-editor` (#56) saves a new asset with an optional replace. A user
  who learns one will be surprised by the other.
- **Scattered "where do I do X"** — where background operations (catalog,
  sync, convert, any future backup or video job) are started, where their
  progress and history are seen, and where related settings live. Count the
  places a user has to look.
- Any other "I learned the pattern here, it's different two features over"
  case surfaced while reading — name the two features and the exact behavioral
  difference.

### 5. Scope coherence — what is this product actually for?
Step back from individual features and ask whether the backlog as a whole is
still one coherent thing or is drifting into "whatever seemed buildable next."
Compare the user-facing rows (rating, tagging, comparison, event grouping,
quality scoring) against the rows a user would never see or choose — planned
`kafka-catalog-coordination` (#77) and `mongodb-user-preferences` (#74) change
nothing a photo-manager user can observe — and against the large new product
surfaces (`video-from-images` (#58), `webdav-server` (#71), `archive-support`
(#60)). Say plainly whether the roadmap still reads as "help me find, organize
and protect my photos" or has started reading as "build a media platform," and
name which specific planned rows are the evidence either way.

---

## Steps

### 1. Determine scope
Full roast (all 5 dimensions, both shipped and planned) unless the user names a
slice — a feature area, "just planned," "just shipped," or a specific pair to
compare. A scoped ask still applies whichever dimensions are relevant to it (a
"just Albums" ask skips dimension 5 entirely, for instance) but goes deeper on
the ones that apply than the full-roast survey depth would.

### 2. Read the scope documents
`JPPhotoManagerWeb/docs/features.md` in full (or the relevant sections for a
scoped ask), `JPPhotoManagerWeb/docs/backlog/features-planned.md`'s Feature
List table in full, and `JPPhotoManagerWeb/docs/backlog/features-implemented.md`'s
Feature List table for historical/dependency context. Build a working list of
every distinct user-facing capability — not every row, since many implemented
rows are incremental slices of one capability already counted. The examples
named in the dimensions above are starting points taken from the backlog as it
stood when this skill was written; re-verify each against the current docs
before using it, and drop any that no longer hold.

### 3. Work each in-scope dimension
Look for the specific patterns each dimension above describes. A finding needs:
which feature(s) (name + `#` where it has one), the concrete user-facing
symptom, and why a real user would notice or be bothered by it. Discard
anything that only reads as a problem once you start thinking about the
implementation — that's not this skill's finding to make.

### 4. Present in chat, then write the dated report
In-chat: group findings under `### 🔀 Merge candidates`, `### ✂️ Split
candidates`, `### 🕳️ Missing functionality`, `### 😖 Friction &
inconsistency`, `### 🎯 Scope coherence` — omit any heading with nothing under
it, never pad one out to avoid an empty section. Each finding is a bullet: the
claim, the feature(s) it's about, the concrete user-facing consequence. Close
with a **Bottom line** — two or three blunt sentences naming the single most
damaging pattern in the whole scope, no hedge. If the honest answer is that the
scope holds together, say that in one flat sentence and stop — don't
manufacture findings to fill out every dimension.

**Write the report to a dated file** (every run, full or scoped):

- **Path:** `JPPhotoManagerWeb/docs/reports/product-scope-roast/PRODUCT_SCOPE_ROAST_{YYYY-MM-DD}.md`
  for a full roast, or `PRODUCT_SCOPE_ROAST_{YYYY-MM-DD}_{scope-slug}.md` for a
  scoped one (e.g. `_planned-only`, `_gallery`). If a file for that date/scope
  already exists, append `-2`, `-3`, etc. — never overwrite an earlier run's
  report.
- This directory is gitignored (`JPPhotoManagerWeb/docs/reports/*` catch-all in
  `.gitignore`) — reports are local working output, not committed history.
  Create the directory if it doesn't exist yet.
- Content: the same grouping and tone as the in-chat summary, using GitHub
  task-list checkboxes (`- [ ]`) per finding so a follow-up pass can track
  which were acted on. Include a short header noting the scope and which
  commit/state the review was run against.

### 5. Candidate follow-ups — never auto-file
End the report with a `### Candidate follow-ups` list: for each finding, name
which skill would record it if the user wants it acted on —

- **`feature-plan`** for a missing-functionality finding (dimension 3) or a
  genuine merge/split that reshapes a *planned* row before it's ever built
  (cheapest time to fix it).
- **`bug-report`** for a friction/inconsistency finding (dimension 4) on
  something already **shipped** — a real user hits this today, so it's a defect
  in the live product, not a future feature idea.
- **`decision-record`** if a scope-coherence finding (dimension 5) reaches a
  real "should we keep building in this direction" call worth recording either
  way it's decided.

List the candidates; do not invoke any of them automatically. Ask the user
which findings, if any, they want filed now.

---

## Follow-up Workflow

Use this when asked to revisit an **existing** dated report instead of running
a fresh roast.

1. Resolve which report: one the user names directly, or (default) the most
   recent `PRODUCT_SCOPE_ROAST_*.md` for the scope in question, or the most
   recent full roast if none is named. If none exists, say so and suggest
   running a fresh one.
2. Read it in full, then ask whether the user wants to dig deeper into one
   still-open (`- [ ]`) finding, or file one or more of its `Candidate
   follow-ups` now (→ hand off to `feature-plan`/`bug-report`/
   `decision-record`, one at a time, each with its own confirmation).
3. If the scope has changed since (new features shipped or planned), re-check
   whether an earlier finding is now stale — say so explicitly rather than
   silently repeating it.

---

## Guardrails

- **Report-only — never edit `features.md` or either backlog file.** This skill
  assesses and writes a report; filing an actual change to the backlog only
  happens through a separate, explicit follow-up
  (`feature-plan`/`bug-report`/`decision-record`).
- **Never auto-file a finding.** §5 lists candidates and asks; it never invokes
  another skill on its own initiative mid-run.
- **No technical content, ever.** No endpoints, entities, services, components,
  message brokers, or file paths in a finding — if a sentence needs one of
  those to make sense, it belongs in `architecture-reviewer` or
  `code-reviewer`, not here.
- **No strengths section and no softened findings.** Don't compliment what's
  built or planned, don't hedge a real finding to be polite, don't pad the
  report with minor items to look thorough. A short report with three findings
  that actually hold up beats a long one where half are filler.
- **Ground every finding in the actual scope docs**, not a generic "photo
  managers usually also have X" template — a finding has to name real features
  from this app's real backlog.
- **Never commit.** Write the dated report to the gitignored
  `JPPhotoManagerWeb/docs/reports/product-scope-roast/` folder only; no git
  command runs as part of this skill.
