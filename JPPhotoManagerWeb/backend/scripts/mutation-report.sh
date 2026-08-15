#!/usr/bin/env bash
# Wraps a PIT mutation-testing run into a dated snapshot report under
# JPPhotoManagerWeb/docs/reports/mutation/, the same convention
# complexity-report.sh/coverage-report.sh use.
#
# Uses a report-only maven-pitest-plugin execution (id "mutation-report",
# phase=none — see backend/pom.xml's comment on this plugin, same
# on-demand pattern as maven-pmd-plugin's complexity-report execution) so
# it never runs during mvn test/verify/package. Scoped to
# com.jpablodrexler.photomanager.application.usecase.* only — the real
# business-logic layer; infrastructure.web/infrastructure.persistence are
# thin translation code not worth mutating.
#
# Invoked via `mvn org.pitest:pitest-maven:mutationCoverage@mutation-report`
# (the goal invoked directly with its execution id, since phase=none means
# it never runs through the normal lifecycle). Unlike Stryker on the
# frontend side, PIT runs entirely inside the JVM test process — no browser
# boundary to bridge, no per-mutant process-spawn overhead, so this is far
# faster than the frontend's command-runner approach for a comparable
# mutant count.
#
# Parses target/pit-reports/mutations.csv (one row per mutant: fileName,
# className, mutator, method, lineNumber, status, killingTest — see
# pom.xml's <outputFormats>) for the per-class breakdown, and
# target/pit-reports/index.html's "Project Summary" table (a small,
# predictably-shaped block — see this script's PROJECT_SUMMARY parsing
# below) for PIT's own overall Line Coverage / Mutation Coverage / Test
# Strength percentages, rather than recomputing them independently.
#
# Usage: bash scripts/mutation-report.sh (run from backend/, or anywhere —
# it cd's to backend/ itself)

set -uo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

echo "Running PIT mutation testing (application.usecase scope)..."
mvn org.pitest:pitest-maven:mutationCoverage@mutation-report -q || echo "PIT run reported a non-zero exit — continuing to report whatever mutation data was captured."

CSV="target/pit-reports/mutations.csv"
INDEX_HTML="target/pit-reports/index.html"
if [ ! -f "$CSV" ]; then
  echo "No mutation report found at $CSV — the run may have failed before producing any report." >&2
  exit 1
fi

# Project Summary table: <td>numClasses</td> then three <td>...<coverage_legend>M/N</coverage_legend>...</td>
# cells for Line Coverage / Mutation Coverage / Test Strength, in that column order (see pom.xml's
# <outputFormats>HTML</outputFormats> and this file's header comment). Capture each full <td>...</td>
# cell (not just up to the first nested tag) so both the leading percentage and the
# coverage_legend fraction can be pulled from the same cell afterward.
PROJECT_SUMMARY="$(awk '
  /<h3>Project Summary<\/h3>/ { infield=1 }
  infield && /<td>/ {
    line = $0
    while (match(line, /<td>.*<\/td>/)) {
      cell = substr(line, RSTART+4, RLENGTH-9)
      print cell
      line = substr(line, RSTART+RLENGTH)
    }
  }
  infield && /<\/table>/ { exit }
' "$INDEX_HTML")"

cell() { echo "$PROJECT_SUMMARY" | sed -n "${1}p"; }
pct_of() { cell "$1" | grep -o '^[0-9.]*%' | tr -d '%'; }
frac_of() { cell "$1" | grep -o 'coverage_legend">[0-9]*/[0-9]*' | cut -d'>' -f2; }

NUM_CLASSES="$(cell 1 | tr -d ' ')"
LINE_COV_PCT="$(pct_of 2)"
MUT_COV_PCT="$(pct_of 3)"
TEST_STRENGTH_PCT="$(pct_of 4)"
LINE_COV_FRAC="$(frac_of 2)"
MUT_COV_FRAC="$(frac_of 3)"

# A single pass avoids `grep -c . || echo 0` per status: grep -c already
# prints "0" on no match but still exits 1, so the `|| echo 0` fallback
# would ALSO fire and double-print "0\n0", corrupting the arithmetic below
# whenever a status count is genuinely zero.
STATUS_COUNTS="$(awk -F',' '
  { total++; count[$6]++ }
  END {
    printf "%d\t%d\t%d\t%d\t%d\t%d\n", total+0, count["KILLED"]+0, count["SURVIVED"]+0, count["NO_COVERAGE"]+0, count["TIMED_OUT"]+0, count["NON_VIABLE"]+0
  }
