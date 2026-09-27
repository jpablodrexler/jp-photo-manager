#!/usr/bin/env python3
"""
Compute the bug-tracker status report for jp-photo-manager: counts, the
severity/area/environment breakdown, the open-bug list, and the
data-integrity checks (duplicate Bug IDs, orphaned Details blocks,
stale "still shows Fixed" rows).

Pure table-counting and text-pattern arithmetic over
JPPhotoManagerWeb/docs/backlog/bugs-open.md and
JPPhotoManagerWeb/docs/backlog/bugs-fixed.md — see
bugs-status/SKILL.md for the full rules this implements (steps 1-6).

Usage:
    python3 status_report.py <repo-root> [--json]
"""
import sys
import re
import json
import argparse
from pathlib import Path


def split_row(line):
    parts = re.split(r'(?<!\\)\|', line)
    if parts and parts[0].strip() == "":
        parts = parts[1:]
    if parts and parts[-1].strip() == "":
        parts = parts[:-1]
    return [p.strip().replace('\\|', '|') for p in parts]


def find_table(lines, heading):
    start = None
    for i, line in enumerate(lines):
        if line.strip() == heading:
            start = i
            break
    if start is None:
        return None
    i = start + 1
    while i < len(lines) and not lines[i].lstrip().startswith("|"):
        if lines[i].startswith("## "):
            return None
        i += 1
    if i >= len(lines):
        return None
    header = split_row(lines[i])
    i += 1
    if i < len(lines) and re.match(r'^\|?[\s:-]+\|', lines[i]):
        i += 1
    rows = []
    while i < len(lines) and lines[i].lstrip().startswith("|"):
        rows.append(split_row(lines[i]))
        i += 1
    return header, rows


def read_lines(path):
    if not path.exists():
        return None
    return path.read_text(encoding="utf-8").splitlines()


def row_dict(header, row):
    return {h: (row[idx] if idx < len(row) else "") for idx, h in enumerate(header)}


def find_detail_ids(lines):
    if lines is None:
        return set()
    ids = set()
    for line in lines:
        m = re.match(r'^### (BUG-\d+)\s', line)
        if m:
            ids.add(m.group(1))
    return ids


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root)
    open_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "bugs-open.md"
    fixed_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "bugs-fixed.md"

    result = {
        "open_available": False,
        "fixed_available": False,
        "open_strict": 0,
        "in_progress": 0,
        "wont_fix": 0,
        "cannot_repro": 0,
        "fixed_here": [],
        "fixed": 0,
        "total": None,
        "percent": None,
        "severity_breakdown": {"S1": 0, "S2": 0, "S3": 0, "S4": 0, "none": 0},
        "area_breakdown": {},
        "env_breakdown": {"local": 0, "deployed": 0, "both": 0},
        "active_rows": [],
        "duplicate_ids": [],
        "orphaned_rows_no_details": [],
        "orphaned_details_no_row": [],
    }

    open_lines = read_lines(open_path)
    fixed_lines = read_lines(fixed_path)

    open_rows = []
    if open_lines is not None:
        table = find_table(open_lines, "## Bug List")
        if table:
            header, rows = table
            result["open_available"] = True
            open_rows = [row_dict(header, r) for r in rows]

    fixed_rows = []
    if fixed_lines is not None:
        table = find_table(fixed_lines, "## Bug List")
        if table:
            header, rows = table
            result["fixed_available"] = True
            fixed_rows = [row_dict(header, r) for r in rows]

    result["fixed"] = len(fixed_rows)
    fixed_ids = {r.get("Bug ID", "").strip() for r in fixed_rows if r.get("Bug ID", "").strip()}

    open_ids = []
    active_rows = []
    for r in open_rows:
        bug_id = r.get("Bug ID", "").strip()
        open_ids.append(bug_id)
        status = r.get("Status", "")
        if "In Progress" in status:
            result["in_progress"] += 1
            active_rows.append(r)
        elif "Won't fix" in status or "Won" in status and "fix" in status:
            result["wont_fix"] += 1
        elif "Cannot reproduce" in status:
            result["cannot_repro"] += 1
        elif "Fixed" in status:
            result["fixed_here"].append(bug_id)
        elif "Open" in status:
            result["open_strict"] += 1
            active_rows.append(r)

    active = result["open_strict"] + result["in_progress"]

    if result["open_available"] and result["fixed_available"]:
        total = active + result["wont_fix"] + result["cannot_repro"] + result["fixed"]
        resolved = result["wont_fix"] + result["cannot_repro"] + result["fixed"]
        result["total"] = total
        if total == 0:
            result["percent"] = "0% — no bugs tracked yet"
        else:
            pct = round(100 * resolved / total, 1)
            result["percent"] = f"{pct}% resolved"

    for r in active_rows:
        sev = r.get("Severity", "").strip()
        skey = sev if sev in ("S1", "S2", "S3", "S4") else "none"
        result["severity_breakdown"][skey] += 1

        area = r.get("Area", "").strip() or "(blank)"
        result["area_breakdown"][area] = result["area_breakdown"].get(area, 0) + 1

        env = r.get("Environment", "").strip()
        ekey = env if env in ("local", "deployed", "both") else env or "(blank)"
        result["env_breakdown"][ekey] = result["env_breakdown"].get(ekey, 0) + 1

        result["active_rows"].append({
            "id": r.get("Bug ID", "").strip(),
            "severity": sev,
            "environment": env,
            "summary": r.get("Summary", "").strip(),
            "in_progress": "In Progress" in r.get("Status", ""),
        })

    seen = {}
    for bug_id in (open_ids + list(fixed_ids)):
        if bug_id:
            seen[bug_id] = seen.get(bug_id, 0) + 1
    result["duplicate_ids"] = sorted([b for b, c in seen.items() if c > 1])

    detail_ids = find_detail_ids(open_lines)
    active_open_ids = {r.get("Bug ID", "").strip() for r in open_rows
                        if "Open" in r.get("Status", "") or "In Progress" in r.get("Status", "")}
    result["orphaned_rows_no_details"] = sorted(active_open_ids - detail_ids)
    result["orphaned_details_no_row"] = sorted(detail_ids - {r.get("Bug ID", "").strip() for r in open_rows})

    if args.json:
        print(json.dumps(result, indent=2))
        return

    render_markdown(result)


