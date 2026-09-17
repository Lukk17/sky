# Helm charts

Chart-by-chart reference for the sky platform. Every chart in [config/k8s/helm/](.) is documented here: what it installs, which values it reads, and which `sky-secrets` keys it needs.

Start with the scripts. The manual sequence below them exists for the times you need to install one chart on its own, read what a chart actually does, or debug a failed release.

---

### Deploy with the scripts

[config/k8s/_deployment-scripts/](../_deployment-scripts/) holds one script per operation, in a Linux and a Windows batch flavour. They wrap the whole manual sequence below in dependency order and wait for each dependency to become ready before moving on, which is the part that is easy to get wrong by hand.

| Operation | Linux | Windows |
|---|---|---|
| First install | [helm/linux/helm-app-deploy.sh](../_deployment-scripts/helm/linux/helm-app-deploy.sh) | [helm/win/helm-app-deploy.bat](../_deployment-scripts/helm/win/helm-app-deploy.bat) |
| Upgrade in place | [helm/linux/helm-app-upgrade.sh](../_deployment-scripts/helm/linux/helm-app-upgrade.sh) | [helm/win/helm-app-upgrade.bat](../_deployment-scripts/helm/win/helm-app-upgrade.bat) |
| Remove everything | [helm/linux/helm-app-remove.sh](../_deployment-scripts/helm/linux/helm-app-remove.sh) | [helm/win/helm-app-remove.bat](../_deployment-scripts/helm/win/helm-app-remove.bat) |

Run them from the repository root, not from the script directory, because every path inside them is relative to the root.

Unix shell:

```bash
./config/k8s/_deployment-scripts/helm/linux/helm-app-deploy.sh
```

Windows:

```bat
.\config\k8s\_deployment-scripts\helm\win\helm-app-deploy.bat
```

They read one variable, `ENV`, which defaults to `prod` and selects the `values-<env>.yaml` overlay for the charts that ship one: keycloak, oauth2-proxy, kafka, and the four services. Set `ENV=local` to install the same releases with the local overlays.

Unix shell:

```bash
ENV=local ./config/k8s/_deployment-scripts/helm/linux/helm-app-deploy.sh
```

Windows, set the variable first:

```bat
set ENV=local
```

Then run the script as above.

Two things the deploy scripts do not do, so plan around them:

- They do not install an ingress controller. Install nginx-ingress first, see [config/k8s/local_README.md](../local_README.md) for a local cluster or [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md) for GKE.
- They always install the Sealed Secrets controller and apply the sealed secrets from [config/k8s/secret/sealed/](../secret/sealed/), including under `ENV=local`. One of those three, `sealed-secrets.yaml`, is not committed, so generate it with the command in section 2 first. Both deploy scripts check that it is there before they touch the cluster and stop with a message naming the section that produces it. A local k3d cluster has no sealed-secrets key material at all, so use the plain committed secret and the step-by-step runbook in [config/k8s/local_README.md](../local_README.md) instead of the scripts.

The upgrade script skips the sealed secrets and reinstalls nothing, it only runs `helm upgrade` per release. The remove script deletes every release plus the `sealed-secrets` namespace and the Kafka PVC.

---

### What is in the tree

| Chart | Path | Installs |
|---|---|---|
| Sealed Secrets controller | [api-gateway/sealed-secrets-controller/](api-gateway/sealed-secrets-controller/) | The controller that decrypts SealedSecret resources in the cluster |
| oauth2-proxy | [api-gateway/oauth2-proxy/](api-gateway/oauth2-proxy/) | The OIDC session proxy nginx delegates authentication to |
| Keycloak | [infra/keycloak/](infra/keycloak/) | Keycloak plus its own backing PostgreSQL and the `sky` realm import |
| floci | [infra/floci/](infra/floci/) | The S3-compatible object store that holds offer photos |
| App PostgreSQL | [db/postgres/](db/postgres/) | The `sky` database shared by the three stateful services |
| Database PVC | [db/database-persistent-volume-claim/](db/database-persistent-volume-claim/) | The claim the PostgreSQL StatefulSet mounts |
| Kafka | [kafka/](kafka/) | Single-node KRaft broker on `apache/kafka`, see the Kafka section |
| Services | [service/](service/) | One chart each for sky-offer, sky-booking, sky-message, sky-notify. There is no chart for sky-gateway, which is local-development only |

