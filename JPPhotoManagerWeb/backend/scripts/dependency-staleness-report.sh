#!/usr/bin/env bash
# Reports Maven dependency staleness for the backend module, mirroring the
# frontend's npm-based dependency-staleness-report.js — this is a standing
# "how far behind latest" signal, distinct from a known-vulnerability check
# (security-reviewer's job) or an active-breakage fix (dependency-upgrade's
# job). versions-maven-plugin (added to pom.xml's <build><plugins>) does the
# actual version-comparison work; this script just captures its output into
# the same dated-report convention (docs/reports/dependency-staleness/) the
# frontend script and the code/database/security-reviewer skills use.
#
# Usage: bash scripts/dependency-staleness-report.sh (run from backend/, or
# anywhere — it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/dependency-staleness"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/DEPENDENCY_STALENESS_REPORT_${DATE}_backend.md"

# Deliberately no -q: versions-maven-plugin prints its report at Maven's
# INFO log level, which -q suppresses along with everything else, leaving
# an empty report. The extra build-lifecycle noise is harmless in a report
# file that's read on demand, not parsed.
RAW_OUTPUT="$(mvn versions:display-dependency-updates -DprocessDependencyManagement=false 2>&1 || true)"

{
  echo "# Dependency Staleness Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend/pom.xml (via versions-maven-plugin)"
  echo
  echo '```'
  echo "$RAW_OUTPUT"
  echo '```'
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
