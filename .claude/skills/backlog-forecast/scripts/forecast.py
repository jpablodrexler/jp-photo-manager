#!/usr/bin/env python3
"""
Forecast what the backlog can deliver: what fits in the next cycle, a cycle-by-cycle plan with dates, and how many
cycles and what date it takes to finish the known backlog, each as a range and not as a single date.

    python3 forecast.py <repo-root> [--weeks N | --since YYYY-MM-DD] [--cycle-weeks N] [--cycles N]
                        [--capacity POINTS_PER_WEEK] [--features 19,22,...] [--today YYYY-MM-DD] [--json]

Read-only. The pace is not a number typed in: it is what the backlog actually delivered each week in the window
(the last 12 complete weeks by default), which already contains the limits of the Claude subscription, the time spent on
bugs and the interruptions. The weekly figures are resampled at random (a bootstrap, 20,000 runs, a fixed seed so the
output is reproducible) to see how many weeks the remaining work takes in a good, a typical and a bad run:
  P50  the typical outcome: half of the runs finish by then;
  P85  prudent: 85 of 100 runs finish by then;
  P95  very prudent: only used for the finishing date of the whole backlog.
The order of the work is the one features-next recommends: priority, spec ready, position in the recommended order, no
schema change, with a feature placed after the pending prerequisite it is blocked on.
Exit code is always 0 unless the repo root is bad.
"""
import argparse
import json
import sys
from datetime import date
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_forecast as bf
import backlog_kit as bk
from backlog_forecast import DEFAULT_WEEKS


def fmt_items(items):
    return ", ".join(f"#{i['number']} `{i['name']}` ({i['points']})" for i in items) or "nothing"


