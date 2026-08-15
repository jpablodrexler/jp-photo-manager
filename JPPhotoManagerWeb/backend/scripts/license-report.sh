#!/usr/bin/env bash
# Reports every backend Maven dependency's declared license via
# license-maven-plugin's `add-third-party` goal, and writes a dated
# snapshot report under JPPhotoManagerWeb/docs/reports/license-compliance/,
# the same convention every other quality-metric script in this repo uses.
# The frontend equivalent is frontend/scripts/license-compliance-report.js
# (license-checker-rseidelsohn).
#
# Deliberately invokes the plugin goal via its full coordinate
# (org.codehaus.mojo:license-maven-plugin:GOAL) rather than declaring it in
# pom.xml — Maven allows any plugin goal to be run ad hoc this way, so this
# report needs zero pom.xml changes and can never accidentally hook into
# the normal `mvn test`/`mvn verify` lifecycle the way a declared execution
# without `phase>none<` could. Uses the plain `add-third-party` goal, not
# `aggregate-add-third-party` — the aggregate variant is for multi-module
# (packaging `pom`) projects and silently no-ops here (single `jar` module).
#
# Flags copyleft licenses (GPL/AGPL/LGPL/SSPL/EUPL family) and anything
# with no resolvable license — this repo is public, so an unnoticed
# copyleft dependency is a real concern, not just hygiene.
#
# REVIEWED_EXCEPTIONS below holds dependencies that were already
# investigated and found not to be a real compliance problem, so the
# report doesn't re-demand the same investigation on every run. Add an
# entry only after actually confirming (not assuming) how the dependency
# is linked — LGPL specifically permits unmodified dynamic/API-level
# linking without imposing copyleft on the consuming application; it is
# only GPL-equivalent if the library itself were modified/vendored, which
# is not the case for anything on this list. A reviewed dependency still
# appears in the report, in its own section, so nothing is hidden — it
# just isn't counted toward "needs action."
#
# Usage: bash scripts/license-report.sh (run from backend/, or anywhere —
# it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/license-compliance"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/LICENSE_COMPLIANCE_REPORT_${DATE}_backend.md"

echo "Collecting dependency licenses with license-maven-plugin..."
mvn -q org.codehaus.mojo:license-maven-plugin:2.5.0:add-third-party -Dlicense.excludedScopes=test

THIRD_PARTY_FILE="target/generated-sources/license/THIRD-PARTY.txt"
if [ ! -f "$THIRD_PARTY_FILE" ]; then
  echo "ERROR: $THIRD_PARTY_FILE was not generated." >&2
  exit 1
fi

TOTAL="$(grep -c '^\s*(' "$THIRD_PARTY_FILE" || true)"

# groupId:artifactId => one-line reason it's confirmed compliant despite a
# copyleft license, so the report can explain the exception inline rather
# than just naming it.
declare -A REVIEWED_EXCEPTIONS=(
  ["net.jthink:jaudiotagger"]="LGPLv3. Confirmed compile-scope, unmodified dependency (no vendored/patched jaudiotagger source anywhere in this repo) consumed only through its public API in a single adapter class (infrastructure/service/AudioMetadataService.java, ~15 lines: AudioFileIO.read() plus getters on the returned AudioFile/Tag/Artwork) — exactly the unmodified dynamic-linking case LGPL permits without imposing copyleft on the consuming application. The only realistic permissive alternative (mp3agic, MIT) is MP3/ID3-only, so swapping would risk a functional regression for FLAC/OGG/M4A files versus jaudiotagger's format-agnostic reader, for zero compliance benefit."
)

REVIEWED_ROWS=""
FLAGGED_ROWS=""
while IFS= read -r line; do
  if [[ "$line" =~ ^\ *\(([^\)]*)\)\ *(.*)$ ]]; then
    licenses="${BASH_REMATCH[1]}"
    rest="${BASH_REMATCH[2]}"
  elif [[ "$line" =~ ^\ *Unknown\ license\ *(.*)$ ]]; then
    licenses="UNKNOWN"
    rest="${BASH_REMATCH[1]}"
  else
    continue
  fi
  if [[ ! "$licenses" =~ (A?GPL|LGPL|SSPL|EUPL|CC-BY-SA|OSL|CPAL) ]] && [[ "$licenses" != "UNKNOWN" ]]; then
    continue
  fi
  coordinate="$(echo "$rest" | grep -oE '[A-Za-z0-9_.\-]+:[A-Za-z0-9_.\-]+' | head -1)"
  reason=""
  for key in "${!REVIEWED_EXCEPTIONS[@]}"; do
    if [[ "$coordinate" == "$key"* ]]; then
      reason="${REVIEWED_EXCEPTIONS[$key]}"
      break
    fi
  done
  if [ -n "$reason" ]; then
    REVIEWED_ROWS="${REVIEWED_ROWS}| $rest | $licenses | $reason |\n"
  else
    FLAGGED_ROWS="${FLAGGED_ROWS}| $rest | $licenses |\n"
  fi
done < "$THIRD_PARTY_FILE"

FLAGGED_COUNT="$(echo -en "$FLAGGED_ROWS" | grep -c '^|' || true)"
REVIEWED_COUNT="$(echo -en "$REVIEWED_ROWS" | grep -c '^|' || true)"

{
  echo "# License Compliance Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** backend Maven dependency tree, excluding test scope (license-maven-plugin add-third-party)"
  echo
  echo "**$TOTAL package(s) scanned, $FLAGGED_COUNT need action, $REVIEWED_COUNT previously reviewed and accepted.**"
  echo
  if [ "$FLAGGED_COUNT" -gt 0 ]; then
    echo "## Needs action"
    echo
    echo "| Component | License(s) |"
    echo "| --- | --- |"
    echo -en "$FLAGGED_ROWS"
    echo
    echo "A copyleft license here is worth a second look before shipping — check whether the flagged component is genuinely bundled into the deployed backend image (most Spring Boot dependencies are) versus a build-time-only tool. \`UNKNOWN\` means license-maven-plugin could not resolve a license from the artifact's own POM metadata — check its repository directly rather than assuming the worst. Once reviewed, add it to this script's own \`REVIEWED_EXCEPTIONS\` map with the reasoning rather than re-investigating it on every future run."
    echo
  fi
  if [ "$REVIEWED_COUNT" -gt 0 ]; then
    echo "## Previously reviewed and accepted"
    echo
    echo "| Component | License(s) | Why it's accepted |"
    echo "| --- | --- | --- |"
    echo -en "$REVIEWED_ROWS"
    echo
  fi
  if [ "$FLAGGED_COUNT" -eq 0 ] && [ "$REVIEWED_COUNT" -eq 0 ]; then
    echo "No copyleft or unresolvable licenses found among scanned dependencies."
  fi
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
