#!/usr/bin/env python3
"""
Scan <decisions> (the `decisions` folder next to the backlog folder) for the next free ADR number —
decision-record/SKILL.md step 2's mechanical numbering rule (scan
filenames for the 4-digit `NNNN-` prefix, take max+1; one global
sequence, never reused even for a later-superseded record).

Usage:
    python3 next_id.py <repo-root>
"""
import re
import sys
import json
import argparse
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_kit as bk


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    args = ap.parse_args()

    root = Path(args.repo_root)
    decisions_dir = (root / bk.load_config(root)["paths"]["backlog_dir"].strip("/")).parent / "decisions"
    nums = []
    if decisions_dir.exists():
        for f in decisions_dir.glob("*.md"):
            m = re.match(r'^(\d{4})-', f.name)
            if m:
                nums.append(int(m.group(1)))

    next_n = (max(nums) + 1) if nums else 1
    print(json.dumps({"next_number": f"{next_n:04d}", "directory_exists": decisions_dir.exists()}))


if __name__ == "__main__":
    main()
