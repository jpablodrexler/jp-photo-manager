#!/usr/bin/env python3
# VENDORED from lib/backlog_forecast.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
The forecast itself, shared by backlog-forecast (which prints it) and backlog-cycle (which plans a cycle from it).

`forecast(root, args, today)` returns a dict: the pace learnt from the weekly history, the next cycle (a prudent and a
likely set of features), the cycle table, the finish of the known backlog and the warnings. `args` carries `weeks`,
`since`, `cycle_weeks`, `cycles`, `capacity`, `features` and `seed`. Read-only; no function here writes a file.
"""
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
import backlog_cycles as bc
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
    notes = ["P50, P85 and P95 are read off 20,000 simulated runs: P85 is reached or passed in 85 of 100 of them. For a capacity "
             "a higher percentile gives fewer points; for a date it gives a later one.",
             "Only the known backlog is forecast: features added later are not in it.",
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
              "capacity": None, "calibration": None, "next_cycle": None, "cycles": [], "beyond": None, "completion": None, "set": None,
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

    # what the closed cycles say about the commitments: from MIN_CYCLES of them the capacity is scaled down by the share that was delivered
    cal = bc.calibration(bc.list_cycles(root))
    factor = cal["factor"] if not manual and not getattr(args, "no_calibration", False) else None
    result["calibration"] = {**cal, "applied": factor}

    # next cycle
    likely, prudent = fill_prefix(items, c50 * (factor or 1)), fill_prefix(items, c85 * (factor or 1))
    first = next((c for c in items if c["points"] is not None), None)
    cycle_end = add_days(today, cycle_weeks)
    result["next_cycle"] = {"starts": today.isoformat(), "ends": cycle_end.isoformat(),
                            "likely": [{k: c[k] for k in ("number", "name", "priority", "points")} for c in likely],
                            "prudent": [{k: c[k] for k in ("number", "name", "priority", "points")} for c in prudent],
                            "first_item_points": first["points"] if first else None,
                            "first_item": first["number"] if first else None}

    # cycles
    plan_capacity = c50 * (factor or 1)
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
