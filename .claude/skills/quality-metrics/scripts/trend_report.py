#!/usr/bin/env python3
"""
Build the quality-metrics trend report for jp-photo-manager: for each of
the ~23 category/side series under JPPhotoManagerWeb/docs/reports/<category>/,
find every committed report file, extract its key metrics, order the
series by the embedded **Generated:** timestamp (never filename/mtime —
see quality-metrics/SKILL.md's own guardrail on why), and classify each
series' shape (Flat / Steadily improving|declining / Volatile / One-off /
First data point / Too little history).

This script owns the mechanical part only — glob, regex-extract, sort,
classify, render. It never runs scripts/run-all-quality-reports.sh itself
and never decides whether a regression is worth acting on; the skill
still reads this output and writes the prose interpretation (in
particular: judging whether a `permitAll()` count change or a
spring-boot-starter-* dead-code line is a real regression is a call this
script deliberately leaves to the skill — see SKILL.md's Guardrails).

Two independent series share a folder for shared categories (complexity,
dead-code, code-coverage, dependency-staleness, license-compliance,
dependency-vulnerabilities, mutation), disambiguated by a `_frontend.md`
/ `_backend.md` filename suffix — this script never merges them.

Usage:
    python3 trend_report.py <repo-root> [--full] [--json]
"""
import re
import sys
import json
import argparse
from pathlib import Path
from datetime import datetime


def g(pattern, text, *, flags=re.IGNORECASE, cast=str, default=None):
    m = re.search(pattern, text, flags)
    if not m:
        return default
    val = m.group(1)
    try:
        return cast(val)
    except (ValueError, TypeError):
        return default


def extract_generated(text):
    ts = g(r'\*\*Generated:\*\*\s*(\S+)', text)
    if not ts:
        return None
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except ValueError:
        return None


# --- per-category extractors -------------------------------------------------

def ext_type_coverage(t):
    return {
        "headline": g(r'\*\*Type coverage:\*\*\s*([\d.]+)%', t, cast=float),
        "untyped": g(r'\*\*Untyped \(`any`\) identifiers:\*\*\s*(\d+)', t, cast=int),
    }


def ext_complexity_frontend(t):
    files = g(r'\*\*Files scanned:\*\*\s*(\d+)', t, cast=int)
    m = re.search(r'\*\*Functions analyzed:\*\*\s*(\d+)\s*\(average complexity:\s*([\d.]+)', t)
    funcs, avg = (int(m.group(1)), float(m.group(2))) if m else (None, None)
    size = g(r'\*\*Average file size:\*\*\s*(\d+) lines', t, cast=int)
    return {"headline": avg, "files_scanned": files, "functions": funcs, "avg_file_size": size}


def ext_complexity_backend(t):
    files = g(r'\*\*Files scanned:\*\*\s*(\d+)', t, cast=int)
    m = re.search(r'\*\*Methods analyzed:\*\*\s*(\d+)\s*\(average complexity:\s*([\d.]+)', t)
    methods, avg = (int(m.group(1)), float(m.group(2))) if m else (None, None)
    size = g(r'\*\*Average file size:\*\*\s*(\d+) lines', t, cast=int)
    return {"headline": avg, "files_scanned": files, "methods": methods, "avg_file_size": size}


def ext_dead_code_frontend(t):
    m = re.search(r'\*\*Total findings:\*\*\s*(\d+)\s*\(([^)]*)\)', t)
    total, breakdown = (int(m.group(1)), m.group(2)) if m else (None, "")
    return {"headline": total, "breakdown": breakdown}


DEP_LINE_PATTERN = re.compile(r'^\[WARNING\]\s+(\S+):(\S+):jar:', re.MULTILINE)


