#!/usr/bin/env bash
set -euo pipefail

# Builds the backend and frontend Docker images, installs the ingress-nginx
# controller (if not already present), and applies the full Kubernetes
# configuration for the web application — the exact steps documented in
# README.md's "Running with Kubernetes" section (Prerequisites + Setup),
# automated so you don't have to run them by hand. Safe to re-run — every
# step here is idempotent.
#
# Usage: ./build-and-deploy-k8s.sh   (run from anywhere; the script cds to JPPhotoManagerWeb/)
#
# Prerequisite: k8s/secret.yaml and k8s/catalog-volumes.yaml must already
# exist (copied from their .example templates and filled in) — this script
# does not create them, since they hold machine-specific / sensitive values
# that must never be scripted into existence with placeholder content.
#
# After applying the stack, this script calls ./port-forward-k8s.sh to start
# background port-forwards for Grafana, PostgreSQL, and MongoDB so those
# services are reachable at localhost immediately. That script lives on its
# own rather than being inlined here because it needs re-running any time a
# tunnel drops — a machine reboot, a Docker Desktop restart, or one of
# those pods being replaced — none of which require rebuilding images or
# reapplying manifests, so it shouldn't require rerunning this whole script.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

INGRESS_NGINX_MANIFEST="https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml"

# Under WSL, `kubectl` commonly has no kubeconfig of its own even when the
# binary itself resolves fine — it then silently falls back to the legacy
# localhost:8080 default and fails with "connection refused" instead of a
# clear error. This can't be fixed via ~/.bashrc: `wsl bash build-and-deploy-k8s.sh`
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

echo "==> Building backend and frontend images ..."
docker build -t photomanager-backend:latest ./backend
docker build -t photomanager-frontend:latest ./frontend

echo "==> Making images visible to the cluster ..."
CURRENT_CONTEXT="$(kubectl config current-context)"
case "$CURRENT_CONTEXT" in
    kind-*)
        kind load docker-image photomanager-backend:latest photomanager-frontend:latest --name "${CURRENT_CONTEXT#kind-}"
        ;;
    minikube)
        minikube image load photomanager-backend:latest
        minikube image load photomanager-frontend:latest
        ;;
    *)
        echo "    Context '$CURRENT_CONTEXT' shares the local Docker image cache directly (e.g. Docker Desktop) — no extra step needed."
        ;;
esac

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

echo "==> Applying the namespace (secret below targets it and must not run first on a fresh cluster) ..."
kubectl apply -f k8s/namespace.yaml

echo "==> Applying the secret ..."
kubectl apply -f k8s/secret.yaml

echo "==> Applying the Kubernetes stack (namespace, ConfigMaps, Services, StatefulSets, Deployments, PVCs, Ingress) ..."
kubectl apply -k .

echo "==> Restarting backend/frontend so they pick up the freshly built images ..."
# imagePullPolicy: IfNotPresent means the kubelet won't repull a :latest tag
# it already has cached, even though the image we just built above replaced
# its content — an explicit rollout restart is the only way already-running
# pods pick up the new image. Harmless to run on a brand-new deployment too.
kubectl rollout restart deployment/backend deployment/frontend -n photomanager

echo ""
echo "Deployment applied successfully!"
echo ""
echo "==> Current pod status:"
kubectl get pods -n photomanager

echo ""
echo "==> Starting Grafana/PostgreSQL/MongoDB port-forwards ..."
"$SCRIPT_DIR/port-forward-k8s.sh"

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
echo "  4. Grafana, PostgreSQL, and MongoDB port-forwards were started above —"
echo "     see the output for their addresses and the 'kill <PID>' command to"
echo "     stop them. Re-run ./port-forward-k8s.sh standalone any time a"
echo "     tunnel drops (reboot, Docker Desktop restart, pod replaced) —"
echo "     no rebuild or redeploy needed."
