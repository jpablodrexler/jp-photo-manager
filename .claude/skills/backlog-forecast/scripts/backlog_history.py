# VENDORED from lib/backlog_history.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
What the backlog says about its own history: when each feature and bug was delivered, how many story points
each feature was worth, and the weekly series built from them. Shared by backlog-report and backlog-forecast.

Dates of a feature come, in this order, from:
  * the `archived YYYY-MM-DD` date in the Details cell of its row, which features-archive writes: no OpenSpec and no git
    history needed;
  * the archive folder of the SDD change its row links (`[spec](…/openspec/changes/archive/YYYY-MM-DD-name/…)`),
    which is written in the file and does not depend on how the commits look;
  * the day of the first commit that added the row to features-implemented.md (one `git log -p` over the file);
  * today, for a row that is not committed yet and links no spec.
Bugs use the `Fixed` column of bugs-fixed.md. Weeks start on Monday.
"""
import re
from datetime import date, timedelta
from pathlib import Path

import backlog_kit as bk
import gitread

LEGACY_POINTS = {"S": 1, "M": 3, "L": 5, "XL": 8}     # t-shirt sizes of a backlog not migrated yet
DATE_RE = re.compile(r"\d{4}-\d{2}-\d{2}")
SDD_RE = re.compile(r"openspec/changes/archive/(\d{4}-\d{2}-\d{2})-")   # the archive folder of a change is named YYYY-MM-DD-<name>
ARCHIVED_RE = re.compile(r"\barchived (\d{4}-\d{2}-\d{2})\b", re.I)         # `archived YYYY-MM-DD` in the Details cell, written by features-archive
BULK_MIN = 5                                           # features on one date that make a bulk load worth a warning
BULK_SHARE = 0.5                                       # ...when that date holds at least this share of the period's features
UNESTIMATED_SHARE = 0.25                               # delivered features without points that make the points unreliable


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


def archived_date(details):
    """The date a row says it was archived on (`archived YYYY-MM-DD` in its Details cell, written by features-archive), or None.
    It needs no OpenSpec and no git history, so it is the most reliable date a feature can have."""
    found = []
    for m in ARCHIVED_RE.finditer(details or ""):
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


def load_history(root, today):
    """Read the backlog files and date everything that was delivered.

    Returns the folder, which of the three files had their table, the pending rows of features-planned.md and `items`:
    one dict per delivered feature or fixed bug (`kind`, `id`, `name`, `points`, `date`, `source`, `uncommitted`).
    A feature's `source` is "archived", "sdd", "git" or "uncommitted"; a bug's is "fixed column"."""
    cfg = bk.load_config(root)
    backlog = cfg["paths"]["backlog_dir"].strip("/")
    b = Path(root) / backlog
    impl_ok, impl = bk.table_rows(b / "features-implemented.md", "## Feature List")
    plan_ok, plan = bk.table_rows(b / "features-planned.md", "## Feature List")
    bugs_ok, fixed = bk.table_rows(b / "bugs-fixed.md", "## Bug List", id_column="Bug ID")
    explicit = {}                                       # dates the row itself carries: its `archived` date, else its SDD change's
    for r in impl:
        num = bk.strip_backticks(r.get("#", ""))
        if not num.isdigit():
            continue
        if archived_date(r.get("Details")):
            explicit[int(num)] = (archived_date(r.get("Details")), "archived")
        elif sdd_date(r.get("Details")):
            explicit[int(num)] = (sdd_date(r.get("Details")), "sdd")
    needs_git = any(bk.strip_backticks(r.get("#", "")).isdigit() and int(bk.strip_backticks(r.get("#", ""))) not in explicit for r in impl)
    dates = feature_dates(root, f"{backlog}/features-implemented.md") if impl_ok and needs_git else {}

    items = []
    for r in impl:
        num = bk.strip_backticks(r.get("#", ""))
        if not num.isdigit():
            continue
        if int(num) in explicit:                        # explicit: written in the row, whatever the commits look like
            d, source = explicit[int(num)]
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
    pending = [r for r in plan if "Implemented" not in r.get("Implementation", "")]
    return {"backlog": backlog, "impl_ok": impl_ok, "plan_ok": plan_ok, "bugs_ok": bugs_ok, "plan": plan, "pending": pending,
            "items": items}


def weekly_series(items, start, today):
    """One dict per week from `start` to `today` (weeks start on Monday): `week` (its Monday), `complete`
    (it has ended), and the `features`, `points` and `bugs` delivered in it."""
    in_range = [i for i in items if start <= i["date"] <= today]
    weeks = []
    for w in weeks_between(start, today):
        mine = [i for i in in_range if monday(i["date"]) == w]
        feats = [i for i in mine if i["kind"] == "feature"]
        weeks.append({"week": w.isoformat(), "complete": w + timedelta(days=6) < today,
                      "features": len(feats), "points": sum(i["points"] or 0 for i in feats),
                      "bugs": sum(i["kind"] == "bug" for i in mine)})
    return weeks
