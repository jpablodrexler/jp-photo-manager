#!/usr/bin/env python3
"""
Plan a cycle from the forecast, close it against what was really delivered, and show what the closed cycles say about the
estimates.

    python3 cycle.py plan  <repo-root> [--commit prudent|likely] [--features 12,14] [--start DATE] [--cycle-weeks N]
                           [--weeks N | --since DATE] [--capacity POINTS_PER_WEEK] [--write] [--today DATE]
    python3 cycle.py close <repo-root> [--cycle FILE] [--reason "#12=why"]... [--note "text"]... [--write] [--today DATE]
    python3 cycle.py stats <repo-root> [--json]

Without --write nothing is saved: the cycle file is only printed. With --write the script writes one file,
`<backlog>/cycles/<start>.md`, and nothing else: it never commits. A cycle commits features to a capacity: by default the
prudent set of the forecast (what 85 of 100 cycles reached). What a cycle delivered is not tracked by hand: `close` reads it
out of the archived features (the same dates as backlog-report), so it covers features done inside a batch or outside one.
Exit code is 1 when the request cannot be done (no capacity, an open cycle already, nothing to close).
"""
import argparse
import json
import re
import sys
from datetime import date, timedelta
from pathlib import Path
from types import SimpleNamespace

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_cycles as bc
import backlog_forecast as bf
import backlog_history as bh
import backlog_kit as bk


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)


def cycles_dir(root):
    return bk.Paths(root, bk.load_config(root)).cycles_dir


def feat(c):
    return f"#{c['number']} `{c['name']}` ({c['points']})"


# --- plan --------------------------------------------------------------------

def plan(root, args, today):
    start = date.fromisoformat(args.start) if args.start else today
    open_cycles = [c for c in bc.list_cycles(root) if c["status"] != "closed"]
    if open_cycles:
        fail(f"{open_cycles[0]['file']} is still open ({open_cycles[0]['start']} → {open_cycles[0]['end']}): close it first.")
    fargs = SimpleNamespace(weeks=args.weeks, since=args.since, cycle_weeks=args.cycle_weeks, cycles=1, capacity=args.capacity,
                            features=None, seed=1, no_calibration=False)
    result = bf.forecast(root, fargs, today)
    h = bh.load_history(root, today)
    if not h["plan_ok"]:
        fail("features-planned.md is missing or has no `## Feature List` table.")
    items = bf.ordered_pending(h, bk.read_lines(Path(root) / h["backlog"] / "features-planned.md") or [])
    cap = result["capacity"]
    if args.features:
        wanted = [int(x) for x in args.features.split(",") if x.strip().isdigit()]
        by_number = {c["number"]: c for c in items}
        missing = [n for n in wanted if n not in by_number or by_number[n]["points"] is None]
        if missing:
            fail("Not pending or without points: " + ", ".join(f"#{n}" for n in missing))
        chosen = [by_number[n] for n in wanted]
        commitment = "chosen by hand"
    else:
        if not cap:
            msg = next((w["message"] for w in result["warnings"] if w["code"] == "no_history"), "There is no pace to plan from.")
            fail(msg + " Or choose the features yourself with --features.")
        chosen = result["next_cycle"][args.commit]
        commitment = args.commit
    if not chosen:
        shown = f" ({commitment} capacity {cap['prudent_points' if commitment == 'prudent' else 'likely_points']:g} points)" if cap else ""
        fail(f"Nothing fits in the cycle{shown}: the first feature of the order is bigger than that capacity, or the history has "
             "weeks with nothing delivered. Try --commit likely, a longer --cycle-weeks, or choose the features with --features.")
    end = start + timedelta(days=7 * args.cycle_weeks - 1)
    forecast_line = (f"prudent {cap['prudent_points']:g} points, likely {cap['likely_points']:g} points" if cap
                     else "none (no pace to forecast from)")
    if cap and result["calibration"] and result["calibration"]["applied"]:
        forecast_line += f" (scaled by {result['calibration']['applied']} for past commitments)"
    points = sum(c["points"] for c in chosen)
    lines = [f"# Cycle {start.isoformat()}", "",
             f"**Start:** {start.isoformat()}", f"**End:** {end.isoformat()}", "**Status:** open",
             f"**Commitment:** {commitment}", f"**Forecast at planning:** {forecast_line}", "",
             "## Plan", "", "| # | Change name | Points |", "| --- | --- | --- |"]
    lines += [f"| #{c['number']} | `{c['name']}` | {c['points']} |" for c in chosen]
    lines += ["", f"**Committed:** {len(chosen)} feature(s), {points} point(s)", ""]
    path = cycles_dir(root) / f"{start.isoformat()}.md"
    return path, "\n".join(lines), {"start": start.isoformat(), "end": end.isoformat(), "committed": len(chosen), "points": points,
                                    "warnings": [w["message"] for w in result["warnings"]]}