---

### Prerequisites

Point `kubectl` at the right cluster before running any install. Getting this wrong installs the production stack onto whatever context was last active.

```shell
kubectl config current-context
```

```shell
kubectl config use-context my-cluster-name
```

---

### Values overlays

Every chart has a default `values.yaml`. Charts that differ per environment also ship `values-local.yaml` and `values-prod.yaml`, and you pass the overlay with `-f`. Helm merges the overlay on top of the defaults, so the overlay only carries the keys that change.

| Chart | Default `values.yaml` targets | Overlays |
|---|---|---|
| oauth2-proxy | No environment at all: the OIDC issuer, the redirect URL, the ingress host and the TLS secret name are empty and the templates wrap each one in `required` | [values-local.yaml](api-gateway/oauth2-proxy/values-local.yaml) carries the nip.io issuer, the `http://localhost:5777` callback, host `localhost`, `dev-ssl-cert`, and turns off cookie-secure and the SSL redirect, [values-prod.yaml](api-gateway/oauth2-proxy/values-prod.yaml) carries the production issuer, callback, host and `sky-tls-cert` |
| Keycloak | No environment at all: the hostname, the ingress host and the TLS secret name are empty and the templates wrap each one in `required` | [values-local.yaml](infra/keycloak/values-local.yaml) carries the nip.io hostname, `dev-ssl-cert` and no SSL redirect, [values-prod.yaml](infra/keycloak/values-prod.yaml) carries `keycloak.luksarna.com` and `sky-tls-cert` |
| Services | No environment at all: the ingress host, the TLS secret name, the OIDC issuer and the cross-origin allow-list are empty and the templates wrap each one in `required`, so a render with no overlay fails instead of pointing somewhere real | `values-local.yaml` per chart points at locally built `:latest` images with `pullPolicy: Never`, hosts `localhost`, the nip.io issuer, the two local browser origins, and nulls out the auth annotations, `values-prod.yaml` carries the production host, the production issuer, the production frontend origin, `sky-tls-cert`, and the oauth2-proxy auth annotations |
| Kafka | Both environments: the single-node KRaft broker is identical either way | [values-local.yaml](kafka/values-local.yaml) and [values-prod.yaml](kafka/values-prod.yaml) are both deliberately empty of overrides, so the `-f values-<env>.yaml` argument the scripts pass resolves for this chart too |
| floci | Both environments, with the public ingress off by default | [values-local.yaml](infra/floci/values-local.yaml) turns the ingress on for host `s3.localhost`, [values-prod.yaml](infra/floci/values-prod.yaml) states the off position explicitly |
| PostgreSQL, PVC, Sealed Secrets | Both environments | No overlay, the defaults are environment-neutral |

Namespace is never a value. Every template uses `.Release.Namespace`, so `-n <namespace>` on the Helm command decides where a release lands. The one cross-service address a chart holds follows the same rule: `offerService.hostname` in [service/sky-booking/values.yaml](service/sky-booking/values.yaml) is written as `http://sky-offer-service.{{ .Release.Namespace }}.svc.cluster.local` and the deployment template renders it through `tpl`, so installing the pair into another namespace points `sky-booking` at the `sky-offer` beside it rather than at the one in `default`.

---

### 1. Sealed Secrets controller

Sealed Secrets encrypts Kubernetes secrets in the repository. The controller decrypts them at deploy time using a TLS key that lives outside the repository, in a password manager or a secrets vault. Creating and rotating that key pair is covered in [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md).

Create the namespace:

```shell
kubectl create namespace sealed-secrets
```

