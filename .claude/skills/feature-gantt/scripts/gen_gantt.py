#!/usr/bin/env python3
"""
Generate the JPPhotoManager "Feature Timeline" Gantt artifact: a
horizontal timeline of every shipped feature
(JPPhotoManagerWeb/docs/backlog/features-implemented.md) plus a projection
of the planned ones (JPPhotoManagerWeb/docs/backlog/features-planned.md's
own Recommended implementation order).

This script owns every piece of geometry math and HTML templating that
caused real bugs the first time this chart was built by hand: label text
clipped inside narrow bars, labels running off the right edge of the
chart, and a regex-based post-hoc row-tagging pass that silently
mis-tagged rows. None of that is re-derived per run — feed it data, get
a correct chart.

Usage:
    python3 gen_gantt.py <data.json> <output.html>

See ../references/data-schema.md for the input JSON shape, and
../SKILL.md for how to gather that data from the repo.
"""
import sys
import json
import html
import calendar
import datetime
from collections import Counter

AREA_META = {
    "backend":   "Backend",
    "frontend":  "Frontend",
    "fullstack": "Full-stack",
    "infra":     "Infra",
}

# Illustrative only for shipped bars — S/M/L just needs to read as
# "denser effort = visually heavier"; the bar width is not a real
# elapsed-time measurement (see the footnote copy for the actual caveat).
SHIPPED_DUR = {"S": 0.62, "M": 1.15, "L": 1.85}

# Literal workday estimate for planned bars, stacked back-to-back with
# no parallelism and no days off starting the day after "today".
PLANNED_DUR = {"S": 1, "M": 2, "L": 3}

LEFT_FALLBACK_THRESHOLD = 88.0  # % — past this, put the label before the bar, not after