def ext_dead_code_backend(t):
    """No clean summary line — raw `mvn dependency:analyze` output. Count
    every `[WARNING]    groupId:artifactId:jar:...` line (both the "Used
    undeclared" and "Unused declared" sections combined) as the raw total,
    and separately the same count excluding spring-boot-starter-* entries
    (documented project-wide as expected noise — see SKILL.md Guardrails).
    Headline is the raw total, matching the literal "count lines" spec;
    the skill's own trend narrative is the place that judges whether a
    delta is really just starter-dependency churn, not this script."""
    lines = DEP_LINE_PATTERN.findall(t)
    total = len(lines)
    non_starter = sum(1 for group_id, artifact_id in lines if not artifact_id.startswith("spring-boot-starter-"))
    return {"headline": total, "non_starter": non_starter}


def ext_route_coverage(t):
    routes_line = g(r'\*\*Routes with no E2E coverage in either tier:\*\*\s*(.*)', t, default="")
    routes_line = routes_line.strip()
    if not routes_line or routes_line.lower().startswith("none"):
        uncovered = 0
        names = []
    else:
        names = [p.strip() for p in routes_line.split(",") if p.strip()]
        uncovered = len(names)
    return {"headline": uncovered, "routes": names}


AUTH_ROW_PATTERN = re.compile(
    r'^\|\s*(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\s*\|.*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|$',
    re.MULTILINE,
)


def ext_auth_coverage(t):
    """No clean summary line — a raw per-endpoint table. Count data rows
    for the total, and rows whose Governing rule column is `permitAll()`
    for the second figure. See SKILL.md Guardrails: this count is
    informational, never a pass/fail signal on its own."""
    rows = AUTH_ROW_PATTERN.findall(t)
    total = len(rows)
    permit_all = sum(1 for _, governing_rule, _ in rows if governing_rule.strip() == "permitAll()")
    return {"headline": total, "permit_all": permit_all}


def ext_lighthouse(t):
    rows = re.findall(r'^\|\s*(/\S*)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*$', t, re.MULTILINE)
    if not rows:
        return {"headline": None, "routes": {}}
    routes = {r[0]: {"performance": int(r[1]), "accessibility": int(r[2]),
                      "best_practices": int(r[3]), "seo": int(r[4])} for r in rows}
    avg_perf = sum(v["performance"] for v in routes.values()) / len(routes)
    return {"headline": round(avg_perf, 1), "routes": routes}


def ext_a11y(t):
    total = g(r'\*\*Total violations:\*\*\s*(\d+)', t, cast=int)
    breakdown = {}
    for impact in ("critical", "serious", "moderate", "minor"):
        breakdown[impact] = g(rf'\|\s*{impact}\s*\|\s*(\d+)\s*\|', t, cast=int, default=0)
    return {"headline": total, "breakdown": breakdown}


def ext_code_coverage_frontend(t):
    metrics = {}
    for name in ("Statements", "Branches", "Functions", "Lines"):
        m = re.search(rf'\*\*{name}:\*\*\s*([\d.]+)%\s*\((\d+)/(\d+)\)', t)
        if m:
            metrics[name.lower()] = float(m.group(1))
    below = g(r'##\s*(\d+)\s*file\(s\) below 80%', t, cast=int, default=0)
    return {"headline": metrics.get("branches"), "metrics": metrics, "files_below_80": below}


def ext_code_coverage_backend(t):
    metrics = {}
    for name in ("Lines", "Branches", "Methods"):
        m = re.search(rf'\*\*{name}:\*\*\s*([\d.]+)%\s*\((\d+)/(\d+)\)', t)
        if m:
            metrics[name.lower()] = float(m.group(1))
    below = g(r'##\s*(\d+)\s*class\(es\) below 80%', t, cast=int, default=0)
    return {"headline": metrics.get("lines"), "metrics": metrics, "classes_below_80": below}


def ext_bundle_size(t):
    initial = g(r'\*\*Initial bundle size:\*\*\s*([\d.]+)\s*kB', t, cast=float)
    budget = g(r'\*\*Budget status:\*\*\s*([^\n(]*)', t, default="")
    total_js = g(r'\*\*Total JS shipped[^:]*:\*\*\s*([\d.]+)\s*kB', t, cast=float)
    total_dist = g(r'\*\*Total dist size[^:]*:\*\*\s*([\d.]+)\s*kB', t, cast=float)
    return {"headline": initial, "budget_status": budget.strip(), "total_js": total_js, "total_dist": total_dist}