' "$CSV")"
TOTAL="$(echo "$STATUS_COUNTS" | cut -f1)"
KILLED="$(echo "$STATUS_COUNTS" | cut -f2)"
SURVIVED="$(echo "$STATUS_COUNTS" | cut -f3)"
NO_COVERAGE="$(echo "$STATUS_COUNTS" | cut -f4)"
TIMED_OUT="$(echo "$STATUS_COUNTS" | cut -f5)"
NON_VIABLE="$(echo "$STATUS_COUNTS" | cut -f6)"
OTHER="$((TOTAL - KILLED - SURVIVED - NO_COVERAGE - TIMED_OUT - NON_VIABLE))"

# Per-class breakdown, sorted worst (lowest kill rate) first.
PER_CLASS="$(awk -F',' '
  {
    cls=$2
    total[cls]++
    if ($6=="KILLED") killed[cls]++
    else if ($6=="SURVIVED") survived[cls]++
    else if ($6=="NO_COVERAGE") nocov[cls]++
    else if ($6=="TIMED_OUT") timedout[cls]++
  }
  END {
    for (c in total) {
      k = killed[c]+0
      pct = (total[c] > 0) ? (100*k/total[c]) : 0
      printf "%.1f\t%s\t%d\t%d\t%d\t%d\t%d\n", pct, c, total[c], k, survived[c]+0, nocov[c]+0, timedout[c]+0
    }
  }
' "$CSV" | sort -t$'\t' -k1,1n)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/mutation"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/MUTATION_REPORT_${DATE}_backend.md"

{
  echo "# Mutation Testing Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** com.jpablodrexler.photomanager.application.usecase.* ($NUM_CLASSES classes)"
  echo "**Tool:** PIT (\`mvn org.pitest:pitest-maven:mutationCoverage@mutation-report\`)"
  echo
  echo "**Line coverage:** ${LINE_COV_PCT}% (${LINE_COV_FRAC})"
  echo "**Mutation coverage:** ${MUT_COV_PCT}% (${MUT_COV_FRAC})"
  echo "**Test strength:** ${TEST_STRENGTH_PCT}%"
  echo "**Total mutants:** $TOTAL"
  echo "**Killed:** $KILLED  **Survived:** $SURVIVED  **No coverage:** $NO_COVERAGE  **Timed out:** $TIMED_OUT  **Non-viable:** $NON_VIABLE  **Other:** $OTHER"
  echo
  echo "A survived mutant means the test suite ran and passed even though the mutated line changed the code's behavior — the tests exercise that code path but don't actually assert on the behavior a bug there would break. A \"no coverage\" mutant means no test reaches that line at all."
  echo
  echo "## Per-class results (worst mutation score first)"
  echo
  echo "| Class | Score | Total | Killed | Survived | No cov | Timed out |"
  echo "| --- | --- | --- | --- | --- | --- | --- |"
  echo "$PER_CLASS" | awk -F'\t' '{printf "| %s | %s%% | %s | %s | %s | %s | %s |\n", $2, $1, $3, $4, $5, $6, $7}'
  echo
  SURVIVED_ROWS="$(awk -F',' '$6=="SURVIVED" || $6=="NO_COVERAGE" {printf "%s\t%s\t%s\t%s\t%s\n", $2, $4, $5, $3, $6}' "$CSV" | sort)"
  if [ -n "$SURVIVED_ROWS" ]; then
    SURVIVED_COUNT="$(echo "$SURVIVED_ROWS" | grep -c .)"
    echo "## $SURVIVED_COUNT surviving / uncovered mutant(s)"
    echo
    echo "| Class | Method | Line | Mutator | Status |"
    echo "| --- | --- | --- | --- | --- |"
    echo "$SURVIVED_ROWS" | awk -F'\t' '{n=split($4,parts,"."); mutator=parts[n]; printf "| %s | %s | %s | %s | %s |\n", $1, $2, $3, mutator, $5}'
  else
    echo "No surviving or uncovered mutants — every mutant in scope was killed."
  fi
  echo
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
