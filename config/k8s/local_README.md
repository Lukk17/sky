# Local Kubernetes cluster

Single source of commands for standing the full sky backend stack up on a local k3d cluster and verifying it with the Bruno collection.

The cluster is named `k3d-sky`. Host port 5777 maps to the cluster load balancer port 80. All traffic enters through nginx-ingress. Keycloak is the OIDC provider at `http://keycloak.127.0.0.1.nip.io:5777`. The nip.io domain resolves to 127.0.0.1 on the host without touching the hosts file, and CoreDNS resolves it to the nginx-ingress ClusterIP inside the cluster so services can reach Keycloak for OIDC discovery.

Every command below is a single line and runs unchanged in a Unix shell and in PowerShell 7. Run all of them from the repository root.

For running the services without Kubernetes (Gradle or Docker Compose) see [config/local-dev/local_README.md](../local-dev/local_README.md). For the chart-by-chart reference see [config/k8s/helm/helm_README.md](helm/helm_README.md).

---

### Architecture, what actually runs

The cluster the create command in step 1 produces runs as exactly two Docker containers, regardless of how many applications you deploy:

- `k3d-sky-server-0`, the single k3s node. Every pod (the four services plus PostgreSQL, floci, Kafka, Keycloak, and Keycloak's own PostgreSQL) runs inside this one container as a containerd container, not as a Docker container. `docker ps` does not show them, `kubectl get pods` does.
- `k3d-sky-serverlb`, a small proxy. It is not a second Kubernetes server. It is the load balancer that forwards host port 5777 into the cluster's nginx-ingress on port 80.

So you do not run two Docker containers per app. You run two Docker containers for the entire cluster, and the sky pods live inside the server node. The name "serverlb" is k3d's, it means "load balancer in front of the server", not "a second server".

#### A cluster with agent nodes

A `sky` cluster created with `--agents N` is the other shape you may be sitting on, and nothing else on this page changes for it. Each agent is one more Docker container and one more schedulable node, so the pods spread across them, and `svclb-ingress-nginx-controller` in `kube-system` then runs one pod per node rather than one in total. A long-lived cluster can also still be carrying the `k3d-sky-tools` image loader from step 2, which is not a node at all. A three-node cluster with that leftover is five containers: one server, two agents, the load balancer, and the loader.

Ask the cluster which shape you have rather than counting containers:

```bash
kubectl get nodes
```

The `control-plane` row is the server. Every row with no role printed is an agent, and a single `control-plane` row on its own means this is the one-node cluster the bullets above describe. For the container side of the same question:

```bash
k3d node list
```

Its `ROLE` column separates `server` from `agent` from `loadbalancer`, and a row with an empty role is the tools container rather than any kind of node.

The pod names, the wait selectors, and every command below are the same either way, because nothing in the charts pins a pod to a node.

The charts are also not k3d specific. They work on any Kubernetes whose ingress answers on host port 5777, which is the only assumption the `values-local.yaml` overlays and the Bruno `k8s` environment make about the distribution.

The full first-time bring-up is slow because it does three heavy things: building the four service images (Gradle compiles inside Docker, minutes each), the first cluster create (k3d pulls the k3s image), and starting the pods (each Spring Boot service needs about 25 seconds). None of that repeats while iterating. To redeploy one changed service, rebuild just its image, `k3d image import` it, and `kubectl rollout restart deploy/<name>`, which takes about a minute.

`sky-notify` ships an Ingress in its chart, on `/notifyWebsocket` with host `localhost` locally, so the WebSocket endpoint is reachable from the host at `ws://localhost:5777/notifyWebsocket` like every other route. No `kubectl port-forward` is needed. The Ingress raises `proxy-read-timeout` and `proxy-send-timeout` to 3600 seconds, because nginx otherwise closes an idle WebSocket after 60.

---

### Prerequisites

- k3d 5 or newer (`k3d version`)
- kubectl (`kubectl version --client`)
- Helm 3.12 or newer (`helm version`)
- Bruno CLI 1 or newer (`bru --version`)
- Docker, with the four service images built locally as `sky-offer:latest`, `sky-booking:latest`, `sky-message:latest`, `sky-notify:latest`

Build them with Compose rather than by hand. [config/docker/docker-compose.yaml](../docker/docker-compose.yaml) is the one place the build is defined, and one build per service produces both tags the convention requires: `sky-<service>:latest` and a version tag from `SKY_VERSION`. With the variable unset the compose file's own fallback applies, and that fallback tracks the module `version` in each module's `build.gradle.kts`, so a release moves the two together in one change. Export `SKY_VERSION` to tag a build at a different version. Every command below uses `latest`, so nothing on this page changes when the version moves.

```bash
docker compose -f config/docker/docker-compose.yaml build sky-offer sky-booking sky-message sky-notify
```

Building by hand works too and is documented in [config/local-dev/local_README.md](../local-dev/local_README.md). It means repeating both tags for each service, which is how this page drifted onto a tag nothing produced.

---

### 1. Create the k3d cluster

Run once. Skip if the cluster already exists. Traefik is disabled because the stack uses nginx-ingress.

```bash
k3d cluster create sky --port "5777:80@loadbalancer" --k3s-arg "--disable=traefik@server:0"
```

If every `kubectl` command from here on fails to connect while the cluster itself is healthy, the kubeconfig address k3d just wrote is the likely cause. See item 6 under [Known issues](#known-issues-and-design-notes).

---

### 2. Import local Docker images into k3d

The cluster's k3s node has its own containerd image store, separate from host Docker, so locally built images must be copied in. `--mode direct` streams the images straight into the node and never creates the `k3d-tools` helper container:

```bash
k3d image import --mode direct sky-offer:latest sky-booking:latest sky-message:latest sky-notify:latest -c sky
```

Without `--mode direct`, k3d spins up a short-lived `k3d-sky-tools` helper container to do the copy. It is meant to be removed automatically after the import, but an interrupted or repeated import can leave it running. It is harmless (just an image loader, not a cluster node), and you can remove a lingering one at any time:

```bash
docker rm -f k3d-sky-tools
```

---

### 3. Install nginx-ingress

Add the chart repository, then refresh its index, then install from the named repository. All three commands are needed:

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
```

```bash
helm repo update ingress-nginx
```

```bash
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace --set controller.service.type=LoadBalancer
```

Do not delete the `helm repo update` line as redundant: `helm repo add` writes the repository index only when it creates the entry, so on a machine where the entry already survives in `repositories.yaml` while the index cache under the Windows temp directory has been cleaned, the add prints `already exists with the same configuration, skipping` and puts nothing back. The install then looks for a chart version in an index that is not there and dies on `Error: no cached repo found. (try 'helm repo update'): open C:\Users\...\Temp\helm\repository\ingress-nginx-index.yaml` before it touches the cluster. Only `helm repo update` restores that file.

An earlier version of this step installed in one command, naming the chart `ingress-nginx` and passing `--repo https://kubernetes.github.io/ingress-nginx`. That flag resolves through whatever repository entry already matches the URL and reads the same cached index, so it hits the same failure rather than avoiding it, and the step after it then reports `error: no matching resources found` because the controller was never installed.

Wait for the controller to be ready before continuing:

```bash
kubectl wait pod -n ingress-nginx -l app.kubernetes.io/component=controller --for=condition=Ready --timeout=120s
```

---

### 4. Apply the local secrets

The local credentials are committed at [config/k8s/local/sky-secrets-local.yaml](local/sky-secrets-local.yaml), so there is nothing to type and nothing to keep in sync by hand. They are plain-text development values. Never use them anywhere else.

```bash
kubectl apply -f config/k8s/local/sky-secrets-local.yaml
```

The cluster charts read this one `sky-secrets` Secret. The production path uses the same key names through a SealedSecret instead, see [config/k8s/helm/helm_README.md](helm/helm_README.md) for the full key inventory.

---

### 5. Apply the development TLS secret

The self-signed development certificate and its ready-made Secret manifest are committed under [config/k8s/secret/ssl/](secret/ssl/). Apply the manifest rather than regenerating a certificate:

```bash
kubectl apply -f config/k8s/secret/ssl/dev-ssl-cert.yaml
```

The certificate carries `CN=localhost` and is valid for ten years. Its subject alternative names are `localhost`, `keycloak.127.0.0.1.nip.io`, `127.0.0.1` and `::1`, which is every host a local Ingress serves: the four services on `localhost` and Keycloak on the nip.io name. Trust it in a browser and those names verify, anything else does not. The local overlays set `ssl-redirect: "false"` and the cluster only maps host port 5777 to the load balancer's port 80, so the default local path is plain HTTP and the certificate mostly just satisfies the `tls` block on each Ingress.

---

### 6. Give CoreDNS an in-cluster record for Keycloak

`keycloak.127.0.0.1.nip.io` resolves to 127.0.0.1 everywhere, which is right on the host and wrong in a pod, where 127.0.0.1 is the pod itself. Without a record of its own, a service fetching the token signing keys asks itself for them. CoreDNS has to send that name to nginx-ingress instead.

The record is committed at [config/k8s/local/coredns-custom-local.yaml](local/coredns-custom-local.yaml), so there is no address to look up and nothing to paste:

```bash
kubectl apply -f config/k8s/local/coredns-custom-local.yaml
```

```bash
kubectl rollout restart deployment coredns -n kube-system
```

It goes into a ConfigMap named `coredns-custom` rather than into the `NodeHosts` key of the `coredns` ConfigMap, and that is the whole reason a cluster restart no longer breaks authentication. The k3s Corefile ends with `import /etc/coredns/custom/*.override` inside the server block and `import /etc/coredns/custom/*.server` outside it, and both read a directory the CoreDNS Deployment mounts from `coredns-custom` as an optional volume. k3d neither creates that ConfigMap nor touches it, while it rewrites `NodeHosts` on every `k3d cluster start`. A record put in `NodeHosts` is therefore gone by the next start, and item 7 under [Known issues](#known-issues-and-design-notes) is what that looks like from the caller's side.

The record is a rewrite to the ingress controller's Service name, not a host record pointing at its ClusterIP. The upstream chart allocates no fixed address, its `controller.service.clusterIP` is empty, so reinstalling nginx-ingress hands out a different ClusterIP and any pasted address goes stale. `ingress-nginx-controller.ingress-nginx.svc.cluster.local` is fixed by the release name and namespace step 3 installs under, so it survives that.

Verifying needs a pod, so run this once the service pods are up in step 8. Any of the four answers the same, and the address must be the ingress controller's ClusterIP rather than 127.0.0.1:

```bash
kubectl exec deploy/sky-offer-deployment -- getent hosts keycloak.127.0.0.1.nip.io
```

```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.spec.clusterIP}'
```

---

### 7. Deploy the infrastructure charts

```bash
helm upgrade --install database-persistent-volume-claim config/k8s/helm/db/database-persistent-volume-claim -n default
```

```bash
helm upgrade --install postgres config/k8s/helm/db/postgres -n default
```

```bash
helm upgrade --install floci config/k8s/helm/infra/floci -f config/k8s/helm/infra/floci/values-local.yaml -n default
```

The local overlay at [config/k8s/helm/infra/floci/values-local.yaml](helm/infra/floci/values-local.yaml) is what turns the object store's Ingress on, and the store does not work from your machine without it. The overlay adds an Ingress on host `s3.localhost`, so the store answers at `http://s3.localhost:5777`, on the same host port as everything else, and that name resolves to 127.0.0.1 with no hosts-file entry. `sky-offer` needs both addresses and they are not interchangeable: it uploads to `http://floci-service:4566`, the in-cluster Service name, and signs its presigned photo URLs against `http://s3.localhost:5777`, which is set as `s3.presignEndpoint` in [config/k8s/helm/service/sky-offer/values-local.yaml](helm/service/sky-offer/values-local.yaml), because the internal name resolves for no client outside the cluster. Drop the overlay and the Ingress is never rendered, uploads still succeed, and every photo URL the cluster hands out is unreachable.

Scaling the `floci-deployment` StatefulSet to zero replicas is the supported way to see the outage path: an upload then answers 503 with a `Retry-After: 10` header, while a photo delete and an offer delete both still answer as they would against a reachable store, and scaling back to one replica restores uploads with every stored object still in the claim.

```bash
helm upgrade --install keycloak config/k8s/helm/infra/keycloak -f config/k8s/helm/infra/keycloak/values-local.yaml -n default
```

Kafka has one definition, the chart at [config/k8s/helm/kafka/](helm/kafka/), which is what the deployment scripts install and what the GCP cluster runs. It stands up a single-node KRaft broker on `apache/kafka:3.7.1` with a storage-format init container and auto-create topics:

```bash
helm upgrade --install kafka-service config/k8s/helm/kafka -f config/k8s/helm/kafka/values-local.yaml -n default
```

Wait for infrastructure to be ready, one wait per command:

```bash
kubectl wait pod -l component=postgres --for=condition=Ready --timeout=120s
```

```bash
kubectl wait pod -l component=keycloak-postgres --for=condition=Ready --timeout=120s
```

```bash
kubectl wait pod -l component=keycloak --for=condition=Ready --timeout=180s
```

```bash
kubectl wait pod -l component=floci --for=condition=Ready --timeout=120s
```

```bash
kubectl wait pod -l app.kubernetes.io/name=kafka --for=condition=Ready --timeout=120s
```

That last selector is the odd one out because the Kafka chart labels its pod `app.kubernetes.io/name=kafka` and sets no plain `component` label, while every other chart on this page sets `component`. Getting a selector wrong is silent: `kubectl wait` prints `error: no matching resources found` and returns immediately, and the next step starts against a broker that is not up yet. Every wait on this page was checked against the chart that creates the pod.

---

### 8. Deploy the service charts

```bash
helm upgrade --install sky-offer config/k8s/helm/service/sky-offer -f config/k8s/helm/service/sky-offer/values-local.yaml -n default
```

```bash
helm upgrade --install sky-booking config/k8s/helm/service/sky-booking -f config/k8s/helm/service/sky-booking/values-local.yaml -n default
```

```bash
helm upgrade --install sky-message config/k8s/helm/service/sky-message -f config/k8s/helm/service/sky-message/values-local.yaml -n default
```

```bash
helm upgrade --install sky-notify config/k8s/helm/service/sky-notify -f config/k8s/helm/service/sky-notify/values-local.yaml -n default
```

Wait for the service pods:

```bash
kubectl wait pod -l app=sky-offer --for=condition=Ready --timeout=180s
```

```bash
kubectl wait pod -l app=sky-booking --for=condition=Ready --timeout=180s
```

```bash
kubectl wait pod -l app=sky-message --for=condition=Ready --timeout=180s
```

```bash
kubectl wait pod -l app=sky-notify --for=condition=Ready --timeout=180s
```

---

### 9. Verify the stack

Check every pod is 1/1 Running:

```bash
kubectl get pods
```

Expected, plus the ingress-nginx controller in its own namespace:

```text
floci-deployment-0               1/1 Running
kafka-service-0                  1/1 Running
keycloak-deployment-*            1/1 Running
keycloak-postgres-0              1/1 Running
postgres-deployment-0            1/1 Running
sky-booking-deployment-*         1/1 Running
sky-message-deployment-*         1/1 Running
sky-notify-deployment-*          1/1 Running
sky-offer-deployment-*           1/1 Running
```

Verify the Keycloak OIDC issuer:

```bash
curl -s "http://keycloak.127.0.0.1.nip.io:5777/realms/sky/.well-known/openid-configuration"
```

The `issuer` field must read `http://keycloak.127.0.0.1.nip.io/realms/sky`. If it reads anything else the services will reject every token the cluster Keycloak mints.

---

### 10. Run the Bruno collection

Use the `k8s` environment, not `local`. The cluster runs its own Keycloak with issuer `http://keycloak.127.0.0.1.nip.io/realms/sky`, while `--env local` mints tokens from your host Keycloak at `https://keycloak.test:9443`. A token from the wrong issuer is rejected by the cluster services, so `--env local` against the cluster gives a valid token and a 401 on every authenticated call. The public endpoints (get-all-offers, search) still pass, which is the tell-tale sign you picked the wrong environment.

```bash
cd docs/api/request
```

```bash
bru run -r --env k8s --insecure
```

The collection passes in full against the cluster, every request and every assertion, with nothing to work around. The message flow used to fail here: `sky-message` asked the Keycloak realm whether the receiver address existed before it stored anything, that lookup did not succeed against the cluster Keycloak, and the send answered 503 instead of 201. The check is gone. The module makes no outbound call of any kind now, and a message to an address nobody owns is accepted.

A green run is worth more than a row of 200s, because three of its checks reach back into the object store rather than trusting a response body:

- The photo round trip. The upload refetches its own presigned `photoUrl` through the floci ingress and asserts the canary marker is in the bytes that come back, so the object in the bucket is the fixture that was posted and not just any image.
- The photo survives an edit. `PUT /api/v1/owner/offers` refetches the photo afterwards, which is the regression guard for the defect where a client-supplied path overwrote the server's object key and orphaned the real object.
- The object really goes. The photo replace, the photo delete, and the offer delete each refetch the address they just invalidated and require a 404, so a leaked object fails the run rather than passing quietly.

The collection and its environments are documented in [docs/api/README.md](../../docs/api/README.md).

---

### Bruno k8s environment

The environment file is [docs/api/request/environments/k8s.yml](../../docs/api/request/environments/k8s.yml). It is named `k8s` rather than `k3d` because the same environment works against any Kubernetes distro (k3d, minikube, kind) as long as that distro's ingress is exposed at `localhost:5777` and its Keycloak issues the same nip.io issuer. The values are:

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:5777` |
| `keycloakUrl` | `http://keycloak.127.0.0.1.nip.io:5777` |
| `keycloakClientId` | `sky-backend` |
| `keycloakClientSecret` | `dev-only-change-in-prod` |
| `keycloakUsername` | `lukk` |
| `keycloakPassword` | `test1234` |
| `bearerToken` | empty, filled at run time by [docs/api/request/auth/get-token.yml](../../docs/api/request/auth/get-token.yml) |

---

### Credentials

Development-only, non-secret, intentionally committed. They come from [config/k8s/local/sky-secrets-local.yaml](local/sky-secrets-local.yaml) and the realm file at [config/k8s/helm/infra/keycloak/files/sky-realm.json](helm/infra/keycloak/files/sky-realm.json). Never use them anywhere else.

| What | Username | Password |
|---|---|---|
| PostgreSQL (sky database) | postgres | local |
| S3 access key and secret | root | localdev |
| Keycloak admin console | admin | admin |
| Keycloak realm user (admin role) | lukk | test1234 |
| Keycloak realm user (admin role) | owner | owner |
| Keycloak realm user (user role) | user | user |
| Keycloak client `sky-backend` | client secret | dev-only-change-in-prod |

The last row is still needed and the list of who needs it has shrunk, which is worth saying so nobody puts it back where it no longer belongs. `oauth2-proxy` reads the secret to run its OIDC session flow, and [docs/api/request/auth/get-token.yml](../../docs/api/request/auth/get-token.yml) reads it to run the password grant that mints the caller's token, which is also why `sky-backend` is the `keycloakClientId` in the table above. No sky service reads it any more: `sky-message` used to fetch a service-account token with it for a receiver lookup against the Keycloak administration interface, and both the lookup and its `USER_DIRECTORY_CLIENT_SECRET` are gone. Validating a JWT needs no client secret, so a service environment block should never carry one.

Do not read the S3 row as a control. floci, the object store the cluster now runs, authenticates nobody: it accepts any credentials, verifies no signature, and serves an unsigned GET of any object it holds. The two keys exist only because the AWS SDK refuses to build a client without them, in [sky-offer/src/main/java/com/lukk/sky/offer/config/S3Config.java](../../sky-offer/src/main/java/com/lukk/sky/offer/config/S3Config.java), which hands them to `AwsBasicCredentials.create`. The values are kept at a length MinIO would also accept, because [config/local-dev/local_README.md](../local-dev/local_README.md) still offers MinIO as the local alternative for when you do want the credential and signature checks exercised.

---

### Stop and start the cluster

You do not need to tear down and rebuild to pause work. One command stops every cluster container (the k3s node and the load balancer) together, and one starts them again with all deployed charts and data intact. This is the single on/off switch for the whole stack:

```bash
k3d cluster stop sky
```

```bash
k3d cluster start sky
```

That claim is only true because the Keycloak record lives in the `coredns-custom` ConfigMap from step 6. k3d rewrites the `NodeHosts` key of the `coredns` ConfigMap on every start, so the same record put there instead is gone the moment the cluster comes back, and the stack answers 401 on every authenticated call while looking healthy. That is item 7 under [Known issues](#known-issues-and-design-notes).

Docker Desktop will not show the k3d containers as one grouped stack with a single toggle, because k3d does not tag them as a Compose project and labelling them as one would fight k3d's own lifecycle management. The two commands above are the equivalent single control.

---

### Tear down

Remove the chart releases:

```bash
helm uninstall sky-offer sky-booking sky-message sky-notify keycloak floci postgres database-persistent-volume-claim -n default
```

That line takes the stored photos with it. [config/k8s/helm/infra/floci/templates/floci-pvc.yaml](helm/infra/floci/templates/floci-pvc.yaml) is a plain template rather than a StatefulSet volume claim template, so Helm owns `floci-pvc` and `helm uninstall floci` deletes the claim and every object in the store. To pause work and keep the objects, do not tear down at all, use `k3d cluster stop sky` from the section above.

Then remove Kafka:

```bash
helm uninstall kafka-service -n default
```

```bash
kubectl delete secret sky-secrets dev-ssl-cert
```

```bash
kubectl delete pvc --all -n default
```

Destroy the cluster completely:

```bash
k3d cluster delete sky
```

---

### Known issues and design notes

1. PostgreSQL readiness probe. An earlier version of the charts used `exec: pg_isready -U $(POSTGRES_USER)`, but Kubernetes does not expand environment variables in probe exec commands, so `$(POSTGRES_USER)` was passed literally and the probe failed. Both the app PostgreSQL and the Keycloak PostgreSQL readiness probes now use `tcpSocket` in the chart templates, consistent with their startup and liveness probes, so no runtime patch is needed.

2. Keycloak ingress class. The upstream keycloak example used the deprecated `kubernetes.io/ingress.class` annotation. The chart template at [config/k8s/helm/infra/keycloak/templates/keycloak-ingress.yaml](helm/infra/keycloak/templates/keycloak-ingress.yaml) sets `spec.ingressClassName: nginx` instead.

3. Spring Boot 4 breaking change. The `SPRING_SECURITY_USER` environment variable maps to `spring.security.user`, which Spring Boot 4 requires to be a structured object rather than a plain string. The variable was a leftover from the basic-auth era and has been removed from all four service deployment templates, along with the matching `spring.securityUser` and `spring.securityPass` values keys.

4. Auth annotations. The nginx `auth-url` and `auth-signin` annotations that route to the production oauth2-proxy live in `values-prod.yaml`, not in the default `values.yaml`, so a local install never sees them. Each `values-local.yaml` still nulls them out for every affected ingress section, and the ingress templates skip nil-valued annotations, which keeps the local overlay correct on its own rather than by relying on what the defaults happen to hold.

5. Image user UID. Kubernetes requires a numeric UID when `runAsNonRoot: true` is set, and the deployment templates set `runAsUser: 1000`. The Dockerfiles pin the `sky` runtime user to a fixed UID and GID of 1000 (`adduser -S -u 1000 sky`), so the container's user matches the `runAsUser` value deterministically across rebuilds rather than relying on the base image's auto-assigned UID.

6. Stale `host.docker.internal` in the kubeconfig address. Not a defect in this repository, and it stops every command on the page dead, so it is written up here. The symptom is that every `kubectl` command fails at once, naming an address you never typed:

    ```text
    Unable to connect to the server: dial tcp 192.168.1.10:51639: connectex: No connection could be made because the target machine actively refused it.
    ```

    k3d writes the API server address into the kubeconfig as `https://host.docker.internal:PORT`, and a Windows hosts file pinning `host.docker.internal` to an address that is no longer right sends every call somewhere nothing is listening. The cluster itself is usually fine.

    One command tells it apart from a dead cluster. Use the port from the failing message, and read `401` as the good answer, because it is the correct reply to an unauthenticated request and it proves the API server is up:

    ```bash
    curl -sk https://127.0.0.1:51639/version
    ```

    If that answers, point the kubeconfig at loopback on the same port:

    ```bash
    kubectl config set-cluster k3d-sky --server=https://127.0.0.1:51639
    ```

    k3d picks that port when it creates the cluster, so `k3d cluster delete sky` followed by `k3d cluster create` gives you a different one and this fix has to be redone with the new port. Read the port the kubeconfig currently holds:

    ```bash
    kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'
    ```

7. Every authenticated call answers 401 while the token endpoint still answers 200, after a cluster restart, on a cluster that was working. Nothing was redeployed and nothing in the charts changed. Minting a token keeps succeeding because the collection asks the host ingress for it, so authentication looks healthy right up to the point it is not, and the symptom points at Keycloak while the cause is DNS.

    CoreDNS has no record for `keycloak.127.0.0.1.nip.io`, so the name falls through to public nip.io and resolves to 127.0.0.1. In a pod that is the pod itself, so every service fetches the token signing keys from its own port 80, gets nothing, and rejects every token it is handed.

    One command tells it apart, and any answer other than the ingress controller's ClusterIP is this issue, 127.0.0.1 above all:

    ```bash
    kubectl exec deploy/sky-offer-deployment -- getent hosts keycloak.127.0.0.1.nip.io
    ```

    The fix is step 6: apply the `coredns-custom` ConfigMap and restart CoreDNS. Do not put the record back into the `NodeHosts` key of the `coredns` ConfigMap, which is where this page used to send it. k3d re-injects its own host records into that key on every `k3d cluster start`, saying so in its own startup log as injecting records for host aliases and network members into the CoreDNS configmap, and the added line goes with them. That is what makes this reappear on a cluster that was fine the day before.

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../README.md) | Platform overview, modules, build, ports |
| [config/local-dev/local_README.md](../local-dev/local_README.md) | Running locally without Kubernetes: Gradle and Docker Compose |
| [config/local-dev/e2e-stack_README.md](../local-dev/e2e-stack_README.md) | The self-contained Compose stack and the Bruno gate CI runs on it |
| [config/k8s/helm/helm_README.md](helm/helm_README.md) | Chart-by-chart reference, secret key inventory, upgrades |
| [config/k8s/_deployment-scripts/deployment_README.md](_deployment-scripts/deployment_README.md) | Deploying to the GCP cluster, sealed secrets, deployment scripts |
| [config/k8s/k8s_README.md](k8s_README.md) | Operating a running cluster with kubectl |
| [config/keycloak/SETUP.md](../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
| [docs/api/README.md](../../docs/api/README.md) | Bruno collection and OpenAPI specs |
| [e2e/README.md](../../e2e/README.md) | End-to-end capability suite and how a run is recorded |