Create the TLS secret from your stored key pair:

```shell
kubectl create secret tls sealed-secrets-key --cert=./config/k8s/secret/sealed-public.crt --key=./config/k8s/secret/sealed-private.key -n sealed-secrets
```

Install the vendored chart:

```shell
helm install sealed-secrets-controller ./config/k8s/helm/api-gateway/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
```

Or install the current upstream chart instead:

```shell
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
```

```shell
helm install sealed-secrets-controller sealed-secrets/sealed-secrets -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
```

Apply the two sealed secrets that are committed:

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
```

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-dev-ssl-cert.yaml
```

The third one, `sky-secrets`, is not committed. Generate it in section 2 below and apply it there. Nothing that needs a credential starts until it exists.

---

### 2. The sky-secrets key inventory

Every chart that needs a credential reads it from one Secret named `sky-secrets`. In a cluster with sealed secrets it is what the controller decrypts from the SealedSecret you generate below, which lands in [config/k8s/secret/sealed/](../secret/sealed/) as `sealed-secrets.yaml`. On a local cluster it is the plain committed [config/k8s/local/sky-secrets-local.yaml](../local/sky-secrets-local.yaml).

| Key | Read by | Purpose |
|---|---|---|
| `postgres-user` | postgres chart, all three stateful services | Username for the `sky` database |
| `postgres-password` | postgres chart, all three stateful services | Password for the `sky` database |
| `s3-access-key` | sky-offer | S3 access key. The AWS SDK refuses to build a client without one, floci accepts any value |
| `s3-secret-key` | sky-offer | S3 secret key. Same rule, and the same caveat about floci |
| `keycloak-admin` | keycloak chart | Keycloak admin console username |
| `keycloak-admin-password` | keycloak chart | Keycloak admin console password |
| `keycloak-db-user` | keycloak chart | Username for Keycloak's own backing PostgreSQL, not the app database |
| `keycloak-db-password` | keycloak chart | Password for Keycloak's own backing PostgreSQL |
| `keycloak-client-id` | oauth2-proxy | OIDC client id, `sky-backend` |
| `keycloak-client-secret` | oauth2-proxy | OIDC client secret |
| `keycloak-client-cookie-secret` | oauth2-proxy | Cookie encryption secret, 32 random bytes base64-encoded |

No SealedSecret for `sky-secrets` is committed, so generating one is a prerequisite of every production deploy rather than something you do when a credential changes. The file that used to sit here was sealed in July 2023, before the MySQL-to-PostgreSQL move and before the Auth0-to-Keycloak move: of the eleven keys above it carried only `postgres-user`, alongside twelve dead ones left over from MySQL, Auth0, the basic auth era, and the pre-rename `postgres-pass`. Applying it gave every pod a `CreateContainerConfigError`, so it was deleted rather than resealed with placeholder values, on the grounds that a file which looks deployable and is not costs more than an explicit step. Until the command below has been run there is nothing to apply. The path is listed in [config/.gitignore](../../.gitignore), so the file you generate never shows up in `git status` and nobody commits it back. Why a SealedSecret is excluded here when the tool makes it safe to commit is answered in section 3 of [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md). Neither local path is affected: a local cluster uses the plain committed [config/k8s/local/sky-secrets-local.yaml](../local/sky-secrets-local.yaml), which carries all eleven keys, and Docker Compose reads its credentials from inline fallbacks in [config/docker/docker-compose.yaml](../../docker/docker-compose.yaml). `minio-root-user` and `minio-root-password` appear in neither the table nor the command: the floci chart needs no credentials, so nothing reads them.

Create the Secret and seal it, and repeat the whole command whenever any single credential changes. Seal against the certificate the target cluster's controller is using right now. The committed [config/k8s/secret/sealed-public.crt](../secret/sealed-public.crt) dates from July 2023, and a controller that has been reinstalled since then no longer holds the matching private key, so the command below omits `--cert` and lets `kubeseal` fetch the certificate through the two controller flags it already passes. The reasoning is in [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md). Substitute real values:

