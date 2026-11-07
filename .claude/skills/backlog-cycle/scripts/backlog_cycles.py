#!/usr/bin/env python3
# VENDORED from lib/backlog_cycles.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
Reading the cycle files of `<backlog>/cycles/` and what closed cycles say about the estimates.

A cycle file is a small markdown record: a header (`**Start:**`, `**End:**`, `**Status:**` open or closed, the capacity the
forecast gave when it was planned), a `## Plan` table of the committed features, and, once closed, a `## Close` section with
what was delivered, what was carried over and why. `backlog-cycle` writes them; `backlog-forecast` reads them to calibrate.
Read-only here. Pure stdlib.
"""
import re
import sys
from datetime import date
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_kit as bk

KEY_RE = re.compile(r"^\*\*([^:*]+):\*\*\s*(.*?)\s*$")
FORECAST_RE = re.compile(r"prudent (\d+(?:\.\d+)?) points?, likely (\d+(?:\.\d+)?) points?")
OF_RE = re.compile(r"(\d+) of (\d+) feature\(s\), (\d+) of (\d+) point\(s\)")
TOTAL_RE = re.compile(r"(\d+) feature\(s\), (\d+) point\(s\), (\d+) bug\(s\)")
MIN_CYCLES = 4              # closed cycles needed before the commitment factor is applied to a forecast
FACTOR_FLOOR = 0.3          # a factor below this is not believed: it would shrink every cycle to almost nothing


def _num(cell):
    m = re.search(r"\d+", cell or "")
    return int(m.group(0)) if m else None


def _items(lines, heading):
    table = bk.find_table(lines, heading)
    if not table:
        return []
    header, rows = table
    out = []
    for r in rows:
        d = bk.row_dict(header, r)
        n = _num(d.get("#"))
        if n is not None:
            out.append({"number": n, "name": bk.strip_backticks(d.get("Change name", "")), "points": _num(d.get("Points")),
                        "reason": d.get("Reason", "")})
    return out


def parse_cycle(path):
    """One cycle file as a dict, or None when it has no readable start and end date."""
    lines = bk.read_lines(path)
    if lines is None:
        return None
    meta = {}
    for line in lines:
        m = KEY_RE.match(line)
        if m and m.group(1) not in meta:
            meta[m.group(1)] = m.group(2)
    try:
        start, end = date.fromisoformat(meta["Start"]), date.fromisoformat(meta["End"])
    except (KeyError, ValueError):
        return None
    fc = FORECAST_RE.search(meta.get("Forecast at planning", ""))
    cycle = {"file": Path(path).name, "start": start, "end": end, "status": meta.get("Status", "open").strip().lower(),
             "commitment": meta.get("Commitment", ""), "prudent": float(fc.group(1)) if fc else None,
             "likely": float(fc.group(2)) if fc else None, "committed": _items(lines, "## Plan"), "close": None}
    if cycle["status"] == "closed":
        of, total = OF_RE.search(meta.get("Delivered from the plan", "")), TOTAL_RE.search(meta.get("Delivered in total", ""))
        cycle["close"] = {
            "closed": meta.get("Closed", ""),
            "plan_features": int(of.group(1)) if of else None, "committed_features": int(of.group(2)) if of else None,
            "plan_points": int(of.group(3)) if of else None, "committed_points": int(of.group(4)) if of else None,
            "total_points": int(total.group(2)) if total else None, "bugs": int(total.group(3)) if total else None,
            "carried": _items(lines, "### Carried over")}
    return cycle


def list_cycles(root):
    """Every readable cycle file of the project, oldest first."""
    cfg = bk.load_config(root)
    folder = bk.Paths(root, cfg).cycles_dir
    if not folder.is_dir():
        return []
    cycles = [c for c in (parse_cycle(p) for p in sorted(folder.glob("*.md"))) if c]
    return sorted(cycles, key=lambda c: c["start"])


def _median(values):
    s = sorted(values)
    n = len(s)
    return None if not n else (s[n // 2] if n % 2 else (s[n // 2 - 1] + s[n // 2]) / 2)


def calibration(cycles):
    """What the closed cycles say about the estimates: how often the forecast capacity was reached, which share of the
    committed points was delivered (the `factor`, applied to a forecast only from MIN_CYCLES closed cycles), and how often
    a feature of each size was carried over."""
    closed = [c for c in cycles if c["close"] and c["close"]["committed_points"]]
    ratios = [c["close"]["plan_points"] / c["close"]["committed_points"] for c in closed]
    met = {}
    for key in ("prudent", "likely"):
        have = [c for c in closed if c[key] is not None and c["close"]["total_points"] is not None]
        met[key] = {"met": sum(1 for c in have if c["close"]["total_points"] >= c[key]), "of": len(have)}
    by_size = {}
    for c in closed:
        carried = {f["number"] for f in c["close"]["carried"]}
        for f in c["committed"]:
            if f["points"] is None:
                continue
            row = by_size.setdefault(f["points"], {"committed": 0, "carried": 0})
            row["committed"] += 1
            row["carried"] += f["number"] in carried
    median = _median(ratios)
    factor = None
    if len(closed) >= MIN_CYCLES and median is not None and median < 1:
        factor = round(max(median, FACTOR_FLOOR), 2)
    return {"closed": len(closed), "median_delivered_share": round(median, 2) if median is not None else None,
            "factor": factor, "forecast_met": met, "carried_by_size": dict(sorted(by_size.items())),
            "min_cycles": MIN_CYCLES}
