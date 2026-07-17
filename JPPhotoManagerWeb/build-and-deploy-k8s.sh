#!/usr/bin/env bash
set -euo pipefail

# Builds the backend and frontend Docker images, installs the ingress-nginx
# controller (if not already present), and applies the full Kubernetes
# configuration for the web application — the exact steps documented in
# README.md's "Running with Kubernetes" section (Prerequisites + Setup),
# automated so you don't have to run them by hand. Safe to re-run — every
# step here is idempotent, including the background port-forwards started
# at the end (see below): a port already forwarded from a previous run is
# detected and skipped rather than duplicated.
#
# Usage: ./build-and-deploy-k8s.sh   (run from anywhere; the script cds to its own dir)
#
# Prerequisite: k8s/secret.yaml and k8s/catalog-volumes.yaml must already
# exist (copied from their .example templates and filled in) — this script
# does not create them, since they hold machine-specific / sensitive values
# that must never be scripted into existence with placeholder content.
#
# After applying the stack, the script also starts background
# `kubectl port-forward` tunnels for Grafana, PostgreSQL, and MongoDB (the
# same commands documented in README.md's "Accessing services" section)
# so those services are reachable at localhost immediately, without an
# extra manual step. They keep running after the script exits — use the
# `kill <PID>` commands printed at the end to stop them.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

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
echo "==> Starting background port-forwards for Grafana, PostgreSQL, and MongoDB ..."
echo "    (a port already forwarded from a previous run of this script is left alone, not duplicated)"

# Checks whether something is already listening on 127.0.0.1:<port> using
# bash's built-in /dev/tcp pseudo-device — no extra tool (nc, lsof, ss)
# required, so this works the same in Git Bash, WSL, and native Linux/macOS
# bash, all of which this script already has to support (see the WSL
# kubeconfig handling above).
port_in_use() {
    (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# Starts `kubectl port-forward` in the background for one service, unless
# the local port is already in use (most likely by a still-running
# port-forward from a previous invocation of this script). Prints the PID
# so it can be killed later; PIDs are also collected into PORT_FORWARD_PIDS
# for the summary at the end.
PORT_FORWARD_PIDS=()
start_port_forward() {
    local label="$1" svc="$2" local_port="$3" remote_port="$4"
    if port_in_use "$local_port"; then
        echo "    ${label}: localhost:${local_port} is already in use — skipping (already forwarded?)."
        return
    fi
    nohup kubectl port-forward -n photomanager "svc/${svc}" "${local_port}:${remote_port}" \
        >/dev/null 2>&1 &
    local pid=$!
    PORT_FORWARD_PIDS+=("$pid")
    echo "    ${label}: localhost:${local_port} (PID ${pid})"
}

start_port_forward "Grafana"    grafana 3000  3000
start_port_forward "PostgreSQL" db      5433  5432
start_port_forward "MongoDB"    mongo   27017 27017

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
echo "  4. Grafana, PostgreSQL, and MongoDB are now reachable at localhost:3000,"
echo "     localhost:5433, and localhost:27017 respectively (see README's"
echo "     'Connecting DBeaver to the database' / MongoDB client sections for"
echo "     connection settings). These port-forwards keep running after this"
echo "     script exits; stop them when no longer needed with:"
if [ "${#PORT_FORWARD_PIDS[@]}" -gt 0 ]; then
    echo "       kill ${PORT_FORWARD_PIDS[*]}"
else
    echo "       kill <PID>   (all three were already forwarded — see 'skipping' lines above)"
fi
