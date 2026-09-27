#!/usr/bin/env python3
"""
Score and rank actionable bugs from JPPhotoManagerWeb/docs/backlog/bugs-open.md per
bugs-next/SKILL.md's steps 2-3: fixed point-tier scoring (In Progress >
Severity > deployed/both environment > fix-order position > no schema
change) with its tie-breaks.

This script only computes the ranking — it never calls AskUserQuestion or
returns a final decision; the skill still owns presenting the result and
getting the user's mandatory confirmation (see SKILL.md steps 4-5).

Usage:
    python3 rank_bugs.py <repo-root> [--select BUG-NNN] [--json]
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
    while i < len(lines) and lines[i].lstrip().startswith("|"):
        rows.append(split_row(lines[i]))
        i += 1
    return header, rows


def row_dict(header, row):
    return {h: (row[idx] if idx < len(row) else "") for idx, h in enumerate(header)}


def parse_fix_order(lines):
    """Return {BUG-NNN: position(1-based)} from '## Recommended fix order'."""
    positions = {}
    start = None
    for i, line in enumerate(lines):
        if line.strip() == "## Recommended fix order":
            start = i
            break
    if start is None:
        return positions
    pos = 0
    for line in lines[start + 1:]:
        if line.startswith("## "):
            break
        m = re.match(r'^\s*-\s*(BUG-\d+)', line)
        if m:
            pos += 1
            positions[m.group(1)] = pos
    return positions


def find_detail_block(lines, bug_id):
    """Return the raw text of a bug's '### BUG-NNN — ...' block, or ''."""
    start = None
    for i, line in enumerate(lines):
        if line.startswith(f"### {bug_id} "):
            start = i
            break
    if start is None:
        return ""
    end = len(lines)
    for i in range(start + 1, len(lines)):
        if lines[i].startswith("### ") or lines[i].startswith("## "):
            end = i
            break
    return "\n".join(lines[start:end])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--select", help="a Bug ID to resolve directly, skipping scoring")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root)
    open_path = root / "JPPhotoManagerWeb" / "docs" / "backlog" / "bugs-open.md"
    if not open_path.exists():
        print(json.dumps({"error": "bugs-open.md not found"}))
        return

    lines = open_path.read_text(encoding="utf-8").splitlines()
    table = find_table(lines, "## Bug List")
    if not table:
        print(json.dumps({"error": "no ## Bug List table in bugs-open.md"}))
        return
    header, rows = table
    all_rows = [row_dict(header, r) for r in rows]

    fix_order = parse_fix_order(lines)

    candidates = []
    duplicate_ids = set()
    seen = set()
    for r in all_rows:
        status = r.get("Status", "")
        bug_id = r.get("Bug ID", "").strip()
        if "Open" in status or "In Progress" in status:
            if bug_id in seen:
                duplicate_ids.add(bug_id)
            seen.add(bug_id)
            detail = find_detail_block(lines, bug_id)
            schema = bool(re.search(r'Flyway|migration|JPA entity', detail, re.IGNORECASE))
            candidates.append({
                "id": bug_id,
                "severity": r.get("Severity", "").strip(),
                "area": r.get("Area", "").strip(),
                "environment": r.get("Environment", "").strip(),
                "in_progress": "In Progress" in status,
                "schema": schema,
                "summary": r.get("Summary", "").strip(),
            })

    if not candidates:
        print(json.dumps({"error": "no open or in-progress bugs in the backlog"}))
        return

    def score(c):
        s = 0
        if c["in_progress"]:
            s += 1000
        s += {"S1": 400, "S2": 300, "S3": 200, "S4": 100}.get(c["severity"], 50)
        if c["environment"] in ("deployed", "both"):
            s += 20
        if fix_order.get(c["id"]) is not None:
            s += 10
        if not c["schema"]:
            s += 5
        return s

    if args.select:
        sel = args.select.strip().upper()
        matches = [c for c in candidates if c["id"] == sel]
        if not matches:
            print(json.dumps({"error": f"'{args.select}' does not match an Open/In Progress bug"}))
            return
        match = matches[0]
        result = {"selected": match, "duplicate": sel in duplicate_ids}
        print(json.dumps(result, indent=2) if args.json else render_selected(result))
        return

    ranked = []
    for c in candidates:
        entry = dict(c)
        entry["score"] = score(c)
        entry["order_position"] = fix_order.get(c["id"])
        ranked.append(entry)

    ranked.sort(key=lambda c: (
        -c["score"],
        c["order_position"] if c["order_position"] is not None else 10 ** 9,
        int(c["id"].split("-")[1]) if "-" in c["id"] and c["id"].split("-")[1].isdigit() else 10 ** 9,
    ))

    result = {"top": ranked[0], "runners_up": ranked[1:4]}
    if duplicate_ids:
        result["warning"] = f"Duplicate Bug ID(s) in bugs-open.md: {', '.join(sorted(duplicate_ids))} — run bugs-status for the full integrity report."

    if args.json:
        print(json.dumps(result, indent=2))
    else:
        render_ranked(result)


def reason_bullets(c):
    bullets = []
    if c["in_progress"]:
        bullets.append("🔶 Already in progress — a bug-fix run started this; resuming beats starting a new one")
    tier_label = {"S1": "S1 — blocker", "S2": "S2 — major", "S3": "S3 — minor", "S4": "S4 — trivial"}.get(c["severity"], "no explicit severity")
    reason = tier_label
    if c["environment"] in ("deployed", "both"):
        reason += f", reproduces on {c['environment']}"
    bullets.append(reason)
    if c["order_position"]:
        bullets.append(f"position {c['order_position']} in the Recommended fix order")
    if not c["schema"]:
        bullets.append("no schema fix needed")
    return bullets


def render_selected(result):
    m = result["selected"]
    lines = [f"{m['id']} — {m['summary']}"]
    if result["duplicate"]:
        lines.append(f"⚠ {m['id']} appears more than once in bugs-open.md's Bug List table.")
    return "\n".join(lines)


def render_ranked(result):
    lines = []
    top = result["top"]
    lines.append("## Recommended Next Bug")
    lines.append("")
    if result.get("warning"):
        lines.append(f"⚠ {result['warning']}")
        lines.append("")
    lines.append(f"**{top['id']}**")
    lines.append(top["summary"])
    lines.append("")
    lines.append(f"**Severity:** {top['severity']}  **Area:** {top['area']}  **Environment:** {top['environment']}  "
                  f"**Schema fix:** {'yes' if top['schema'] else 'no'}")
    lines.append("")
    lines.append("**Why this bug:**")
    for b in reason_bullets(top):
        lines.append(f"- {b}")
    lines.append("")

    if result["runners_up"]:
        lines.append("### Runners-up")
        lines.append("")
        for r in result["runners_up"]:
            lines.append(f"- {r['id']} (score {r['score']}) — {reason_bullets(r)[0]}")

    print("\n".join(lines))


if __name__ == "__main__":
    main()
