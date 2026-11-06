#!/usr/bin/env python3
"""
Triage the open bugs: which ones bug-fix could not start from, which look like duplicates, and which are
not in the recommended fix order.

    python3 triage.py <repo-root> [--threshold 0.5] [--json]

Read-only; it only proposes. Checks over <backlog>/bugs-open.md, its bugs/BUG-NNN.md files and bugs-fixed.md:
  * missing_repro      no steps to reproduce (empty, or still `<unknown — needs repro>`): bug-fix cannot write its failing test
  * missing_expected   no Expected or no Actual line
  * vague_summary      a table summary of fewer than 4 words
  * duplicates         open bugs whose summaries share most of their words (same area, or a very high overlap)
  * regressions        an open bug that looks like one already fixed (the fixed bug is named)
  * not_in_fix_order   an open bug missing from "Recommended fix order"
Exit code is always 0 unless the repo root is bad.
"""
import argparse
import json
import re
import sys
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_kit as bk

STOP = {"the", "a", "an", "of", "on", "in", "to", "and", "or", "is", "are", "when", "after", "before", "with", "for", "not", "no", "it", "its",
        "shows", "show", "page", "bug", "error", "wrong", "fails", "fail", "does", "doesn", "instead"}
UNKNOWN = re.compile(r"<unknown|needs repro|\btbd\b|\bto do\b", re.I)


def words(text):
    return {w for w in re.findall(r"[a-z0-9]{3,}", text.lower()) if w not in STOP}


def similarity(a, b):
    wa, wb = words(a), words(b)
    return len(wa & wb) / len(wa | wb) if wa and wb else 0.0


def field(text, name):
    """The value of a `- **Name:** value` bullet, with its indented continuation lines; '' when absent."""
    m = re.search(rf"^- \*\*{re.escape(name)}:\*\*[ \t]*(.*?)(?=^- \*\*|\Z)", text, re.M | re.S)
    return re.sub(r"\s+", " ", m.group(1)).strip() if m else ""


def usable(value):
    return bool(value) and not UNKNOWN.search(value) and len(value) > 3


def triage(root, threshold):
    cfg = bk.load_config(root)
    backlog = cfg["paths"]["backlog_dir"].strip("/")
    b = Path(root) / backlog
    open_ok, open_rows = bk.table_rows(b / "bugs-open.md", "## Bug List", id_column="Bug ID")
    fixed_ok, fixed_rows = bk.table_rows(b / "bugs-fixed.md", "## Bug List", id_column="Bug ID")
    lines = bk.read_lines(b / "bugs-open.md") or []
    order_text = "\n".join(lines[next((i for i, l in enumerate(lines) if l.strip() == "## Recommended fix order"), len(lines)):])
    active = [r for r in open_rows if r.get("Status", "").strip() in ("", "⬜ Open", "🔶 In Progress")]

    findings = {k: [] for k in ("missing_repro", "missing_expected", "vague_summary", "duplicates", "regressions", "not_in_fix_order")}
    for r in active:
        bid = r["Bug ID"].strip()
        summary = r.get("Summary", "")
        f = b / "bugs" / f"{bid}.md"
        text = f.read_text(encoding="utf-8", errors="replace") if f.is_file() else ""
        if not usable(field(text, "Steps to reproduce")):
            findings["missing_repro"].append(bid)
        if not (usable(field(text, "Expected")) and usable(field(text, "Actual"))):
            findings["missing_expected"].append(bid)
        if len(words(summary)) < 3:
            findings["vague_summary"].append(bid)
        if bid not in order_text:
            findings["not_in_fix_order"].append(bid)

    def score(x, y):
        s = similarity(x.get("Summary", ""), y.get("Summary", ""))
        same_area = x.get("Area", "").strip() == y.get("Area", "").strip()
        return s, (s >= threshold and same_area) or s >= 0.8
    for i, x in enumerate(active):
        for y in active[i + 1:]:
            s, hit = score(x, y)
            if hit:
                findings["duplicates"].append({"bugs": [x["Bug ID"].strip(), y["Bug ID"].strip()], "similarity": round(s, 2)})
        for y in fixed_rows:
            s, hit = score(x, y)
            if hit:
                findings["regressions"].append({"bug": x["Bug ID"].strip(), "looks_like_fixed": y["Bug ID"].strip(), "similarity": round(s, 2)})
    return {"available": {"open": open_ok, "fixed": fixed_ok}, "open_bugs": len(active), "findings": findings}


def render(r):
    f, out = r["findings"], ["## Bug Triage", ""]
    if not r["available"]["open"]:
        return "## Bug Triage\n\nbugs-open.md is missing or has no `## Bug List` table: nothing to triage."
    out.append(f"{r['open_bugs']} active bug(s) checked.")
    sections = [
        ("missing_repro", "No usable steps to reproduce (`bug-fix` cannot write its failing test)", lambda x: f"- {x}"),
        ("missing_expected", "Expected or Actual missing", lambda x: f"- {x}"),
        ("vague_summary", "Summary too vague to rank or search", lambda x: f"- {x}"),
        ("duplicates", "Possible duplicates", lambda x: f"- {x['bugs'][0]} ~ {x['bugs'][1]} (similarity {x['similarity']})"),
        ("regressions", "Looks like a bug already fixed", lambda x: f"- {x['bug']} ~ {x['looks_like_fixed']} (similarity {x['similarity']})"),
        ("not_in_fix_order", "Not in the recommended fix order", lambda x: f"- {x}"),
    ]
    clean = True
    for key, title, fmt in sections:
        if f[key]:
            clean = False
            out += ["", f"### {title}"] + [fmt(x) for x in f[key]]
    if clean:
        out += ["", "Nothing to triage: every active bug is reproducible, distinct and ordered."]
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--threshold", type=float, default=0.5, help="word overlap (0-1) that makes two same-area bugs a duplicate candidate")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if not Path(args.repo_root).is_dir():
        sys.exit(f"{args.repo_root} is not a directory")
    result = triage(Path(args.repo_root), args.threshold)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