```shell
kubectl create secret generic sky-secrets --from-literal=postgres-user=<postgres-user> --from-literal=postgres-password=<postgres-password> --from-literal=s3-access-key=<s3-access-key> --from-literal=s3-secret-key=<s3-secret-key> --from-literal=keycloak-admin=admin --from-literal=keycloak-admin-password=<keycloak-admin-password> --from-literal=keycloak-db-user=keycloak_user --from-literal=keycloak-db-password=<keycloak-db-password> --from-literal=keycloak-client-id=sky-backend --from-literal=keycloak-client-secret=<client-secret> --from-literal=keycloak-client-cookie-secret=<32-byte-random-base64> --dry-run=client -o yaml | kubeseal --controller-namespace sealed-secrets --controller-name sealed-secrets-controller -o yaml > config/k8s/secret/sealed/sealed-secrets.yaml
```

Apply the result:

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
```

Generate the cookie secret:

```shell
python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())"
```

---

### 3. Keycloak

Keycloak 26 runs with a dedicated backing PostgreSQL managed inside the same chart. Do not point it at the app database. The `sky` realm is imported on first boot from [infra/keycloak/files/sky-realm.json](infra/keycloak/files/sky-realm.json), which the chart publishes as a ConfigMap mounted at `/opt/keycloak/data/import`. That file is the only copy of the realm in the repository, so `helm install` and `helm template` both work straight from a clean checkout with no copy step.

The hostname is not in the chart default. `hostname` reaches the container as `KC_HOSTNAME` and also fills the Ingress host, and both, together with the TLS secret name, are empty behind `required`, so an install with no overlay fails the render instead of announcing itself as the production identity provider. Keycloak builds every issuer, authorization and token URL from `KC_HOSTNAME`, so a wrong value here mints tokens no service will accept.

```shell
helm install keycloak ./config/k8s/helm/infra/keycloak/ -f ./config/k8s/helm/infra/keycloak/values-prod.yaml
```

Wait for the backing PostgreSQL, then for Keycloak:

```shell
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak-postgres
```

```shell
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak
```

In production Keycloak answers at `https://keycloak.luksarna.com`, with the admin console at `https://keycloak.luksarna.com/admin`. Realm contents, users, and token minting are documented in [config/keycloak/SETUP.md](../../keycloak/SETUP.md).

---

### 4. oauth2-proxy

`oauth2-proxy` sits in front of the authenticated routes. nginx delegates to it through `auth-url` annotations, it validates the OIDC session against Keycloak, and it forwards the caller's identity in the `x-auth-request-email`, `x-auth-request-access-token`, and `authorization` headers so each downstream resource server can verify the bearer token independently.

The chart uses provider `keycloak-oidc` against the `sky` realm. Its default `values.yaml` names no environment: the issuer, the callback URL, the ingress host and the TLS secret name are empty behind `required`, and `cookie-secure` defaults to the safe `true`. Every install therefore passes an overlay, and a forgotten one fails the render rather than quietly building a session cookie for the wrong front door.

```shell
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/ -f ./config/k8s/helm/api-gateway/oauth2-proxy/values-prod.yaml
```

The local equivalent, which turns `cookie-secure` back off because the local cluster answers on plain HTTP:

```shell
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/ -f ./config/k8s/helm/api-gateway/oauth2-proxy/values-local.yaml
```

---

### 5. App PostgreSQL

PostgreSQL 16 holds one database named `sky`. All three stateful services connect to it at `jdbc:postgresql://postgres-service:5432/sky` and each owns its own Flyway history table. The PVC is a separate chart so the database pod can be recreated without losing the claim.

```shell
helm install database-persistent-volume-claim ./config/k8s/helm/db/database-persistent-volume-claim/
```

```shell
helm install postgres ./config/k8s/helm/db/postgres/
```

```shell
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=postgres
```

---

### 6. floci, the object store

