#!/usr/bin/env python3
"""
What the backlog delivered over a period: features archived and bugs fixed per week,
story points per week (velocity), and how long the pending points would take at that pace.

    python3 backlog_report.py <repo-root> [--weeks N | --since YYYY-MM-DD] [--today YYYY-MM-DD] [--json]

Read-only. Dates come from two places:
  * bugs: the `Fixed` column of `bugs-fixed.md` (written by bugs-archive);
  * features: `features-implemented.md` has no date column, so a feature's date is the day of the
    first commit that added its row (one `git log -p` over the file). A row that is not committed
    yet counts as today and is marked "uncommitted". Without git, features are reported as undated.
Weeks start on Monday. Exit code is always 0 unless the repo root is bad.
"""
import argparse
import json
import re
import sys
from datetime import date, timedelta
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_kit as bk
import gitread

LEGACY_POINTS = {"S": 1, "M": 3, "L": 5, "XL": 8}     # t-shirt sizes of a backlog not migrated yet
DATE_RE = re.compile(r"\d{4}-\d{2}-\d{2}")
AVERAGE_OVER = 4                                       # complete weeks the velocity is averaged over


def points(effort):
    e = (effort or "").strip().strip("`")
    if e.isdigit():
        return int(e)
    return LEGACY_POINTS.get(e.upper())


def monday(d):
    return d - timedelta(days=d.weekday())


def feature_dates(root, rel_path):
    """{feature number: date it was first committed into features-implemented.md}."""
    try:
        out = gitread.git(root, "log", "--reverse", "--format=@@%aI", "-p", "-U0", "--", rel_path, check=False)
    except gitread.GitError:
        return {}
    dates, current = {}, None
    for line in out.splitlines():
        if line.startswith("@@") and DATE_RE.match(line[2:12]):
            current = date.fromisoformat(line[2:12])
        elif line.startswith("+|") and current:
            cells = bk.split_row(line[1:])
            if cells and cells[0].isdigit():
                dates.setdefault(int(cells[0]), current)
    return dates


def weeks_between(start, end):
    out, w = [], monday(start)
    while w <= end:
        out.append(w)
        w += timedelta(days=7)
    return out


def build(root, start, today):
    cfg = bk.load_config(root)
    backlog = cfg["paths"]["backlog_dir"].strip("/")
    b = Path(root) / backlog

    impl_ok, impl = bk.table_rows(b / "features-implemented.md", "## Feature List")
    plan_ok, plan = bk.table_rows(b / "features-planned.md", "## Feature List")
    bugs_ok, fixed = bk.table_rows(b / "bugs-fixed.md", "## Bug List", id_column="Bug ID")
    dates = feature_dates(root, f"{backlog}/features-implemented.md") if impl_ok else {}

    items = []                                          # one dict per delivered feature or bug
    for r in impl:
        num = bk.strip_backticks(r.get("#", ""))
        if not num.isdigit():
            continue
        d = dates.get(int(num))
        items.append({"kind": "feature", "id": f"#{num}", "name": bk.strip_backticks(r.get("Change name", "")),
                      "points": points(r.get("Effort")), "date": d or today, "uncommitted": d is None})
    for r in fixed:
        m = DATE_RE.search(r.get("Fixed", ""))
        if m and r.get("Status", "").strip() in ("", "✅ Fixed", "Fixed"):
            items.append({"kind": "bug", "id": r.get("Bug ID", "").strip(), "name": "", "severity": r.get("Severity", "").strip(),
                          "points": None, "date": date.fromisoformat(m.group(0)), "uncommitted": False})

    in_range = [i for i in items if start <= i["date"] <= today]
    weeks = []
    for w in weeks_between(start, today):
        mine = [i for i in in_range if monday(i["date"]) == w]
        feats = [i for i in mine if i["kind"] == "feature"]
        weeks.append({"week": w.isoformat(), "complete": w + timedelta(days=6) < today,
                      "features": len(feats), "points": sum(i["points"] or 0 for i in feats),
                      "bugs": sum(i["kind"] == "bug" for i in mine)})

    complete = [w for w in weeks if w["complete"]][-AVERAGE_OVER:]
    velocity = None
    if complete and any(w["features"] for w in weeks):          # no delivered feature, nothing to measure a pace from
        velocity = round(sum(w["points"] for w in complete) / len(complete), 1)

    pending_rows = [r for r in plan if "Implemented" not in r.get("Implementation", "")]
    pending_points = sum(points(r.get("Effort")) or 0 for r in pending_rows)
    weeks_left = round(pending_points / velocity, 1) if velocity and plan_ok else None

    severity = {}
    for i in in_range:
        if i["kind"] == "bug":
            severity[i["severity"] or "none"] = severity.get(i["severity"] or "none", 0) + 1

    return {
        "period": {"start": start.isoformat(), "end": today.isoformat()},
        "available": {"features": impl_ok, "pending": plan_ok, "bugs": bugs_ok},
        "weeks": weeks,
        "features_delivered": sum(i["kind"] == "feature" for i in in_range),
        "points_delivered": sum(i["points"] or 0 for i in in_range if i["kind"] == "feature"),
        "unestimated": [i["id"] for i in in_range if i["kind"] == "feature" and i["points"] is None],
        "bugs_fixed": sum(i["kind"] == "bug" for i in in_range),
        "bugs_by_severity": dict(sorted(severity.items())),
        "velocity": {"points_per_week": velocity, "over_weeks": len(complete)},
        "pending": {"features": len(pending_rows), "points": pending_points, "weeks_at_that_pace": weeks_left},
        "delivered": [{**i, "date": i["date"].isoformat()} for i in sorted(in_range, key=lambda i: (i["date"], i["id"]))],
    }