def ext_dependency_staleness_frontend(t):
    total = g(r'\*\*Total dependencies \(direct\):\*\*\s*(\d+)', t, cast=int)
    m = re.search(r'\*\*Outdated:\*\*\s*(\d+)\s*\((\d+) major,\s*(\d+) minor,\s*(\d+) patch behind\)', t)
    if m:
        outdated, major, minor, patch = (int(x) for x in m.groups())
    else:
        outdated = major = minor = patch = None
    return {"headline": outdated, "total_deps": total, "major": major, "minor": minor, "patch": patch}


MAVEN_UPDATE_LINE_PATTERN = re.compile(r'^\[INFO\]\s+.*->.*$', re.MULTILINE)


def ext_dependency_staleness_backend(t):
    """No clean summary line — raw `mvn versions:display-dependency-updates`
    output. A dependency's "old -> new" arrow sometimes wraps onto its own
    continuation line (still prefixed `[INFO]`) when the artifact name is
    long, so counting lines containing "->" naturally counts each update
    exactly once regardless of wrapping."""
    outdated = len(MAVEN_UPDATE_LINE_PATTERN.findall(t))
    return {"headline": outdated}


def ext_e2e_run(t):
    m = re.search(r'\*\*Tests:\*\*\s*(\d+)\s*\((\d+) passed,\s*(\d+) failed,\s*(\d+) skipped\)', t)
    total = passed = failed = skipped = None
    if m:
        total, passed, failed, skipped = (int(x) for x in m.groups())
    flaky = g(r'\*\*Flaky tests[^:]*:\*\*\s*(\d+)', t, cast=int, default=0)
    duration = g(r'\*\*Total duration:\*\*\s*([\d.]+)s', t, cast=float)
    return {"headline": failed, "total": total, "passed": passed, "skipped": skipped,
            "flaky": flaky, "duration_s": duration}


def ext_secrets_scan(t):
    return {"headline": g(r'\*\*Findings:\*\*\s*(\d+) potential secret', t, cast=int)}


def ext_license_compliance_frontend(t):
    m = re.search(r'\*\*(\d+) package\(s\) scanned,\s*(\d+) flagged\*\*', t)
    scanned, flagged = (int(m.group(1)), int(m.group(2))) if m else (None, None)
    return {"headline": flagged, "scanned": scanned}


def ext_license_compliance_backend(t):
    m = re.search(
        r'\*\*(\d+) package\(s\) scanned,\s*(\d+) need action,\s*(\d+) previously reviewed and accepted\.\*\*', t)
    if not m:
        return {"headline": None}
    scanned, need_action, accepted = (int(x) for x in m.groups())
    return {"headline": need_action, "scanned": scanned, "previously_accepted": accepted}


def ext_dependency_vulnerabilities_frontend(t):
    m = re.search(r'\*\*(\d+) vulnerable package\(s\):\*\*\s*(\d+) critical,\s*(\d+) high,\s*(\d+) moderate,\s*(\d+) low,\s*(\d+) info', t)
    if not m:
        return {"headline": None}
    vuln, crit, high, mod, low, info = (int(x) for x in m.groups())
    return {"headline": vuln, "critical": crit, "high": high, "moderate": mod, "low": low, "info": info}


def ext_dependency_vulnerabilities_backend(t):
    count = g(r'\*\*(\d+) known vulnerabilit(?:y|ies) across the resolved dependency tree\.\*\*', t, cast=int)
    return {"headline": count}


def ext_mutation_frontend(t):
    m = re.search(r'\*\*Overall mutation score:\*\*\s*([\d.]+)%\s*\((\d+)/(\d+) tested mutants killed\)', t)
    score = killed = tested = None
    if m:
        score, killed, tested = float(m.group(1)), int(m.group(2)), int(m.group(3))
    return {"headline": score, "killed": killed, "tested": tested}