`sky-offer` stores offer photos in floci, the same AWS emulator the local developer stack runs, pinned to the same image digest. Nothing about the application changed with the swap: floci speaks the S3 API, the application creates its bucket on first boot, and there is no bucket-init step.

```shell
helm install floci ./config/k8s/helm/infra/floci/ -f ./config/k8s/helm/infra/floci/values-local.yaml
```

```shell
kubectl wait --namespace default --for=condition=ready --timeout=120s pod -l component=floci
```

Swap `values-local.yaml` for `values-prod.yaml` outside a local cluster. The deployment scripts pass `values-${ENV}.yaml` and pick the right one for you.

floci authenticates nobody. It accepts any credentials, does not verify a SigV4 signature, and serves an unsigned `GET` of any object. That was measured against `floci/floci:2.0.1`, not assumed. The two consequences are that `s3-access-key` and `s3-secret-key` exist only because the AWS SDK refuses to build a client without credentials, and that the public ingress is off unless an overlay turns it on. Turning it on in an environment reachable from the internet publishes every offer photo, and every write path, to anyone who can resolve the host.

#### The two addresses

The store has two addresses and they are not interchangeable.

| Property | Chart value | What it is |
|---|---|---|
| `sky.s3.endpoint` | `s3.endpoint`, `http://floci-service:4566` | Where `sky-offer` sends its own S3 API calls: upload, delete, bucket creation. A cluster-internal name |
| `sky.s3.presign-endpoint` | `s3.presignEndpoint`, empty by default | The address baked into a presigned URL handed to a client. Must resolve for that client |

A presigned URL is signed against the host it names, port included, so the store has to receive the same `Host` header the signature was computed over. nginx forwards the client `Host` verbatim (`proxy_set_header Host $best_http_host`, where `$best_http_host` is `$http_host`), so routing a signed URL through the ingress works as long as the signed host is the one the client actually dials.

Leave `s3.presignEndpoint` empty and the presigner falls back to `s3.endpoint`, which is the pre-existing behaviour: correct under docker-compose, where one hostname resolves on both sides, and useless in a cluster, where the internal service name resolves for nobody outside it. `values-local.yaml` sets it to `http://s3.localhost:5777`, the floci ingress on the same host port the rest of the stack answers on.

---

### 7. Kafka

The chart under [kafka/](kafka/) started life as Bitnami's and has since been repointed at upstream Apache images, because Bitnami delisted its whole public image catalogue and every `bitnami/*` tag the chart shipped now returns 404 from Docker Hub. [kafka/values.yaml](kafka/values.yaml) sets `image.repository: apache/kafka` at tag `3.7.1` and carries a literal `config` block with a single-node KRaft `server.properties`: `node.id=1`, `process.roles=broker,controller`, a controller quorum of `1@localhost:9093`, and replication factors of 1 throughout. `kraft.enabled` is true, `zookeeper.enabled` is false, and a `format-storage` init container runs `kafka-storage.sh format --ignore-formatted` against that same config before the broker starts, which is the step KRaft needs and the old ZooKeeper path did not. Data lives on an 8Gi PVC mounted at `/kafka`.

```shell
helm install kafka-service ./config/k8s/helm/kafka/ -f ./config/k8s/helm/kafka/values-local.yaml
```

Topic creation no longer goes through the Bitnami provisioning Job. `provisioning.enabled` is `false` and the broker config sets `auto.create.topics.enable=true`, so `offerTopic-1`, `bookingTopic-1` and their `.DLT` partners appear when a producer or consumer first touches them. The topic list under `provisioning.topics` is kept as documentation of what the platform expects, not as something the chart acts on.

Both [kafka/values-local.yaml](kafka/values-local.yaml) and [kafka/values-prod.yaml](kafka/values-prod.yaml) are deliberately empty of overrides: `values.yaml` already renders the broker both environments run. They exist so `ENV=local` and `ENV=prod` resolve the same `-f values-<env>.yaml` path the scripts pass to every other chart, and all four deployment scripts now do pass it for Kafka. The command above is what the scripts run, not a workaround for them.

