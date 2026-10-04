#!/usr/bin/env python3
"""
Scan JPPhotoManagerWeb/docs/backlog/bugs-open.md, bugs-fixed.md (their
`Bug ID` cells, plain or linked as `[BUG-NNN](bugs/BUG-NNN.md)`) and the
filenames in JPPhotoManagerWeb/docs/backlog/bugs/ for the next free BUG-NNN
id — bug-report/SKILL.md step 3's mechanical numbering rule (take max+1,
zero-padded to 3 digits; one global sequence, never reused).

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


def collect_filenames(folder):
    d = Path(folder)
    if not d.is_dir():
        return []
    out = []
    for f in d.glob("BUG-*.md"):
        m = re.match(r'BUG-(\d+)\.md$', f.name)
        if m:
            out.append(int(m.group(1)))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    args = ap.parse_args()

    root = Path(args.repo_root)
    backlog = root / "JPPhotoManagerWeb" / "docs" / "backlog"
    nums = (collect_numbers(backlog / "bugs-open.md")
            + collect_numbers(backlog / "bugs-fixed.md")
            + collect_filenames(backlog / "bugs"))
    next_n = (max(nums) + 1) if nums else 1
    print(json.dumps({"next_id": f"BUG-{next_n:03d}"}))


if __name__ == "__main__":
    main()
