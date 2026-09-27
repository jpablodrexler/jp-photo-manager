#!/usr/bin/env python3
"""
Compute the feature-tracker status report for jp-photo-manager: counts,
the priority/effort/SDD-readiness breakdown, the pending list, and the
data-integrity checks (duplicate feature numbers, stale dependency notes
claiming a feature is still pending when it's actually already
implemented).

This is pure table-counting and text-pattern arithmetic over
JPPhotoManagerWeb/docs/backlog/features-planned.md and
JPPhotoManagerWeb/docs/backlog/features-implemented.md — see
features-status/SKILL.md for the full rules this implements (steps 1-6).
The skill's job is reading this script's output and presenting it; this
script owns none of the judgment, only the counting.

The stale-dependency-note check covers this project's two annotation
forms (see SKILL.md step 5): a rare heading annotation naming an explicit
still-pending state, and the actually-common prose aside ("#N is still
pending", "#N and #M are still pending", "(#N, still pending)") used to
explain why a Dependencies block is duplicated across both backlog files.

Usage:
    python3 status_report.py <repo-root> [--json]

Exit code is always 0 (a missing/malformed backlog file is reported in
the output, not a hard failure) unless the repo root itself is bad.
"""
import sys
import re
import json
import argparse
from pathlib import Path


def split_row(line):
    """Split a markdown table row on unescaped pipes; unescape \\| back to |."""
    parts = re.split(r'(?<!\\)\|', line)
    if parts and parts[0].strip() == "":
        parts = parts[1:]
    if parts and parts[-1].strip() == "":
        parts = parts[:-1]
    return [p.strip().replace('\\|', '|') for p in parts]


def find_table(lines, heading):
    """Find `## <heading>` and return (header_cells, [row_cells, ...]) or None."""
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


def strip_backticks(s):
    return s.strip().strip("`")


# Prose aside: "#N is still pending" / "#N and #M are still pending" /
# "#N, #M and #P are still pending".
PROSE_LIST_PATTERN = re.compile(
    r'((?:#\d+(?:\s*,\s*|\s+and\s+))*#\d+)\s+(?:is|are)\s+still pending\b',
    re.IGNORECASE,
)
# Prose aside, comma form: "(#N, still pending)".
PROSE_PAREN_PATTERN = re.compile(r'\(#(\d+),\s*still pending\)', re.IGNORECASE)
# Rare heading annotation: "**Feature X → Feature Y** (... still pending ...)".
HEADING_PATTERN = re.compile(
    r'\*\*Feature (\d+)\s*(?:→|->)\s*Feature (\d+)\*\*\s*\(([^)]*)\)'
)
NUMBER_PATTERN = re.compile(r'#(\d+)')


def find_stale_notes(lines, label, implemented_numbers):
    if lines is None:
        return []
    found = []
    for line in lines:
        for m in PROSE_LIST_PATTERN.finditer(line):
            claim = m.group(0)
            for num in NUMBER_PATTERN.findall(m.group(1)):
                if num in implemented_numbers:
                    found.append({"number": num, "claim": claim.strip(), "file": label})
        for m in PROSE_PAREN_PATTERN.finditer(line):
            num = m.group(1)
            if num in implemented_numbers:
                found.append({"number": num, "claim": m.group(0), "file": label})
        m = HEADING_PATTERN.search(line)
        if m:
            a, b, note = m.group(1), m.group(2), m.group(3)
            if "still pending" in note.lower():
                for num in (a, b):
                    if num in implemented_numbers:
                        found.append({
                            "number": num,
                            "claim": f"Feature {a} → Feature {b} ({note.strip()})",
                            "file": label,
                        })
    return found


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--json", action="store_true", help="emit machine-readable JSON instead of the markdown report")
    args = ap.parse_args()

    root = Path(args.repo_root)
    planned_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "features-planned.md"
    implemented_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "features-implemented.md"

    result = {
        "planned_available": False,
        "implemented_available": False,
        "pending_strict": 0,
        "in_progress": 0,
        "implemented": 0,
        "leftover_implemented_in_planned": [],
        "total": None,
        "percent": None,
        "priority_breakdown": {"P0": 0, "P1": 0, "P2": 0, "P3": 0, "none": 0},
        "effort_breakdown": {"S": 0, "M": 0, "L": 0, "none": 0},
        "sdd_ready": 0,
        "pending_rows": [],
        "duplicate_numbers": [],
        "stale_dependency_notes": [],
    }

    planned_lines = read_lines(planned_path)
    implemented_lines = read_lines(implemented_path)

    planned_rows = []
    if planned_lines is not None:
        table = find_table(planned_lines, "## Feature List")
        if table:
            header, rows = table
            result["planned_available"] = True
            planned_rows = [row_dict(header, r) for r in rows]

    implemented_rows = []
    if implemented_lines is not None:
        table = find_table(implemented_lines, "## Feature List")
        if table:
            header, rows = table
            result["implemented_available"] = True
            implemented_rows = [row_dict(header, r) for r in rows]

    implemented_numbers = set()
    for r in implemented_rows:
        n = r.get("#", "").strip()
        if n:
            implemented_numbers.add(n)
    result["implemented"] = len(implemented_rows)

    all_numbers = list(implemented_numbers)
    pending_like = []
    for r in planned_rows:
        n = r.get("#", "").strip()
        if n:
            all_numbers.append(n)
        impl = r.get("Implementation", "").strip()
        if "Pending" in impl and "In Progress" not in impl:
            result["pending_strict"] += 1
            pending_like.append(r)
        elif "In Progress" in impl:
            result["in_progress"] += 1
            pending_like.append(r)
        elif "Implemented" in impl:
            result["leftover_implemented_in_planned"].append(n)

    pending = result["pending_strict"] + result["in_progress"]

    if result["planned_available"] and result["implemented_available"]:
        total = pending + result["implemented"]
        result["total"] = total
        if total == 0:
            result["percent"] = "0% — no features tracked yet"
        else:
            pct = round(100 * result["implemented"] / total, 1)
            result["percent"] = f"{pct}%"

    for r in pending_like:
        priority = r.get("Priority", "").strip()
        key = priority if priority in ("P0", "P1", "P2", "P3") else "none"
        result["priority_breakdown"][key] += 1

        effort = r.get("Effort", "").strip()
        ekey = effort if effort in ("S", "M", "L") else "none"
        result["effort_breakdown"][ekey] += 1

        if "Created" in r.get("SDD Artifacts", ""):
            result["sdd_ready"] += 1

        result["pending_rows"].append({
            "number": r.get("#", "").strip(),
            "name": strip_backticks(r.get("Change name", "")),
            "priority": priority,
            "in_progress": "In Progress" in r.get("Implementation", ""),
        })

    seen = {}
    for n in all_numbers:
        seen[n] = seen.get(n, 0) + 1
    result["duplicate_numbers"] = sorted([n for n, c in seen.items() if c > 1], key=lambda x: int(x) if x.isdigit() else 0)

    stale = []
    stale += find_stale_notes(planned_lines, "features-planned.md", implemented_numbers)
    stale += find_stale_notes(implemented_lines, "features-implemented.md", implemented_numbers)
    # De-duplicate identical (number, claim, file) triples — the same aside
    # sentence can appear verbatim in more than one Dependencies subsection.
    dedup = {}
    for s in stale:
        key = (s["number"], s["claim"], s["file"])
        dedup[key] = s
    result["stale_dependency_notes"] = list(dedup.values())

    if args.json:
        print(json.dumps(result, indent=2))
        return

    render_markdown(result)