CSS = """
  :root {
    color-scheme: light;
    --bg: #f5f6f9;
    --surface: #ffffff;
    --surface-2: #f0f1f5;
    --text-primary: #14181f;
    --text-secondary: #545e6d;
    --text-muted: #8891a0;
    --border: rgba(20, 24, 31, 0.10);
    --border-strong: rgba(20, 24, 31, 0.16);
    --gridline: #e7e9ee;
    --gap-band: rgba(20, 24, 31, 0.04);
    --backend: #1baf7a;
    --frontend: #2a78d6;
    --fullstack: #eb6834;
    --infra: #8257e6;
    --chip-bg: #f0f1f5;
    --today-line: #14181f;
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      color-scheme: dark;
      --bg: #0f1216;
      --surface: #171b21;
      --surface-2: #1d222a;
      --text-primary: #f2f4f7;
      --text-secondary: #aab2c0;
      --text-muted: #798295;
      --border: rgba(255, 255, 255, 0.08);
      --border-strong: rgba(255, 255, 255, 0.16);
      --gridline: #262b34;
      --gap-band: rgba(255, 255, 255, 0.035);
      --backend: #199e70;
      --frontend: #3987e5;
      --fullstack: #d95926;
      --infra: #9772f0;
      --chip-bg: #232833;
      --today-line: #f2f4f7;
    }
  }
  :root[data-theme="dark"] {
    color-scheme: dark;
    --bg: #0f1216;
    --surface: #171b21;
    --surface-2: #1d222a;
    --text-primary: #f2f4f7;
    --text-secondary: #aab2c0;
    --text-muted: #798295;
    --border: rgba(255, 255, 255, 0.08);
    --border-strong: rgba(255, 255, 255, 0.16);
    --gridline: #262b34;
    --gap-band: rgba(255, 255, 255, 0.035);
    --backend: #199e70;
    --frontend: #3987e5;
    --fullstack: #d95926;
    --infra: #9772f0;
    --chip-bg: #232833;
    --today-line: #f2f4f7;
  }

  * { box-sizing: border-box; }

  body {
    margin: 0;
    background: var(--bg);
    color: var(--text-primary);
    font-family: -apple-system, "Segoe UI", system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
  }

  .page {
    max-width: 1220px;
    margin: 0 auto;
    padding: 56px 28px 72px;
  }

  .eyebrow {
    font-family: ui-monospace, "SF Mono", "Cascadia Code", "Roboto Mono", monospace;
    font-size: 11.5px;
    font-weight: 600;
    letter-spacing: 0.11em;
    text-transform: uppercase;
    color: var(--text-muted);
    margin: 0 0 12px;
  }

  h1 {
    font-size: clamp(28px, 4vw, 38px);
    font-weight: 700;
    letter-spacing: -0.015em;
    margin: 0 0 10px;
    text-wrap: balance;
  }

  .lede {
    font-size: 15.5px;
    line-height: 1.6;
    color: var(--text-secondary);
    max-width: 68ch;
    margin: 0 0 30px;
  }
  .lede strong { color: var(--text-primary); font-weight: 600; }

  /* ---------- stat tiles ---------- */
  .stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1px;
    background: var(--border);
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 26px;
  }
  .stat {
    background: var(--surface);
    padding: 16px 20px;
  }
  .stat-value {
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.01em;
    font-variant-numeric: tabular-nums;
    line-height: 1.15;
  }
  .stat-label {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    color: var(--text-muted);
    margin-top: 6px;
  }
  .stat-sub {
    font-size: 12.5px;
    color: var(--text-secondary);
    margin-top: 3px;
  }
  .stats.is-projected .stat-value { color: var(--text-secondary); }
  .stats-row { margin-bottom: 22px; }
  .stats-row:last-of-type { margin-bottom: 26px; }

  /* ---------- legend ---------- */
  .legend {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 18px;
    padding: 12px 16px;
    margin-bottom: 14px;
    border: 1px solid var(--border);
    border-radius: 10px;
    background: var(--surface);
    font-size: 12.5px;
    color: var(--text-secondary);
  }
  .legend-group { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
  .legend-item { display: flex; align-items: center; gap: 7px; }
  .legend-swatch {
    width: 12px; height: 12px; border-radius: 3px; flex: none;
  }
  .legend-swatch.is-projected {
    background-color: color-mix(in srgb, var(--text-muted) 22%, var(--surface));
    background-image: repeating-linear-gradient(45deg, var(--text-muted) 0 2px, transparent 2px 5px);
    border: 1px dashed var(--text-muted);
  }
  .legend-sep {
    width: 1px; align-self: stretch; background: var(--border-strong);
  }
  .legend-note-schema {
    display: inline-flex; align-items: center; justify-content: center;
    font-family: ui-monospace, monospace; font-size: 9px; font-weight: 700;
    width: 18px; height: 14px; border-radius: 3px;
    border: 1px solid var(--border-strong); color: var(--text-muted);
  }

  /* ---------- gantt ---------- */
  .gantt-card {
    border: 1px solid var(--border);
    border-radius: 12px;
    background: var(--surface);
    padding: 20px 20px 16px;
  }
  .gantt-scroll {
    overflow-x: auto;
  }
  .gantt {
    position: relative;
    min-width: __MIN_WIDTH__px;
    --label-w: 336px;
    --axis-h: 32px;
  }

  .gantt-axis {
    display: flex;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border-strong);
    margin-bottom: 6px;
  }
  .axis-spacer { width: var(--label-w); flex: none; display: flex; align-items: flex-end; }
  .axis-month {
    font-family: ui-monospace, monospace;
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--text-muted);
  }
  .axis-track {
    flex: 1 1 auto;
    position: relative;
    height: var(--axis-h);
  }
  .axis-tick {
    position: absolute;
    top: 12px;
    transform: translateX(-50%);
    font-family: ui-monospace, monospace;
    font-size: 10.5px;
    font-variant-numeric: tabular-nums;
    color: var(--text-muted);
  }
  .axis-tick.is-first, .axis-tick.is-last { color: var(--text-secondary); font-weight: 600; }
  .axis-tick.is-today { color: var(--text-primary); font-weight: 700; }
  .axis-annotation {
    position: absolute;
    top: -1px;
    font-family: ui-monospace, monospace;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    white-space: nowrap;
  }
  .axis-annotation.today { color: var(--text-primary); transform: translateX(-50%); }
  .axis-annotation.month { color: var(--text-muted); }

  .gantt-body { display: flex; flex-direction: column; gap: 3px; padding: 4px 0 6px; position: relative; z-index: 1; }

  .row { display: flex; align-items: stretch; min-height: 34px; }
  .row[data-status="planned"] .row-name { color: var(--text-secondary); }
  .row[data-status="planned"] .row-num { opacity: 0.7; }

  .row-label {
    width: var(--label-w);
    flex: none;
    display: flex;
    align-items: center;
    gap: 7px;
    padding-right: 14px;
    min-width: 0;
  }
  .row-num {
    font-family: ui-monospace, monospace;
    font-size: 10.5px;
    color: var(--text-muted);
    width: 16px;
    flex: none;
    text-align: right;
  }
  .row-name {
    font-family: ui-monospace, "SF Mono", "Cascadia Code", "Roboto Mono", monospace;
    font-size: 11.5px;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1 1 auto;
    min-width: 0;
  }
  .row-pri {
    flex: none;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.02em;
    color: var(--text-muted);
    border: 1px solid var(--border-strong);
    border-radius: 4px;
    padding: 1px 5px;
  }
  .row-schema {
    flex: none;
    font-family: ui-monospace, monospace;
    font-size: 9px;
    font-weight: 700;
    color: var(--text-muted);
    border: 1px solid var(--border-strong);
    border-radius: 3px;
    padding: 1px 4px;
  }

  .row-track {
    flex: 1 1 auto;
    position: relative;
    border-radius: 4px;
    background-image: linear-gradient(to right, var(--gridline) 1px, transparent 1px);
    background-size: __DAY_PCT__% 100%;
  }
  .row:hover .row-track { background-color: var(--surface-2); }
  .row:hover .row-name { color: var(--frontend); }

  .bar {
    position: absolute;
    top: 6px;
    bottom: 6px;
    border-radius: 4px;
    min-width: 5px;
  }
  .bar-backend { background: var(--backend); }
  .bar-frontend { background: var(--frontend); }
  .bar-fullstack { background: var(--fullstack); }
  .bar-infra { background: var(--infra); }

  .bar-projected {
    border-width: 1.5px;
    border-style: dashed;
    background-image: repeating-linear-gradient(45deg, rgba(255,255,255,0.4) 0 3px, transparent 3px 7px);
  }
  .bar-backend.bar-projected { background-color: color-mix(in srgb, var(--backend) 22%, var(--surface)); border-color: var(--backend); }
  .bar-frontend.bar-projected { background-color: color-mix(in srgb, var(--frontend) 22%, var(--surface)); border-color: var(--frontend); }
  .bar-fullstack.bar-projected { background-color: color-mix(in srgb, var(--fullstack) 22%, var(--surface)); border-color: var(--fullstack); }
  .bar-infra.bar-projected { background-color: color-mix(in srgb, var(--infra) 22%, var(--surface)); border-color: var(--infra); }

  .bar-date-outside {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    margin-left: 6px;
    font-family: ui-monospace, monospace;
    font-size: 10px;
    font-weight: 600;
    color: var(--text-secondary);
    white-space: nowrap;
    pointer-events: none;
  }
  .bar-date-outside.is-left { margin-left: 0; transform: translate(calc(-100% - 6px), -50%); }
  .bar-date-outside.is-projected-label { color: var(--text-muted); font-weight: 500; font-style: italic; }

  .gap-overlay, .marker-overlay {
    position: absolute;
    left: var(--label-w);
    right: 0;
    top: 46px;
    bottom: 6px;
    pointer-events: none;
    z-index: 0;
  }
  .gap-band {
    position: absolute;
    top: 0;
    bottom: 0;
    background: var(--gap-band);
    border-left: 1px dashed var(--border-strong);
    border-right: 1px dashed var(--border-strong);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 6px;
  }
  .gap-band span {
    font-size: 10.5px;
    color: var(--text-muted);
    text-align: center;
    line-height: 1.35;
    max-width: 90%;
  }
  .today-line {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 2px;
    background: var(--today-line);
    opacity: 0.55;
  }
  .month-line {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 1px;
    background: var(--border-strong);
  }

  /* ---------- insights ---------- */
  .insights {
    margin-top: 30px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 14px;
  }
  .insight {
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 16px 18px;
    background: var(--surface);
  }
  .insight-title {
    font-size: 12.5px;
    font-weight: 700;
    margin-bottom: 6px;
  }
  .insight-body {
    font-size: 13px;
    line-height: 1.55;
    color: var(--text-secondary);
  }

  /* ---------- table view ---------- */
  details.table-view {
    margin-top: 30px;
    border: 1px solid var(--border);
    border-radius: 10px;
    background: var(--surface);
  }
  details.table-view summary {
    cursor: pointer;
    padding: 14px 18px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
    list-style: none;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  details.table-view summary::-webkit-details-marker { display: none; }
  details.table-view summary::before {
    content: "\\25B8";
    font-size: 11px;
    color: var(--text-muted);
    transition: transform 0.15s ease;
  }
  details.table-view[open] summary::before { transform: rotate(90deg); }
  .table-wrap { overflow-x: auto; border-top: 1px solid var(--border); }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12.5px;
  }
  thead th {
    text-align: left;
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--text-muted);
    padding: 10px 14px;
    border-bottom: 1px solid var(--border-strong);
    white-space: nowrap;
  }
  tbody td {
    padding: 9px 14px;
    border-bottom: 1px solid var(--border);
    color: var(--text-secondary);
    white-space: nowrap;
  }
  tbody tr:last-child td { border-bottom: none; }
  td.name { color: var(--text-primary); font-family: ui-monospace, monospace; font-size: 12px; }
  td.num { font-variant-numeric: tabular-nums; color: var(--text-muted); }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 7px; vertical-align: middle; }
  .status-chip {
    font-size: 10px; font-weight: 700; letter-spacing: 0.03em;
    border-radius: 4px; padding: 2px 6px; text-transform: uppercase;
  }
  .status-chip.status-shipped { color: var(--text-secondary); background: var(--chip-bg); }
  .status-chip.status-planned { color: var(--text-muted); background: transparent; border: 1px dashed var(--border-strong); }

  /* ---------- footer ---------- */
  .footnote {
    margin-top: 26px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--text-muted);
    max-width: 82ch;
  }
  .footnote + .footnote { margin-top: 14px; }
  .footnote code {
    font-family: ui-monospace, monospace;
    font-size: 11.5px;
    background: var(--chip-bg);
    padding: 1px 5px;
    border-radius: 4px;
  }

  a { color: var(--frontend); }

  @media (max-width: 720px) {
    .stats { grid-template-columns: repeat(2, 1fr); }
  }

  @media (prefers-reduced-motion: reduce) {
    * { transition: none !important; }
  }
"""


