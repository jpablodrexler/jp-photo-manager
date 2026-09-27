#!/usr/bin/env python3
"""
Scan JPPhotoManagerWeb/docs/backlog/features-planned.md and
features-implemented.md for the next free feature number and check a
candidate change-name for a collision — feature-plan/SKILL.md step 3's
mechanical numbering rule (scan both files' `#` columns, take max+1;
numbers are one global sequence, never reused).

Usage:
    python3 next_id.py <repo-root> [--check-name <kebab-case-name>]
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


def collect_numbers_and_names(path):
    p = Path(path)
    if not p.exists():
        return [], []
    lines = p.read_text(encoding="utf-8").splitlines()
    numbers, names = [], []
    in_table = False
    for line in lines:
        if line.strip() == "## Feature List":
            in_table = True
            continue
        if in_table and line.startswith("## "):
            break
        if in_table and line.lstrip().startswith("|"):
            cells = split_row(line)
            if cells and cells[0].strip().isdigit():
                numbers.append(int(cells[0].strip()))
                if len(cells) > 1:
                    names.append(cells[1].strip().strip("`"))
    return numbers, names


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--check-name")
    args = ap.parse_args()

    root = Path(args.repo_root)
    backlog = root / "JPPhotoManagerWeb" / "docs" / "backlog"
    planned = backlog / "features-planned.md"
    implemented = backlog / "features-implemented.md"

    nums_p, names_p = collect_numbers_and_names(planned)
    nums_i, names_i = collect_numbers_and_names(implemented)

    all_numbers = nums_p + nums_i
    next_number = (max(all_numbers) + 1) if all_numbers else 1

    result = {"next_number": next_number}

    if args.check_name:
        existing = set(names_p) | set(names_i)
        result["name_collision"] = args.check_name in existing

    print(json.dumps(result))


if __name__ == "__main__":
    main()