def render(r):
    p, av = r["period"], r["available"]
    out = [f"## Backlog Report ({p['start']} → {p['end']})", ""]
    missing = [name for name, ok in (("features-implemented.md", av["features"]), ("features-planned.md", av["pending"]),
                                     ("bugs-fixed.md", av["bugs"])) if not ok]
    if missing:
        out += [f"Unavailable (missing or without its table): {', '.join(missing)}", ""]
    out += ["| Week (Mon) | Features | Points | Bugs fixed |", "| --- | --- | --- | --- |"]
    for w in r["weeks"]:
        out.append(f"| {w['week']}{'' if w['complete'] else ' (in progress)'} | {w['features']} | {w['points']} | {w['bugs']} |")
    out += ["", f"**Delivered:** {r['features_delivered']} feature(s), {r['points_delivered']} point(s), {r['bugs_fixed']} bug(s) fixed"
            + (f" ({', '.join(f'{k}: {v}' for k, v in r['bugs_by_severity'].items())})" if r["bugs_by_severity"] else "")]
    v = r["velocity"]
    if v["points_per_week"] is None:
        out.append("**Velocity:** none: no complete week, or no feature delivered, in the period")
    else:
        out.append(f"**Velocity:** {v['points_per_week']} points/week (average of the last {v['over_weeks']} complete week(s))")
    pend = r["pending"]
    if av["pending"]:
        line = f"**Pending:** {pend['features']} feature(s), {pend['points']} point(s)"
        if pend["weeks_at_that_pace"] is not None:
            line += f" — about {pend['weeks_at_that_pace']} week(s) at that pace"
        elif pend["points"]:
            line += " — no projection without a velocity"
        out.append(line)
    if r["unestimated"]:
        out.append(f"**Without points (not counted):** {', '.join(r['unestimated'])}")
    if any(i.get("uncommitted") for i in r["delivered"]):
        out.append("**Uncommitted:** features archived but not committed yet count as today.")
    if r["delivered"]:
        out += ["", "### Delivered in the period"]
        for i in r["delivered"]:
            extra = f", {i['points']} pt" if i["kind"] == "feature" and i["points"] else (f", {i['severity']}" if i["kind"] == "bug" and i.get("severity") else "")
            out.append(f"- {i['date']} {i['id']}" + (f" `{i['name']}`" if i["name"] else "") + extra)
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--weeks", type=int, default=8, help="how many weeks back to report, the current one included (default 8)")
    ap.add_argument("--since", help="start of the period, YYYY-MM-DD (replaces --weeks)")
    ap.add_argument("--today", help="treat this date as today, YYYY-MM-DD (for reproducible output)")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = Path(args.repo_root)
    if not root.is_dir():
        sys.exit(f"{root} is not a directory")
    today = date.fromisoformat(args.today) if args.today else date.today()
    start = date.fromisoformat(args.since) if args.since else monday(today) - timedelta(days=7 * (max(args.weeks, 1) - 1))
    result = build(root, start, today)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
