#!/usr/bin/env python3
"""
Scan JPPhotoManagerWeb/docs/decisions/ for the next free ADR number —
decision-record/SKILL.md step 2's mechanical numbering rule (scan
filenames for the 4-digit `NNNN-` prefix, take max+1; one global
sequence, never reused even for a later-superseded record).

Usage:
    python3 next_id.py <repo-root>
"""
import re
import json
import argparse
from pathlib import Path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    args = ap.parse_args()

    decisions_dir = Path(args.repo_root) / "JPPhotoManagerWeb" / "docs" / "decisions"
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