def render_markdown(r):
    lines = []
    lines.append("## Bug Tracker Status")
    lines.append("")
    if r["total"] is None:
        missing = []
        if not r["open_available"]:
            missing.append("JPPhotoManagerWeb/docs/backlog/bugs-open.md")
        if not r["fixed_available"]:
            missing.append("JPPhotoManagerWeb/docs/backlog/bugs-fixed.md")
        lines.append(f"**Total/Progress unavailable** — missing or table-less: {', '.join(missing)}")
        lines.append("")
    else:
        lines.append("| Metric           | Count |")
        lines.append("| ---------------- | ----- |")
        lines.append(f"| Total bugs        | {r['total']} |")
        lines.append(f"| ✅ Fixed           | {r['fixed']} |")
        lines.append(f"| 🚫 Won't fix       | {r['wont_fix']} |")
        lines.append(f"| ❓ Cannot reproduce | {r['cannot_repro']} |")
        lines.append(f"| ⬜ Open            | {r['open_strict']} |")
        lines.append(f"| 🔶 In Progress     | {r['in_progress']} |")
        lines.append(f"| **Progress**       | **{r['percent']}** |")
        lines.append("")

    lines.append("### Open bugs by severity")
    lines.append("")
    lines.append("| Severity | Count |")
    lines.append("| -------- | ----- |")
    lines.append(f"| S1 — blocker  | {r['severity_breakdown']['S1']} |")
    lines.append(f"| S2 — major    | {r['severity_breakdown']['S2']} |")
    lines.append(f"| S3 — minor    | {r['severity_breakdown']['S3']} |")
    lines.append(f"| S4 — trivial  | {r['severity_breakdown']['S4']} |")
    lines.append(f"| No explicit severity | {r['severity_breakdown']['none']} |")
    lines.append("")

    lines.append("### Open bugs by area")
    lines.append("")
    lines.append("| Area | Count |")
    lines.append("| ---- | ----- |")
    if r["area_breakdown"]:
        for area, count in sorted(r["area_breakdown"].items(), key=lambda kv: (-kv[1], kv[0])):
            lines.append(f"| {area} | {count} |")
    else:
        lines.append("| _None_ | 0 |")
    lines.append("")

    lines.append("### Open bugs by environment")
    lines.append("")
    lines.append("| Environment | Count |")
    lines.append("| ----------- | ----- |")
    lines.append(f"| local | {r['env_breakdown'].get('local', 0)} |")
    lines.append(f"| deployed | {r['env_breakdown'].get('deployed', 0)} |")
    lines.append(f"| both  | {r['env_breakdown'].get('both', 0)} |")
    lines.append("")

    lines.append("### Open bugs")
    lines.append("")
    if r["active_rows"]:
        for row in r["active_rows"]:
            suffix = " — 🔶 In Progress" if row["in_progress"] else ""
            lines.append(f"- {row['id']} `{row['severity'] or '?'}` ({row['environment'] or '?'}) — {row['summary']}{suffix}")
    else:
        lines.append("_None._")
    lines.append("")

    integrity = []
    for bug_id in r["duplicate_ids"]:
        integrity.append(f"- ⚠ Duplicate Bug ID {bug_id}.")
    for bug_id in r["orphaned_rows_no_details"]:
        integrity.append(f"- ⚠ {bug_id} has a table row but no Details block.")
    for bug_id in r["orphaned_details_no_row"]:
        integrity.append(f"- ⚠ {bug_id} has a Details block but no table row.")
    for bug_id in r["fixed_here"]:
        integrity.append(f"- ⚠ {bug_id} still shows ✅ Fixed in bugs-open.md — run `bugs-archive {bug_id}` to finish the move.")
    if integrity:
        lines.append("### Data integrity")
        lines.append("")
        lines.extend(integrity)

    print("\n".join(lines))


if __name__ == "__main__":
    main()
