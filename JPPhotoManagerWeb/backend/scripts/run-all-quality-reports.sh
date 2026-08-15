#!/usr/bin/env bash
# Runs every backend dated quality-metrics report script back to back (the
# "Running every metric manually" list in the root README's Quality Metrics
# section), so there's one command instead of running each scripts/*.sh
# file by hand. Each underlying script already writes its own dated file
# under docs/reports/<category>/ and is safe to re-run on its own — this is
# just a sequencing convenience, not a new report of its own. The frontend
# (Angular) equivalent is frontend/scripts/run-all-quality-reports.js, which
# does the same thing over the npm report scripts in frontend/.
#
# mutation-report.sh runs a full PIT mutation-testing pass and is by far the
# slowest report here — skipped by default, pass --with-mutation to include
# it.
#
# A failing report does not stop the run — every remaining report still
# gets a chance to produce its snapshot. This script exits non-zero at the
# end if anything failed, so it still works as a CI-style gate if wired
# into one later.
#
# Usage: bash scripts/run-all-quality-reports.sh [--with-mutation] [--only=key1,key2]
# (run from backend/, or anywhere — it cd's to backend/ itself)

set -uo pipefail

cd "$(dirname "$0")/.."

WITH_MUTATION=false
ONLY=""
for arg in "$@"; do
  case "$arg" in
    --with-mutation) WITH_MUTATION=true ;;
    --only=*) ONLY="${arg#--only=}" ;;
  esac
done

declare -a KEYS=(complexity dead-code auth-coverage code-coverage dependency-staleness license-compliance dependency-vulnerabilities mutation)
declare -A LABELS=(
  [complexity]="Complexity / file size"
  [dead-code]="Dead code"
  [auth-coverage]="Auth coverage (Spring Security rules)"
  [code-coverage]="Code coverage trend"
  [dependency-staleness]="Dependency staleness"
  [license-compliance]="License compliance"
  [dependency-vulnerabilities]="Dependency vulnerabilities (SCA)"
  [mutation]="Mutation testing"
)
declare -A SCRIPTS=(
  [complexity]="scripts/complexity-report.sh"
  [dead-code]="scripts/dead-code-report.sh"
  [auth-coverage]="scripts/auth-coverage-report.sh"
  [code-coverage]="scripts/coverage-report.sh"
  [dependency-staleness]="scripts/dependency-staleness-report.sh"
  [license-compliance]="scripts/license-report.sh"
  [dependency-vulnerabilities]="scripts/sca-report.sh"
  [mutation]="scripts/mutation-report.sh"
)

is_only_selected() {
  local key="$1"
  [[ -z "$ONLY" ]] && return 0
  IFS=',' read -ra want <<< "$ONLY"
  for w in "${want[@]}"; do
    [[ "$w" == "$key" ]] && return 0
  done
  return 1
}

declare -A STATUS

for key in "${KEYS[@]}"; do
  if [[ -n "$ONLY" ]] && ! is_only_selected "$key"; then
    continue
  fi
  if [[ "$key" == "mutation" && "$WITH_MUTATION" == false && -z "$ONLY" ]]; then
    echo
    echo "=== Skipping: ${LABELS[$key]} (by far the slowest report — a full PIT run; pass --with-mutation to include) ==="
    STATUS[$key]="SKIP"
    continue
  fi

  echo
  echo "=== Running: ${LABELS[$key]} (bash ${SCRIPTS[$key]}) ==="
  if bash "${SCRIPTS[$key]}"; then
    STATUS[$key]="OK"
  else
    echo
    echo "!!! ${LABELS[$key]} failed (bash ${SCRIPTS[$key]}) — continuing with the rest."
    STATUS[$key]="FAILED"
  fi
done

echo
echo "=== Backend quality metrics report summary ==="
FAILED_COUNT=0
for key in "${KEYS[@]}"; do
  [[ -z "${STATUS[$key]:-}" ]] && continue
  printf '%-6s  %s\n' "${STATUS[$key]}" "${LABELS[$key]}"
  [[ "${STATUS[$key]}" == "FAILED" ]] && FAILED_COUNT=$((FAILED_COUNT + 1))
done

if [[ "$FAILED_COUNT" -gt 0 ]]; then
  echo
  echo "$FAILED_COUNT report(s) failed. See output above for details."
  exit 1
fi

echo
echo "All requested reports completed. See docs/reports/<category>/ for the dated output files."