def parse_date(s):
    return datetime.date.fromisoformat(s)


def fmt_short(d):
    return d.strftime("%b ") + str(d.day)


def esc(s):
    return html.escape(s, quote=True)


def label_span(text, left_pct, width_pct, extra_cls=""):
    end_pct = left_pct + width_pct
    if end_pct > LEFT_FALLBACK_THRESHOLD:
        cls = ("bar-date-outside is-left " + extra_cls).strip()
        return f'<span class="{cls}" style="left:{left_pct:.2f}%">{text}</span>'
    cls = ("bar-date-outside " + extra_cls).strip()
    return f'<span class="{cls}" style="left:{end_pct:.2f}%">{text}</span>'


def row_block(row, left, width, title, label_html, status, projected):
    area = row["area"]
    schema_badge = ('<span class="row-schema" title="Changed the database schema">DB</span>'
                     if row.get("schema") else '')
    bar_cls = f"bar bar-{area}" + (" bar-projected" if projected else "")
    return (
        f'          <div class="row" data-status="{status}" data-area="{area}">\n'
        f'            <div class="row-label">\n'
        f'              <span class="row-num">{esc(row["num"])}</span>\n'
        f'              <span class="row-name">{esc(row["name"])}</span>\n'
        f'              <span class="row-pri">{esc(row["priority"])}</span>\n'
        f'              {schema_badge}\n'
        f'            </div>\n'
        f'            <div class="row-track">\n'
        f'              <div class="{bar_cls}" style="left:{left:.2f}%;width:{width:.2f}%" title="{title}"></div>\n'
        f'              {label_html}\n'
        f'            </div>\n'
        f'          </div>'
    )


