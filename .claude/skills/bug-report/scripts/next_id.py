#!/usr/bin/env python3
"""
Scan JPPhotoManagerWeb/docs/backlog/bugs-open.md and bugs-fixed.md for the
next free BUG-NNN id — bug-report/SKILL.md step 3's mechanical numbering
rule (scan both files' `Bug ID` columns, take max+1, zero-padded to 3
digits; one global sequence, never reused).

Usage:
    python3 next_id.py <repo-root>
"""
import re
import json
import argparse
from pathlib import Path


def collect_numbers(path):
    p = Path(path)
    if not p.exists():
        return []
    text = p.read_text(encoding="utf-8")
    return [int(m) for m in re.findall(r'BUG-(\d+)', text)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    args = ap.parse_args()

    root = Path(args.repo_root)
    backlog = root / "JPPhotoManagerWeb" / "docs" / "backlog"
    nums = (collect_numbers(backlog / "bugs-open.md")
            + collect_numbers(backlog / "bugs-fixed.md"))
    next_n = (max(nums) + 1) if nums else 1
    print(json.dumps({"next_id": f"BUG-{next_n:03d}"}))


if __name__ == "__main__":
    main()
