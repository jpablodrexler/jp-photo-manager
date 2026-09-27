#!/usr/bin/env python3
"""
Enumerate feature/fix/doc/skill/chore/release/hotfix branch candidates
for gitflow's "Cleanup branches" action (SKILL.md step 3f, parts 1-2):
`git fetch --prune`, then union the local and remote branch lists per
prefix, noting whether each exists locally, remotely, or both.

This script is deliberately local-git-only — it never calls `gh` or the
GitHub API and never claims a branch is or isn't merged (see SKILL.md's
own guardrail: merge status must always come from a direct PR-merge
check, never from local git state). It only replaces the fiddly, easy-
to-transpose-by-hand text processing of parsing two `git branch --list`
outputs across 7 prefixes and unioning them by name; the skill still
runs the actual merge-status check per branch and does every
confirmation/deletion step itself.

Usage:
    python3 list_merge_candidates.py <repo-root> [--no-fetch] [--json]
"""
import json
import argparse
import subprocess
from pathlib import Path

PREFIXES = ["feature", "fix", "doc", "skill", "chore", "release", "hotfix"]


def run(root, *args):
    out = subprocess.run(["git", *args], cwd=str(root), capture_output=True, text=True)
    return out.stdout.strip()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--no-fetch", action="store_true", help="skip `git fetch origin --prune` (assume already fresh)")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root)

    if not args.no_fetch:
        fetch_out = subprocess.run(["git", "fetch", "origin", "--prune"], cwd=str(root),
                                    capture_output=True, text=True)
        if fetch_out.returncode != 0:
            print(json.dumps({"error": f"git fetch origin --prune failed: {fetch_out.stderr.strip()}"}))
            return

    local_patterns = [f"{p}/*" for p in PREFIXES]
    remote_patterns = [f"origin/{p}/*" for p in PREFIXES]

    local_raw = run(root, "branch", "--list", *local_patterns, "--format=%(refname:short)")
    remote_raw = run(root, "branch", "-r", "--list", *remote_patterns, "--format=%(refname:short)")

    local_branches = {b for b in local_raw.splitlines() if b.strip()}
    remote_branches = {b[len("origin/"):] for b in remote_raw.splitlines() if b.strip() and b.startswith("origin/")}

    current = run(root, "branch", "--show-current")

    all_names = sorted(local_branches | remote_branches)
    candidates = []
    for name in all_names:
        prefix = name.split("/")[0] if "/" in name else None
        if prefix not in PREFIXES:
            continue
        where = []
        if name in local_branches:
            where.append("local")
        if name in remote_branches:
            where.append("remote")
        candidates.append({
            "branch": name,
            "type": prefix,
            "where": where,
            "is_current": name == current,
        })

    result = {"candidates": candidates, "current_branch": current}

    if args.json:
        print(json.dumps(result, indent=2))
        return

    if not candidates:
        print("No feature/fix/doc/skill/chore/release/hotfix branches found (local or remote).")
        return
    for c in candidates:
        marker = " (current)" if c["is_current"] else ""
        print(f"- {c['branch']} [{c['type']}] — {'/'.join(c['where'])}{marker}")


if __name__ == "__main__":
    main()
