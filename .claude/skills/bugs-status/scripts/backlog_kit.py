#!/usr/bin/env python3
# VENDORED from lib/backlog_kit.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
Shared helpers for backlog-kit's skill scripts: config loading, markdown-table
parsing, and path resolution. Pure stdlib (Python 3.11+ for tomllib).

Every script takes a <repo-root>; the project's settings live in an optional
`<repo-root>/.backlog-kit.toml`. Anything missing from it falls back to the
defaults below, so a repo with no config file works out of the box.
"""
import copy
import json
import re
import sys
from pathlib import Path, PurePosixPath

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover - Python < 3.11
    tomllib = None

CONFIG_FILE = ".backlog-kit.toml"

# Scripts print status emoji (✅ 🔶). Windows consoles default stdout to a legacy
# codepage (cp1252) and would crash with UnicodeEncodeError, so force UTF-8.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")
    except (AttributeError, ValueError):  # captured / replaced streams
        pass

DEFAULTS = {
    # backlog_dir is the one folder setting. The decisions, explorations and reports folders sit next to
    # it (decision D19): <docs>/decisions, <docs>/explorations, <docs>/reports, derived by _derive_paths.
    "paths": {
        "backlog_dir": "docs/backlog",
    },
    # Devices the viewport check of feature-development / bug-fix verifies, as "Name WIDTHxHEIGHT"
    # (CSS pixels). The skills read this table themselves; no script uses it.
    "viewports": {
        "phones": ["Galaxy S23 Ultra 384x824"],
        "desktop": "1920x1080",
    },
}


def _merge(base, override):
    out = copy.deepcopy(base)
    for k, v in (override or {}).items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = _merge(out[k], v)
        else:
            out[k] = v
    return out


def _derive_paths(cfg):
    """The auxiliary folders sit next to the backlog folder: <docs>/decisions, /explorations, /reports."""
    parent = str(PurePosixPath(norm_path(cfg["paths"]["backlog_dir"]).strip("/")).parent)
    docs = "" if parent == "." else parent + "/"
    cfg["paths"].update(decisions_dir=docs + "decisions", explorations_dir=docs + "explorations",
                        reports_dir=docs + "reports")
    return cfg


def load_config(root):
    """Defaults merged with <root>/.backlog-kit.toml (if present)."""
    p = Path(root) / CONFIG_FILE
    if not p.is_file():
        return _derive_paths(copy.deepcopy(DEFAULTS))
    if tomllib is None:
        raise SystemExit(f"{CONFIG_FILE} needs Python 3.11+ (tomllib) to be read.")
    with p.open("rb") as fh:
        return _derive_paths(_merge(DEFAULTS, tomllib.load(fh)))


class Paths:
    """Resolved locations of every backlog-kit file, from a config."""

    def __init__(self, root, cfg):
        self.root = Path(root)
        self.backlog = self.root / cfg["paths"]["backlog_dir"]
        self.rel_backlog = cfg["paths"]["backlog_dir"].strip("/")
        self.features_planned = self.backlog / "features-planned.md"
        self.features_implemented = self.backlog / "features-implemented.md"
        self.features_dir = self.backlog / "features"
        self.bugs_open = self.backlog / "bugs-open.md"
        self.bugs_fixed = self.backlog / "bugs-fixed.md"
        self.bugs_dir = self.backlog / "bugs"
        self.decisions = self.root / cfg["paths"]["decisions_dir"]
        self.explorations = self.root / cfg["paths"]["explorations_dir"]
        self.reports = self.root / cfg["paths"]["reports_dir"]


# --- markdown tables ---------------------------------------------------------

def split_row(line):
    """Split a table row on unescaped pipes; unescape `\\|` back to `|`."""
    parts = re.split(r"(?<!\\)\|", line)
    if parts and parts[0].strip() == "":
        parts = parts[1:]
    if parts and parts[-1].strip() == "":
        parts = parts[:-1]
    return [p.strip().replace("\\|", "|") for p in parts]


def find_table(lines, heading):
    """Find the first table under `heading`; return (header, rows) or None.

    A single stray blank line inside the table does not end it (an easy
    hand-edit slip), but two blank lines or any other text does.
    """
    start = next((i for i, ln in enumerate(lines) if ln.strip() == heading), None)
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
    if i < len(lines) and re.match(r"^\|?[\s:-]+\|", lines[i]):
        i += 1
    rows = []
    while i < len(lines):
        if lines[i].lstrip().startswith("|"):
            rows.append(split_row(lines[i]))
            i += 1
        elif (lines[i].strip() == "" and i + 1 < len(lines)
              and lines[i + 1].lstrip().startswith("|")):
            i += 1
        else:
            break
    return header, rows


def row_dict(header, row, id_column=None):
    """Header-keyed dict for a row; unwraps a `[text](link)` in `id_column`."""
    d = {h: (row[i] if i < len(row) else "") for i, h in enumerate(header)}
    if id_column and id_column in d:
        m = re.match(r"^\[([^\]]+)\]\([^)]*\)$", d[id_column].strip())
        if m:
            d[id_column] = m.group(1)
    return d


def read_lines(path):
    """Lines of `path`, or None when it is missing. A byte that is not valid UTF-8
    reads as U+FFFD instead of raising, so one badly saved row cannot stop a script;
    `invalid_utf8_lines` names such lines so a report can flag them."""
    p = Path(path)
    return p.read_text(encoding="utf-8", errors="replace").splitlines() if p.is_file() else None


def invalid_utf8_lines(path):
    """1-based numbers of the lines of `path` whose bytes are not valid UTF-8, typically
    a file saved in the system codepage instead of UTF-8. Empty for a missing file."""
    p = Path(path)
    if not p.is_file():
        return []
    bad = []
    for n, raw in enumerate(p.read_bytes().split(b"\n"), 1):
        try:
            raw.decode("utf-8")
        except UnicodeDecodeError:
            bad.append(n)
    return bad


def strip_backticks(s):
    return s.strip().strip("`")


def table_rows(path, heading, id_column=None):
    """(available, [row_dict, ...]) for the table under `heading` in `path`."""
    lines = read_lines(path)
    if lines is None:
        return False, []
    table = find_table(lines, heading)
    if not table:
        return False, []
    header, rows = table
    return True, [row_dict(header, r, id_column) for r in rows]


# --- scoring -----------------------------------------------------------------

# --- ranking, shared by the batch runners' --all (features-next / bugs-next keep their own copy,
# which is the original projects' scripts; tests/test_batch.py checks the two agree) ----------------

IN_PROGRESS_BONUS, ENVIRONMENT_BONUS, SPEC_BONUS, ORDER_BONUS, NO_SCHEMA_BONUS = 1000, 20, 20, 10, 5
PRIORITY_POINTS = {"P0": 400, "P1": 300, "P2": 200, "P3": 100}
SEVERITY_POINTS = {"S1": 400, "S2": 300, "S3": 200, "S4": 100}
UNTIERED = 50


def score_feature(priority, schema_change, in_progress, artifacts_ready, in_order):
    s = IN_PROGRESS_BONUS if in_progress else 0
    s += PRIORITY_POINTS.get(priority, UNTIERED)
    s += SPEC_BONUS if artifacts_ready else 0
    s += ORDER_BONUS if in_order else 0
    s += NO_SCHEMA_BONUS if schema_change == "No" else 0
    return s


def score_bug(severity, environment, in_progress, in_order, schema):
    s = IN_PROGRESS_BONUS if in_progress else 0
    s += SEVERITY_POINTS.get(severity, UNTIERED)
    s += ENVIRONMENT_BONUS if environment not in ("", "local") else 0
    s += ORDER_BONUS if in_order else 0
    s += NO_SCHEMA_BONUS if not schema else 0
    return s


def id_number(identifier):
    """Numeric tail of `BUG-007` / `7` for ordering (so BUG-1000 sorts after BUG-999)."""
    tail = str(identifier).rsplit("-", 1)[-1]
    return int(tail) if tail.isdigit() else 10 ** 9


def order_list_positions(lines, heading, entry_regex):
    """{captured-id: 1-based position} from a bullet list under `heading`.

    `heading` is the exact line (e.g. `### Recommended implementation order`);
    `entry_regex` must capture the id in group 1.
    """
    positions = {}
    start = next((i for i, ln in enumerate(lines) if ln.strip() == heading), None)
    if start is None:
        return positions
    level = len(heading) - len(heading.lstrip("#"))
    pos = 0
    for ln in lines[start + 1:]:
        m_head = re.match(r"^(#+) ", ln)
        if m_head and len(m_head.group(1)) <= level:
            break
        # A numbered list ("1. `x` (#2)") is as valid an order list as a bulleted one.
        m = re.match(entry_regex, re.sub(r"^(\s*)\d+[.)]\s", r"\1- ", ln))
        if m:
            pos += 1
            positions[m.group(1)] = pos
    return positions


DEPENDENCY_RE = re.compile(r"\*\*Feature (\d+)\s*(?:→|->)\s*Feature (\d+)\*\*")


def hard_dependencies(lines):
    """[(dependent, prerequisite), ...] from `### Hard implementation dependencies`.

    `**Feature A → Feature B**` means A is blocked until B ships; B is the prerequisite.
    """
    start = next((i for i, ln in enumerate(lines)
                  if ln.strip() == "### Hard implementation dependencies"), None)
    if start is None:
        return []
    deps = []
    for ln in lines[start + 1:]:
        if ln.startswith("## ") or (ln.startswith("### ") and "Hard implementation" not in ln):
            break
        m = DEPENDENCY_RE.search(ln)
        if m:
            deps.append((m.group(1), m.group(2)))
    return deps


# --- free-text "blocked on #N" notes in the Recommended implementation order --------------------

_ITEM = r"(?:`[^`]+`\s*\(#\d+\)|#\d+)"
HARD_DEPS_HEADING = "### Hard implementation dependencies"


def emit(obj):
    print(json.dumps(obj, indent=2))


def norm_path(path):
    """Forward slashes and no leading "./" or "/" — a prefix is removed, never a character set,
    so a leading dot in `.github/...` or `.env` survives."""
    p = str(path).replace("\\", "/")
    while p.startswith("./"):
        p = p[2:]
    return p.lstrip("/")
