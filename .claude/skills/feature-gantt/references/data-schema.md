# `gen_gantt.py` input schema

The script takes one JSON file and writes one standalone HTML file. Every
field below is **required** unless marked optional. Dates are `YYYY-MM-DD`.

```json
{
  "today": "2026-08-16",
  "page_title": "Feature Timeline",
  "eyebrow": "JPPhotoManager · delivery history & projection",
  "title": "Sixty shipped, twelve ahead",
  "lede": "The solid bars are every SDD change that has actually shipped into <code>develop</code> ... <strong>projection</strong> ...",

  "shipped": [
    {
      "num": "04",
      "name": "virtual-albums",
      "area": "fullstack",
      "priority": "P2",
      "schema": true,
      "effort": "M",
      "date": "2026-06-14",
      "desc": "User-defined albums backed by an albums table and an album-membership join."
    }
  ],

  "planned": [
    {
      "num": "47",
      "name": "two-factor-authentication",
      "area": "fullstack",
      "priority": "P1",
      "schema": true,
      "effort": "L",
      "desc": "TOTP enrolment and challenge flow, a user_totp table, and a SecurityConfig filter."
    }
  ],

  "insights": [
    {
      "title": "Weekend cadence, one weekday sprint",
      "body": "Nine of the eleven ship days fall on a Saturday or Sunday. <code>inline code</code> and &mdash; entities are fine here — this HTML goes straight into the page."
    }
  ],

  "gap_notes": [
    { "start": "2026-08-13", "end": "2026-08-14", "label": "quality-tooling work, no new feature" }
  ],
  "footnote_extra": "Local git history is shallow before Aug&nbsp;8, so the five earliest features are dated from their OpenSpec archive folders only."
}
```

## Field notes

**`today`** — the actual current date at generation time, not the last shipped
date. Planned bars stack starting the day *after* this.

**`page_title` / `eyebrow` / `title` / `lede`** — copy, authored fresh each
run (see SKILL.md step 5). `title`/`lede` are plain strings dropped straight
into `<h1>`/`<p class="lede">` — HTML entities and `<code>`/`<strong>` tags
are fine, but the script does not escape them, so don't put raw user/repo
data in here without checking it doesn't break the markup.

**`shipped[]`** (one entry per row in
`JPPhotoManagerWeb/docs/backlog/features-implemented.md`, in the display
order you want top-to-bottom):
- `num` — the `#` column, as a string (so `"—"` works for an undocumented row)
- `name` — the backtick-wrapped change name, without the backticks
- `area` — `backend` | `frontend` | `fullstack` | `infra` (must be a key in
  the script's `AREA_META` — see "Adding a fifth area" below if a real
  fifth value ever appears)
- `priority` — `P0`–`P3` (display only for shipped rows; not scored)
- `schema` — `true`/`false`, drives the `DB` badge and the schema-touched stat
- `effort` — `S`/`M`/`L`
- `date` — the day it shipped (see SKILL.md step 2 for how to derive this)
- `desc` — one clause, no trailing period needed (the script adds the rest
  of the tooltip sentence around it)

**`planned[]`** — same shape minus `date`, **in the exact order** of
`JPPhotoManagerWeb/docs/backlog/features-planned.md`'s own `### Recommended
implementation order` list (first entry = next feature). The script derives
each bar's position purely from list order plus `effort` — it does not
re-derive priority/dependency ordering itself.

**`insights[]`** — 3–4 cards, `body` is raw HTML (matches the existing
insight cards' use of `<code>`, `&mdash;`, `&nbsp;`). This is the one section
the script cannot generate — see SKILL.md step 5.

**`gap_notes[]`** (optional) — label a specific date range in the shipped
history with a *reason* (e.g. "quality-tooling work, no new feature") instead
of the generic auto-detected "no features shipped" label. A noted range
always renders, regardless of length — the script's own auto-detection only
kicks in for *unnoted* gaps of 3+ days, as a safety net so a real gap can't
silently go unlabeled.

**`footnote_extra`** (optional) — one sentence appended to the "Shipped
bars" footnote for a caveat specific to this run (e.g. shallow git history
before a certain date). Omit if there's nothing to add.

## Adding a fifth area

`AREA_META` and the CSS's four `--backend`/`--frontend`/`--fullstack`/`--infra`
custom properties are hardcoded to the four `Area` values the backlog
`features-planned.md` column legend documents project-wide. If a genuinely
new `Area` value ever appears in the backlog docs, the script will refuse
to run (see its startup validation) rather than silently drop the row or
guess a color — that's deliberate. To add one: load the `dataviz` skill,
pick the next unused categorical slot from `references/palette.md`, run
`scripts/validate_palette.js` against the existing four plus the new one
for both `--mode light` and `--mode dark` (the skill's own default
reference surfaces are fine here; this chart's actual `--surface` tokens,
`#ffffff` light / `#171b21` dark, are close enough to those defaults not to
need a custom `--surface` override), and only then add the new hex pair to
both `AREA_META` and every CSS custom-property block in `gen_gantt.py`.

The `infra` slot's current hex pair (`#8257e6` light / `#9772f0` dark) was
added when this skill was first set up here and has not yet been run
through `validate_palette.js` against this chart's real surfaces — if a
future run actually plots an `infra` row, validate it then and adjust if
needed.
