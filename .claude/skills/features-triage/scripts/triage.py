#!/usr/bin/env python3
"""
Triage the planned features: which ones are not ready to be built, which are too big, which look
like duplicates, and which dependencies have been resolved.

    python3 triage.py <repo-root> [--threshold 0.6] [--json]

Read-only; it only proposes. Checks over the pending and in-progress rows of <backlog>/features-planned.md,
their briefs in <backlog>/features/, features-implemented.md and the git branches:
  * unestimated        Effort empty, or not a story point value (1, 2, 3, 5, 8, 13)
  * legacy_size        Effort still a t-shirt size (S/M/L/XL) from an unmigrated backlog
  * oversized          Effort 8 or 13: feature-plan offers to split these
  * missing_story      the brief is not written as a user story ("**As a** ... **I want** ...")
  * missing_acceptance the brief has no acceptance criteria (AC1: Given/When/Then)
  * now_unblocked      a hard dependency whose prerequisite is implemented: the feature can start
  * dangling_dependency a hard dependency whose prerequisite is in neither table
  * stale_in_progress  marked In Progress but no feature/<name> branch exists locally or on origin
  * duplicates         pending features whose summaries share most of their words
  * not_in_order       a pending feature missing from "Recommended implementation order"
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
import gitread

POINTS = {"1", "2", "3", "5", "8", "13"}
LEGACY = {"S", "M", "L", "XL"}
STOP = {"the", "a", "an", "of", "on", "in", "to", "and", "or", "is", "are", "when", "for", "with", "can", "that", "this", "so", "as", "by", "from"}
STORY_RE = re.compile(r"^\*\*As an?\*\*", re.M)
AC_RE = re.compile(r"^\s*[-*]\s*AC\d+\s*:", re.M)
ORDER_ENTRY = r"^\s*-\s+`[^`]+`\s*\(#(\d+)\)"


def words(text):
    return {w for w in re.findall(r"[a-z0-9]{3,}", text.lower()) if w not in STOP}


def similarity(a, b):
    wa, wb = words(a), words(b)
    return len(wa & wb) / len(wa | wb) if wa and wb else 0.0


def branch_names(root):
    """Short names of local and remote branches, or None outside a git repository."""
    try:
        out = gitread.git(root, "for-each-ref", "--format=%(refname:short)", "refs/heads", "refs/remotes")
    except gitread.GitError:
        return None
    return {l.strip() for l in out.splitlines() if l.strip()}


def triage(root, threshold):
    backlog = bk.load_config(root)["paths"]["backlog_dir"].strip("/")
    b = Path(root) / backlog
    plan_ok, planned = bk.table_rows(b / "features-planned.md", "## Feature List")
    impl_ok, implemented = bk.table_rows(b / "features-implemented.md", "## Feature List")
    plan_lines = bk.read_lines(b / "features-planned.md") or []
    implemented_nums = {bk.strip_backticks(r.get("#", "")) for r in implemented}
    planned_nums = {bk.strip_backticks(r.get("#", "")) for r in planned}
    order = bk.order_list_positions(plan_lines, "### Recommended implementation order", ORDER_ENTRY)
    branches = branch_names(root)

    active = [r for r in planned if "Implemented" not in r.get("Implementation", "")]
    findings = {k: [] for k in ("unestimated", "legacy_size", "oversized", "missing_story", "missing_acceptance", "now_unblocked",
                                "dangling_dependency", "stale_in_progress", "duplicates", "not_in_order")}
    ident = lambda r: {"number": bk.strip_backticks(r.get("#", "")), "name": bk.strip_backticks(r.get("Change name", ""))}

    for r in active:
        i = ident(r)
        effort = r.get("Effort", "").strip().strip("`")
        if effort in LEGACY:
            findings["legacy_size"].append({**i, "effort": effort})
        elif effort not in POINTS:
            findings["unestimated"].append(i)
        elif effort in ("8", "13"):
            findings["oversized"].append({**i, "effort": effort})
        brief = next(iter(sorted((b / "features").glob(f"{int(i['number']):03d}-*.md"))), None) if i["number"].isdigit() else None
        text = brief.read_text(encoding="utf-8", errors="replace") if brief else ""
        if brief and not STORY_RE.search(text):
            findings["missing_story"].append(i)
        if brief and not AC_RE.search(text):
            findings["missing_acceptance"].append(i)
        if "In Progress" in r.get("Implementation", ""):
            if branches is not None and not any(n.endswith(f"feature/{i['name']}") for n in branches):
                findings["stale_in_progress"].append(i)
        elif i["number"] not in order:
            findings["not_in_order"].append(i)

    active_nums = {ident(r)["number"]: ident(r) for r in active}
    for dependent, prerequisite in bk.hard_dependencies(plan_lines):
        if dependent not in active_nums:
            continue
        if prerequisite in implemented_nums:
            findings["now_unblocked"].append({**active_nums[dependent], "prerequisite": prerequisite})
        elif prerequisite not in planned_nums:
            findings["dangling_dependency"].append({**active_nums[dependent], "prerequisite": prerequisite})

    rows = [r for r in active if "In Progress" not in r.get("Implementation", "")]
    for n, x in enumerate(rows):
        for y in rows[n + 1:]:
            s = similarity(x.get("Summary", ""), y.get("Summary", ""))
            same_area = x.get("Area", "").strip() == y.get("Area", "").strip()
            if (s >= threshold and same_area) or s >= 0.8:
                findings["duplicates"].append({"features": [ident(x)["number"], ident(y)["number"]], "similarity": round(s, 2)})
    return {"available": {"planned": plan_ok, "implemented": impl_ok}, "branches_checked": branches is not None,
            "active_features": len(active), "findings": findings}


def render(r):
    if not r["available"]["planned"]:
        return "## Feature Triage\n\nfeatures-planned.md is missing or has no `## Feature List` table: nothing to triage."
    f = r["findings"]
    label = lambda x: f"- #{x['number']} `{x['name']}`"
    sections = [
        ("unestimated", "No story points (`feature-plan` asks for them)", label),
        ("legacy_size", "Still a t-shirt size", lambda x: f"{label(x)} ({x['effort']})"),
        ("oversized", "Large: consider splitting into stories of 5 or less", lambda x: f"{label(x)} ({x['effort']} points)"),
        ("missing_story", "Brief is not a user story", label),
        ("missing_acceptance", "Brief has no acceptance criteria", label),
        ("now_unblocked", "Prerequisite implemented: ready to start", lambda x: f"{label(x)} (was blocked on #{x['prerequisite']})"),
        ("dangling_dependency", "Blocked on a feature that is in neither table", lambda x: f"{label(x)} (needs #{x['prerequisite']})"),
        ("stale_in_progress", "In Progress but no feature branch found", label),
        ("duplicates", "Possible duplicates", lambda x: f"- #{x['features'][0]} ~ #{x['features'][1]} (similarity {x['similarity']})"),
        ("not_in_order", "Not in the recommended implementation order", label),
    ]
    out = ["## Feature Triage", "", f"{r['active_features']} pending or in-progress feature(s) checked."]
    if not r["branches_checked"]:
        out.append("Branches were not checked (not a git repository).")
    clean = True
    for key, title, fmt in sections:
        if f[key]:
            clean = False
            out += ["", f"### {title}"] + [fmt(x) for x in f[key]]
    if clean:
        out += ["", "Nothing to triage: every feature is estimated, specified, distinct and ordered."]
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--threshold", type=float, default=0.6, help="word overlap (0-1) that makes two same-area features a duplicate candidate")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if not Path(args.repo_root).is_dir():
        sys.exit(f"{args.repo_root} is not a directory")
    result = triage(Path(args.repo_root), args.threshold)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