# --- close -------------------------------------------------------------------

def batch_reasons(root, names):
    """Reasons a batch run gave for the features it could not finish: {change name: text}."""
    folder = bk.Paths(root, bk.load_config(root)).reports / "feature-batch"
    out = {}
    for report in sorted(folder.glob("FEATURE_BATCH_REPORT_*.md")) if folder.is_dir() else []:
        for line in bk.read_lines(report) or []:
            m = re.search(r"Blocked\W+(.*)$", line)
            if m and "Blocked" in line:
                for name in names:
                    if name and name in line:
                        out[name] = "batch report: " + m.group(1).strip()
    return out


def close(root, args, today):
    cycles = bc.list_cycles(root)
    if args.cycle:
        mine = [c for c in cycles if c["file"] in (args.cycle, args.cycle + ".md")]
    else:
        mine = [c for c in cycles if c["status"] != "closed"][-1:]
    if not mine:
        fail("There is no open cycle to close." if not args.cycle else f"No cycle file {args.cycle}.")
    cycle = mine[0]
    if cycle["status"] == "closed":
        fail(f"{cycle['file']} is already closed.")
    upto = min(cycle["end"], today)
    h = bh.load_history(root, today)
    delivered = [i for i in h["items"] if i["kind"] == "feature" and cycle["start"] <= i["date"] <= upto]
    committed = {c["number"]: c for c in cycle["committed"]}
    by_number = {int(i["id"][1:]): i for i in h["items"] if i["kind"] == "feature" and i["id"][1:].isdigit()}
    done = [c for n, c in committed.items() if any(int(i["id"][1:]) == n for i in delivered)]
    carried = [c for c in cycle["committed"] if c not in done]
    unplanned = [i for i in delivered if int(i["id"][1:]) not in committed]
    bugs = [i for i in h["items"] if i["kind"] == "bug" and cycle["start"] <= i["date"] <= upto]
    plan_points = sum(c["points"] or 0 for c in done)
    committed_points = sum(c["points"] or 0 for c in cycle["committed"])
    total_points = sum(i["points"] or 0 for i in delivered)
    given = dict(r.split("=", 1) for r in args.reason if "=" in r)
    given = {k.strip().lstrip("#"): v.strip() for k, v in given.items()}
    hints = batch_reasons(root, [c["name"] for c in carried])

    def reason(c):
        later = by_number.get(c["number"])
        if str(c["number"]) in given:
            return given[str(c["number"])]
        if later:
            return f"delivered after the cycle ({later['date']})"
        return hints.get(c["name"], "")

    def met(label, value):
        return "not set" if value is None else f"{value:g} points: " + ("met" if total_points >= value else "not met")

    lines = ["## Close", "", f"**Closed:** {today.isoformat()}"
             + (f" (before the cycle ended on {cycle['end']})" if today < cycle["end"] else ""),
             f"**Delivered from the plan:** {len(done)} of {len(cycle['committed'])} feature(s), {plan_points} of {committed_points} point(s)",
             f"**Delivered in total:** {len(delivered)} feature(s), {total_points} point(s), {len(bugs)} bug(s) fixed",
             f"**Forecast result:** prudent {met('prudent', cycle['prudent'])}; likely {met('likely', cycle['likely'])}", ""]
    lines += ["### Delivered", ""]
    if delivered:
        lines += ["| # | Change name | Points | Date | In the plan |", "| --- | --- | --- | --- | --- |"]
        lines += [f"| {i['id']} | `{i['name']}` | {i['points'] if i['points'] is not None else '-'} | {i['date']} | "
                  f"{'yes' if int(i['id'][1:]) in committed else 'no'} |" for i in sorted(delivered, key=lambda i: (i["date"], i["id"]))]
    else:
        lines.append("Nothing was delivered in the cycle.")
    lines += ["", "### Carried over", ""]
    if carried:
        lines += ["| # | Change name | Points | Reason |", "| --- | --- | --- | --- |"]
        lines += [f"| #{c['number']} | `{c['name']}` | {c['points'] if c['points'] is not None else '-'} | {reason(c) or 'not given'} |" for c in carried]
    else:
        lines.append("Everything committed was delivered.")
    lines += ["", "### Retro", ""]
    lines += [f"- {n}" for n in args.note] or ["- No notes."]
    lines.append("")
    summary = {"cycle": cycle["file"], "delivered_from_plan": len(done), "committed": len(cycle["committed"]),
               "carried": [c["number"] for c in carried], "unplanned": [i["id"] for i in unplanned], "bugs": len(bugs),
               "points": {"plan": plan_points, "committed": committed_points, "total": total_points},
               "without_reason": [c["number"] for c in carried if not reason(c)]}
    path = cycles_dir(root) / cycle["file"]
    return path, "\n".join(lines), summary