This chart is the only definition of the broker. A hand-written standalone manifest used to sit beside it at `config/k8s/local/kafka-local.yaml` and named its Service and its StatefulSet `kafka-service`, exactly as this chart does under `fullnameOverride`, so whichever was applied second collided with the first. It has been deleted and [config/k8s/local_README.md](../local_README.md) now installs the chart in both environments.

---

### 8. Service charts

Each service chart lives under [service/](service/). Install them in any order once the infrastructure charts are healthy. Every install pairs the chart defaults with the environment overlay.

All four deployment templates wrap the image coordinates in Helm's `required`:

```text
image: "{{ required "deployment.image.repository must be set, see values.yaml" .Values.deployment.image.repository }}:{{ required "deployment.image.tag must be set, see values.yaml" .Values.deployment.image.tag }}"
```

So an overlay or a `--set` that empties either value fails the render and names the value, instead of producing a pod spec with a half-formed image reference that only fails at pull time. `values.yaml` supplies both for every chart, `values-local.yaml` overrides them to the locally built `:latest` tag, and the release pipeline rewrites the tag in `values.yaml`. The overlay uses `latest` rather than a version because the local loop rebuilds and reimports the same tag on every change, so a version pin there would need editing in four values files and in the import command on every bump.

Four more values are wrapped the same way, and these are empty in `values.yaml` rather than supplied by it: `ingress.tls.secretName`, every `hosts[].host`, `oauth2.issuerUri`, and `crossOrigin.allowed`. Each one names a concrete environment, so the chart holds none of them and only an overlay can fill them in. A `helm template` or a `helm install` with no `-f` therefore exits non-zero on the first of the four, naming it:

```text
Error: execution error at (sky-offer/templates/ingress.yaml:19:13): ingress hosts[].host must be set by a values-<env>.yaml overlay
```

That is deliberate and it is the loud half of the design. A placeholder hostname would render a complete set of manifests that `kubectl apply` accepts, leaving a forgotten overlay to surface as an Ingress nobody can reach rather than as a failed command. `helm lint` does not exercise `required`, so `helm template` is the check that proves it.

```shell
helm install sky-offer ./config/k8s/helm/service/sky-offer -f ./config/k8s/helm/service/sky-offer/values-prod.yaml
```

```shell
helm install sky-booking ./config/k8s/helm/service/sky-booking -f ./config/k8s/helm/service/sky-booking/values-prod.yaml
```

```shell
helm install sky-message ./config/k8s/helm/service/sky-message -f ./config/k8s/helm/service/sky-message/values-prod.yaml
```

```shell
helm install sky-notify ./config/k8s/helm/service/sky-notify -f ./config/k8s/helm/service/sky-notify/values-prod.yaml
```

---

### Ingress and routing

nginx-ingress terminates TLS and rewrites paths. Every service chart templates its own Ingress resources, `sky-notify` included. `sky-notify` is the one that does not rewrite: its single Ingress serves `/notifyWebsocket` with `pathType: Prefix`, passes the path through unchanged, carries no oauth2-proxy auth annotations because the JWT is checked on the STOMP `CONNECT` frame, and raises `proxy-read-timeout` and `proxy-send-timeout` to 3600 seconds so nginx does not close an idle WebSocket after its default 60.

Using `sky-offer` as the example:

| Public path | Rewritten to | Authentication |
|---|---|---|
| `/offer/api(/\|$)(.*)` | `/api/v1/$2` | None, these are the public list and search endpoints |
| `/offer/api/owner(/\|$)(.*)` | `/api/v1/owner/$2` | Through oauth2-proxy |
| `/offer/swagger-ui(/\|$)(.*)` | `/swagger-ui/$2` | Through oauth2-proxy |
| `/offer/v3/api-docs(/\|$)(.*)` | `/v3/api-docs/$2` | Through oauth2-proxy |

