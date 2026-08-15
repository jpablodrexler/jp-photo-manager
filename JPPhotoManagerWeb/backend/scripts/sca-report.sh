#!/usr/bin/env bash
# Reports known vulnerabilities in the backend's resolved Maven dependency
# tree, via OSV.dev's public batch-query API — the backend equivalent of
# the frontend's `npm run sca:report` (npm audit). Distinct from
# dependency-staleness's own backend report: staleness tracks *outdated*
# dependencies, this tracks *vulnerable* ones — a dependency can be current
# and vulnerable, or stale and perfectly safe.
#
# Uses OSV.dev rather than OWASP Dependency-Check specifically because
# Dependency-Check's first run downloads the full NVD CVE database, which
# is rate-limited to a multi-hour pull without a personally-requested NVD
# API key (https://nvd.nist.gov/developers/request-an-api-key) — a real
# blocker for a report meant to run routinely. OSV.dev needs no API key,
# no local database, and answers a batch query over HTTPS in seconds; it
# aggregates GitHub Security Advisories (which cover Maven Central) among
# other sources. Uses curl + Perl's core JSON::PP module for the
# request/response — no jq (not guaranteed present) and no Node (this
# module has none, deliberately — see dead-code-report.sh's header comment
# for the same reasoning).
#
# Usage: bash scripts/sca-report.sh (run from backend/, or anywhere — it
# cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/dependency-vulnerabilities"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/SCA_REPORT_${DATE}_backend.md"

echo "Resolving the Maven dependency tree..."
DEPS_FILE="$(mktemp)"
mvn -q dependency:list -DincludeScope=compile -DoutputFile="$DEPS_FILE" -DappendOutput=false

echo "Querying OSV.dev for known vulnerabilities..."
QUERY_FILE="$(mktemp)"
RESULT_FILE="$(mktemp)"

# Build the batch-query JSON payload from the dependency:list output
# ("   groupId:artifactId:jar:version:scope" per line) and, once curl
# returns, cross-reference each query back to its package for the report —
# all with Perl's core JSON::PP, no external module install needed.
perl -e '
  use strict; use warnings; use JSON::PP;
  open(my $fh, "<", $ARGV[0]) or die $!;
  my @queries;
  my @packages;
  while (<$fh>) {
    # Maven appends module-info detail after the scope word on JDK 9+
    # ("...:compile -- module foo.bar [auto]", with ANSI color codes) — do
    # not end-anchor after (?:compile|runtime), just require it as a whole
    # word, and ignore whatever trails it.
    if (/^\s*([\w.\-]+):([\w.\-]+):[\w.\-]+:([\w.\-]+):(?:compile|runtime)\b/) {
      my ($group, $artifact, $version) = ($1, $2, $3);
      push @queries, { package => { name => "$group:$artifact", ecosystem => "Maven" }, version => $version };
      push @packages, "$group:$artifact:$version";
    }
  }
  close $fh;
  open(my $pf, ">", "'"$QUERY_FILE"'.packages") or die $!;
  print $pf "$_\n" for @packages;
  close $pf;
  my $json = JSON::PP->new->utf8->canonical;
  open(my $out, ">", $ARGV[1]) or die $!;
  print $out $json->encode({ queries => \@queries });
  close $out;
' "$DEPS_FILE" "$QUERY_FILE"

curl -s -X POST "https://api.osv.dev/v1/querybatch" \
  -H "Content-Type: application/json" \
  --data-binary "@$QUERY_FILE" \
  -o "$RESULT_FILE"

RAW_TABLE="$(perl -e '
  use strict; use warnings; use JSON::PP;
  my $json = JSON::PP->new->utf8;
  open(my $pf, "<", "'"$QUERY_FILE"'.packages") or die $!;
  my @packages = <$pf>;
  chomp @packages;
  close $pf;
  local $/;
  open(my $rf, "<", $ARGV[0]) or die $!;
  my $data = $json->decode(<$rf>);
  close $rf;
  my $results = $data->{results} || [];
  my $total = 0;
  for my $i (0 .. $#$results) {
    my $vulns = $results->[$i]{vulns} || [];
    next unless @$vulns;
    $total += scalar @$vulns;
    my $pkg = $packages[$i] // "unknown";
    my @ids = map { $_->{id} } @$vulns;
    print "| $pkg | " . scalar(@ids) . " | " . join(", ", @ids) . " |\n";
  }
  print STDERR "$total\n";
' "$RESULT_FILE" 2>"$RESULT_FILE.count")"

TOTAL_VULNS="$(cat "$RESULT_FILE.count")"

{
  echo "# Dependency Vulnerability (SCA) Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend Maven dependency tree, compile scope (OSV.dev batch query, https://api.osv.dev)"
  echo
  echo "**$TOTAL_VULNS known vulnerabilit$([ "$TOTAL_VULNS" = "1" ] && echo y || echo ies) across the resolved dependency tree.**"
  echo
  if [ -n "$RAW_TABLE" ]; then
    echo "| Package | Vuln count | OSV IDs |"
    echo "| --- | --- | --- |"
    echo "$RAW_TABLE"
    echo
    echo "Look up any ID at https://osv.dev/vulnerability/<id> for severity, affected version ranges, and the fixed version — the batch endpoint used here returns IDs only, not full advisory detail, to keep this a fast routine check rather than one HTTP call per finding. A vulnerability in a *test-scope-only* transitive dependency (not shown here — this query is compile-scope only) is lower priority than one reachable at runtime."
  else
    echo "No known vulnerabilities found in the resolved compile-scope dependency tree."
  fi
} > "$REPORT_PATH"

rm -f "$DEPS_FILE" "$QUERY_FILE" "$QUERY_FILE.packages" "$RESULT_FILE" "$RESULT_FILE.count"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
