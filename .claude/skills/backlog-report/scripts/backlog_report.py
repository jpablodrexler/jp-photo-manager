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
SDD_RE = re.compile(r"openspec/changes/archive/(\d{4}-\d{2}-\d{2})-")   # the archive folder of a change is named YYYY-MM-DD-<name>
AVERAGE_OVER = 4                                       # complete weeks the velocity is averaged over
BULK_MIN = 5                                           # features on one date that make a bulk load worth a warning
BULK_SHARE = 0.5                                       # ...when that date holds at least this share of the period's features
UNESTIMATED_SHARE = 0.25                               # delivered features without points that make the points unreliable
UNESTIMATED_LISTED = 10                                # ids shown in the "without points" line


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


def sdd_date(details):
    """The date in the archive folder of the SDD change a row links to (`[spec](…/openspec/changes/archive/YYYY-MM-DD-name/…)`),
    or None. features-archive writes that link; with several, the latest date, as it does."""
    found = []
    for m in SDD_RE.finditer(details or ""):
        try:
            found.append(date.fromisoformat(m.group(1)))
        except ValueError:
            pass
    return max(found) if found else None


def weeks_between(start, end):
    out, w = [], monday(start)
    while w <= end:
        out.append(w)
        w += timedelta(days=7)
    return out


def history_is_cut(root):
    """True when the history reachable from HEAD stops at a shallow boundary, so older rows would get a wrong date.
    A clone can be shallow because of another branch while the history of HEAD is complete: that is not a problem."""
    try:
        if gitread.git(root, "rev-parse", "--is-shallow-repository", check=False).strip() != "true":
            return False
        path = Path(gitread.git(root, "rev-parse", "--git-path", "shallow", check=False).strip())
        boundary = set((path if path.is_absolute() else Path(root) / path).read_text(encoding="utf-8").split())
        return bool(boundary & set(gitread.git(root, "rev-list", "HEAD", check=False).split()))
    except (gitread.GitError, OSError):
        return False


def reliability(root, impl_ok, in_range, uses_git, pending_unestimated, pending_points):
    """Reasons not to trust the weekly figures as they are: each is {code, message}."""
    out = []
    feats = [i for i in in_range if i["kind"] == "feature"]
    committed = [i for i in feats if i["source"] == "git"]        # only these dates are inferred from commits
    if len(committed) >= BULK_MIN:
        per_day = {}
        for i in committed:
            per_day[i["date"]] = per_day.get(i["date"], 0) + 1
        day, n = max(per_day.items(), key=lambda kv: (kv[1], kv[0]))        # the busiest date; the later one on a tie
        if n >= BULK_MIN and n / len(committed) >= BULK_SHARE:
            out.append({"code": "bulk_date", "message":
                        f"{n} of {len(committed)} features dated from git carry the date {day}: their rows were committed together (a backlog "
                        "loaded in one go, or one batch archive), so the weekly figures around that date say nothing about the pace."})
    unestimated = [i for i in feats if i["points"] is None]
    if len(feats) >= 4 and len(unestimated) / len(feats) >= UNESTIMATED_SHARE:
        out.append({"code": "unestimated", "message":
                    f"{len(unestimated)} of {len(feats)} delivered features have no story points: the points delivered and the "
                    "velocity understate the work."})
    if pending_unestimated:
        out.append({"code": "pending_unestimated", "message":
                    f"{pending_unestimated} pending feature(s) have no story points and are not in the {pending_points} pending "
                    "points, so any projection is optimistic."})
    if impl_ok and uses_git and history_is_cut(root):
        out.append({"code": "shallow_clone", "message":
                    "The history of this clone is cut short (a shallow clone): older rows get the date of the oldest commit "
                    "that was fetched. git fetch --unshallow gives the real dates."})
    if any(i["uncommitted"] for i in feats):
        out.append({"code": "uncommitted", "message":
                    "Features archived but not committed yet count as today."})
    return out


