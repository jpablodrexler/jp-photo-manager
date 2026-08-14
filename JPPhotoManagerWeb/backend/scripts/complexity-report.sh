#!/usr/bin/env bash
# Reports the top cyclomatic-complexity and file-size (LOC) hotspots across
# the backend source tree, as a trending snapshot rather than the pass/fail
# gate `mvn pmd:check` already runs (code-reviewer skill §18.2's max-15
# gate). Even when every method is under threshold, this shows which
# methods/files are closest to it and growing, before they become an actual
# violation — mirroring the frontend's scripts/complexity-report.js.
#
# Uses a second, report-only maven-pmd-plugin execution (id
# "complexity-report", goal `pmd` not `check` — see pom.xml's comment on
# why `check` can't be reused here: it internally forks its own `pmd:pmd`
# execution using the plugin's *default* configuration, silently ignoring
# an execution-specific ruleset override) bound to
# pmd-complexity-report-ruleset.xml, which sets methodReportLevel=1 so PMD
# reports every method's complexity as a "violation" instead of only ones
# over the real gate's threshold of 15 — this script then ranks all of them
# and takes the top N. Never touches the real gate's pmd-complexity-
# ruleset.xml or its methodReportLevel=16 threshold.
#
# Usage: bash scripts/complexity-report.sh (run from backend/, or anywhere
# — it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
TOP_N=20

echo "Running PMD complexity scan (report-only ruleset)..."
mvn pmd:pmd@complexity-report -q

# Each violation is two lines: the opening <violation ...> tag (attributes,
# including package/class/method) and the message text on the next line
# ("The method 'sig' has a cyclomatic complexity of N."). Emit one
# tab-separated row per violation: complexity, package.Class#method, line.
HOTSPOTS="$(awk '
  /<violation / {
    match($0, /beginline="[0-9]+"/); line = substr($0, RSTART+11, RLENGTH-12)
    match($0, /package="[^"]*"/); pkg = substr($0, RSTART+9, RLENGTH-10)
    match($0, /class="[^"]*"/); cls = substr($0, RSTART+7, RLENGTH-8)
    match($0, /method="[^"]*"/); meth = substr($0, RSTART+8, RLENGTH-9)
    getline msg
    match(msg, /complexity of [0-9]+/)
    complexity = substr(msg, RSTART+14, RLENGTH-14)
    printf "%s\t%s.%s#%s\t%s\n", complexity, pkg, cls, meth, line
  }
' target/pmd.xml | sort -t$'\t' -k1,1 -rn | head -n "$TOP_N")"

TOTAL_METHODS="$(grep -c '<violation ' target/pmd.xml || echo 0)"
AVG_COMPLEXITY="$(awk -F'\t' '/^[0-9]/{sum+=$1; n++} END{if(n>0) printf "%.2f", sum/n; else print "0"}' <(awk '
  /<violation / { getline msg; match(msg, /complexity of [0-9]+/); print substr(msg, RSTART+14, RLENGTH-14) }
' target/pmd.xml))"

FILE_SIZES="$(find src/main/java -name '*.java' -exec wc -l {} \; | sort -rn | head -n "$TOP_N" | awk '{printf "%s\t%s\n", $1, $2}')"
TOTAL_FILES="$(find src/main/java -name '*.java' | wc -l | tr -d ' ')"
AVG_FILE_SIZE="$(find src/main/java -name '*.java' -exec wc -l {} \; | awk '{sum+=$1; n++} END{if(n>0) printf "%d", sum/n; else print 0}')"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/complexity"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/COMPLEXITY_REPORT_${DATE}_backend.md"

{
  echo "# Complexity & File-Size Hotspots Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend/src/main/java"
  echo
  echo "**Files scanned:** $TOTAL_FILES"
  echo "**Methods analyzed:** $TOTAL_METHODS (average complexity: $AVG_COMPLEXITY; gate threshold: 15, see \`mvn pmd:check\`)"
  echo "**Average file size:** $AVG_FILE_SIZE lines"
  echo
  echo "## Top $TOP_N methods by cyclomatic complexity"
  echo
  echo "| Complexity | Method | Line |"
  echo "| --- | --- | --- |"
  echo "$HOTSPOTS" | awk -F'\t' '{printf "| %s | %s | %s |\n", $1, $2, $3}'
  echo
  echo "## Top $TOP_N files by line count"
  echo
  echo "| Lines | File |"
  echo "| --- | --- |"
  echo "$FILE_SIZES" | awk -F'\t' '{printf "| %s | %s |\n", $1, $2}'
  echo
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
