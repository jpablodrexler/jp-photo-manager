#!/usr/bin/env bash
# Reports unused/undeclared dependencies for the backend module, via Maven's
# own built-in `dependency:analyze` — the closest backend equivalent to the
# frontend's knip-based dead-code-report.js (unused-*dependency* findings
# only; Maven has no direct analog to knip's unused-export/unused-file
# detection for application source, which would need a different tool —
# e.g. a PMD "best practices" ruleset for unused private methods/fields —
# not added here). Mirrors dependency-staleness-report.sh's convention of
# capturing the plugin's own INFO/WARNING-level output verbatim into the
# dated report rather than re-parsing it.
#
# Known, expected noise: `dependency:analyze` works by scanning compiled
# bytecode for direct class references, so every Spring Boot
# `spring-boot-starter-*` "umbrella" dependency (which has no classes of
# its own — it exists purely to pull in a bundle of real dependencies
# transitively) is reported as "unused declared" even though removing any
# of them would break the build. This is a well-documented limitation of
# bytecode-based dependency analysis for Spring Boot specifically, not a
# bug in this script — skim past every `spring-boot-starter-*` entry under
# "Unused declared dependencies" and focus on anything else in that list.
#
# Usage: bash scripts/dead-code-report.sh (run from backend/, or anywhere —
# it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/dead-code"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/DEAD_CODE_REPORT_${DATE}_backend.md"

# Deliberately no -q, same reasoning as dependency-staleness-report.sh:
# dependency:analyze prints its findings at WARNING level, not via a
# separate report file — -q would suppress them along with everything else.
RAW_OUTPUT="$(mvn dependency:analyze 2>&1 | grep -E '^\[(WARNING|INFO)\] ' | grep -v '^\[INFO\] ---\|^\[INFO\] $\|^\[INFO\] <<<\|^\[INFO\] >>>\|^\[INFO\] Copying\|^\[INFO\] Nothing to compile\|^\[INFO\] argLine' || true)"

{
  echo "# Dead Code Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend/pom.xml (via \`mvn dependency:analyze\`)"
  echo
  echo "Every \`spring-boot-starter-*\` entry under \"Unused declared dependencies\" below is expected noise (see this script's own header comment for why) — not a real finding."
  echo
  echo '```'
  echo "$RAW_OUTPUT"
  echo '```'
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
