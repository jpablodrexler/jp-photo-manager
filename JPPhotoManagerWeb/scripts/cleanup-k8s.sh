#!/usr/bin/env bash
set -euo pipefail

# Tears down the whole `photomanager` Kubernetes stack — Deployments,
# StatefulSets, Services, ConfigMaps, the Secret, Ingress, and every
# PersistentVolumeClaim (PostgreSQL, MongoDB, Kafka, Grafana, backend
# thumbnail cache) — plus the locally built photomanager-backend/frontend
# Docker images, so ./build-and-deploy-k8s.sh can be re-run for a completely
# fresh deployment. Automates the teardown commands documented in README.md's
# "Common commands" section ("Tear down and wipe all persistent data").
#
# WARNING: this permanently deletes ALL persistent data in the cluster for
# this app — there is no undo. Your actual photo library on disk (the
# k8s/catalog-volumes.yaml hostPath mounts) is NOT touched; only the
# cluster's copies of catalog metadata/thumbnails/audit logs/etc. are wiped.
# Re-running build-and-deploy-k8s.sh followed by a catalog scan rebuilds
# them from your files.
#
# The ingress-nginx controller is intentionally left in place — it's shared,
# cluster-wide infrastructure (not namespaced to `photomanager`) that's slow
# to reinstall and safe to leave running between deployments.
#
# Usage: ./cleanup-k8s.sh [-y|--yes]   (run from anywhere; cds to JPPhotoManagerWeb/)
#   -y, --yes   Skip the confirmation prompt (for scripted/non-interactive use).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

SKIP_CONFIRM=0
for arg in "$@"; do
    case "$arg" in
        -y|--yes) SKIP_CONFIRM=1 ;;
        *) echo "Unknown argument: $arg" >&2; exit 1 ;;
    esac
done

# Under WSL, `kubectl` commonly has no kubeconfig of its own even when the
# binary itself resolves fine — it then silently falls back to the legacy
# localhost:8080 default and fails with "connection refused" instead of a
# clear error. Same fallback used by build-and-deploy-k8s.sh / port-forward-k8s.sh.
if [ -z "${KUBECONFIG:-}" ] && [ ! -s "${HOME}/.kube/config" ] && grep -qi microsoft /proc/version 2>/dev/null; then
    WIN_PROFILE="$(cmd.exe /c "echo %USERPROFILE%" 2>/dev/null | tr -d '\r')"
    if [ -n "$WIN_PROFILE" ]; then
        WIN_KUBECONFIG="$(wslpath -u "$WIN_PROFILE")/.kube/config"
        if [ -f "$WIN_KUBECONFIG" ]; then
            export KUBECONFIG="$WIN_KUBECONFIG"
            echo "==> WSL detected with no local kubeconfig — using Windows kubeconfig: $KUBECONFIG"
        fi
    fi
fi

echo "==> Checking kubectl connectivity ..."
kubectl cluster-info >/dev/null
echo "    Connected."

NAMESPACE_EXISTS=0
if kubectl get namespace photomanager >/dev/null 2>&1; then
    NAMESPACE_EXISTS=1
fi

if [ "$SKIP_CONFIRM" -ne 1 ]; then
    echo ""
    echo "This will PERMANENTLY delete:"
    echo "  - The entire 'photomanager' namespace (all Deployments, StatefulSets,"
    echo "    Services, ConfigMaps, the Secret, and Ingress)"
    echo "  - Every PersistentVolumeClaim in that namespace — PostgreSQL, MongoDB,"
    echo "    Kafka, Grafana, and the backend thumbnail cache all lose their data"
    echo "  - The local 'photomanager-backend:latest' and 'photomanager-frontend:latest'"
    echo "    Docker images"
    echo ""
    echo "Your actual photo library on disk (k8s/catalog-volumes.yaml hostPath"
    echo "mounts) is NOT touched. The ingress-nginx controller is NOT removed"
    echo "(shared cluster-wide infrastructure)."
    echo ""
    read -r -p "Continue? [y/N] " REPLY
    case "$REPLY" in
        [Yy]*) ;;
        *) echo "Aborted."; exit 1 ;;
    esac
fi

echo "==> Stopping any background port-forwards for the photomanager namespace ..."
# port-forward-k8s.sh starts plain `kubectl port-forward -n photomanager ...`
# background processes with no PID file of their own — matching on the
# command line is the only way to find and stop them before the namespace
# they point at disappears out from under them.
PF_PIDS="$(pgrep -f 'kubectl port-forward -n photomanager' || true)"
if [ -n "$PF_PIDS" ]; then
    # shellcheck disable=SC2086
    kill $PF_PIDS 2>/dev/null || true
    echo "    Stopped: $PF_PIDS"
else
    echo "    None running."
fi

if [ "$NAMESPACE_EXISTS" -eq 1 ]; then
    echo "==> Scaling backend/frontend to 0 first (avoids 'container is using its referenced image' on docker rmi below) ..."
    kubectl scale deployment/backend deployment/frontend -n photomanager --replicas=0 2>/dev/null || true

    echo "==> Deleting the Kubernetes stack (kubectl delete -k .) ..."
    kubectl delete -k . --ignore-not-found --wait=true

    echo "==> Deleting any PVCs left behind in the photomanager namespace ..."
    kubectl delete pvc --all -n photomanager --ignore-not-found 2>/dev/null || true

    echo "==> Waiting for the namespace to fully terminate ..."
    kubectl wait --for=delete namespace/photomanager --timeout=180s 2>/dev/null || true
else
    echo "==> Namespace 'photomanager' does not exist — nothing to delete in the cluster."
fi

echo "==> Checking for orphaned PersistentVolumes (a Retain reclaim policy could leave these behind) ..."
ORPHANED_PVS="$(kubectl get pv -o jsonpath='{range .items[?(@.spec.claimRef.namespace=="photomanager")]}{.metadata.name}{"\n"}{end}' 2>/dev/null || true)"
if [ -n "$ORPHANED_PVS" ]; then
    echo "    Found orphaned PVs (not auto-deleted — review before removing):"
    echo "$ORPHANED_PVS" | sed 's/^/      /'
    echo "    Remove with: kubectl delete pv <name>"
else
    echo "    None found."
fi

echo "==> Removing local Docker images ..."
docker rmi -f photomanager-backend:latest photomanager-frontend:latest 2>/dev/null || true
echo "    Done (already-missing images are silently skipped)."

echo ""
echo "Cleanup complete. Run ./build-and-deploy-k8s.sh for a fresh deployment."
