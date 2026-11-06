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
import math
import random
import sys
from datetime import date, timedelta
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_history as bh
import backlog_kit as bk

DEFAULT_WEEKS = 12
RUNS = 20000
MAX_WEEKS = 520                  # ten years: a run that has not finished by then is counted as that
FEW_WEEKS = 8                    # fewer weeks of history than this are worth a warning
PAUSE_WEEKS = 4                  # complete weeks without a delivery that count as a pause
ORDER_ENTRY = r"^\s*-\s+`[^`]+`\s*\(#(\d+)\)"
PRUDENT_CAPACITY = 0.15          # capacity reached in 85% of the cycles is the 15th percentile of what a cycle delivered


def quantile(sorted_values, q):
    return sorted_values[min(len(sorted_values) - 1, int(len(sorted_values) * q))]


def simulate(samples, targets, runs, seed):
    """For each target (points or features), the sorted list of how many weeks it took to reach it in each run,
    drawing weeks at random from `samples`. `targets` must be ascending."""
    rnd = random.Random(seed)
    out = [[] for _ in targets]
    n_samples = len(samples)
    for _ in range(runs):
        total, weeks, idx = 0, 0, 0
        while idx < len(targets):
            if weeks >= MAX_WEEKS:
                for j in range(idx, len(targets)):
                    out[j].append(MAX_WEEKS)
                break
            total += samples[rnd.randrange(n_samples)]
            weeks += 1
            while idx < len(targets) and total >= targets[idx]:
                out[idx].append(weeks)
                idx += 1
    for lst in out:
        lst.sort()
    return out


def capacities(samples, cycle_weeks, runs, seed):
    """Sorted points delivered in one cycle of `cycle_weeks` weeks drawn at random from the weekly samples."""
    rnd = random.Random(seed + 1)
    caps = sorted(sum(samples[rnd.randrange(len(samples))] for _ in range(cycle_weeks)) for _ in range(runs))
    return caps


def ordered_pending(h, planned_lines):
    """The pending and in-progress features in the order features-next would take them, each with its points."""
    positions = bk.order_list_positions(planned_lines, "### Recommended implementation order", ORDER_ENTRY)
    cands = []
    for r in h["pending"]:
        n = bk.strip_backticks(r.get("#", ""))
        if not n.isdigit():
            continue
        in_progress = "In Progress" in r.get("Implementation", "")
        pos = positions.get(n)
        score = bk.score_feature(r.get("Priority", "").strip(), r.get("Schema Change", "").strip(), in_progress,
                                 "Created" in r.get("SDD Artifacts", ""), pos is not None)
        cands.append({"number": int(n), "name": bk.strip_backticks(r.get("Change name", "")), "priority": r.get("Priority", "").strip(),
                      "points": bh.points(r.get("Effort")), "in_progress": in_progress, "score": score,
                      "position": pos if pos is not None else 10 ** 9})
    cands.sort(key=lambda c: (-c["score"], c["position"], c["number"]))
    pending_numbers = {c["number"] for c in cands}
    needs = {}                                          # a feature waits for the pending prerequisites it is blocked on
    for a, b in bk.hard_dependencies(planned_lines):
        if a.isdigit() and b.isdigit() and int(b) in pending_numbers and int(a) in pending_numbers:
            needs.setdefault(int(a), set()).add(int(b))
    placed, remaining = [], list(cands)
    while remaining:
        for c in remaining:
            if needs.get(c["number"], set()) <= {p["number"] for p in placed}:
                placed.append(c)
                remaining.remove(c)
                break
        else:                                           # a cycle of dependencies: keep the rest in rank order
            placed.extend(remaining)
            break
    return placed


def fill_prefix(items, capacity):
    """The longest run of items from the front whose points fit in `capacity`; an item without points is skipped."""
    out, total = [], 0
    for c in items:
        if c["points"] is None:
            continue
        if total + c["points"] > capacity:
            break
        out.append(c)
        total += c["points"]
    return out


def plan_cycles(items, capacity, max_cycles):
    """Chunk the ordered items into cycles of `capacity` points; an item larger than a cycle gets a cycle of its own."""
    cycles, current, total = [], [], 0
    for c in items:
        if c["points"] is None:
            continue
        if current and total + c["points"] > capacity:
            cycles.append(current)
            current, total = [], 0
        current.append(c)
        total += c["points"]
    if current:
        cycles.append(current)
    return cycles[:max_cycles], cycles[max_cycles:]


def add_days(d, weeks):
    return d + timedelta(days=7 * weeks)