def ext_mutation_backend(t):
    line_cov = g(r'\*\*Line coverage:\*\*\s*([\d.]+)%', t, cast=float)
    mutation_cov = g(r'\*\*Mutation coverage:\*\*\s*([\d.]+)%', t, cast=float)
    test_strength = g(r'\*\*Test strength:\*\*\s*([\d.]+)%', t, cast=float)
    return {"headline": mutation_cov, "line_coverage": line_cov, "test_strength": test_strength}


def ext_claude_md_size(t):
    # Headline is JPPhotoManagerWeb/CLAUDE.md — the fields appearing before
    # the "### Repo-root CLAUDE.md" section, if present, which covers the
    # legacy WPF app's file (informational only, not part of the headline).
    head = t.split("### Repo-root CLAUDE.md")[0]
    lines_ = g(r'\*\*Lines:\*\*\s*(\d+)', head, cast=int)
    words = g(r'\*\*Words:\*\*\s*(\d+)', head, cast=int)
    size = g(r'\*\*Size:\*\*\s*(\d+) bytes', head, cast=int)
    headings = g(r'\*\*Headings:\*\*\s*(\d+)', head, cast=int)
    return {"headline": lines_, "words": words, "bytes": size, "headings": headings}


CATEGORIES = {
    "type-coverage": dict(folder="type-coverage", glob="TYPE_COVERAGE_REPORT_*.md",
                           group="Coverage & correctness", label="Type coverage",
                           higher_better=True, extract=ext_type_coverage),
    "code-coverage-frontend": dict(folder="code-coverage", glob="CODE_COVERAGE_REPORT_*_frontend.md",
                                    group="Coverage & correctness", label="Component test coverage (branches)",
                                    higher_better=True, extract=ext_code_coverage_frontend),
    "code-coverage-backend": dict(folder="code-coverage", glob="CODE_COVERAGE_REPORT_*_backend.md",
                                   group="Coverage & correctness", label="Backend coverage (JaCoCo, lines)",
                                   higher_better=True, extract=ext_code_coverage_backend),
    "mutation-frontend": dict(folder="mutation", glob="MUTATION_REPORT_*_frontend.md",
                               group="Coverage & correctness", label="Mutation score (frontend)",
                               higher_better=True, extract=ext_mutation_frontend, opt_in=True),
    "mutation-backend": dict(folder="mutation", glob="MUTATION_REPORT_*_backend.md",
                              group="Coverage & correctness", label="Mutation coverage (backend, PIT)",
                              higher_better=True, extract=ext_mutation_backend, opt_in=True),
    "e2e-run-mocked": dict(folder="e2e-run", glob="E2E_RUN_REPORT_*_mocked.md",
                            group="Coverage & correctness", label="E2E (mocked) failed tests",
                            higher_better=False, extract=ext_e2e_run),
    "e2e-run-real": dict(folder="e2e-run", glob="E2E_RUN_REPORT_*_real.md",
                          group="Coverage & correctness", label="E2E (real) failed tests",
                          higher_better=False, extract=ext_e2e_run, opt_in=True),
    "complexity-frontend": dict(folder="complexity", glob="COMPLEXITY_REPORT_*_frontend.md",
                                 group="Code health", label="Complexity (frontend, avg)",
                                 higher_better=False, extract=ext_complexity_frontend),
    "complexity-backend": dict(folder="complexity", glob="COMPLEXITY_REPORT_*_backend.md",
                                group="Code health", label="Complexity (backend, avg)",
                                higher_better=False, extract=ext_complexity_backend),
    "dead-code-frontend": dict(folder="dead-code", glob="DEAD_CODE_REPORT_*_frontend.md",
                                group="Code health", label="Dead code findings (frontend)",
                                higher_better=False, extract=ext_dead_code_frontend),
    "dead-code-backend": dict(folder="dead-code", glob="DEAD_CODE_REPORT_*_backend.md",
                               group="Code health", label="Unused/undeclared dep lines (backend)",
                               higher_better=False, extract=ext_dead_code_backend),
    "bundle-size": dict(folder="bundle-size", glob="BUNDLE_SIZE_REPORT_*.md",
                         group="Code health", label="Initial bundle size (kB)",
                         higher_better=False, extract=ext_bundle_size),
    "claude-md-size": dict(folder="claude-md-size", glob="CLAUDE_MD_SIZE_REPORT_*.md",
                            group="Code health", label="CLAUDE.md lines (JPPhotoManagerWeb)",
                            higher_better=None, extract=ext_claude_md_size),
    "dependency-vulnerabilities-frontend": dict(folder="dependency-vulnerabilities", glob="SCA_REPORT_*_frontend.md",
                                                 group="Security & dependencies", label="Vulnerable packages (frontend)",
                                                 higher_better=False, extract=ext_dependency_vulnerabilities_frontend),
    "dependency-vulnerabilities-backend": dict(folder="dependency-vulnerabilities", glob="SCA_REPORT_*_backend.md",
                                                group="Security & dependencies", label="Known vulnerabilities (backend, OSV)",
                                                higher_better=False, extract=ext_dependency_vulnerabilities_backend),
    "dependency-staleness-frontend": dict(folder="dependency-staleness", glob="DEPENDENCY_STALENESS_REPORT_*_frontend.md",
                                           group="Security & dependencies", label="Outdated dependencies (frontend)",
                                           higher_better=False, extract=ext_dependency_staleness_frontend),
    "dependency-staleness-backend": dict(folder="dependency-staleness", glob="DEPENDENCY_STALENESS_REPORT_*_backend.md",
                                          group="Security & dependencies", label="Outdated dependencies (backend, Maven)",
                                          higher_better=False, extract=ext_dependency_staleness_backend),
    "secrets-scan": dict(folder="secrets-scan", glob="SECRETS_SCAN_REPORT_*.md",
                          group="Security & dependencies", label="Secret findings",
                          higher_better=False, extract=ext_secrets_scan),
    "license-compliance-frontend": dict(folder="license-compliance", glob="LICENSE_COMPLIANCE_REPORT_*_frontend.md",
                                         group="Security & dependencies", label="Flagged licenses (frontend)",
                                         higher_better=False, extract=ext_license_compliance_frontend),
    "license-compliance-backend": dict(folder="license-compliance", glob="LICENSE_COMPLIANCE_REPORT_*_backend.md",
                                        group="Security & dependencies", label="Licenses needing action (backend)",
                                        higher_better=False, extract=ext_license_compliance_backend),
    "route-coverage": dict(folder="route-coverage", glob="ROUTE_COVERAGE_REPORT_*_frontend.md",
                            group="Security & dependencies", label="Uncovered routes",
                            higher_better=False, extract=ext_route_coverage),
    "auth-coverage": dict(folder="auth-coverage", glob="AUTH_COVERAGE_REPORT_*_backend.md",
                           group="Security & dependencies", label="Endpoints tracked",
                           higher_better=None, extract=ext_auth_coverage),
    "lighthouse": dict(folder="lighthouse", glob="LIGHTHOUSE_REPORT_*_frontend.md",
                        group="Accessibility & performance", label="Avg. Performance score",
                        higher_better=True, extract=ext_lighthouse),
    "a11y": dict(folder="a11y", glob="A11Y_REPORT_*_frontend.md",
                 group="Accessibility & performance", label="Total a11y violations",
                 higher_better=False, extract=ext_a11y),
}


