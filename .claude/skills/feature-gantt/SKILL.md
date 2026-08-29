---
name: feature-gantt
description: Regenerates the JPPhotoManager "Feature Timeline" Gantt artifact — a horizontal chart of every shipped feature (JPPhotoManagerWeb/docs/backlog/features-implemented.md) plus a projection of the planned ones (JPPhotoManagerWeb/docs/backlog/features-planned.md's own Recommended implementation order). Gathers fresh data from the two backlog docs, openspec/changes/archive/ dates, and git log, feeds it to a bundled Python generator that owns all the geometry/label math, then publishes the result as an Artifact. TRIGGER when the user asks to update, regenerate, rebuild, or refresh the feature timeline/Gantt chart, or asks for a new Gantt/delivery-timeline visualization of JPPhotoManager's shipped and planned features.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Regenerate the Feature Timeline artifact from the current state of the backlog docs and publish it.

**Input**: Optional — the artifact URL to update in place (if updating a previously published chart) and/or a note about what changed since last time (e.g. "just added feature #47, re-run it").

---

## Why this exists

The bar geometry, label placement (including the right-edge overflow
case), row/status tagging, and every stat tile's math are **arithmetic and
templating that has no business being redone by hand or re-derived from
scratch by an LLM each time** — they live in `scripts/gen_gantt.py`. Doing
any of it inline invites the classic failures: text clipped inside narrow
bars, labels running off the right edge, a post-hoc regex that
mis-tags half the rows, a stat tile that goes stale the moment a row is
added. This skill's job is gathering fresh, correct *data* and handing it
to that script — never re-implementing the chart.

## Steps

### 1. Skip the design pass

The bundled script already encodes a categorical palette (4 area slots)
and the full light/dark token scaffolding (`artifact-design`'s three-state
theme pattern). You do **not** need to load `dataviz` or `artifact-design`
fresh, or re-derive colors, type, or layout — that work is pinned in the
script's CSS. Only load those skills if you are deliberately changing the
visual design itself (a new area color, a different chart shape) rather
than just regenerating with fresh data — see `references/data-schema.md`'s
"Adding a fifth area" for that path. (Note: the `infra` slot's colour pair
has not yet been validated against this chart's real surfaces — if a run
actually plots an `infra` row, validate it per that same section.)

### 2. Gather shipped features

Read `JPPhotoManagerWeb/docs/backlog/features-implemented.md`'s
`## Feature List` table in full. For each row you'll need `#`, change
name, Priority, Schema Change, Effort, Area, and a one-clause summary
distilled from the Brief description (don't paste the whole paragraph —
see the `desc` field note in the data schema).

**Deriving each row's ship date** (needed for every shipped row, not in the
table itself):
1. Primary source: the date prefix on that change's folder under
   `openspec/changes/archive/YYYY-MM-DD-<name>/`.
2. Cross-check: `git log --pretty=format:'%ad|%s' --date=short --all | grep -i <name>`
   for the merge-commit date. They should agree; if they don't, prefer the
   archive-folder date and note the discrepancy to the user.
3. If a row has no archive folder at all (rare — e.g. it predates the SDD
   workflow, or was reverted and re-landed under a different flow), fall
   back to the nearest merge-commit date you can find, and flag it as a
   lower-confidence date in your summary to the user rather than silently
   presenting it as certain.