def forecast(root, args, today):
    h = bh.load_history(root, today)
    start = date.fromisoformat(args.since) if args.since else bh.monday(today) - timedelta(days=7 * args.weeks)
    weeks = [w for w in bh.weekly_series(h["items"], start, today) if w["complete"]]
    pts = [w["points"] for w in weeks]
    feats = [w["features"] for w in weeks]
    planned_lines = bk.read_lines(Path(root) / h["backlog"] / "features-planned.md") or []
    items = ordered_pending(h, planned_lines) if h["plan_ok"] else []
    sized = [c for c in items if c["points"] is not None]
    remaining_points = sum(c["points"] for c in sized)
    in_range = [i for i in h["items"] if start <= i["date"] <= today]
    uses_git = any(i["source"] == "git" for i in h["items"] if i["kind"] == "feature")
    pending_unestimated = sum(1 for c in items if c["points"] is None)
    warnings = bh.reliability(root, h["impl_ok"], in_range, uses_git, pending_unestimated, remaining_points)
    notes = ["Only the known backlog is forecast: features added later are not in it.",
             "Story points are ordinal, not proportional: a sum of points is a rough size, which is why the same forecast "
             "is also made by number of features.",
             "It assumes the next weeks look like the window. If your plan, its limits or the way you work changed, start "
             "the window after the change (--since) or give a pace by hand (--capacity)."]

    result = {"as_of": today.isoformat(), "available": {"features": h["impl_ok"], "pending": h["plan_ok"]},
              "window": {"start": start.isoformat(),
                         "end": (date.fromisoformat(weeks[-1]["week"]) + timedelta(days=6)).isoformat() if weeks else today.isoformat(),
                         "weeks": len(weeks), "weekly_points": pts,
                         "weekly_features": feats, "weeks_with_deliveries": sum(1 for f in feats if f)},
              "pending": {"features": len(items), "points": remaining_points, "not_sized": [c["number"] for c in items if c["points"] is None]},
              "capacity": None, "next_cycle": None, "cycles": [], "beyond": None, "completion": None, "set": None,
              "warnings": warnings, "notes": notes}

    manual = args.capacity
    if not manual and not any(pts):
        result["warnings"] = [{"code": "no_history", "message":
                               "No feature was delivered in the window, so there is no pace to forecast from. Choose a window that "
                               "includes deliveries (--since YYYY-MM-DD) or give a pace by hand (--capacity POINTS_PER_WEEK)."}] + warnings
        return result

    cycle_weeks = args.cycle_weeks
    if manual:
        c50 = c85 = manual * cycle_weeks
        result["capacity"] = {"source": "manual", "points_per_week": manual, "cycle_weeks": cycle_weeks,
                              "likely_points": c50, "prudent_points": c85}
    else:
        caps = capacities(pts, cycle_weeks, RUNS, args.seed)
        c50, c85 = quantile(caps, 0.5), quantile(caps, PRUDENT_CAPACITY)
        result["capacity"] = {"source": "history", "cycle_weeks": cycle_weeks, "likely_points": c50, "prudent_points": c85}
        result["window"].update({"mean_points": round(sum(pts) / len(pts), 1), "median_points": sorted(pts)[len(pts) // 2]})

    def weeks_for(targets, samples):
        """{target: (p50, p85, p95)} in weeks."""
        if manual and samples is pts:
            return {t: (math.ceil(t / manual),) * 3 for t in targets}
        ordered = sorted(set(targets))
        sims = simulate(samples, ordered, RUNS, args.seed)
        return {t: (quantile(s, 0.5), quantile(s, 0.85), quantile(s, 0.95)) for t, s in zip(ordered, sims)}

    def dated(w):
        return {"weeks": w, "date": add_days(today, w).isoformat()}

    # next cycle
    likely, prudent = fill_prefix(items, c50), fill_prefix(items, c85)
    first = next((c for c in items if c["points"] is not None), None)
    cycle_end = add_days(today, cycle_weeks)
    result["next_cycle"] = {"starts": today.isoformat(), "ends": cycle_end.isoformat(),
                            "likely": [{k: c[k] for k in ("number", "name", "priority", "points")} for c in likely],
                            "prudent": [{k: c[k] for k in ("number", "name", "priority", "points")} for c in prudent],
                            "first_item_points": first["points"] if first else None,
                            "first_item": first["number"] if first else None}

    # cycles
    plan_capacity = c50
    if plan_capacity <= 0 and not manual:               # the typical cycle delivered nothing: plan with the mean pace instead
        plan_capacity = max(1, round(sum(pts) / len(pts) * cycle_weeks))
        result["capacity"]["planned_with"] = plan_capacity
    planned, rest = plan_cycles(items, max(plan_capacity, 1), args.cycles)
    cumulative, targets = 0, []
    for chunk in planned:
        cumulative += sum(c["points"] for c in chunk)
        targets.append(cumulative)
    when = weeks_for(targets, pts) if targets else {}
    cumulative = 0
    for k, chunk in enumerate(planned, 1):
        cumulative += sum(c["points"] for c in chunk)
        p50, p85, _ = when[cumulative]
        result["cycles"].append({"cycle": k, "ends": add_days(today, cycle_weeks * k).isoformat(),
                                 "items": [{key: c[key] for key in ("number", "name", "priority", "points")} for c in chunk],
                                 "points": sum(c["points"] for c in chunk), "cumulative": cumulative,
                                 "likely_by": dated(p50), "prudent_by": dated(p85)})
    result["beyond"] = {"features": sum(len(c) for c in rest), "points": sum(x["points"] for c in rest for x in c)}

    # the whole known backlog, by points and by number of features
    completion = {}
    if remaining_points:
        w = weeks_for([remaining_points], pts)[remaining_points]
        completion["points"] = {"remaining": remaining_points, "weeks": dict(zip(("p50", "p85", "p95"), w)),
                                "cycles": {k: math.ceil(v / cycle_weeks) for k, v in zip(("p50", "p85", "p95"), w)},
                                "dates": {k: add_days(today, v).isoformat() for k, v in zip(("p50", "p85", "p95"), w)}}
    if items and any(feats):
        w = weeks_for([len(items)], feats)[len(items)]
        completion["features"] = {"remaining": len(items), "weeks": dict(zip(("p50", "p85", "p95"), w)),
                                  "cycles": {k: math.ceil(v / cycle_weeks) for k, v in zip(("p50", "p85", "p95"), w)},
                                  "dates": {k: add_days(today, v).isoformat() for k, v in zip(("p50", "p85", "p95"), w)}}
    result["completion"] = completion or None

    # a given set of features
    if args.features:
        wanted = [int(x) for x in args.features.split(",") if x.strip().isdigit()]
        by_number = {c["number"]: c for c in items}
        chosen = [by_number[n] for n in wanted if n in by_number]
        unknown = [n for n in wanted if n not in by_number]
        total = sum(c["points"] or 0 for c in chosen)
        entry = {"features": [c["number"] for c in chosen], "not_pending": unknown, "points": total,
                 "not_sized": [c["number"] for c in chosen if c["points"] is None]}
        if total:
            w = weeks_for([total], pts)[total]
            entry["weeks"] = dict(zip(("p50", "p85", "p95"), w))
            entry["dates"] = {k: add_days(today, v).isoformat() for k, v in zip(("p50", "p85", "p95"), w)}
        result["set"] = entry

    # reasons to read the result with care
    extra = []
    if not manual:
        if len(weeks) < FEW_WEEKS:
            extra.append({"code": "few_weeks", "message":
                          f"Only {len(weeks)} complete week(s) of history: the range below is a first impression."})
        recent = feats[-PAUSE_WEEKS:]
        if len(recent) == PAUSE_WEEKS and not any(recent):
            extra.append({"code": "recent_pause", "message":
                          f"No feature was delivered in the last {PAUSE_WEEKS} complete weeks. The forecast assumes the pace of the window "
                          "comes back; if the pause is a change of pace, start the window after it."})
        if c50 <= 0:
            extra.append({"code": "zero_median", "message":
                          f"The typical {cycle_weeks}-week cycle of the window delivered nothing; the cycle table is planned with the mean "
                          f"pace ({plan_capacity} points per cycle) and the likely set is empty."})
        zero = sum(1 for p in pts if p == 0)
        if pts and zero * 2 >= len(pts):
            extra.append({"code": "many_zero_weeks", "message":
                          f"{zero} of the {len(pts)} weeks of the window delivered nothing, so the pace is very uneven and the range is wide."})
        cp = (completion or {}).get("points")
        if cp and cp["weeks"]["p50"] >= 1 and cp["weeks"]["p95"] > 2 * cp["weeks"]["p50"]:
            extra.append({"code": "wide_range", "message":
                          f"The very prudent finish takes more than twice the typical one ({cp['weeks']['p95']} against {cp['weeks']['p50']} weeks): "
                          "treat the date as a range."})
        cf = (completion or {}).get("features")
        if cp and cf and max(cp["weeks"]["p50"], cf["weeks"]["p50"]) >= 2:
            a, b = cp["weeks"]["p50"], cf["weeks"]["p50"]
            if abs(a - b) / max(a, b) >= 0.5:
                extra.append({"code": "units_disagree", "message":
                              f"By points the typical finish is {a} week(s) and by number of features {b}: the remaining features are "
                              "bigger or smaller than the ones delivered, so trust neither number alone."})
    result["warnings"] = extra + warnings
    return result


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
    ap.add_argument("--cycle-weeks", type=int, default=1, help="length of a cycle in weeks (default 1)")
    ap.add_argument("--cycles", type=int, default=6, help="how many cycles the plan table shows (default 6)")
    ap.add_argument("--capacity", type=float, help="points per week, given by hand instead of learnt from the history")
    ap.add_argument("--features", help="feature numbers to ask about as a set, for example 19,22,25")
    ap.add_argument("--today", help="treat this date as today, YYYY-MM-DD (for reproducible output)")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if not Path(args.repo_root).is_dir():
        sys.exit(f"{args.repo_root} is not a directory")
    args.weeks, args.cycle_weeks, args.cycles = max(args.weeks, 1), max(args.cycle_weeks, 1), max(args.cycles, 1)
    today = date.fromisoformat(args.today) if args.today else date.today()
    result = forecast(Path(args.repo_root), args, today)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
