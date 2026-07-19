[← Back to README](../README.md)

# Running with Kubernetes

Manifests live under `JPPhotoManagerWeb/k8s/` and mirror `docker-compose.yml` one-for-one: each compose service becomes a Kubernetes Service plus either a StatefulSet (for services with a named Docker volume — `db`, `kafka`, `mongo`) or a Deployment (`redis`, `backend`, `frontend`, `prometheus`, `grafana`).

| `docker-compose.yml` service | Kubernetes workload | Manifest |
|---|---|---|
| `db` | Headless Service + StatefulSet (PVC `pgdata`) | `k8s/postgres.yaml` |
| `kafka` | Headless Service + StatefulSet (PVC `kafka-data`) | `k8s/kafka.yaml` |
| `redis` | Service + Deployment (no volume, same as compose) | `k8s/redis.yaml` |
| `mongo` | Headless Service + StatefulSet (PVC `mongodata`) | `k8s/mongo.yaml` |
| `backend` | Service + Deployment (PVC `thumbnails`, hostPath `catalog*` via patch) | `k8s/backend.yaml` + `k8s/catalog-volumes.yaml` |
| `frontend` | Service + Deployment | `k8s/frontend.yaml` |
| `prometheus` | Service + Deployment (ConfigMap `prometheus-config`) | `k8s/prometheus.yaml` |
| `grafana` | Service + Deployment (PVC `grafana-data`, provisioning ConfigMaps) | `k8s/grafana.yaml` |
| — | Ingress routing to `frontend` (nginx proxies `/api` to `backend`) | `k8s/ingress.yaml` |

All resources live in a dedicated `photomanager` namespace (`k8s/namespace.yaml`). Non-secret configuration (`POSTGRES_HOST`, `KAFKA_BOOTSTRAP`, `MONGO_URI`, …) lives in the `photomanager-config` ConfigMap (`k8s/configmap.yaml`) and is wired into the backend Deployment via `envFrom`, the same values `docker-compose.yml` passes as plain environment variables.

## Architecture differences from Docker Compose