The annotations that delegate to oauth2-proxy, which live in each service chart's `values-prod.yaml` because the first two name a concrete environment's front door:

```yaml
nginx.ingress.kubernetes.io/auth-url: "https://skycloud.luksarna.com/oauth2/auth"
nginx.ingress.kubernetes.io/auth-signin: "https://skycloud.luksarna.com/oauth2/start"
nginx.ingress.kubernetes.io/auth-response-headers: "x-auth-request-user, x-auth-request-email, x-auth-request-access-token, authorization"
```

The third names no environment and stays in `values.yaml`. Each `values-local.yaml` sets all three to `~`, and the ingress templates skip nil-valued annotations, so a local install gets the same paths with no auth hop.

---

### Cross-origin origins

The frontend and the API answer on different hosts, `https://sky.luksarna.com` and `https://skycloud.luksarna.com`, so every call the frontend makes is cross-origin and each service has to answer with that exact origin or the browser throws the response away. A wildcard is not an option: the calls carry a bearer token, and a browser refuses a wildcard on a credentialed request.

`sky-booking`, `sky-offer` and `sky-message` read the allow-list from `sky.crossOrigin.allowed`, which takes its value from the `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable. Each chart holds it as `crossOrigin.allowed`, empty in `values.yaml` behind `required`, and the deployment template renders it into that variable.

| Overlay | Value | Why |
|---|---|---|
| `values-prod.yaml` | `https://sky.luksarna.com` | The deployed frontend, and nothing else |
| `values-local.yaml` | `http://localhost:5777,http://localhost:4200` | The two origins a browser loads a page from locally: the ingress on host port 5777, and the Angular dev server |

The per-service ports are deliberately absent from the local list. In a cluster the four Services are `ClusterIP` with no host publishing, so nothing reaches `localhost:5552` and friends from a browser, and under Docker Compose, where those ports are published, a page served by a service calling that same service is same-origin and never consults the allow-list.

Until this value existed in the charts nothing set the variable anywhere, so every deployed service fell back to the committed default in its own `application.yaml`, which names the API host plus three localhost origins. That default is unchanged and still applies to a bare `bootRun` or a Compose run. What changed is that a chart-installed service no longer uses it.

`sky-notify` is not in the table and cannot be. Its allowed origins are a hardcoded list in `WebSocketConfig`, pinned by a test, and no chart value reaches them. Narrowing it is a source change, tracked separately.

The ingress carries a second, independent allow-list. `nginx.ingress.kubernetes.io/cors-allow-origin` on the `sky-offer` owner, swagger and swagger-resource ingresses names the same production frontend, and lives in [service/sky-offer/values-prod.yaml](service/sky-offer/values-prod.yaml) beside `enable-cors`. The two move together: enabling CORS at the ingress without naming an origin makes nginx answer with a wildcard.

---

### Changing the application hostname

Every hostname the production environment answers on lives in an overlay, so this is an edit to `values-prod.yaml` files and never to a chart default:

1. Update every `hosts[].host` under `ingress.service`, `ingress.serviceOwner`, `ingress.swagger`, and `ingress.swaggerResource` in each service chart's `values-prod.yaml`.
2. Update the `auth-url` and `auth-signin` annotations in the same files.
3. Update `args.redirectUrl` and `args.oidcIssuerUrl` in [api-gateway/oauth2-proxy/values-prod.yaml](api-gateway/oauth2-proxy/values-prod.yaml).
4. Update `hostname` and `ingress.hosts[].host` in [infra/keycloak/values-prod.yaml](infra/keycloak/values-prod.yaml) if the identity provider moves with it, and `oauth2.issuerUri` in each service chart's `values-prod.yaml` to match.
5. Add the new callback URL to the `sky-backend` client's redirect URIs in [infra/keycloak/files/sky-realm.json](infra/keycloak/files/sky-realm.json), and to the running Keycloak if the realm is already imported.
6. Update the Bruno environments in [docs/api/request/environments/](../../../docs/api/request/environments/).

