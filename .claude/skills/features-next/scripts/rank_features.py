#!/usr/bin/env python3
"""
Score and rank pending features from JPPhotoManagerWeb/docs/backlog/features-planned.md per
features-next/SKILL.md's steps 2-4: resolve hard-dependency blocking from
each candidate's live Implementation-column status (never from a
dependency note's own possibly-stale parenthetical), then apply the fixed
point-tier scoring (In Progress > Priority > SDD-ready > order-list
position > no-schema-change) with its tie-breaks.

This script only computes the ranking — it never calls AskUserQuestion or
returns a final decision; the skill still owns presenting the result and
getting the user's mandatory confirmation (see SKILL.md steps 5-6).

Usage:
    python3 rank_features.py <repo-root> [--select <number-or-name>] [--json]
"""
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
    while i < len(lines):
        if lines[i].lstrip().startswith("|"):
            rows.append(split_row(lines[i]))
            i += 1
        elif (lines[i].strip() == "" and i + 1 < len(lines)
              and lines[i + 1].lstrip().startswith("|")):
            i += 1  # a stray blank line inside the table must not truncate it
        else:
            break
    return header, rows


def row_dict(header, row):
    return {h: (row[idx] if idx < len(row) else "") for idx, h in enumerate(header)}


def strip_backticks(s):
    return s.strip().strip("`")


def parse_order_list(lines):
    """Return {number(str): position(1-based)} from '### Recommended implementation order'."""
    positions = {}
    start = None
    for i, line in enumerate(lines):
        if line.strip() == "### Recommended implementation order":
            start = i
            break
    if start is None:
        return positions
    pos = 0
    for line in lines[start + 1:]:
        if line.startswith("## ") or line.startswith("### "):
            break
        m = re.match(r'^\s*-\s*`[^`]+`\s*\(#(\d+)\)', line)
        if m:
            pos += 1
            positions[m.group(1)] = pos
    return positions


def parse_hard_dependencies(lines):
    """Return [(a, b), ...] feature-number pairs from '### Hard implementation dependencies'."""
    deps = []
    start = None
    for i, line in enumerate(lines):
        if line.strip() == "### Hard implementation dependencies":
            start = i
            break
    if start is None:
        return deps
    pattern = re.compile(r'\*\*Feature (\d+)\s*(?:→|->)\s*Feature (\d+)\*\*')
    for line in lines[start + 1:]:
        if line.startswith("## ") or (line.startswith("### ") and "Hard implementation" not in line):
            break
        m = pattern.search(line)
        if m:
            deps.append((m.group(1), m.group(2)))
    return deps


def brief_file_path(row):
    """Repo-relative path of the feature's full brief: JPPhotoManagerWeb/docs/backlog/features/NNN-<name>.md."""
    n = row.get("#", "").strip()
    name = strip_backticks(row.get("Change name", ""))
    if not n.isdigit() or not name:
        return ""
    return f"JPPhotoManagerWeb/docs/backlog/features/{int(n):03d}-{name}.md"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--select", help="a feature number or change-name to resolve directly, skipping scoring")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root)
    planned_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "features-planned.md"
    implemented_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "features-implemented.md"

    if not planned_path.exists():
        print(json.dumps({"error": "features-planned.md not found"}))
        return

    planned_lines = planned_path.read_text(encoding="utf-8").splitlines()
    implemented_lines = implemented_path.read_text(encoding="utf-8").splitlines() if implemented_path.exists() else []

    table = find_table(planned_lines, "## Feature List")
    if not table:
        print(json.dumps({"error": "no ## Feature List table in features-planned.md"}))
        return
    header, rows = table
    all_rows = [row_dict(header, r) for r in rows]

    implemented_table = find_table(implemented_lines, "## Feature List") if implemented_lines else None
    implemented_numbers = set()
    if implemented_table:
        _, irows = implemented_table
        for r in irows:
            n = r[0].strip() if r else ""
            if n:
                implemented_numbers.add(n)

    pending_numbers = set()
    candidates = []  # list, not a dict — a duplicated feature number must not silently shadow its twin
    duplicate_numbers = set()
    seen_numbers = set()
    for r in all_rows:
        impl = r.get("Implementation", "")
        n = r.get("#", "").strip()
        if "Pending" in impl or "In Progress" in impl:
            pending_numbers.add(n)
            if n in seen_numbers:
                duplicate_numbers.add(n)
            seen_numbers.add(n)
            candidates.append({
                "number": n,
                "name": strip_backticks(r.get("Change name", "")),
                "priority": r.get("Priority", "").strip(),
                "schema_change": r.get("Schema Change", "").strip(),
                "effort": r.get("Effort", "").strip(),
                "area": r.get("Area", "").strip(),
                "artifacts_ready": "Created" in r.get("SDD Artifacts", ""),
                "in_progress": "In Progress" in impl,
                "brief": r.get("Summary", "").strip(),
                "brief_file": brief_file_path(r),
            })

    if not candidates:
        print(json.dumps({"error": "no pending or in-progress features in the backlog"}))
        return

    missing_briefs = [c["brief_file"] for c in candidates if not (root / c["brief_file"]).is_file()]

    candidates_by_number = {}
    for c in candidates:
        candidates_by_number.setdefault(c["number"], []).append(c)

    deps = parse_hard_dependencies(planned_lines)
    blocked = set()
    blocked_reason = {}
    for a, b in deps:
        if a not in candidates_by_number:
            continue
        if b in pending_numbers:
            blocked.add(a)
            blocked_reason[a] = b
        # else: b is implemented (or absent from the pending table) -> resolved, not blocking

    order_positions = parse_order_list(planned_lines)

    def score(c):
        s = 0
        if c["in_progress"]:
            s += 1000
        s += {"P0": 400, "P1": 300, "P2": 200, "P3": 100}.get(c["priority"], 50)
        if c["artifacts_ready"]:
            s += 20
        pos = order_positions.get(c["number"])
        if pos is not None:
            s += 10
        if c["schema_change"] == "No":
            s += 5
        return s

    if args.select:
        sel = args.select.strip().lstrip("#")
        matches = candidates_by_number.get(sel, [])
        if not matches:
            matches = [c for c in candidates if c["name"] == sel]
        if not matches:
            print(json.dumps({"error": f"'{args.select}' does not match a pending/in-progress feature"}))
            return
        if len(matches) > 1:
            print(json.dumps({
                "error": f"'{args.select}' is ambiguous — {len(matches)} rows share this number "
                         "(a duplicate feature number in the backlog; fix the backlog or select by exact name)",
                "candidates": matches,
            }, indent=2))
            return
        match = matches[0]
        result = {
            "selected": match,
            "blocked": match["number"] in blocked,
            "blocked_on": blocked_reason.get(match["number"]),
        }
        if match["brief_file"] in missing_briefs:
            result["warning"] = f"Brief file {match['brief_file']} is missing — run features-status for the full integrity report."
        print(json.dumps(result, indent=2) if args.json else render_selected(result))
        return

    ranked = []
    for c in candidates:
        n = c["number"]
        if n in blocked:
            continue
        entry = dict(c)
        entry["score"] = score(c)
        entry["order_position"] = order_positions.get(n)
        ranked.append(entry)

    if not ranked:
        blocked_list = [{"number": n, "blocked_on": blocked_reason[n]} for n in blocked]
        print(json.dumps({"error": "every pending feature is blocked", "blocked": blocked_list}, indent=2))
        return

    ranked.sort(key=lambda c: (
        -c["score"],
        c["order_position"] if c["order_position"] is not None else 10 ** 9,
        int(c["number"]) if c["number"].isdigit() else 10 ** 9,
    ))

    blocked_out = []
    for n in blocked:
        names = ", ".join(c["name"] for c in candidates_by_number.get(n, []))
        blocked_out.append({"number": n, "blocked_on": blocked_reason[n], "name": names})

    result = {
        "top": ranked[0],
        "runners_up": ranked[1:4],
        "blocked": blocked_out,
    }
    warnings = []
    if duplicate_numbers:
        warnings.append(f"Duplicate feature number(s) in features-planned.md: {', '.join(sorted(duplicate_numbers))} — run features-status for the full integrity report.")
    if missing_briefs:
        warnings.append(f"Brief file(s) missing: {', '.join(missing_briefs)} — run features-status for the full integrity report.")
    if warnings:
        result["warning"] = " ".join(warnings)

    if args.json:
        print(json.dumps(result, indent=2))
    else:
        render_ranked(result)