- **Stateful services get a StatefulSet, not a Deployment.** `db`, `kafka`, and `mongo` need a stable network identity and a PVC that survives pod rescheduling, so each gets a headless Service (`clusterIP: None`) + single-replica StatefulSet with a `volumeClaimTemplate`, in place of the named Docker volumes (`pgdata`, `mongodata`) docker-compose uses.
- **Kafka stays single-broker (KRaft).** The advertised listener (`PLAINTEXT://kafka:9092`) resolves through the headless Service to the one backing pod — the same simplification docker-compose makes. A real multi-broker cluster needs per-pod advertised listeners and is out of scope here.
- **Catalog directories use `hostPath` volumes**, not a PVC — the closest analogue to compose's bind-mounted `HOST_IMAGE_DIR`(`_2`, `_3`). This only works because the backend Pod always lands on the one node with the directory (true for single-node dev clusters — Docker Desktop, minikube, kind). A multi-node cluster needs the `catalog`/`catalog2`/`catalog3` volumes replaced with a `PersistentVolumeClaim` on ReadWriteMany-capable shared storage (NFS, EFS, Azure Files, …). Unlike everything else in this list, these volumes don't live in `k8s/backend.yaml` at all — see the next bullet.
- **Catalog paths are a Kustomize patch, not even a placeholder in a versioned file.** `k8s/backend.yaml` declares zero catalog `hostPath` volumes — a real filesystem path is machine-specific and doesn't belong in a versioned manifest, even as an obviously-fake placeholder. `k8s/catalog-volumes.yaml.example` is the checked-in template; copy it to `k8s/catalog-volumes.yaml` (git-ignored, mirrors `secret.yaml`) and edit in your real path(s). `kustomization.yaml`'s `patches:` entry merges it onto the `backend` Deployment at apply time — and since that entry is unconditional, `kubectl apply -k .` fails outright with "no such file" if you forget the copy step, the same fail-loudly behavior as docker-compose.yml's `${HOST_IMAGE_DIR:?Set HOST_IMAGE_DIR in .env}`.
- **Secrets are not committed.** `k8s/secret.yaml.example` is the checked-in template (mirrors `.env.example`); copy it to `k8s/secret.yaml` (git-ignored) and fill in real values before applying, exactly like the `.env` workflow above.
- **Grafana/Prometheus config isn't duplicated into manifests.** The root `kustomization.yaml` generates the `prometheus-config`, `grafana-datasources`, and `grafana-dashboards` ConfigMaps directly from `prometheus.yml` and `grafana/provisioning/**` — the same files docker-compose bind-mounts — so editing those source files is enough; nothing needs to be kept in sync inside `k8s/`.
- **External access goes through an Ingress**, not host-published ports. `k8s/ingress.yaml` routes traffic to the `frontend` Service (whose bundled nginx still reverse-proxies `/api` to `backend:8080`, unchanged from the Docker image). Direct access to `db`, `kafka`, `mongo`, `prometheus`, or `grafana` — the extra ports docker-compose publishes to the host (`5433`, `9094`, `9090`, `3000`) — is via `kubectl port-forward` instead (see below).
- **Probes are tuned much more generously than a first pass would suggest**, and not for padding's sake — every value below was hit in practice on a resource-constrained Docker Desktop VM and is documented inline in the manifests:
  - `k8s/kafka.yaml`'s headless Service sets `publishNotReadyAddresses: true`. On startup the combined broker+controller must resolve its own hostname (`kafka:9093`) to join the KRaft quorum, but a headless Service only publishes DNS for pods that already pass readiness — without this flag the pod can never resolve itself and can never become ready, observed as `CrashLoopBackOff` with `UnknownHostException: kafka`.
  - `k8s/mongo.yaml` and `k8s/redis.yaml`'s exec probes (`mongosh`, `redis-cli ping`) set explicit `timeoutSeconds` — the Kubernetes default of 1 second is too short for a forked shell process to respond under CPU contention, causing false-positive restarts. Mongo's *liveness* probe specifically uses a plain `tcpSocket` check instead of `mongosh`, since forking a full Node.js process just to answer "is Mongo alive" was itself heavy enough to occasionally time out and trigger the very restart it was meant to prevent.
  - `k8s/backend.yaml` uses a `startupProbe` (10-minute budget: `failureThreshold: 60` × `periodSeconds: 10`) instead of relying on `initialDelaySeconds` on the liveness probe. Spring Boot startup here does Flyway migrations and joins three Kafka consumer groups before `/actuator/health` responds; under real contention (a 4-CPU Docker Desktop VM running the whole stack plus the backend's initial catalog scan of real photo libraries) plain context startup that normally takes ~15 seconds was measured taking 5+ minutes. A `startupProbe` gates liveness/readiness until the app responds once, so a slow-but-healthy startup is never mistaken for a hung one.
  - If your cluster has CPU/RAM to spare, the real fix for slow startups is giving the cluster more resources, not stretching the probes further — see [Troubleshooting](#troubleshooting) below.

## Prerequisites

- A running Kubernetes cluster with `kubectl` (1.27+) pointed at it — Docker Desktop's built-in Kubernetes, minikube, or kind all work for local use.
- The [ingress-nginx](https://kubernetes.github.io/ingress-nginx/) controller, so `k8s/ingress.yaml` actually routes traffic (it's inert without one — the `Ingress` resource just sits there with no listener behind it). Install it once per cluster:
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
  kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s
  ```
  On Docker Desktop this creates a `LoadBalancer` Service that binds directly to `localhost` — no extra tunneling needed. On minikube, run `minikube tunnel` in a separate terminal to get the same effect; kind needs a cluster created with `extraPortMappings` for 80/443 (see the [kind docs](https://kind.sigs.k8s.io/docs/user/ingress/)). If you'd rather skip the controller entirely, `kubectl port-forward` still works for everything (see [Accessing services](#accessing-services)).

  `./build-and-deploy-k8s.sh` (see [Setup](#setup)) runs this install command for you — safe to skip typing it out by hand.
- The backend and frontend images built and available to the cluster. `./build-and-deploy-k8s.sh` (see below) does this for you on local clusters (Docker Desktop, kind, minikube); for a remote cluster, push both images to a registry instead and update the `image:` field (and set `imagePullPolicy: Always`) in `k8s/backend.yaml` and `k8s/frontend.yaml`.

## Setup

Steps 1–2 below are one-time, manual setup — they need real values (secrets, your photo directory paths) that can't be safely scripted with placeholders. Once they're done, `build-and-deploy-k8s.sh` (in this directory) automates step 3 below plus building the images and the ingress-nginx install from [Prerequisites](#prerequisites) — safe to re-run any time you want to rebuild and reapply the latest configuration:
```bash
cd JPPhotoManagerWeb
./build-and-deploy-k8s.sh
```
It checks that `k8s/secret.yaml` and `k8s/catalog-volumes.yaml` exist (failing loudly with the exact `cp` command if not, rather than silently deploying broken config), builds the backend and frontend images and makes them visible to the cluster (`kind load docker-image` / `minikube image load` on those providers, nothing extra needed on Docker Desktop), installs/updates ingress-nginx, waits for it to be ready, applies the secret and the kustomized stack, then restarts the backend/frontend Deployments so they pick up the freshly built images (`imagePullPolicy: IfNotPresent` won't repull a `:latest` tag it already has cached, even after a rebuild).

> **Running `build-and-deploy-k8s.sh` on Windows:** it's a bash script — double-clicking it in Explorer or running it from plain `cmd.exe`/PowerShell (`.\build-and-deploy-k8s.sh`) won't work, since neither knows how to interpret bash syntax. Use one of:
> - **Git Bash** (recommended — already installed if you have Git for Windows, and `kubectl`/`docker` from Docker Desktop are already on its `PATH` with no extra setup): right-click inside `JPPhotoManagerWeb` in Explorer → **"Git Bash Here"** (or open Git Bash from the Start menu and `cd` there), then run `./build-and-deploy-k8s.sh` — or `bash build-and-deploy-k8s.sh` if it complains about permissions.
> - **From PowerShell without switching shells**: `& "C:\Program Files\Git\bin\bash.exe" build-and-deploy-k8s.sh` (adjust the path if Git is installed elsewhere).
> - **WSL**, if installed: `wsl bash build-and-deploy-k8s.sh` from PowerShell, or `./build-and-deploy-k8s.sh` from inside a WSL terminal. Beyond `kubectl`, this script also calls `docker build`, so `docker` must resolve inside WSL too (enable **Docker Desktop → Settings → Resources → WSL Integration** for your distro if it doesn't). WSL commonly has no working kubeconfig for `kubectl` even when the binary itself resolves fine — two failure modes we've hit in practice:
>   - `kubectl: command not found` — `kubectl` isn't installed inside this WSL distro at all. Either install it there directly, or enable **Docker Desktop → Settings → Resources → WSL Integration** for your distro (which installs it and wires up the kubeconfig automatically).
>   - Every command fails with `dial tcp 127.0.0.1:8080: connect: connection refused` — `kubectl` exists but has no kubeconfig, so it silently falls back to the legacy `localhost:8080` default instead of erroring clearly. `build-and-deploy-k8s.sh` handles this itself: it detects WSL (via `/proc/version`), and if `$KUBECONFIG` is unset and `~/.kube/config` doesn't exist, falls back to the Windows-side kubeconfig that Git Bash/PowerShell already use successfully (translated from `%USERPROFILE%` via `wslpath`, so it works regardless of username or Windows drive letter). No manual setup needed for the script itself.
>
>     For running `kubectl` commands directly in an interactive WSL terminal (outside the script), set this once in `~/.bashrc` — note it won't help the script above, since `wsl bash build-and-deploy-k8s.sh` is a non-interactive invocation and non-interactive non-login shells never source `~/.bashrc`:
>     ```bash
>     export KUBECONFIG="/mnt/c/Users/<you>/.kube/config"
>     ```

1. **Create the secret** from the template (mirrors `cp .env.example .env`):
   ```bash
   cp k8s/secret.yaml.example k8s/secret.yaml
   ```
   Edit `k8s/secret.yaml` and replace every `change-me` placeholder — generate `JWT_SECRET` and `GRAFANA_ADMIN_PASSWORD` the same way as in [Generating JWT_SECRET](authentication.md#generating-jwt_secret).

2. **Point the catalog volumes at your photos** — from the template (mirrors step 1, and `.env.example`'s `HOST_IMAGE_DIR`/`HOST_IMAGE_DIR_2`):
   ```bash
   cp k8s/catalog-volumes.yaml.example k8s/catalog-volumes.yaml
   ```
   Edit `k8s/catalog-volumes.yaml`'s `hostPath` entries with your real path(s). `backend.yaml` itself declares no catalog `hostPath` volumes at all — a real filesystem path is machine-specific and must never sit in a versioned file, even as a placeholder. `catalog-volumes.yaml` is git-ignored, and `kustomization.yaml`'s `patches:` entry merges it onto the `backend` Deployment at apply time; if you skip this step, `kubectl apply -k .` fails outright with a clear "no such file" error rather than silently cataloging nothing.

   > **Docker Desktop Kubernetes on Windows:** unlike `docker-compose`, which auto-translates a `C:/Users/...` bind mount, `hostPath` is resolved by the kubelet running *inside* the Docker Desktop VM — it does not see Windows drive letters directly. The VM exposes them at `/run/desktop/mnt/host/<lowercase-drive-letter>/...`, so `C:/Users/you/Pictures` becomes `/run/desktop/mnt/host/c/Users/you/Pictures`. minikube and kind have their own equivalents (`minikube mount`, or a `hostPath`/`extraMounts` entry in the kind cluster config) instead of this Docker Desktop-specific path.

3. **Build the images and apply everything** — `./build-and-deploy-k8s.sh` automates this step (plus the ingress-nginx install from Prerequisites); by hand, run from `JPPhotoManagerWeb/` (same Dockerfiles docker-compose uses):
   ```bash
   docker build -t photomanager-backend:latest ./backend
   docker build -t photomanager-frontend:latest ./frontend
   # kind: kind load docker-image photomanager-backend:latest photomanager-frontend:latest
   # minikube: minikube image load photomanager-backend:latest && minikube image load photomanager-frontend:latest
   # Docker Desktop Kubernetes — no extra step; it shares the local image cache.
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/secret.yaml
   kubectl apply -k .
   ```
   `kubectl apply -k .` renders `kustomization.yaml`, which creates the namespace, ConfigMaps, Services, StatefulSets, Deployments, PVCs, and the Ingress in one shot. After the first apply, rebuilding images with the same `:latest` tag requires `kubectl rollout restart deployment/backend deployment/frontend -n photomanager` to actually pick them up — `build-and-deploy-k8s.sh` does this automatically.

4. **Watch things come up:**
   ```bash
   kubectl get pods -n photomanager -w
   ```
   The backend won't report ready until Postgres, Kafka, and MongoDB are reachable — it's normal for it to restart once or twice while those start up, the same way it retries in Docker Compose.

## Accessing services

With ingress-nginx installed and `k8s/ingress.yaml` applied, add a hosts-file entry pointing `photomanager.local` at `127.0.0.1` (Docker Desktop's `LoadBalancer` binds directly to `localhost`; kind/minikube need `minikube tunnel` or `extraPortMappings` first — see [Prerequisites](#prerequisites)):

- **Windows** — edit `C:\Windows\System32\drivers\etc\hosts` **as Administrator** (a normal, unelevated shell gets `Permission denied`) and add:
  ```powershell
  Add-Content -Path "$env:SystemRoot\System32\drivers\etc\hosts" -Value "127.0.0.1 photomanager.local"
  ```
- **Linux/macOS**:
  ```bash
  echo "127.0.0.1 photomanager.local" | sudo tee -a /etc/hosts
  ```

Then open `http://photomanager.local` — no port-forward needed, and it survives Docker Desktop restarts as long as the `ingress-nginx-controller` and `photomanager` Ingress stay applied. Verify the whole path from the command line first if the browser doesn't work:

```bash
curl -i http://photomanager.local/
curl -i http://photomanager.local/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}'
```

Without an ingress controller, use `kubectl port-forward` instead — the direct equivalent of the ports docker-compose publishes to the host. Note this only lasts for the life of the `kubectl` process; run it with `nohup ... &` (or in its own terminal you leave open) if you need it to survive after your shell session ends:

```bash
# Frontend (compose: http://localhost)
kubectl port-forward -n photomanager svc/frontend 8000:80
# → http://localhost:8000

# Backend REST API + Swagger UI (compose: http://localhost:8080)
kubectl port-forward -n photomanager svc/backend 8080:8080

# PostgreSQL, for DBeaver etc. (compose: localhost:5433)
kubectl port-forward -n photomanager svc/db 5433:5432

# MongoDB, for Compass/mongosh etc. (compose: localhost:27017)
kubectl port-forward -n photomanager svc/mongo 27017:27017

# Redis, for redis-cli etc. (compose: localhost:6379)
kubectl port-forward -n photomanager svc/redis 6379:6379

# Kafka, for KafkIO/kcat/kafka-*.bat etc. (compose: localhost:9094)
# — needs one more step than the others; see the callout right below.
kubectl port-forward -n photomanager svc/kafka 9094:9094

# Prometheus (compose: http://localhost:9090)
kubectl port-forward -n photomanager svc/prometheus 9090:9090

# Grafana (compose: http://localhost:3000)
kubectl port-forward -n photomanager svc/grafana 3000:3000
```

**Kafka needs one more step than the services above.** `k8s/kafka.yaml` advertises the `EXTERNAL` listener as `kafka:9094` — the in-cluster Service name — not `localhost:9094` the way Docker Compose does (`KAFKA_ADVERTISED_LISTENERS: ...EXTERNAL://localhost:9094`). A Kafka client connects in two phases: it first reaches whatever bootstrap address you give it, then reconnects using the *advertised* address returned in that first response for the actual produce/consume/admin calls. With only the port-forward above in place, that second connection to `kafka:9094` can't resolve from the host — the initial connection succeeds, but every real operation then hangs or fails with something like `Connection to node ... (kafka/<unresolved>:9094) could not be established`.

Fix it the same way the Ingress section above resolves `photomanager.local` — add a hosts-file entry pointing `kafka` at `127.0.0.1`, so both connection phases resolve to the same forwarded port:

- **Windows** (PowerShell as Administrator):
  ```powershell
  Add-Content -Path "$env:SystemRoot\System32\drivers\etc\hosts" -Value "127.0.0.1 kafka"
  ```
- **Linux/macOS**:
  ```bash
  echo "127.0.0.1 kafka" | sudo tee -a /etc/hosts
  ```

Then use `kafka:9094` — not `localhost:9094` — as the bootstrap server in your client (KafkIO's **Bootstrap servers** field, or `--bootstrap-server kafka:9094` for the CLI tools from [Installing a Kafka client/admin tool](docker-compose.md#installing-a-kafka-clientadmin-tool-on-windows-and-connecting-to-the-broker)).

For Grafana, PostgreSQL, MongoDB, and Kafka specifically, `./port-forward-k8s.sh` runs those four commands for you in the background in one step and is safe to re-run (a port already forwarded is left alone, not duplicated). `./build-and-deploy-k8s.sh` already calls it automatically after deploying — run it standalone afterward any time a tunnel needs to be re-established (a machine reboot, a Docker Desktop restart, or one of those pods being replaced all kill the tunnel without killing the pod behind it), without needing to rebuild images or reapply manifests. The script starts Kafka's tunnel too, but can't safely edit your hosts file for you (that needs admin rights) — it prints the one-time hosts-file step above as a reminder each time it runs.

## Common commands

```bash
# Apply / update the whole stack after editing manifests
kubectl apply -k .

# Check pod status and recent events
kubectl get pods -n photomanager
kubectl describe pod -n photomanager <pod-name>

# Tail backend logs
kubectl logs -n photomanager -f deployment/backend

# Roll out a new backend image after rebuilding it
docker build -t photomanager-backend:latest ./backend
kind load docker-image photomanager-backend:latest   # or minikube image load / registry push
kubectl rollout restart deployment/backend -n photomanager

# Scale the backend (see the PVC access-mode caveat in k8s/backend.yaml
# before scaling beyond 1 replica)
kubectl scale deployment/backend -n photomanager --replicas=2

# Tear down (keeps PVCs — data preserved)
kubectl delete -k .

# Tear down and wipe all persistent data
kubectl delete -k .
kubectl delete pvc --all -n photomanager
```

## Troubleshooting

**`kafka-0` is `CrashLoopBackOff` with `UnknownHostException: kafka` in the logs.** Already fixed in `k8s/kafka.yaml` (`publishNotReadyAddresses: true` on the headless Service — see [Architecture differences](#architecture-differences-from-docker-compose)), but if you ever see this again after editing the manifest yourself, that flag is the first thing to check.

**Docker Desktop's Kubernetes tab shows no Deployment for Kafka (or PostgreSQL, or MongoDB).** Not a bug — Docker Desktop's "Deployments" view filters strictly by the `Deployment` kind, and `db`, `kafka`, and `mongo` are all `StatefulSet`s instead (see [Architecture differences](#architecture-differences-from-docker-compose) for why: they need a stable network identity and a PVC that survives pod rescheduling). Only `backend`, `frontend`, `prometheus`, and `grafana` are actual Deployments and will show up there. To see Kafka's pod, check Docker Desktop's **Pods** view (or a **StatefulSets** view, if present) for `kafka-0`, or from the command line:
```bash
kubectl get statefulset,pods -n photomanager -l app=kafka
```
`1/1 Running` there means it's healthy — it's just invisible from the Deployments filter by design.

**Backend, mongo, or redis pods restart repeatedly on a fresh `kubectl apply -k .`, or the browser shows `504 Gateway Timeout` on login.** On a resource-constrained cluster (e.g. Docker Desktop's default 4-CPU / ~4 GB VM) running the full 8-service stack at once — plus the backend's first catalog scan of your real photo library — genuinely starves the CPU. We measured Spring Boot startup that normally takes ~15 seconds taking 5+ minutes under this contention. The manifests already budget for this (see the probe-tuning bullet in [Architecture differences](#architecture-differences-from-docker-compose)), but if it's still not enough for your machine:
- Check actual resource usage: `docker stats` (per-container) and `docker info --format '{{.NCPU}} CPUs, {{.MemTotal}} bytes RAM'` (total VM capacity).
- Give Docker Desktop more CPU/RAM: Settings → Resources.
- Or just wait it out — `kubectl get pods -n photomanager -w` and watch for `1/1 Running` with `RESTARTS` stable; a slow-but-successful startup is expected, not a sign of a broken deployment.

**Frontend/backend show old behavior after rebuilding images.** `imagePullPolicy: IfNotPresent` means the kubelet won't repull a `:latest` tag it already has cached, even after you rebuild it with the same tag. After `docker build`, restart the affected Deployment so it actually picks up the new image:
```bash
kubectl rollout restart deployment/backend deployment/frontend -n photomanager
```
If a Deployment's pod is already running from the image you're about to delete, `docker rmi` will fail with `must be forced) - container ... is using its referenced image` — Docker Desktop's Kubernetes here runs pods as real `dockerd` containers holding a genuine reference. Scale the Deployment to 0 first, delete the image, rebuild, then scale back up:
```bash
kubectl scale deployment/backend deployment/frontend -n photomanager --replicas=0
docker rmi photomanager-backend:latest photomanager-frontend:latest
docker build -t photomanager-backend:latest ./backend
docker build -t photomanager-frontend:latest ./frontend
kubectl scale deployment/backend deployment/frontend -n photomanager --replicas=1
```

**Duplicate image tags in `docker images` (e.g. both `jpphotomanagerweb-backend` and `photomanager-backend` pointing at the same image ID).** `docker-compose.yml`'s `backend`/`frontend` services set an explicit `image: photomanager-backend:latest` / `image: photomanager-frontend:latest` — matching `k8s/backend.yaml`/`k8s/frontend.yaml` exactly — precisely so `docker compose build` and `docker build -t photomanager-backend:latest ./backend` produce the same tag and this can't happen going forward. If you still have leftover `jpphotomanagerweb-*` tags from before that field was added, they're harmless (same underlying image, just an extra name); clean them up with `docker rmi jpphotomanagerweb-backend:latest jpphotomanagerweb-frontend:latest` (using `--replicas=0` first as above if either is in use).

**`build-and-deploy-k8s.sh` (or any `kubectl` command) hangs and then fails with `dial tcp <ip>:6443: connectex: A connection attempt failed...`.** `kubectl` is pointed at a context whose cluster isn't actually running. This happens whenever more than one local Kubernetes provider is installed (e.g. both Docker Desktop and Rancher Desktop) — `kubectl config current-context` can silently be left on the one that's stopped. Check and fix:
```bash
kubectl config get-contexts          # shows all contexts; * marks the current one
kubectl config use-context docker-desktop   # switch to the one that's actually running
```
If the target context's provider isn't running at all (e.g. Rancher Desktop's app process isn't started), the fix is to launch that provider — or switch to whichever one you do have running, as above.

**Nothing responds at `http://localhost` or `http://photomanager.local` even though all pods are `1/1 Running`.** Unlike `docker-compose`, nothing is listening on a host port by default in Kubernetes — there's no `frontend` container publishing `80:80`. Confirm something is actually bound:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller   # EXTERNAL-IP should be "localhost", not <pending>
kubectl get ingress -n photomanager                          # ADDRESS should be populated
```
If `ingress-nginx-controller` isn't installed at all, or you're relying on `kubectl port-forward` and the terminal it was running in was closed, that tunnel is gone — port-forwards don't survive past the life of the `kubectl` process that started them.

[← Back to README](../README.md)
