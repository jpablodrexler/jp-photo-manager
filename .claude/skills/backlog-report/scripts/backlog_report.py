#!/usr/bin/env python3
"""
What the backlog delivered over a period: features archived and bugs fixed per week,
story points per week (velocity), and how long the pending points would take at that pace.

    python3 backlog_report.py <repo-root> [--weeks N | --since YYYY-MM-DD] [--today YYYY-MM-DD] [--json]

Read-only. Dates come from two places:
  * bugs: the `Fixed` column of `bugs-fixed.md` (written by bugs-archive);
  * features: `features-implemented.md` has no date column. A feature's date is the `archived YYYY-MM-DD`
    in the Details cell of its row (written by features-archive), else the date of the archived SDD change
    its row links, else the day of the first commit that added the row (one `git log -p` over the file).
    A row that is not committed yet counts as today and is marked "uncommitted".
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
import backlog_history as bh

AVERAGE_OVER = 4                                       # complete weeks the velocity is averaged over
UNESTIMATED_LISTED = 10                                # ids shown in the "without points" line


def build(root, start, today):
    h = bh.load_history(root, today)
    impl_ok, plan_ok, bugs_ok, items = h["impl_ok"], h["plan_ok"], h["bugs_ok"], h["items"]
    in_range = [i for i in items if start <= i["date"] <= today]
    weeks = bh.weekly_series(items, start, today)

    complete = [w for w in weeks if w["complete"]][-AVERAGE_OVER:]
    weekly_points = [w["points"] for w in complete]
    active_weeks = sum(1 for w in complete if w["features"])
    velocity = None
    if complete and active_weeks:                       # a window without a single delivery measures nothing: say so, not "0.0"
        velocity = round(sum(weekly_points) / len(complete), 1)
    bugs_per_week = round(sum(w["bugs"] for w in complete) / len(complete), 1) if complete else None
    last_delivery = max((i["date"] for i in items if i["kind"] == "feature"), default=None)

    pending_rows = h["pending"]
    pending_points = sum(bh.points(r.get("Effort")) or 0 for r in pending_rows)
    pending_unestimated = sum(1 for r in pending_rows if bh.points(r.get("Effort")) is None)
    weeks_left = round(pending_points / velocity, 1) if velocity and plan_ok else None

    severity = {}
    for i in in_range:
        if i["kind"] == "bug":
            severity[i["severity"] or "none"] = severity.get(i["severity"] or "none", 0) + 1

    feature_items = [i for i in items if i["kind"] == "feature"]
    sources = {k: sum(1 for i in feature_items if i["source"] == k) for k in ("archived", "sdd", "git", "uncommitted")}
    warnings = bh.reliability(root, impl_ok, in_range, sources["git"] > 0, pending_unestimated, pending_points)

    return {
        "period": {"start": start.isoformat(), "end": today.isoformat()},
        "available": {"features": impl_ok, "pending": plan_ok, "bugs": bugs_ok},
        "weeks": weeks,
        "features_delivered": sum(i["kind"] == "feature" for i in in_range),
        "points_delivered": sum(i["points"] or 0 for i in in_range if i["kind"] == "feature"),
        "unestimated": [i["id"] for i in in_range if i["kind"] == "feature" and i["points"] is None],
        "bugs_fixed": sum(i["kind"] == "bug" for i in in_range),
        "bugs_by_severity": dict(sorted(severity.items())),
        "velocity": {"points_per_week": velocity, "over_weeks": len(complete), "weekly_points": weekly_points,
                     "weeks_with_deliveries": active_weeks, "last_delivery": last_delivery.isoformat() if last_delivery else None},
        "bugs_per_week": bugs_per_week,
        "dates": sources,
        "pending": {"features": len(pending_rows), "points": pending_points, "unestimated": pending_unestimated,
                    "weeks_at_that_pace": weeks_left},
        "warnings": warnings,
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
    d = r["dates"]
    if any(d.values()):
        parts = [f"{d['archived']} from the archived date written in the row" if d["archived"] else "",
                 f"{d['sdd']} from the archived SDD change" if d["sdd"] else "",
                 f"{d['git']} from the git history of features-implemented.md" if d["git"] else "",
                 f"{d['uncommitted']} not committed yet (counted as today)" if d["uncommitted"] else ""]
        out.append("**Feature dates:** " + ", ".join(x for x in parts if x))
    v = r["velocity"]
    if v["points_per_week"] is not None:
        series = ", ".join(str(x) for x in v["weekly_points"])
        out.append(f"**Velocity:** {v['points_per_week']} points/week, the average of the last {v['over_weeks']} complete week(s) "
                   f"({series} points; deliveries in {v['weeks_with_deliveries']} of {v['over_weeks']})")
    elif v["over_weeks"] == 0:
        out.append("**Velocity:** none yet: the period has no complete week")
    elif v["last_delivery"]:
        out.append(f"**Velocity:** none: no feature was archived in the last {v['over_weeks']} complete week(s); "
                   f"the last one was on {v['last_delivery']}")
    else:
        out.append("**Velocity:** none: no feature has been archived")
    if r["bugs_fixed"] and r["bugs_per_week"] is not None:
        out.append(f"**Bug fixes:** {r['bugs_fixed']} in the period, {r['bugs_per_week']} per week over the same weeks. "
                   "Bugs carry no points, so the velocity does not include this work.")
    pend = r["pending"]
    if av["pending"]:
        line = f"**Pending:** {pend['features']} feature(s), {pend['points']} point(s)"
        if pend["weeks_at_that_pace"] is not None:
            line += f" — about {pend['weeks_at_that_pace']} week(s) at that pace"
        elif pend["points"]:
            line += " — no projection without a velocity"
        out.append(line)
    if r["unestimated"]:
        shown = r["unestimated"][:UNESTIMATED_LISTED]
        more = len(r["unestimated"]) - len(shown)
        out.append(f"**Without points (not counted):** {', '.join(shown)}" + (f" and {more} more" if more > 0 else ""))
    if r["warnings"]:
        out += ["", "### Reliability"] + [f"- ⚠ {w['message']}" for w in r["warnings"]]
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
    start = date.fromisoformat(args.since) if args.since else bh.monday(today) - timedelta(days=7 * (max(args.weeks, 1) - 1))
    result = build(root, start, today)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