def write_close(path, close_text):
    text = path.read_text(encoding="utf-8")
    text = re.sub(r"^\*\*Status:\*\* open\s*$", "**Status:** closed", text, count=1, flags=re.M)
    path.write_text(text.rstrip() + "\n\n" + close_text, encoding="utf-8")


# --- stats -------------------------------------------------------------------

def render_stats(cycles, cal):
    out = ["## Cycles", ""]
    if not cycles:
        return "\n".join(out + ["No cycle yet: `plan` creates the first one."])
    out += ["| Cycle | Status | Committed | Delivered from the plan | Delivered in total | Prudent | Likely |", "| --- | --- | --- | --- | --- | --- | --- |"]
    for c in cycles:
        pts = sum(f["points"] or 0 for f in c["committed"])
        cl = c["close"]
        out.append(f"| {c['start']} → {c['end']} | {c['status']} | {len(c['committed'])} ({pts} pt) | "
                   + (f"{cl['plan_features']} ({cl['plan_points']} pt) | {cl['total_points']} pt | " if cl else "- | - | ")
                   + f"{c['prudent'] if c['prudent'] is not None else '-'} | {c['likely'] if c['likely'] is not None else '-'} |")
    out.append("")
    if not cal["closed"]:
        return "\n".join(out + ["No closed cycle yet: there is nothing to calibrate."])
    m = cal["forecast_met"]
    out.append(f"**Closed cycles:** {cal['closed']}. The forecast capacity was reached: prudent in {m['prudent']['met']} of {m['prudent']['of']}, likely in {m['likely']['met']} of {m['likely']['of']}.")
    out.append(f"**Median share of the committed points delivered:** {round(cal['median_delivered_share'] * 100)}%.")
    if cal["carried_by_size"]:
        out.append("**Carried over by size:** " + ", ".join(f"{k} pt: {v['carried']} of {v['committed']}" for k, v in cal["carried_by_size"].items()) + ".")
    if cal["factor"]:
        out.append(f"**Applied by backlog-forecast:** next-cycle sets scaled by {cal['factor']}.")
    else:
        out.append(f"**Applied by backlog-forecast:** not yet. It takes {cal['min_cycles']} closed cycles and a share below 100% ({cal['closed']} closed so far).")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("action", choices=["plan", "close", "stats"])
    ap.add_argument("repo_root")
    ap.add_argument("--commit", choices=["prudent", "likely"], default="prudent")
    ap.add_argument("--features")
    ap.add_argument("--start")
    ap.add_argument("--cycle-weeks", type=int, help="length of the cycle in weeks (default: [cycles] weeks of .backlog-kit.toml, 2)")
    ap.add_argument("--weeks", type=int, default=bf.DEFAULT_WEEKS)
    ap.add_argument("--since")
    ap.add_argument("--capacity", type=float)
    ap.add_argument("--cycle", help="cycle file name or start date (default: the open cycle)")
    ap.add_argument("--reason", action="append", default=[], help='why a feature was carried over, "#12=text"')
    ap.add_argument("--note", action="append", default=[], help="a line for the retro")
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--today")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    root = Path(args.repo_root)
    if not root.is_dir():
        sys.exit(f"{root} is not a directory")
    args.cycle_weeks = args.cycle_weeks or bk.cycle_weeks(bk.load_config(root))
    args.weeks, args.cycle_weeks = max(args.weeks, 1), max(args.cycle_weeks, 1)
    today = date.fromisoformat(args.today) if args.today else date.today()

    if args.action == "stats":
        cycles = bc.list_cycles(root)
        cal = bc.calibration(cycles)
        print(json.dumps({"cycles": [{k: (v.isoformat() if isinstance(v, date) else v) for k, v in c.items()} for c in cycles],
                          "calibration": cal}, ensure_ascii=False, indent=2) if args.json else render_stats(cycles, cal))
        return
    if args.action == "plan":
        path, text, info = plan(root, args, today)
        if args.write:
            if path.exists():
                fail(f"{path.name} already exists.")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
    else:
        path, text, info = close(root, args, today)
        if args.write:
            write_close(path, text)
    if args.json:
        print(json.dumps({**info, "file": str(path), "written": args.write}, ensure_ascii=False, indent=2))
        return
    print(("Written: " if args.write else "Preview, nothing saved: ") + str(path.relative_to(root)) + "\n")
    print(text)
    if args.action == "plan" and info["warnings"]:
        print("Read this first:\n" + "\n".join(f"- {w}" for w in info["warnings"]))
    if args.action == "close" and info["without_reason"]:
        print("Carried over without a reason: " + ", ".join(f"#{n}" for n in info["without_reason"]) + ". Ask for one (--reason \"#N=text\").")


if __name__ == "__main__":
    main()
