#!/usr/bin/env bash
set -uo pipefail

# Runs both halves of the quality-metrics suite — frontend/scripts/run-all-
# quality-reports.js (via `npm run reports:all`) and backend/scripts/run-all-
# quality-reports.sh — from one place, so there's a single command to run
# instead of remembering to cd into both `frontend/` and `backend/`
# separately. Each half is independently safe to re-run and already skips
# its own slowest/most-setup-dependent report(s) by default (real-backend
# E2E on the frontend side, mutation testing on both) — see those two
# scripts' own header comments for the full breakdown.
#
# Usage: ./scripts/run-all-quality-reports.sh [--with-e2e-real] [--with-mutation]
#   (run from anywhere; cds to JPPhotoManagerWeb/ itself)
#   --with-e2e-real   Forwarded to the frontend run only (needs the full app
#                      already deployed to k8s — see the e2e-suite skill §1).
#   --with-mutation   Forwarded to both the frontend and backend runs.
#
# For scoping to a single report (`--only=<key>`), call the frontend/backend
# scripts directly instead — the two use separate key namespaces, so there's
# no single `--only` value that would mean the same thing to both.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

FRONTEND_ARGS=()
BACKEND_ARGS=()
for arg in "$@"; do
    case "$arg" in
        --with-e2e-real) FRONTEND_ARGS+=("--with-e2e-real") ;;
        --with-mutation) FRONTEND_ARGS+=("--with-mutation"); BACKEND_ARGS+=("--with-mutation") ;;
        *) echo "Unknown argument: $arg" >&2; exit 1 ;;
    esac
done

echo "==> Running frontend quality-metrics reports (npm run reports:all) ..."
FRONTEND_STATUS="OK"
(cd frontend && npm run reports:all -- "${FRONTEND_ARGS[@]}") || FRONTEND_STATUS="FAILED"

echo ""
echo "==> Running backend quality-metrics reports (bash scripts/run-all-quality-reports.sh) ..."
BACKEND_STATUS="OK"
(cd backend && bash scripts/run-all-quality-reports.sh "${BACKEND_ARGS[@]}") || BACKEND_STATUS="FAILED"

echo ""
echo "=== Quality metrics report summary ==="
printf '%-6s  %s\n' "$FRONTEND_STATUS" "Frontend (see per-report summary above)"
printf '%-6s  %s\n' "$BACKEND_STATUS" "Backend (see per-report summary above)"

if [ "$FRONTEND_STATUS" = "FAILED" ] || [ "$BACKEND_STATUS" = "FAILED" ]; then
    echo ""
    echo "One or both halves had a failing report — see the per-report summaries above for which one."
    exit 1
fi

echo ""
echo "All requested reports completed. See docs/reports/<category>/ for the dated output files."
