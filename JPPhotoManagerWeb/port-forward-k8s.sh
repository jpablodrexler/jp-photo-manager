#!/usr/bin/env bash
set -euo pipefail

# Starts background `kubectl port-forward` tunnels for Grafana, PostgreSQL,
# MongoDB, and Kafka — the same commands documented in README.md's
# "Accessing services" section, run for you in one step. Safe to re-run: a
# port already forwarded (e.g. from a previous run of this script, or from
# build-and-deploy-k8s.sh, which calls this script automatically after
# deploying) is detected and left alone rather than duplicated.
#
# Kafka's tunnel alone isn't enough to actually use it from the host — see
# the note printed after it starts below (and README's "Accessing services"
# section) for the one-time hosts-file edit it also needs.
#
# Lives on its own, separate from build-and-deploy-k8s.sh, because
# `kubectl port-forward` is a plain OS process, not something Kubernetes
# restarts for you: it does not survive a machine reboot or Docker Desktop
# restart, and it does not automatically reattach if the pod it's
# forwarding to is replaced (crash, rollout restart, eviction). Re-run this
# script any time a tunnel needs to be re-established — no image rebuild or
# manifest reapply required.
#
# Usage: ./port-forward-k8s.sh   (run from anywhere; the script cds to its own dir)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Under WSL, `kubectl` commonly has no kubeconfig of its own even when the
# binary itself resolves fine — it then silently falls back to the legacy
# localhost:8080 default and fails with "connection refused" instead of a
# clear error. This can't be fixed via ~/.bashrc: a non-interactive
# invocation (e.g. `wsl bash port-forward-k8s.sh`) never sources it.
# Detect that situation and fall back to the Windows-side kubeconfig
# instead, which Docker Desktop's own kubectl (Git Bash, PowerShell)
# already uses successfully.
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

echo "==> Starting background port-forwards for Grafana, PostgreSQL, MongoDB, and Kafka ..."
echo "    (a port already forwarded from a previous run is left alone, not duplicated)"

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
start_port_forward "Kafka"      kafka   9094  9094

echo ""
echo "Grafana, PostgreSQL, and MongoDB are reachable at localhost:3000,"
echo "localhost:5433, and localhost:27017 respectively (see README's"
echo "'Connecting DBeaver to the database' / MongoDB client sections for"
echo "connection settings). These port-forwards keep running after this"
echo "script exits; stop them when no longer needed with:"
if [ "${#PORT_FORWARD_PIDS[@]}" -gt 0 ]; then
    echo "  kill ${PORT_FORWARD_PIDS[*]}"
else
    echo "  kill <PID>   (all four were already forwarded — see 'skipping' lines above)"
fi

echo ""
echo "NOTE on Kafka: the tunnel above alone is not enough to use it from the"
echo "host. k8s/kafka.yaml advertises the broker as kafka:9094 (the in-cluster"
echo "Service name), so a client's initial connection succeeds but every real"
echo "operation afterward fails trying to resolve 'kafka'. One-time fix — add"
echo "a hosts-file entry pointing kafka at 127.0.0.1 (requires admin):"
echo "  Windows (PowerShell as Administrator):"
echo "    Add-Content -Path \"\$env:SystemRoot\\System32\\drivers\\etc\\hosts\" -Value \"127.0.0.1 kafka\""
echo "  Linux/macOS:"
echo "    echo \"127.0.0.1 kafka\" | sudo tee -a /etc/hosts"
echo "Then use kafka:9094 — not localhost:9094 — as the bootstrap server in"
echo "your client. See README's 'Accessing services' section for details."