def render(r):
    out = [f"## Backlog Forecast (as of {r['as_of']})", ""]
    if not r["available"]["pending"]:
        return "\n".join(out + ["features-planned.md is missing or has no `## Feature List` table: nothing to forecast."])
    w = r["window"]
    out.append(f"**History:** {w['weeks']} complete week(s), {w['start']} → {w['end']}"
               + (f", {w['weeks_with_deliveries']} with deliveries; points per week {', '.join(str(x) for x in w['weekly_points'])}"
                  if w["weeks"] else ""))
    pend = r["pending"]
    out.append(f"**Pending:** {pend['features']} feature(s), {pend['points']} point(s)"
               + (f"; {len(pend['not_sized'])} without points are not in the points ({', '.join('#' + str(n) for n in pend['not_sized'][:10])}"
                  + (" …" if len(pend["not_sized"]) > 10 else "") + ")" if pend["not_sized"] else ""))
    cap = r["capacity"]
    if not cap:
        out += [""] + [f"- ⚠ {x['message']}" for x in r["warnings"] if x["code"] == "no_history"]
        return "\n".join(out)
    if cap["source"] == "manual":
        out.append(f"**Pace:** {cap['points_per_week']} points/week, given by hand")
    else:
        out.append(f"**Pace per {cap['cycle_weeks']}-week cycle:** likely (P50) {cap['likely_points']} points, "
                   f"prudent (P85) {cap['prudent_points']} points; mean {w['mean_points']}/week, median {w['median_points']}/week")
    nc = r["next_cycle"]
    out += ["", f"### Next cycle ({nc['starts']} → {nc['ends']})"]
    out.append(f"- **Prudent (P85):** {fmt_items(nc['prudent'])}" + (f" — {sum(i['points'] for i in nc['prudent'])} points" if nc["prudent"] else ""))
    out.append(f"- **Likely (P50):** {fmt_items(nc['likely'])}" + (f" — {sum(i['points'] for i in nc['likely'])} points" if nc["likely"] else ""))
    if not nc["likely"] and nc["first_item"] is not None:
        out.append(f"- The first feature in the order (#{nc['first_item']}, {nc['first_item_points']} points) is bigger than a likely cycle.")
    cal = r.get("calibration")
    if cal and cal["closed"]:
        met = cal["forecast_met"]
        out += ["", f"### What the {cal['closed']} closed cycle(s) say"]
        out.append(f"- The forecast capacity was reached: prudent in {met['prudent']['met']} of {met['prudent']['of']}, "
                   f"likely in {met['likely']['met']} of {met['likely']['of']}.")
        out.append(f"- Median share of the committed points that was delivered: {round(cal['median_delivered_share'] * 100)}%.")
        sizes = ", ".join(f"{k} pt: {v['carried']} of {v['committed']} carried over" for k, v in cal["carried_by_size"].items())
        if sizes:
            out.append(f"- By size: {sizes}.")
        if cal["applied"]:
            out.append(f"- The next-cycle sets above are scaled by {cal['applied']} (the median delivered share); --no-calibration shows them unscaled.")
        elif cal["closed"] < cal["min_cycles"]:
            out.append(f"- Not applied yet: it takes {cal['min_cycles']} closed cycles ({cal['closed']} so far).")
    if r["set"]:
        s = r["set"]
        out += ["", "### The set you asked about"]
        line = f"- {', '.join('#' + str(n) for n in s['features']) or 'no pending feature'}: {s['points']} point(s)"
        if "weeks" in s:
            line += f" — typical {s['weeks']['p50']} week(s) (by {s['dates']['p50']}), prudent {s['weeks']['p85']} (by {s['dates']['p85']}), very prudent {s['weeks']['p95']} (by {s['dates']['p95']})"
        out.append(line)
        if s["not_pending"]:
            out.append(f"- Not pending (ignored): {', '.join('#' + str(n) for n in s['not_pending'])}")
        if s["not_sized"]:
            out.append(f"- Without points (counted as zero): {', '.join('#' + str(n) for n in s['not_sized'])}")
    if r["cycles"]:
        out += ["", "### Cycles in the order features-next recommends", "",
                "| Cycle | Ends | Features (points) | Points | Cumulative | Likely by | Prudent by |", "| --- | --- | --- | --- | --- | --- | --- |"]
        for c in r["cycles"]:
            out.append(f"| {c['cycle']} | {c['ends']} | {fmt_items(c['items'])} | {c['points']} | {c['cumulative']} | "
                       f"{c['likely_by']['date']} | {c['prudent_by']['date']} |")
        if r["beyond"] and r["beyond"]["features"]:
            out.append(f"\n{r['beyond']['features']} more feature(s), {r['beyond']['points']} point(s), come after these cycles.")
    comp = r["completion"]
    if comp:
        out += ["", "### Finishing the known backlog", "", "| By | Remaining | Typical (P50) | Prudent (P85) | Very prudent (P95) |", "| --- | --- | --- | --- | --- |"]
        for unit, label in (("points", "points"), ("features", "number of features")):
            c = comp.get(unit)
            if c:
                cells = [f"{c['cycles'][k]} cycle(s), {c['dates'][k]}" for k in ("p50", "p85", "p95")]
                out.append(f"| {label} | {c['remaining']} | " + " | ".join(cells) + " |")
    if r["warnings"]:
        out += ["", "### Read this first"] + [f"- ⚠ {x['message']}" for x in r["warnings"]]
    out += ["", "### What this does not say"] + [f"- {n}" for n in r["notes"]]
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--weeks", type=int, default=DEFAULT_WEEKS, help=f"complete weeks of history to learn the pace from (default {DEFAULT_WEEKS})")
    ap.add_argument("--since", help="start of the history window, YYYY-MM-DD (replaces --weeks)")
    ap.add_argument("--cycle-weeks", type=int, help="length of a cycle in weeks (default: [cycles] weeks of .backlog-kit.toml, 2)")
    ap.add_argument("--cycles", type=int, default=6, help="how many cycles the plan table shows (default 6)")
    ap.add_argument("--capacity", type=float, help="points per week, given by hand instead of learnt from the history")
    ap.add_argument("--features", help="feature numbers to ask about as a set, for example 19,22,25")
    ap.add_argument("--today", help="treat this date as today, YYYY-MM-DD (for reproducible output)")
    ap.add_argument("--no-calibration", action="store_true", help="do not scale the next cycle by the share delivered in closed cycles")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if not Path(args.repo_root).is_dir():
        sys.exit(f"{args.repo_root} is not a directory")
    args.cycle_weeks = args.cycle_weeks or bk.cycle_weeks(bk.load_config(args.repo_root))
    args.weeks, args.cycle_weeks, args.cycles = max(args.weeks, 1), max(args.cycle_weeks, 1), max(args.cycles, 1)
    today = date.fromisoformat(args.today) if args.today else date.today()
    result = bf.forecast(Path(args.repo_root), args, today)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