def load_series(root, cfg):
    folder = root / "JPPhotoManagerWeb" / "docs" / "reports" / cfg["folder"]
    points = []
    if not folder.exists():
        return points
    for f in folder.glob(cfg["glob"]):
        text = f.read_text(encoding="utf-8", errors="replace")
        ts = extract_generated(text)
        if ts is None:
            continue
        fields = cfg["extract"](text)
        points.append({"file": f.name, "generated": ts.isoformat(), "_ts": ts, **fields})
    points.sort(key=lambda p: p["_ts"])
    for p in points:
        del p["_ts"]
    return points


def classify(values, higher_better):
    """Shape of the whole series (not just the latest delta) — see SKILL.md step 5.
    `higher_better` is None for a purely informational metric (track, don't judge)."""
    n = len(values)
    if n == 0:
        return "no data"
    if n == 1:
        return "First data point"
    if n == 2:
        return "Too little history"
    diffs = [values[i + 1] - values[i] for i in range(n - 1)]
    spread = max(values) - min(values)
    avg = sum(values) / n
    tol = max(abs(avg) * 0.02, 1e-9)
    if spread <= tol:
        return "Flat"
    if all(abs(d) <= tol for d in diffs[:-1]) and abs(diffs[-1]) > tol:
        return "One-off"
    nonzero = [d for d in diffs if abs(d) > tol]
    if nonzero and all((d > 0) == (nonzero[0] > 0) for d in nonzero):
        direction_up = nonzero[0] > 0
        if higher_better is None:
            return "Steadily increasing" if direction_up else "Steadily decreasing"
        return "Steadily improving" if (direction_up == higher_better) else "Steadily declining"
    return "Volatile"