def table_row(row, status_label, when):
    status_cls = "shipped" if status_label == "Shipped" else "planned"
    area_label = AREA_META[row["area"]]
    schema_disp = "Yes" if row.get("schema") else "No"
    return (
        f'          <tr><td class="num">{esc(row["num"])}</td>'
        f'<td class="name"><span class="dot" style="background:var(--{row["area"]})"></span>{esc(row["name"])}</td>'
        f'<td>{area_label}</td><td>{esc(row["priority"])}</td><td>{schema_disp}</td>'
        f'<td>{esc(row["effort"])}</td><td><span class="status-chip status-{status_cls}">{status_label}</span></td>'
        f'<td>{when}</td></tr>'
    )


def main():
    if len(sys.argv) != 3:
        sys.exit("usage: gen_gantt.py <data.json> <output.html>")

    with open(sys.argv[1], encoding="utf-8") as f:
        data = json.load(f)

    shipped = data["shipped"]
    planned = data["planned"]
    today = parse_date(data["today"])

    if not shipped:
        sys.exit("no shipped features supplied — need at least one to anchor the timeline")

    for label, rows in (("shipped", shipped), ("planned", planned)):
        for row in rows:
            if row["area"] not in AREA_META:
                sys.exit(
                    f"unknown area {row['area']!r} on {label} #{row['num']} ({row['name']}) — "
                    "add it to AREA_META in this script AND validate its color against both "
                    "chart surfaces with the dataviz skill's scripts/validate_palette.js before "
                    "generating; do not invent an unvalidated hex here."
                )
            if row["effort"] not in ("S", "M", "L"):
                sys.exit(f"invalid effort {row['effort']!r} on {label} #{row['num']} — must be S, M, or L")

    DAY0 = min(parse_date(r["date"]) for r in shipped)

    def day_index(d):
        return (d - DAY0).days

    today_index = day_index(today)

    # ---- shipped bar geometry ----
    shipped_geo = []
    for r in shipped:
        d = parse_date(r["date"])
        end = day_index(d) + 1
        dur = SHIPPED_DUR[r["effort"]]
        shipped_geo.append({**r, "start": end - dur, "end": end, "date_obj": d})

    last_shipped_date = max(g["date_obj"] for g in shipped_geo)
    last_shipped_index = max(g["end"] for g in shipped_geo)

    # ---- planned bar geometry: cumulative from the day after today ----
    planned_geo = []
    cursor = today_index + 1
    for r in planned:
        dur = PLANNED_DUR[r["effort"]]
        start, end = cursor, cursor + dur
        cursor = end
        planned_geo.append({**r, "start": start, "end": end})

    TOTAL_DAYS = max(last_shipped_index, cursor, today_index + 1)

    # ---- gap bands: explicit gap_notes always render; a >=3-day shipless run
    # not already covered by a note is auto-detected as a safety net so a real
    # gap can never silently go unlabeled just because Claude didn't notice it ----
    shipped_dates = {parse_date(r["date"]) for r in shipped}
    gap_notes = data.get("gap_notes", [])

    gap_bands = []
    noted_ranges = []
    for note in gap_notes:
        ns, ne = parse_date(note["start"]), parse_date(note["end"])
        noted_ranges.append((ns, ne))
        left = day_index(ns) / TOTAL_DAYS * 100
        width = ((ne - ns).days + 1) / TOTAL_DAYS * 100
        gap_bands.append((left, width, note["label"]))

    def already_noted(d):
        return any(ns <= d <= ne for ns, ne in noted_ranges)

    run_start = None
    cur = DAY0
    while cur <= last_shipped_date:
        if cur not in shipped_dates and not already_noted(cur):
            if run_start is None:
                run_start = cur
        else:
            if run_start is not None and (cur - run_start).days >= 3:
                gend = cur - datetime.timedelta(days=1)
                n = (gend - run_start).days + 1
                label = "no features shipped this week" if n >= 5 else f"no features shipped for {n} days"
                left = day_index(run_start) / TOTAL_DAYS * 100
                width = n / TOTAL_DAYS * 100
                gap_bands.append((left, width, label))
            run_start = None
        cur += datetime.timedelta(days=1)
    if run_start is not None and (last_shipped_date - run_start).days + 1 >= 3:
        # a trailing shipless run ending exactly at last_shipped_date can't occur
        # (that date is itself shipped), so this only fires for pathological input
        pass
    gap_bands.sort(key=lambda b: b[0])

    # ---- month boundaries + axis month label ----
    end_date = DAY0 + datetime.timedelta(days=int(TOTAL_DAYS))
    month_lines = []
    y, m = DAY0.year, DAY0.month
    while True:
        y2, m2 = (y + 1, 1) if m == 12 else (y, m + 1)
        boundary = datetime.date(y2, m2, 1)
        if boundary > end_date:
            break
        idx = day_index(boundary)
        if 0 < idx < TOTAL_DAYS:
            month_lines.append((idx / TOTAL_DAYS * 100, calendar.month_name[m2]))
        y, m = y2, m2

    months_spanned = []
    seen = set()
    cur = DAY0
    while cur <= end_date:
        key = (cur.year, cur.month)
        if key not in seen:
            seen.add(key)
            months_spanned.append(key)
        cur += datetime.timedelta(days=1)
    if len(months_spanned) == 1:
        y0, m0 = months_spanned[0]
        axis_month_label = f"{calendar.month_name[m0]} {y0}"
    else:
        y0, m0 = months_spanned[0]
        y1, m1 = months_spanned[-1]
        axis_month_label = (f"{calendar.month_abbr[m0]}–{calendar.month_abbr[m1]} {y0}"
                             if y0 == y1 else
                             f"{calendar.month_abbr[m0]} {y0}–{calendar.month_abbr[m1]} {y1}")

    # ---- axis ticks ----
    axis_ticks = []
    for d in range(int(TOTAL_DAYS)):
        date_d = DAY0 + datetime.timedelta(days=d)
        left = (d + 0.5) / TOTAL_DAYS * 100
        classes = []
        if d == 0:
            classes.append("is-first")
        if d == int(TOTAL_DAYS) - 1:
            classes.append("is-last")
        if d == today_index:
            classes.append("is-today")
        cls_attr = (" " + " ".join(classes)) if classes else ""
        axis_ticks.append(f'          <span class="axis-tick{cls_attr}" style="left:{left:.3f}%">{date_d.day}</span>')

    today_left = (today_index + 1) / TOTAL_DAYS * 100

    # ---- rows + table ----
    row_html, table_rows = [], []
    for g in shipped_geo:
        left, width = g["start"] / TOTAL_DAYS * 100, (g["end"] - g["start"]) / TOTAL_DAYS * 100
        dow = g["date_obj"].strftime("%a")
        date_str = fmt_short(g["date_obj"])
        show_label = g["effort"] in ("M", "L")
        label_html = label_span(date_str, left, width) if show_label else ""
        title = f'{esc(g["name"])} — {esc(g["desc"])} Shipped {date_str}, {dow}. Effort: {g["effort"]}.'
        row_html.append(row_block(g, left, width, title, label_html, "shipped", projected=False))
        table_rows.append(table_row(g, "Shipped", f"{date_str} ({dow})"))

    for g in planned_geo:
        left, width = g["start"] / TOTAL_DAYS * 100, (g["end"] - g["start"]) / TOTAL_DAYS * 100
        start_date = DAY0 + datetime.timedelta(days=int(g["start"]))
        end_date_ = DAY0 + datetime.timedelta(days=int(g["end"]) - 1)
        range_str = fmt_short(start_date) if start_date == end_date_ else f"{fmt_short(start_date)}–{fmt_short(end_date_)}"
        short_label = fmt_short(end_date_)
        title = (f'{esc(g["name"])} — {esc(g["desc"])} Projected {range_str}. Effort: {g["effort"]}. '
                  'Not yet started — order and dates are a priority/dependency-based projection, not a commitment.')
        label_html = label_span(short_label, left, width, extra_cls="is-projected-label")
        row_html.append(row_block(g, left, width, title, label_html, "planned", projected=True))
        table_rows.append(table_row(g, "Planned", range_str))

    # ---- stats (fully mechanical) ----
    shipped_count = len(shipped)
    span_days = (last_shipped_date - DAY0).days + 1
    date_counts = Counter(r["date"] for r in shipped)
    busiest_date_str, busiest_count = date_counts.most_common(1)[0]
    busiest_date = parse_date(busiest_date_str)
    schema_touched = sum(1 for r in shipped if r.get("schema"))

    planned_count = len(planned)
    projected_effort_days = sum(PLANNED_DUR[r["effort"]] for r in planned)
    projected_wrap_date = DAY0 + datetime.timedelta(days=int(cursor) - 1)
    still_p1 = [r["name"] for r in planned if r["priority"] == "P1"]

    def s(n):
        return "" if n == 1 else "s"

    stat_html = f"""
  <div class="stats stats-row">
    <div class="stat">
      <div class="stat-value">{shipped_count}</div>
      <div class="stat-label">Features shipped</div>
      <div class="stat-sub">docs/backlog/features-implemented.md</div>
    </div>
    <div class="stat">
      <div class="stat-value">{span_days} day{s(span_days)}</div>
      <div class="stat-label">Delivery span</div>
      <div class="stat-sub">{fmt_short(DAY0)} – {fmt_short(last_shipped_date)}, {DAY0.year}</div>
    </div>
    <div class="stat">
      <div class="stat-value">{fmt_short(busiest_date)}</div>
      <div class="stat-label">Busiest day</div>
      <div class="stat-sub">{busiest_count} feature{s(busiest_count)} landed at once</div>
    </div>
    <div class="stat">
      <div class="stat-value">{schema_touched} / {shipped_count}</div>
      <div class="stat-label">Touched the schema</div>
      <div class="stat-sub">new tables, columns, or RLS policies</div>
    </div>
  </div>

  <div class="stats stats-row is-projected">
    <div class="stat">
      <div class="stat-value">{planned_count}</div>
      <div class="stat-label">Planned next</div>
      <div class="stat-sub">docs/backlog/features-planned.md</div>
    </div>
    <div class="stat">
      <div class="stat-value">{projected_effort_days:g} days</div>
      <div class="stat-label">Projected effort</div>
      <div class="stat-sub">at S / M / L = 1 / 2 / 3 days</div>
    </div>
    <div class="stat">
      <div class="stat-value">~{fmt_short(projected_wrap_date)}</div>
      <div class="stat-label">Projected wrap</div>
      <div class="stat-sub">if paced like the last {span_days} days</div>
    </div>
    <div class="stat">
      <div class="stat-value">{len(still_p1)} / {planned_count}</div>
      <div class="stat-label">Still P1</div>
      <div class="stat-sub">{", ".join(still_p1[:3]) if still_p1 else "none"}</div>
    </div>
  </div>"""

    # ---- legend (only areas actually used) ----
    used_areas = [a for a in ("backend", "frontend", "fullstack", "infra")
                  if any(r["area"] == a for r in shipped + planned)]
    legend_items = "\n      ".join(
        f'<div class="legend-item"><span class="legend-swatch" style="background:var(--{a})"></span>{AREA_META[a]}</div>'
        for a in used_areas
    )

    # ---- gap bands + markers ----
    gap_band_html = "\n          ".join(
        f'<div class="gap-band" style="left:{left:.3f}%;width:{width:.3f}%"><span>{esc(label)}</span></div>'
        for left, width, label in gap_bands
    ) or ""
    month_line_html = "\n          ".join(
        f'<div class="month-line" style="left:{left:.3f}%"></div>' for left, _ in month_lines
    )
    month_annotation_html = "\n            ".join(
        f'<span class="axis-annotation month" style="left:{left:.3f}%">{name.upper()}</span>'
        for left, name in month_lines
    )

    # ---- insights + table ----
    insight_html = "\n    ".join(
        f'<div class="insight"><div class="insight-title">{esc(i["title"])}</div>'
        f'<div class="insight-body">{i["body"]}</div></div>'
        for i in data["insights"]
    )

    day_pct = 100.0 / TOTAL_DAYS
    # ~24px/day keeps day columns legible; below that a day tick and its
    # gridline neighbor start to collide. label_w must match --label-w above.
    label_w = 336
    min_width = max(900, label_w + 24 * int(TOTAL_DAYS))
    css = CSS.replace("__DAY_PCT__", f"{day_pct:.4f}").replace("__MIN_WIDTH__", str(min_width))

    footnote_extra = data.get("footnote_extra", "")
    footnote_extra_html = f" {footnote_extra}" if footnote_extra else ""

    html_out = f"""<title>{esc(data.get("page_title", "Feature Timeline"))}</title>
<style>{css}</style>

<div class="page">
  <p class="eyebrow">{data["eyebrow"]}</p>
  <h1>{data["title"]}</h1>
  <p class="lede">
    {data["lede"]}
  </p>
{stat_html}

  <div class="legend">
    <div class="legend-group">
      {legend_items}
    </div>
    <div class="legend-sep"></div>
    <div class="legend-group">
      <div class="legend-item"><span class="legend-swatch" style="background:var(--{used_areas[0]})"></span>Shipped</div>
      <div class="legend-item"><span class="legend-swatch is-projected"></span>Projected</div>
    </div>
    <div class="legend-sep"></div>
    <div class="legend-group">
      <div class="legend-item"><span class="legend-note-schema">DB</span>schema changed</div>
      <div class="legend-item">P0&ndash;P3 &nbsp;priority</div>
      <div class="legend-item">bar length &asymp; relative effort (S/M/L)</div>
    </div>
  </div>

  <div class="gantt-card">
    <div class="gantt-scroll">
      <div class="gantt">
        <div class="gantt-axis">
          <div class="axis-spacer"><span class="axis-month">{axis_month_label}</span></div>
          <div class="axis-track" id="axis-track">
            <span class="axis-annotation today" style="left:{today_left:.3f}%">today</span>
            {month_annotation_html}
{chr(10).join(axis_ticks)}
          </div>
        </div>

        <div class="gap-overlay">
          {gap_band_html}
        </div>

        <div class="marker-overlay">
          {month_line_html}
          <div class="today-line" style="left:{today_left:.3f}%"></div>
        </div>

        <div class="gantt-body">
{chr(10).join(row_html)}
        </div>
      </div>
    </div>
  </div>

  <div class="insights">
    {insight_html}
  </div>

  <details class="table-view">
    <summary>Table view &mdash; all {shipped_count + planned_count} rows</summary>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>#</th><th>Feature</th><th>Area</th><th>Priority</th><th>Schema</th><th>Effort</th><th>Status</th><th>When</th>
          </tr>
        </thead>
        <tbody>
{chr(10).join(table_rows)}
        </tbody>
      </table>
    </div>
  </details>

  <p class="footnote">
    <strong>Shipped bars:</strong> dates are the archive date of each change's <code>openspec/changes/archive/YYYY-MM-DD-&lt;name&gt;/</code>
    folder, cross-checked against merge-commit dates in <code>git log</code>. Every shipped change in this project's history has
    landed within the single day it was branched, so bar <em>length</em> there doesn't encode a measured multi-day duration
    &mdash; it's a relative stand-in for effort (S/M/L) so denser changes read visually heavier; the exact ship day is always
    the bar's right edge.{footnote_extra_html}
  </p>
  <p class="footnote">
    <strong>Projected bars:</strong> the {planned_count} rows in <code>docs/backlog/features-planned.md</code>, in the order
    listed under that file's own <code>### Recommended implementation order</code> section. Duration is a literal
    S/M/L&nbsp;=&nbsp;1/2/3 workday estimate, stacked back-to-back with no parallelism and no days off, starting the day
    after today ({fmt_short(today)}). Treat the hatched bars as relative sequencing and scale, not a delivery date.
  </p>
</div>
"""

    with open(sys.argv[2], "w", encoding="utf-8") as f:
        f.write(html_out)

    print(f"wrote {sys.argv[2]} — {shipped_count} shipped, {planned_count} planned, "
          f"{len(gap_bands)} gap band(s), span day0={DAY0.isoformat()} total_days={TOTAL_DAYS:g}")


if __name__ == "__main__":
    main()
