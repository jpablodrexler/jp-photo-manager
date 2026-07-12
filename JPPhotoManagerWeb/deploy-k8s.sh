#!/usr/bin/env bash
set -euo pipefail

# Installs the ingress-nginx controller (if not already present) and applies
# the full Kubernetes configuration for the web application — the exact
# steps documented in README.md's "Running with Kubernetes" section
# (Prerequisites + Setup), automated so you don't have to run them by hand.
# Safe to re-run — every step here is idempotent.
#
# Usage: ./deploy-k8s.sh   (run from anywhere; the script cds to its own dir)
#
# Prerequisite: k8s/secret.yaml and k8s/catalog-volumes.yaml must already
# exist (copied from their .example templates and filled in) — this script
# does not create them, since they hold machine-specific / sensitive values
# that must never be scripted into existence with placeholder content.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

INGRESS_NGINX_MANIFEST="https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml"

# Under WSL, `kubectl` commonly has no kubeconfig of its own even when the
# binary itself resolves fine — it then silently falls back to the legacy
# localhost:8080 default and fails with "connection refused" instead of a
# clear error. This can't be fixed via ~/.bashrc: `wsl bash deploy-k8s.sh`
# is a non-interactive invocation, and non-interactive non-login shells
# never source ~/.bashrc, so a variable exported there is invisible here
# regardless. Detect that situation and fall back to the Windows-side
# kubeconfig instead, which Docker Desktop's own kubectl (Git Bash,
# PowerShell) already uses successfully.
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

echo "==> Checking required local configuration files ..."
MISSING=0
if [ ! -f k8s/secret.yaml ]; then
    echo "ERROR: k8s/secret.yaml not found." >&2
    echo "       Copy the template and fill in real values first:" >&2
    echo "         cp k8s/secret.yaml.example k8s/secret.yaml" >&2
    MISSING=1
fi
if [ ! -f k8s/catalog-volumes.yaml ]; then
    echo "ERROR: k8s/catalog-volumes.yaml not found." >&2
    echo "       Copy the template and set your real photo directory path(s) first:" >&2
    echo "         cp k8s/catalog-volumes.yaml.example k8s/catalog-volumes.yaml" >&2
    MISSING=1
fi
if [ "$MISSING" -eq 1 ]; then
    exit 1
fi
echo "    Found k8s/secret.yaml and k8s/catalog-volumes.yaml."

echo "==> Installing/updating the ingress-nginx controller ..."
kubectl apply -f "$INGRESS_NGINX_MANIFEST"

echo "==> Waiting for the ingress-nginx controller pod to be scheduled ..."
ELAPSED=0
TIMEOUT=120
until kubectl get pods -n ingress-nginx -l app.kubernetes.io/component=controller --no-headers 2>/dev/null | grep -q .; do
    if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
        echo "ERROR: ingress-nginx controller pod was not scheduled within ${TIMEOUT} seconds." >&2
        exit 1
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "==> Waiting for the ingress-nginx controller pod to become ready (can take a few minutes on first install) ..."
kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=300s
echo "    ingress-nginx controller is ready."

echo "==> Applying the secret ..."
kubectl apply -f k8s/secret.yaml

echo "==> Applying the Kubernetes stack (namespace, ConfigMaps, Services, StatefulSets, Deployments, PVCs, Ingress) ..."
kubectl apply -k .

echo ""
echo "Deployment applied successfully!"
echo ""
echo "==> Current pod status:"
kubectl get pods -n photomanager

echo ""
echo "Next steps:"
echo "  1. Watch pods come up (a first-time backend startup can take several"
echo "     minutes under CPU contention — see README's Troubleshooting section"
echo "     if pods keep restarting instead of settling):"
echo "       kubectl get pods -n photomanager -w"
echo "  2. One-time only — add a hosts-file entry so photomanager.local resolves"
echo "     (requires admin):"
echo "       Windows (PowerShell as Administrator):"
echo "         Add-Content -Path \"\$env:SystemRoot\\System32\\drivers\\etc\\hosts\" -Value \"127.0.0.1 photomanager.local\""
echo "       Linux/macOS:"
echo "         echo \"127.0.0.1 photomanager.local\" | sudo tee -a /etc/hosts"
echo "  3. Open http://photomanager.local"
