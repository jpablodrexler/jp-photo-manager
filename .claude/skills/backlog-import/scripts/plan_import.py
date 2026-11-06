#!/usr/bin/env python3
"""
Turn a list of tracker issues into a reviewable import plan for the backlog. Writes nothing.

    python3 plan_import.py <repo-root> <issues.json> [--limit N] [--json]

<issues.json> is the list a tracker CLI or API returns, GitHub or GitLab shaped:
    GitHub: number, title, body, labels [{name}], url / html_url, state, pull_request
    GitLab: iid, title, description, labels [str], web_url, state
For each open issue it proposes bug or feature, and priority / severity / story points / area when a label says so,
and drops what should not be imported: pull requests, closed issues, and issues already imported (a brief under
<backlog>/features/ or <backlog>/bugs/ already contains the issue URL).
The skill then hands each approved item to feature-plan or bug-report, which draft, estimate, ask and write.
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

BUG_LABEL = re.compile(r"(^|[\s:/_-])(bug|defect|regression)s?($|[\s:/_-])", re.I)
PRIORITY = re.compile(r"^(?:priority|prio)?[\s:/-]*(p[0-3]|critical|urgent|high|medium|normal|low)$", re.I)
PRIORITY_MAP = {"p0": "P0", "critical": "P0", "urgent": "P0", "p1": "P1", "high": "P1", "p2": "P2", "medium": "P2", "normal": "P2", "p3": "P3", "low": "P3"}
SEVERITY = re.compile(r"^severity[\s:/-]*(s[1-4]|critical|major|minor|trivial)$", re.I)
SEVERITY_MAP = {"s1": "S1", "critical": "S1", "s2": "S2", "major": "S2", "s3": "S3", "minor": "S3", "s4": "S4", "trivial": "S4"}
SEVERITY_FROM_PRIORITY = {"P0": "S1", "P1": "S2", "P2": "S3", "P3": "S4"}
POINTS = re.compile(r"^(?:points|sp|story[\s-]*points|estimate|effort)[\s:/-]*(1|2|3|5|8|13)$", re.I)
AREA = re.compile(r"^area[\s:/-]+(.+)$", re.I)
STOP = {"the", "a", "an", "of", "on", "in", "to", "and", "or", "is", "are", "as", "when", "for", "with", "add", "support", "fix"}


def normalize(raw):
    labels = [(l.get("name", "") if isinstance(l, dict) else str(l)).strip() for l in raw.get("labels") or []]
    return {
        "number": raw.get("number", raw.get("iid")),
        "title": (raw.get("title") or "").strip(),
        "body": (raw.get("body") or raw.get("description") or "").strip(),
        "labels": [l for l in labels if l],
        "url": raw.get("url") or raw.get("html_url") or raw.get("web_url") or "",
        "state": str(raw.get("state") or "open").lower(),
        "is_pr": "pull_request" in raw or bool(raw.get("isPullRequest")) or "/pull/" in (raw.get("url") or raw.get("html_url") or ""),
    }


def slug(title):
    words = [w for w in re.findall(r"[a-z0-9]+", title.lower()) if w not in STOP]
    return "-".join(words[:4]) or "imported-issue"


def imported_urls(backlog):
    """Text of every brief, to look an issue URL up in."""
    texts = []
    for sub in ("features", "bugs"):
        folder = backlog / sub
        if folder.is_dir():
            texts += [(p.stem, p.read_text(encoding="utf-8", errors="replace")) for p in sorted(folder.glob("*.md"))]
    return texts


def classify(issue):
    labels = issue["labels"]
    priority = next((PRIORITY_MAP[m.group(1).lower()] for l in labels if (m := PRIORITY.match(l))), None)
    severity = next((SEVERITY_MAP[m.group(1).lower()] for l in labels if (m := SEVERITY.match(l))), None)
    points = next((m.group(1) for l in labels if (m := POINTS.match(l))), None)
    area = next((m.group(1).strip() for l in labels if (m := AREA.match(l))), None)
    is_bug = any(BUG_LABEL.search(l) for l in labels)
    out = {"kind": "bug" if is_bug else "feature", "priority": priority, "points": points, "area": area}
    if is_bug:
        out["severity"] = severity or SEVERITY_FROM_PRIORITY.get(priority)
    return out


def plan(root, issues, limit):
    backlog = Path(root) / bk.load_config(root)["paths"]["backlog_dir"].strip("/")
    briefs = imported_urls(backlog)
    importable, skipped = [], []
    for raw in issues:
        i = normalize(raw)
        ref = {"number": i["number"], "title": i["title"]}
        if i["is_pr"]:
            skipped.append({**ref, "reason": "pull request"})
        elif i["state"] not in ("open", "opened"):
            skipped.append({**ref, "reason": f"{i['state']}"})
        elif not i["title"]:
            skipped.append({**ref, "reason": "no title"})
        else:
            hit = next((name for name, text in briefs if i["url"] and re.search(re.escape(i["url"]) + r"(?!\d)", text)), None)
            if hit:
                skipped.append({**ref, "reason": f"already imported as {hit}"})
                continue
            importable.append({**ref, "url": i["url"], "labels": i["labels"], "suggested_name": slug(i["title"]),
                               "body_excerpt": re.sub(r"\s+", " ", i["body"])[:600], **classify(i)})
    importable.sort(key=lambda x: (x["number"] is None, x["number"]))
    return {"importable": importable[:limit], "not_shown": max(len(importable) - limit, 0), "skipped": skipped}


def render(p):
    out = ["## Import plan", ""]
    if not p["importable"]:
        out.append("Nothing to import.")
    else:
        out += ["| Issue | Becomes | Priority / Severity | Points | Area | Title |", "| --- | --- | --- | --- | --- | --- |"]
        for i in p["importable"]:
            rank = i["priority"] or "—"
            if i["kind"] == "bug":
                rank = f"{i.get('severity') or '—'}"
            out.append(f"| {i['number']} | {i['kind']} | {rank} | {i['points'] or '—'} | {i['area'] or '—'} | {i['title']} |")
        if p["not_shown"]:
            out += ["", f"{p['not_shown']} more issue(s) not shown (raise --limit)."]
    if p["skipped"]:
        out += ["", "### Skipped"] + [f"- {s['number']} {s['title']} — {s['reason']}" for s in p["skipped"]]
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("issues_json")
    ap.add_argument("--limit", type=int, default=10)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    if not Path(args.repo_root).is_dir():
        sys.exit(f"{args.repo_root} is not a directory")
    data = json.loads(Path(args.issues_json).read_text(encoding="utf-8"))
    if isinstance(data, dict):
        data = data.get("issues") or data.get("items") or []
    result = plan(args.repo_root, data, max(args.limit, 1))
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else render(result))


if __name__ == "__main__":
    main()