Moving the frontend is a separate edit, because the frontend and the API answer on different hosts. It touches `crossOrigin.allowed` in each service chart's `values-prod.yaml` and the `cors-allow-origin` annotations in [service/sky-offer/values-prod.yaml](service/sky-offer/values-prod.yaml). See the cross-origin section below.

---

### Upgrading a chart

```shell
helm upgrade sky-offer ./config/k8s/helm/service/sky-offer -f ./config/k8s/helm/service/sky-offer/values-prod.yaml
```

Omitting the `-f` overlay reverts that release to the chart defaults, and for a service chart those defaults name no environment, so the upgrade fails on the first `required` value instead of quietly moving the release to another environment's hostname. Always pass the same overlay you installed with. The upgrade scripts listed at the top of this document do that for you.

---

### Uninstalling

Remove the service releases:

```shell
helm uninstall sky-offer sky-booking sky-message sky-notify
```

Remove the infrastructure releases:

```shell
helm uninstall kafka-service floci postgres database-persistent-volume-claim oauth2-proxy keycloak
```

Remove the Sealed Secrets controller and its key:

```shell
helm uninstall sealed-secrets-controller -n sealed-secrets
```

```shell
kubectl delete secret sealed-secrets-key -n sealed-secrets
```

```shell
kubectl delete -f config/k8s/secret/sealed --recursive
```

---

### Troubleshooting

`CreateContainerConfigError` on a pod. The pod cannot read a key from `sky-secrets`. Three causes, in the order worth checking: the `sky-secrets` SealedSecret was never generated and applied, since none is committed and generating it is a prerequisite (see the key inventory above, this is the likely one today), it was applied but the controller holds no private key matching the certificate it was sealed against so nothing decrypted, or it decrypted into a Secret that is missing a key the chart reads. See [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md).

`cannot unmarshal number into Go struct field EnvVar...value of type string`. An environment value in a values file or a deployment template is unquoted. Wrap numeric-looking values in double quotes.

`cannot re-use a name that is still in use`. A release with that name is already installed. Upgrade it, or uninstall it first.

Keycloak fails to start with `KC_DB` errors. Its backing PostgreSQL was not ready when Keycloak started. The `kubectl wait` between the two installs exists for exactly this, and the deploy scripts already include it.

`ImagePullBackOff` on the Kafka pod. The chart defaults pin an image tag Docker Hub no longer serves. See the Kafka section above.

`ImagePullBackOff` on a service pod in a local cluster. The `values-local.yaml` overlay sets `pullPolicy: Never` and expects the `:latest` image already imported into the node. Import it, see [config/k8s/local_README.md](../local_README.md).

---

### Common Helm commands

List installed releases:

```shell
helm list
```

Render a chart without installing it, which is the fastest way to see what a values change actually does:

```shell
helm template sky-offer ./config/k8s/helm/service/sky-offer -f ./config/k8s/helm/service/sky-offer/values-local.yaml
```

Show the values a release was installed with:

```shell
helm get values sky-offer
```

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../../README.md) | Platform overview, modules, build, ports |
| [config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md) | Deploying to the GCP cluster, sealed secrets, deployment scripts |
| [config/k8s/local_README.md](../local_README.md) | Local Kubernetes cluster on k3d: bring-up, verification, teardown |
| [config/k8s/k8s_README.md](../k8s_README.md) | Operating a running cluster with kubectl |
| [config/local-dev/local_README.md](../../local-dev/local_README.md) | Running locally without Kubernetes: Gradle and Docker Compose |
| [config/keycloak/SETUP.md](../../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
| [api-gateway/sealed-secrets-controller/sealedSecrets_README.md](api-gateway/sealed-secrets-controller/sealedSecrets_README.md) | Vendored Sealed Secrets chart parameter reference |
| [kafka/README.md](kafka/README.md) | Upstream Bitnami parameter reference the Kafka chart was forked from. Still useful for the parameter names, stale on every image tag and on the ZooKeeper and provisioning paths this chart no longer uses |