4. `git log`'s history can be shallow (a fresh clone or a CI checkout may
   not go back far) — if dates for the earliest rows aren't derivable
   because the commits simply aren't present locally, say so, use the
   archive-folder dates alone (they don't require git history), and put a
   note in `footnote_extra`.

Order the `shipped` array chronologically by ship date; for two features
that shipped the same day, order them by any dependency relationship you can
find in `## Dependencies` → `### Hard implementation dependencies` in either
backlog doc (a prerequisite before its dependent), then by feature number.

### 3. Gather planned features, in the backlog's own order

Read `JPPhotoManagerWeb/docs/backlog/features-planned.md`'s `## Feature
List` table for every row whose Implementation column is `⬜ Pending` or
`🔶 In Progress` (`feature-development` marks a row `🔶 In Progress` as
soon as it starts work on it — see that skill's Step 1.6 — so it's still
unshipped and belongs in this projection, not in the shipped set), and its
`### Recommended implementation order` subsection under `## Dependencies`
for their sequence.

**The order list is authoritative — use it as given, don't re-derive it.**
`feature-plan` and `features-next` both keep it in sync as the backlog
changes (see those skills), so by the time you're running this skill it
should already reflect current priorities and dependencies. Build the
`planned` array in exactly that order.

This project's `### Recommended implementation order` section is a
hand-maintained narrative (clustered code blocks, a "no hard
dependencies" paragraph, "within dependent clusters" arrows) rather than a
single flat numbered list — read it in full and flatten it into a single
sequence yourself: dependent clusters in prerequisite-first order, then
the no-dependency features, using priority tier (P1→P2→P3) then feature
number to order within each group.

If a still-`⬜ Pending` / `🔶 In Progress` row isn't mentioned anywhere in
that section (or the whole section is empty), the backlog doc is out of
sync — tell the user, then fall back to deriving order yourself for the
affected rows only (priority tier P1→P2→P3, hard dependency, then feature
number) rather than blocking the whole chart on a doc-hygiene issue.

### 4. Identify real gaps worth annotating

Skim `git log` across the shipped date range for stretches where nothing
shipped. A gap is worth a `gap_note` (rather than leaving it to the script's
generic auto-detection) when you have an actual reason — e.g. commits show
tooling/testing/skills work with no user-facing feature, or the gap
corresponds to a release-branch/hardening period. If you can't find a reason
for a multi-day gap, leave it out of `gap_notes`; the script will still flag
any run of 3+ shipless days automatically with a neutral label, so nothing
real goes unlabeled — you're only adding value by replacing the generic
label with a specific one where you actually know the reason.

### 5. Write the insights

This is the one part of the chart the script cannot generate — 3 to 4 short
cards, each a genuine, specific finding from the data you just gathered, not
generic filler. Good examples: a cadence pattern (which days of the week
ship things), a structural pattern (schema-touching work front-loaded vs.
not, or backend-vs-frontend-vs-infra balance over time), a forward-looking
read of the planned set (what the dependency chain implies about
sequencing), and a methodology/caveat note (what the projected order
actually is and isn't). **Regenerate these fresh from the current data — do
not copy-paste a previous run's insights forward without checking they still
hold.** If a previous run's insight is still true, say so in your own words
with current numbers; if it's gone stale (e.g. a stat changed), replace it.

Also write `eyebrow`, `title`, and `lede` fresh — short, specific, matching
the existing tone (see `references/data-schema.md`'s field notes and the
currently-published chart for calibration). Don't default to a generic
"Gantt Chart" framing.

### 6. Assemble the data file and run the script

Write the JSON described in `references/data-schema.md` to a scratch file,
then run:

```
python3 <this-skill-dir>/scripts/gen_gantt.py <data.json> <output.html>
```

The script prints a one-line summary (counts, gap bands found, computed
span) on success, or a clear error naming the offending row if something in
your data is malformed (bad area, bad effort, no shipped rows). Fix the
*data* and re-run — never hand-patch the generated HTML to work around a
data problem.

### 7. QA before publishing

Render both themes, and **do not trust a same-width screenshot for the
right-hand side of the chart.** The Gantt's `.gantt` element has its own
`min-width` (computed from the day span) inside an `overflow-x: auto`
container that's narrower than the page's `max-width`, so a naive
screenshot at a viewport just wide enough to show the page comfortably
will silently clip everything past roughly the two-week mark — the
elements are still there and correctly styled (verify with
`elementHandle.boundingBox()` if a screenshot looks empty and you're not
sure why), they're just scrolled out of the captured viewport. Check both
ends explicitly:

```js
// scroll the inner container before shooting the right edge
await page.evaluate(() => { document.querySelector('.gantt-scroll').scrollLeft = 999999; });
```

Take: a full-page screenshot in light mode, one in dark mode
(`page.emulateMedia({ colorScheme: 'dark' })`), and a `.gantt-card`-scoped
screenshot scrolled to the right edge in at least one theme. Look for: label
text clipped inside a bar (shouldn't happen — labels render outside the bar
by construction — but check), the "today" line landing on the correct date,
gap bands sitting where you expect, and the table view (`page.click`ing the
`<summary>` first) showing every row with the right Status chip.

### 8. Publish

Use the Artifact tool. If you have the existing chart's URL (from the
user's input, or by asking, or via `Artifact` `action: "list"`), pass it as
`url` to update in place rather than creating a new artifact — this chart is
meant to have one durable URL across regenerations, not a new one per run.

---

## Guardrails

- Never hand-edit the generated HTML's bar positions, labels, or row tags to
  fix something that looks wrong — fix the input JSON or the script, and
  regenerate. The script exists precisely so that geometry/label/tagging
  math never needs doing by hand.
- The effort→duration mapping is fixed by design, not a per-run choice:
  shipped bars use S/M/L = 0.62/1.15/1.85 illustrative days (visual weight
  only — the bar width is not a real elapsed-time measurement); planned
  bars use a literal S/M/L = 1/2/3 workday estimate, stacked with no
  parallelism. Don't invent different multipliers for a "more accurate"
  look — the footnote text explains the real caveat instead.
- Keep the "shape, not a schedule" framing in the projected-bars copy
  (footnote and at least one insight card) every run — the projection reads
  very close to a real delivery date if that caveat is trimmed for space.
- `planned` order comes from `features-planned.md`'s own Recommended
  implementation order — this skill is a consumer of that section, not
  another place that re-derives it (that's `feature-plan`'s job when a
  feature is added, and `features-next`'s job when picking what's next).
- If `JPPhotoManagerWeb/docs/backlog/features-implemented.md` or
  `features-planned.md` don't exist yet, there's nothing to chart — tell
  the user to run `feature-plan` first, and stop.
