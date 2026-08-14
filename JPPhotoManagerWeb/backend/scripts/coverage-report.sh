#!/usr/bin/env bash
# Wraps the backend's JaCoCo line-coverage run into a dated trend snapshot
# under JPPhotoManagerWeb/docs/reports/code-coverage/, the same convention
# bundle-size-report.js/dependency-staleness-report.js use. `mvn
# jacoco:check` (code-reviewer skill §19.2) is a pass/fail gate against a
# fixed 80% threshold — useful in CI, but it doesn't leave a record of the
# *actual* percentage over time, or which specific classes are furthest
# from the threshold. This captures both, mirroring the frontend's
# scripts/code-coverage-report.js.
#
# Runs the test suite itself (so this is self-contained — no need to have
# already run `mvn test`), then parses target/site/jacoco/jacoco.xml
# (produced by the `report` execution already bound to the `test` phase —
# see pom.xml's jacoco-maven-plugin config) via a small inline Perl script:
# jacoco.xml is single-line, deeply nested XML (method-level, class-level,
# package-level, and report-level <counter> elements all share the same
# tag name, distinguished only by nesting position), which plain grep/awk
# can't reliably disambiguate but a real (if minimal) parser can. Perl is
# used here rather than Node specifically to keep this script's own
# dependencies to what's already available in a bash-script context — no
# new tool to install, unlike introducing Node into a Java-only module.
#
# Usage: bash scripts/coverage-report.sh (run from backend/, or anywhere —
# it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
THRESHOLD=80

echo "Running backend test suite with JaCoCo instrumentation..."
mvn test -q || echo "Test run reported failures — continuing to report whatever coverage was captured."

JACOCO_XML="target/site/jacoco/jacoco.xml"
if [ ! -f "$JACOCO_XML" ]; then
  echo "No JaCoCo report found at $JACOCO_XML — the test run may have failed before producing coverage data." >&2
  exit 1
fi

PARSED="$(perl -0777 -ne '
  # Report-level (whole-project) totals: everything between the last
  # </package> and </report> is exactly the report-level <counter> run —
  # package/class/method-level counters are nested earlier in the file.
  my %totals;
  if (/<\/package>((?:<counter[^>]*\/>)+)<\/report>/s) {
    my $report_counters = $1; # save before the inner match below overwrites $1/$2/$3
    while ($report_counters =~ /<counter type="(\w+)" missed="(\d+)" covered="(\d+)"\/>/g) {
      $totals{$1} = [$2, $3];
    }
  }
  for my $type (qw(LINE BRANCH METHOD)) {
    my ($m, $c) = @{ $totals{$type} // [0, 0] };
    print "TOTAL\t$type\t$m\t$c\n";
  }

  # Per-class LINE coverage: split on <class blocks, and within each, take
  # the LAST <counter type="LINE" .../> before that class'\''s own </class>
  # (methods have their own earlier LINE counters; the class summary is
  # always the final one in the block).
  for my $chunk (split /<class /) {
    next unless $chunk =~ /^name="([^"]+)"/;
    my $name = $1;
    next unless $chunk =~ /^.*?<\/class>/s;
    my ($classBody) = $chunk =~ /^(.*?<\/class>)/s;
    my @lineCounters = $classBody =~ /<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/g;
    next unless @lineCounters;
    my $n = scalar(@lineCounters) / 2;
    my $missed = $lineCounters[($n - 1) * 2];
    my $covered = $lineCounters[($n - 1) * 2 + 1];
    next if $missed + $covered == 0;
    my $pct = 100 * $covered / ($missed + $covered);
    printf "CLASS\t%s\t%.1f\t%d\t%d\n", $name, $pct, $missed, $covered;
  }
' "$JACOCO_XML")"

TOTAL_LINE="$(echo "$PARSED" | awk -F'\t' '$1=="TOTAL" && $2=="LINE" {print $3, $4}')"
TOTAL_BRANCH="$(echo "$PARSED" | awk -F'\t' '$1=="TOTAL" && $2=="BRANCH" {print $3, $4}')"
TOTAL_METHOD="$(echo "$PARSED" | awk -F'\t' '$1=="TOTAL" && $2=="METHOD" {print $3, $4}')"

LINE_MISSED=$(echo "$TOTAL_LINE" | awk '{print $1}')
LINE_COVERED=$(echo "$TOTAL_LINE" | awk '{print $2}')
LINE_PCT=$(awk -v m="$LINE_MISSED" -v c="$LINE_COVERED" 'BEGIN{ t=m+c; printf "%.2f", (t>0 ? 100*c/t : 0) }')

BRANCH_MISSED=$(echo "$TOTAL_BRANCH" | awk '{print $1}')
BRANCH_COVERED=$(echo "$TOTAL_BRANCH" | awk '{print $2}')
BRANCH_PCT=$(awk -v m="$BRANCH_MISSED" -v c="$BRANCH_COVERED" 'BEGIN{ t=m+c; printf "%.2f", (t>0 ? 100*c/t : 0) }')

METHOD_MISSED=$(echo "$TOTAL_METHOD" | awk '{print $1}')
METHOD_COVERED=$(echo "$TOTAL_METHOD" | awk '{print $2}')
METHOD_PCT=$(awk -v m="$METHOD_MISSED" -v c="$METHOD_COVERED" 'BEGIN{ t=m+c; printf "%.2f", (t>0 ? 100*c/t : 0) }')

BELOW_THRESHOLD="$(echo "$PARSED" | awk -F'\t' -v th="$THRESHOLD" '$1=="CLASS" && $3+0 < th {printf "%s\t%.1f%%\t%s\t%s\n", $2, $3, $4, $5}' | sort -t$'\t' -k2,2n)"
BELOW_COUNT="$(echo "$BELOW_THRESHOLD" | grep -c . || true)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/code-coverage"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/CODE_COVERAGE_REPORT_${DATE}_backend.md"

{
  echo "# Code Coverage Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend (target/site/jacoco/jacoco.xml)"
  echo "**Gate:** \`mvn jacoco:check\` enforces ${THRESHOLD}% minimum line coverage, project-wide (code-reviewer skill §19.2)"
  echo
  echo "**Lines:** ${LINE_PCT}% (${LINE_COVERED}/$((LINE_MISSED + LINE_COVERED)))"
  echo "**Branches:** ${BRANCH_PCT}% (${BRANCH_COVERED}/$((BRANCH_MISSED + BRANCH_COVERED)))"
  echo "**Methods:** ${METHOD_PCT}% (${METHOD_COVERED}/$((METHOD_MISSED + METHOD_COVERED)))"
  echo
  if [ -n "$BELOW_THRESHOLD" ]; then
    echo "## $BELOW_COUNT class(es) below ${THRESHOLD}% line coverage"
    echo
    echo "| Class | Line coverage | Lines missed | Lines covered |"
    echo "| --- | --- | --- | --- |"
    echo "$BELOW_THRESHOLD" | awk -F'\t' '{printf "| %s | %s | %s | %s |\n", $1, $2, $3, $4}'
  else
    echo "Every class is at or above ${THRESHOLD}% line coverage."
  fi
  echo
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