def trend_marker(shape, delta, higher_better):
    if delta is None or higher_better is None:
        return "—"
    if abs(delta) < 1e-9:
        return "→"
    improved = (delta > 0) == higher_better
    return "▲" if improved else "▼"


def analyze(points, higher_better):
    values = [p["headline"] for p in points if p.get("headline") is not None]
    shape = classify(values, higher_better)
    delta = None
    if len(values) >= 2:
        delta = values[-1] - values[-2]
    marker = trend_marker(shape, delta, higher_better)
    return {"values": values, "shape": shape, "delta": delta, "marker": marker}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("repo_root")
    ap.add_argument("--full", action="store_true", help="include mutation and real-backend E2E categories")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root)
    out = {}
    for key, cfg in CATEGORIES.items():
        if cfg.get("opt_in") and not args.full:
            continue
        points = load_series(root, cfg)
        higher_better = cfg["higher_better"]
        analysis = analyze(points, higher_better)
        out[key] = {
            "label": cfg["label"],
            "group": cfg["group"],
            "higher_better": higher_better,
            "points": points,
            **analysis,
        }

    if args.json:
        print(json.dumps(out, indent=2, default=str))
        return

    render_markdown(out)


def fmt_history(values):
    if not values:
        return "—"

    def f(v):
        if isinstance(v, float):
            return f"{v:.2f}".rstrip("0").rstrip(".")
        return str(v)

    return "→".join(f(v) for v in values)


def render_markdown(out):
    lines = ["## Quality Metrics Trend Report", ""]
    groups = []
    for cfg in CATEGORIES.values():
        if cfg["group"] not in groups:
            groups.append(cfg["group"])

    regressions = []
    first_time = []

    for group in groups:
        rows = [(k, v) for k, v in out.items() if v["group"] == group]
        if not rows:
            continue
        lines.append(f"### {group}")
        lines.append("")
        lines.append("| Category | Headline | History (oldest→newest) | Latest Δ | Shape |")
        lines.append("| --- | --- | --- | --- | --- |")
        for key, v in rows:
            history = fmt_history(v["values"])
            delta = f"{v['marker']} {v['delta']:+.2f}".rstrip("0").rstrip(".") if v["delta"] is not None else v["marker"]
            lines.append(f"| {v['label']} | {v['values'][-1] if v['values'] else '—'} | {history} | {delta} | {v['shape']} |")
            if v["shape"] in ("no data", "First data point", "Too little history"):
                first_time.append(f"- **{v['label']}**: {v['shape']}"
                                   f"{' — ' + str(v['values']) if v['values'] else ''}")
            elif v["marker"] == "▼":
                sev = "One-off" if v["shape"] == "One-off" else v["shape"]
                regressions.append(f"- **{v['label']}**: {sev}, latest Δ {v['delta']:+.2f}")
        lines.append("")

    lines.append("### ⚠ Regressions")
    lines.append("")
    lines.extend(regressions if regressions else ["None."])
    lines.append("")
    lines.append("### First-time / too little history")
    lines.append("")
    lines.extend(first_time if first_time else ["None."])

    print("\n".join(lines))


if __name__ == "__main__":
    main()