def render_markdown(r):
    lines = []
    lines.append("## Feature Tracker Status")
    lines.append("")
    if r["total"] is None:
        missing = []
        if not r["planned_available"]:
            missing.append("JPPhotoManagerWeb/docs/backlog/features-planned.md")
        if not r["implemented_available"]:
            missing.append("JPPhotoManagerWeb/docs/backlog/features-implemented.md")
        lines.append(f"**Total/Progress unavailable** — missing or table-less: {', '.join(missing)}")
        lines.append("")
        if r["implemented_available"]:
            lines.append(f"- ✅ Implemented: {r['implemented']}")
        if r["planned_available"]:
            lines.append(f"- ⬜ Pending: {r['pending_strict']}")
            lines.append(f"- 🔶 In Progress: {r['in_progress']}")
        lines.append("")
    else:
        lines.append("| Metric              | Count |")
        lines.append("| -------------------- | ----- |")
        lines.append(f"| Total features        | {r['total']} |")
        lines.append(f"| ✅ Implemented         | {r['implemented']} |")
        lines.append(f"| ⬜ Pending             | {r['pending_strict']} |")
        lines.append(f"| 🔶 In Progress         | {r['in_progress']} |")
        lines.append(f"| **Progress**           | **{r['percent']}** |")
        lines.append("")

    lines.append("### Pending breakdown by priority")
    lines.append("")
    lines.append("| Tier | Count |")
    lines.append("| ---- | ----- |")
    lines.append(f"| P0 — production-safety gaps | {r['priority_breakdown']['P0']} |")
    lines.append(f"| P1 — high-impact             | {r['priority_breakdown']['P1']} |")
    lines.append(f"| P2 — scalability              | {r['priority_breakdown']['P2']} |")
    lines.append(f"| P3 — operational convenience  | {r['priority_breakdown']['P3']} |")
    lines.append(f"| No explicit tier              | {r['priority_breakdown']['none']} |")
    lines.append("")

    lines.append("### Pending breakdown by effort")
    lines.append("")
    lines.append("| Effort | Count |")
    lines.append("| ------ | ----- |")
    lines.append(f"| S      | {r['effort_breakdown']['S']} |")
    lines.append(f"| M      | {r['effort_breakdown']['M']} |")
    lines.append(f"| L      | {r['effort_breakdown']['L']} |")
    lines.append(f"| No explicit effort | {r['effort_breakdown']['none']} |")
    lines.append("")

    pending_count = r["pending_strict"] + r["in_progress"]
    lines.append("### SDD Artifacts readiness")
    lines.append("")
    lines.append(f"{r['sdd_ready']} of {pending_count} pending features already have SDD artifacts "
                  "created and can be implemented immediately without a propose step.")
    lines.append("")

    lines.append("### Pending features")
    lines.append("")
    if r["pending_rows"]:
        for row in r["pending_rows"]:
            suffix = " — 🔶 In Progress" if row["in_progress"] else ""
            lines.append(f"- #{row['number']} `{row['name']}` ({row['priority'] or 'no tier'}){suffix}")
    else:
        lines.append("_None._")
    lines.append("")

    integrity = []
    for n in r["duplicate_numbers"]:
        integrity.append(f"- ⚠ Duplicate feature number {n}.")
    for d in r["stale_dependency_notes"]:
        integrity.append(f"- ⚠ Stale dependency note: \"{d['claim']}\" asserts feature #{d['number']} is "
                          f"still pending, but it's already implemented. Found in {d['file']}.")
    for n in r["leftover_implemented_in_planned"]:
        integrity.append(f"- ⚠ #{n} still shows ✅ Implemented in features-planned.md — an archive pass "
                          "was interrupted; run features-archive to finish the move.")
    if integrity:
        lines.append("### Data integrity")
        lines.append("")
        lines.extend(integrity)

    print("\n".join(lines))


if __name__ == "__main__":
    main()
