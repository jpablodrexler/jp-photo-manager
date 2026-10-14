#!/usr/bin/env python3
# VENDORED from lib/dep_scan.py — do not edit here; edit the source and run `python3 tools/vendor.py`.
"""
Find hard dependencies a feature brief implies but the backlog does not record.

Whether a feature needs something that doesn't exist yet used to rest on a model noticing it
(a vague brief once hid one). This does the mechanical part and leaves the judgment to the
caller as a short, evidenced candidate list instead of "go look".

A feature X gets a candidate dependency on a *pending* feature Y when:

  mention  X's brief names Y (its change name, `feature N` or `#N`), and no
           `**Feature X -> Feature Y**` is recorded; or
  symbol   X's brief uses a backticked identifier or file name that does not exist anywhere
           in the repository, and exactly one other pending brief *defines* it — names it within a
           few words after a defining verb (extract / add / create / introduce / new / export /
           implement / define / build / write / provide), in a sentence not about tests.

Flags (`--x`), tokens under 4 characters and anything already in the repository are ignored.
Candidates are evidence, not verdicts: callers confirm or dismiss each one.

Usage:
    python3 dep_scan.py <repo-root> [--feature N|name] [--json]
    python3 dep_scan.py <repo-root> --new --name <change-name> --number N < brief.md
        (a feature that is not in the backlog yet: its brief is read from stdin)

Prints JSON, one entry per (dependent, prerequisite) pair:
    {"candidates": [{"dependent": "4", "dependent_name": "...", "prerequisite": "3",
                     "prerequisite_name": "...", "kind": ["mention", "symbol"], "evidence": ["...", "..."]}]}
"""
import argparse
import os
import re
import sys
from pathlib import Path

_here = Path(__file__).resolve()
for _cand in (_here.parent, _here.parents[3] / "lib"):
    if (_cand / "backlog_kit.py").is_file():
        sys.path.insert(0, str(_cand))
        break
import backlog_kit as bk

DEFINING = re.compile(r"\b(extract\w*|add\w*|creat\w*|introduc\w*|new|export\w*|implement\w*|defin\w*|"
                      r"build\w*|writ\w*|provid\w*)\b", re.IGNORECASE)
SKIP_DIRS = {".git", "node_modules", "__pycache__", "dist", "build", "target", "venv", ".venv", ".claude",
             "openspec", ".idea", ".vscode", "coverage"}
MAX_FILES, MAX_BYTES = 5000, 1_000_000


def tokens(text):
    out = set()
    for span in re.findall(r"`([^`\n]+)`", text):
        m = re.match(r"[A-Za-z_][\w./-]*", span.strip())
        if not m:
            continue
        t = m.group(0).rstrip(".-/")
        if len(t) >= 4 and not span.strip().startswith("-"):
            out.add(t)
    return out


WINDOW_WORDS = 9


def defined_tokens(text):
    """Tokens a brief introduces: named within a few words after a defining verb, in a sentence
    that is not about tests. (A token merely *used* in the same sentence is not defined there.)"""
    out = set()
    for sentence in re.split(r"(?<=[.!?:;])\s+|\n+", text):
        if "test" in sentence.lower():
            continue
        for m in DEFINING.finditer(sentence):
            window = " ".join(sentence[m.end():].split()[:WINDOW_WORDS])
            out |= tokens(window)
    return out


def repo_corpus(root, cfg):
    skip = {Path(cfg["paths"][k]).parts[0] for k in ("backlog_dir", "reports_dir", "decisions_dir", "explorations_dir")}
    names, texts, seen = set(), [], 0
    for base, dirs, files in os.walk(root):
        rel = Path(base).relative_to(root)
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS and not (rel == Path(".") and d in skip)]
        for f in files:
            names.add(f)
            names.add(str(rel / f).replace(os.sep, "/"))
            seen += 1
            if seen > MAX_FILES:
                break
            p = Path(base) / f
            try:
                if p.stat().st_size <= MAX_BYTES:
                    texts.append(p.read_text(encoding="utf-8"))
            except (UnicodeDecodeError, OSError):
                pass
    return names, "\n".join(texts)