def reason_bullets(c):
    bullets = []
    if c["in_progress"]:
        bullets.append("🔶 Already in progress — feature-development started this one; resuming beats starting something new")
    tier_label = {"P0": "P0 production-safety gap", "P1": "P1 high-impact", "P2": "P2 scalability", "P3": "P3 operational convenience"}.get(c["priority"], "no explicit priority tier")
    bullets.append(tier_label)
    if c["artifacts_ready"]:
        bullets.append("SDD artifacts already created — no propose step needed")
    if c["order_position"]:
        bullets.append(f"position {c['order_position']} in the Recommended implementation order")
    if c["schema_change"] == "No":
        bullets.append("no schema change — simpler to deliver")
    return bullets


def render_selected(result):
    m = result["selected"]
    lines = [f"#{m['number']} `{m['name']}`", m["brief"], f"Full brief: {m['brief_file']}", ""]
    if result.get("warning"):
        lines.append(f"⚠ {result['warning']}")
    if result["blocked"]:
        lines.append(f"⚠ BLOCKED on feature {result['blocked_on']}, which is still pending.")
    return "\n".join(lines)


def render_ranked(result):
    lines = []
    top = result["top"]
    lines.append("## Recommended Next Feature")
    lines.append("")
    if result.get("warning"):
        lines.append(f"⚠ {result['warning']}")
        lines.append("")
    lines.append(f"**#{top['number']} `{top['name']}`**")
    lines.append(top["brief"])
    lines.append(f"Full brief: {top['brief_file']}")
    lines.append("")
    lines.append(f"**Priority:** {top['priority']}  **Effort:** {top['effort']}  **Area:** {top['area']}  "
                  f"**Schema change:** {top['schema_change']}")
    lines.append("")
    lines.append("**Why this feature:**")
    for b in reason_bullets(top):
        lines.append(f"- {b}")
    lines.append("")
    lines.append(f"**SDD Artifacts:** {'✅ Created' if top['artifacts_ready'] else '⬜ Not yet created'}")
    lines.append("")

    if result["runners_up"]:
        lines.append("### Runners-up")
        lines.append("")
        for r in result["runners_up"]:
            lines.append(f"- #{r['number']} `{r['name']}` (score {r['score']}) — {reason_bullets(r)[0] if reason_bullets(r) else ''}")
        lines.append("")

    if result["blocked"]:
        lines.append("### Blocked (excluded from ranking)")
        lines.append("")
        for b in result["blocked"]:
            lines.append(f"- #{b['number']} `{b['name']}` — blocked on feature {b['blocked_on']}")

    print("\n".join(lines))


if __name__ == "__main__":
    main()