def build(root, start, today):
    cfg = bk.load_config(root)
    backlog = cfg["paths"]["backlog_dir"].strip("/")
    b = Path(root) / backlog

    impl_ok, impl = bk.table_rows(b / "features-implemented.md", "## Feature List")
    plan_ok, plan = bk.table_rows(b / "features-planned.md", "## Feature List")
    bugs_ok, fixed = bk.table_rows(b / "bugs-fixed.md", "## Bug List", id_column="Bug ID")
    sdd = {}                                            # the date of the archived SDD change, when the row links one
    for r in impl:
        num = bk.strip_backticks(r.get("#", ""))
        if num.isdigit() and sdd_date(r.get("Details")):
            sdd[int(num)] = sdd_date(r.get("Details"))
    needs_git = any(bk.strip_backticks(r.get("#", "")).isdigit() and int(bk.strip_backticks(r.get("#", ""))) not in sdd for r in impl)
    dates = feature_dates(root, f"{backlog}/features-implemented.md") if impl_ok and needs_git else {}

    items = []                                          # one dict per delivered feature or bug
    for r in impl:
        num = bk.strip_backticks(r.get("#", ""))
        if not num.isdigit():
            continue
        if int(num) in sdd:                             # explicit: written in the row, whatever the commits look like
            d, source = sdd[int(num)], "sdd"
        elif dates.get(int(num)):
            d, source = dates[int(num)], "git"
        else:
            d, source = today, "uncommitted"
        items.append({"kind": "feature", "id": f"#{num}", "name": bk.strip_backticks(r.get("Change name", "")),
                      "points": points(r.get("Effort")), "date": d, "source": source, "uncommitted": source == "uncommitted"})
    for r in fixed:
        m = DATE_RE.search(r.get("Fixed", ""))
        if m and r.get("Status", "").strip() in ("", "✅ Fixed", "Fixed"):
            items.append({"kind": "bug", "id": r.get("Bug ID", "").strip(), "name": "", "severity": r.get("Severity", "").strip(),
                          "points": None, "date": date.fromisoformat(m.group(0)), "source": "fixed column", "uncommitted": False})

    in_range = [i for i in items if start <= i["date"] <= today]
    weeks = []
    for w in weeks_between(start, today):
        mine = [i for i in in_range if monday(i["date"]) == w]
        feats = [i for i in mine if i["kind"] == "feature"]
        weeks.append({"week": w.isoformat(), "complete": w + timedelta(days=6) < today,
                      "features": len(feats), "points": sum(i["points"] or 0 for i in feats),
                      "bugs": sum(i["kind"] == "bug" for i in mine)})

    complete = [w for w in weeks if w["complete"]][-AVERAGE_OVER:]
    weekly_points = [w["points"] for w in complete]
    active_weeks = sum(1 for w in complete if w["features"])
    velocity = None
    if complete and active_weeks:                       # a window without a single delivery measures nothing: say so, not "0.0"
        velocity = round(sum(weekly_points) / len(complete), 1)
    bugs_per_week = round(sum(w["bugs"] for w in complete) / len(complete), 1) if complete else None
    last_delivery = max((i["date"] for i in items if i["kind"] == "feature"), default=None)

    pending_rows = [r for r in plan if "Implemented" not in r.get("Implementation", "")]
    pending_points = sum(points(r.get("Effort")) or 0 for r in pending_rows)
    pending_unestimated = sum(1 for r in pending_rows if points(r.get("Effort")) is None)
    weeks_left = round(pending_points / velocity, 1) if velocity and plan_ok else None

    severity = {}
    for i in in_range:
        if i["kind"] == "bug":
            severity[i["severity"] or "none"] = severity.get(i["severity"] or "none", 0) + 1

    feature_items = [i for i in items if i["kind"] == "feature"]
    sources = {k: sum(1 for i in feature_items if i["source"] == k) for k in ("sdd", "git", "uncommitted")}
    warnings = reliability(root, impl_ok, in_range, sources["git"] > 0, pending_unestimated, pending_points)

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
        parts = [f"{d['sdd']} from the archived SDD change" if d["sdd"] else "",
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
    start = date.fromisoformat(args.since) if args.since else monday(today) - timedelta(days=7 * (max(args.weeks, 1) - 1))
    result = build(root, start, today)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