def exists_in_repo(token, names, corpus):
    if token in names or any(n.endswith("/" + token) for n in names):
        return True
    return re.search(rf"(?<![\w]){re.escape(token)}(?![\w])", corpus) is not None


def load(root):
    cfg = bk.load_config(root)
    p = bk.Paths(root, cfg)
    _, planned = bk.table_rows(p.features_planned, "## Feature List")
    pending = []
    for r in planned:
        if "Pending" not in r.get("Implementation", "") and "In Progress" not in r.get("Implementation", ""):
            continue
        n, name = r.get("#", "").strip(), bk.strip_backticks(r.get("Change name", ""))
        brief = p.backlog / "features" / f"{int(n):03d}-{name}.md" if n.isdigit() else None
        text = brief.read_text(encoding="utf-8") if brief and brief.is_file() else ""
        pending.append({"number": n, "name": name, "text": text + "\n" + r.get("Summary", "")})
    deps = set(bk.hard_dependencies(bk.read_lines(p.features_planned) or []))
    return cfg, pending, deps


def scan(root, only=None, new=None):
    cfg, pending, deps = load(root)
    if new:
        pending = [f for f in pending if f["number"] != new["number"]] + [new]
        only = new["number"]
    targets = [f for f in pending if only is None or f["number"] == only or f["name"] == only]
    names, corpus = repo_corpus(Path(root), cfg)
    definers = {}
    for f in pending:
        for t in defined_tokens(f["text"]):
            definers.setdefault(t, set()).add(f["number"])
    out = []
    for x in targets:
        for y in pending:
            if y["number"] == x["number"] or (x["number"], y["number"]) in deps:
                continue
            body = x["text"]
            hit = re.search(rf"(?<![\w-]){re.escape(y['name'])}(?![\w-])", body) or \
                re.search(rf"(?i)\bfeature\s+#?{re.escape(y['number'])}\b", body) or \
                re.search(rf"(?<![\w/]){'#' + re.escape(y['number'])}\b", body)
            if hit and (y["number"], x["number"]) not in deps:
                out.append({"dependent": x["number"], "dependent_name": x["name"], "prerequisite": y["number"],
                            "prerequisite_name": y["name"], "kind": "mention",
                            "evidence": f"the brief mentions {hit.group(0)!r}, but no `Feature {x['number']} → Feature {y['number']}` is recorded"})
        own = defined_tokens(x["text"])
        for t in sorted(tokens(x["text"]) - own):
            owners = definers.get(t, set()) - {x["number"]}
            if len(owners) != 1 or exists_in_repo(t, names, corpus):
                continue
            y = next(f for f in pending if f["number"] == next(iter(owners)))
            if (x["number"], y["number"]) in deps or any(c["dependent"] == x["number"] and c["prerequisite"] == y["number"]
                                                         and c["kind"] == "symbol" and f"`{t}`" in c["evidence"] for c in out):
                continue
            out.append({"dependent": x["number"], "dependent_name": x["name"], "prerequisite": y["number"],
                        "prerequisite_name": y["name"], "kind": "symbol",
                        "evidence": f"`{t}` is used by this brief, does not exist in the repository, and is introduced by "
                                    f"feature {y['number']} (`{y['name']}`)"})
    grouped = {}
    for c in out:
        key = (c["dependent"], c["prerequisite"])
        g = grouped.setdefault(key, {**c, "kind": [], "evidence": []})
        g["kind"].append(c["kind"])
        g["evidence"].append(c["evidence"])
    return list(grouped.values())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--feature")
    ap.add_argument("--new", action="store_true")
    ap.add_argument("--name")
    ap.add_argument("--number")
    a = ap.parse_args()
    new = None
    if a.new:
        if not (a.name and a.number):
            sys.exit("--new needs --name and --number")
        new = {"number": a.number, "name": a.name, "text": sys.stdin.read()}
    bk.emit({"candidates": scan(a.repo_root, a.feature, new)})


if __name__ == "__main__":
    main()
