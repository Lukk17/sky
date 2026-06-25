# Helm deployment guide

Charts live under [config/k8s/helm/](.). The stack installs in dependency order: Sealed Secrets controller, then
Keycloak (with its own backing PostgreSQL), then `oauth2-proxy`, then the app PostgreSQL, MinIO, Kafka, and the four
service charts. Each step is independent; partial installs are safe to resume.

---

### Prerequisites

Make sure your `kubectl` context points at the right cluster before running any `helm install` command.

Check the current context:

```shell
kubectl config current-context
```

Switch context:

```shell
kubectl config use-context my-cluster-name
```

---

### 1. Sealed Secrets controller

Sealed Secrets encrypts Kubernetes secrets in-repo. The controller decrypts them at deploy time using a TLS key that
lives outside the repo (store it in a password manager or a secrets vault).

Create the `sealed-secrets` namespace:

```shell
kubectl create namespace sealed-secrets
```

Create the TLS secret from your stored key pair:

```shell
kubectl create secret tls sealed-secrets-key --cert=./config/k8s/secret/sealed-public.crt --key=./config/k8s/secret/sealed-private.key -n sealed-secrets
```

Install the bundled chart (v0.22.0):

```shell
helm install sealed-secrets-controller ./config/k8s/helm/api-gateway/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
```

Alternatively, install the latest upstream chart:

```shell
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
```

```shell
helm repo update
```

```shell
helm install sealed-secrets-controller sealed-secrets/sealed-secrets -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
```

Apply the sealed secrets (already committed to the repo):

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
```

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
```

---

### 2. Sky secrets — full key inventory

The single `sky-secrets` SealedSecret must contain all keys listed below. Re-create and re-seal it whenever any
credential changes.

**Keycloak keys**

| Key | Description |
|---|---|
| `keycloak-client-id` | oauth2-proxy OIDC client ID (`sky-backend`) |
| `keycloak-client-secret` | oauth2-proxy OIDC client secret |
| `keycloak-client-cookie-secret` | oauth2-proxy cookie encryption secret (32-byte random base64) |
| `keycloak-admin` | Keycloak admin username |
| `keycloak-admin-password` | Keycloak admin password |
| `keycloak-db-user` | PostgreSQL username for the Keycloak-internal backing database |
| `keycloak-db-password` | PostgreSQL password for the Keycloak-internal backing database |

**App database keys (PostgreSQL)**

| Key | Description |
|---|---|
| `postgres-user` | PostgreSQL username for the sky application database |
| `postgres-password` | PostgreSQL password for the sky application database |

**MinIO / S3 keys**

| Key | Description |
|---|---|
| `minio-root-user` | MinIO root user (acts as S3 access key for admin operations) |
| `minio-root-password` | MinIO root password |
| `s3-access-key` | S3 access key used by sky-offer (may equal `minio-root-user`) |
| `s3-secret-key` | S3 secret key used by sky-offer (may equal `minio-root-password`) |

**Spring Security keys (used by all four services)**

| Key | Description |
|---|---|
| `spring-security-user` | Spring Boot basic-auth username for local/dev profiles |
| `spring-security-pass` | Spring Boot basic-auth password for local/dev profiles |

**kubeseal command — re-create `sky-secrets`**

Build the plain secret first (substitute real values):

```shell
kubectl create secret generic sky-secrets \
  --from-literal=keycloak-client-id=sky-backend \
  --from-literal=keycloak-client-secret=<client-secret> \
  --from-literal=keycloak-client-cookie-secret=<32-byte-random-base64> \
  --from-literal=keycloak-admin=admin \
  --from-literal=keycloak-admin-password=<keycloak-admin-password> \
  --from-literal=keycloak-db-user=keycloak \
  --from-literal=keycloak-db-password=<keycloak-db-password> \
  --from-literal=postgres-user=sky \
  --from-literal=postgres-password=<postgres-password> \
  --from-literal=minio-root-user=<minio-root-user> \
  --from-literal=minio-root-password=<minio-root-password> \
  --from-literal=s3-access-key=<s3-access-key> \
  --from-literal=s3-secret-key=<s3-secret-key> \
  --from-literal=spring-security-user=<spring-user> \
  --from-literal=spring-security-pass=<spring-pass> \
  --dry-run=client -o yaml \
  | kubeseal --controller-namespace sealed-secrets --controller-name sealed-secrets-controller \
  --cert config/k8s/secret/sealed-public.crt -o yaml \
  > config/k8s/secret/sealed/sealed-secrets.yaml
```

Then apply it:

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
```

To generate the 32-byte cookie secret:

```shell
python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())"
```

---

### 3. Keycloak

Keycloak 26.x runs with a dedicated backing PostgreSQL (managed inside the same Helm chart, do not share the app DB).
The `sky` realm is pre-imported via `--import-realm` on first boot. The canonical realm file is
`config/keycloak/sky-realm.json`; the `helm-app-deploy` script syncs it into this chart's `files/sky-realm.json`
(gitignored) before installing, so there is only one committed copy. If you run `helm install`/`helm template` for this
chart by hand, copy the realm in first:

```shell
cp ./config/keycloak/sky-realm.json ./config/k8s/helm/infra/keycloak/files/sky-realm.json
```

Install (production overlay swaps the TLS secret name):

```shell
helm install keycloak ./config/k8s/helm/infra/keycloak/ \
  -f ./config/k8s/helm/infra/keycloak/values.yaml \
  -f ./config/k8s/helm/infra/keycloak/values-prod.yaml
```

Wait for the backing PostgreSQL to be ready before Keycloak finishes startup:

```shell
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak-postgres
```

```shell
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak
```

Keycloak is reachable at `https://keycloak.luksarna.com`. The admin console is at
`https://keycloak.luksarna.com/admin`.

---

### 4. oauth2-proxy (API gateway)

`oauth2-proxy` sits in front of authenticated routes. It validates OIDC sessions with Keycloak and forwards the
caller's identity in `x-auth-request-email`, `x-auth-request-access-token`, and `authorization` headers so the
downstream JWT resource-servers can verify the bearer token independently.

Install oauth2-proxy with the production overlay:

```shell
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/ -f ./config/k8s/helm/api-gateway/oauth2-proxy/values.yaml -f ./config/k8s/helm/api-gateway/oauth2-proxy/values-prod.yaml
```

---

### 5. App database (PostgreSQL)

The app database is PostgreSQL 16. All four services connect to it at `jdbc:postgresql://postgres-service:5432/sky`.
The PVC (`database-persistent-volume-claim`) is installed in a separate chart to allow the database pod to be
recreated without losing the claim.

Install the PVC:

```shell
helm install database-persistent-volume-claim ./config/k8s/helm/db/database-persistent-volume-claim/
```

Install PostgreSQL:

```shell
helm install postgres ./config/k8s/helm/db/postgres/
```

Wait for it to be ready:

```shell
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=postgres
```

---

### 6. MinIO (object storage)

sky-offer stores offer photos in MinIO (S3-compatible). Services reach it at `http://minio-service:9000`. The
application creates its bucket on first boot; no manual bucket-init step is required.

```shell
helm install minio ./config/k8s/helm/infra/minio/
```

```shell
kubectl wait --namespace default --for=condition=ready --timeout=120s pod -l component=minio
```

---

### 7. Kafka

Install the bundled Bitnami chart (v3.5.0):

```shell
helm install kafka-service ./config/k8s/helm/kafka/
```

Install the latest upstream chart:

```shell
helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka
```

---

### 8. Service charts

Each service chart lives under [config/k8s/helm/service/](./service/). Install them in any order after the
infrastructure charts are healthy.

```shell
helm install sky-offer ./config/k8s/helm/service/sky-offer -f ./config/k8s/helm/service/sky-offer/values.yaml -f ./config/k8s/helm/service/sky-offer/values-prod.yaml
```

```shell
helm install sky-booking ./config/k8s/helm/service/sky-booking -f ./config/k8s/helm/service/sky-booking/values.yaml -f ./config/k8s/helm/service/sky-booking/values-prod.yaml
```

```shell
helm install sky-message ./config/k8s/helm/service/sky-message -f ./config/k8s/helm/service/sky-message/values.yaml -f ./config/k8s/helm/service/sky-message/values-prod.yaml
```

```shell
helm install sky-notify ./config/k8s/helm/service/sky-notify -f ./config/k8s/helm/service/sky-notify/values.yaml -f ./config/k8s/helm/service/sky-notify/values-prod.yaml
```

---

### Ingress and routing

Each service chart templates its own Ingress resources. The `nginx-ingress` controller handles TLS termination and
path rewriting. Example for `sky-offer`:

- Public path `/offer/api(/|$)(.*)` rewrites to `/api/v1/$2` inside the pod.
- Owner path `/offer/api/owner(/|$)(.*)` goes through `oauth2-proxy` auth before rewriting.
- Swagger paths `/offer/swagger-ui(/|$)(.*)` also require auth.

The ingress annotations for auth are:

```yaml
nginx.ingress.kubernetes.io/auth-url: "https://skycloud.luksarna.com/oauth2/auth"
nginx.ingress.kubernetes.io/auth-signin: "https://skycloud.luksarna.com/oauth2/start"
nginx.ingress.kubernetes.io/auth-response-headers: "x-auth-request-user, x-auth-request-email, x-auth-request-access-token, authorization"
```

---

### Changing the application hostname

When moving from one hostname to another (e.g. `sky.luksarna.com` to `skycloud.luksarna.com`):

1. Update `spec.rules.host` in every service `values.yaml`.
2. Update `nginx.ingress.kubernetes.io/auth-url` and `auth-signin` annotations in every service `values.yaml`.
3. Update `redirect-url` in `oauth2-proxy/values.yaml`.
4. Update Keycloak: add the new hostname to Allowed Redirect URIs for the `sky` client at `https://keycloak.luksarna.com/admin`.
5. Update Postman environments.

---

### Upgrading a chart

```shell
helm upgrade sky-offer ./config/k8s/helm/service/sky-offer
```

---

### Uninstalling

Remove service charts:

```shell
helm uninstall sky-offer
```

```shell
helm uninstall sky-booking
```

```shell
helm uninstall sky-message
```

```shell
helm uninstall sky-notify
```

Remove infrastructure:

```shell
helm uninstall kafka-service
```

```shell
helm uninstall minio
```

```shell
helm uninstall postgres
```

```shell
helm uninstall database-persistent-volume-claim
```

```shell
helm uninstall oauth2-proxy
```

```shell
helm uninstall keycloak
```

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

**`CreateContainerConfigError`**

The pod cannot read a secret. Either the sealed secrets were not applied, or the Sealed Secrets controller was
reinstalled with a new key (requiring all secrets to be re-encrypted). See
[deployment_README.md](../_deployment-scripts/deployment_README.md#create-new-sealed-secrets).

**`cannot unmarshal number into Go struct field EnvVar...value of type string`**

Every env value in `values.yaml` and `deployment.yaml` templates must be quoted. Wrap numeric-looking values in `""`.

**`cannot re-use a name that is still in use`**

A release with that name is already installed. Either upgrade it or uninstall it first:

```shell
helm uninstall sky-offer
```

**Keycloak fails to start with `KC_DB` errors**

The Keycloak-backing PostgreSQL pod must be ready before Keycloak starts. The `kubectl wait` commands in the deploy
script handle this ordering, but if installing manually ensure `component=keycloak-postgres` is Ready before
installing or upgrading the keycloak chart.

---

### Common Helm commands

List all installed releases:

```shell
helm list
```

Download a chart as a `.tgz`:

```shell
helm pull <chart name>
```

Install from a local folder:

```shell
helm install <release name> ./<chart folder>
```

---

### Docs map

| Document | What it covers |
|---|---|
| [../k8s_README.md](../k8s_README.md) | kubectl reference, secrets, cluster access |
| [../../local-dev/local_README.md](../../local-dev/local_README.md) | Local dev: Gradle, Docker, Minikube |
| [../../../README.md](../../../README.md) | Root README: platform overview, modules, build |
| [api-gateway/sealed-secrets-controller/sealedSecrets_README.md](api-gateway/sealed-secrets-controller/sealedSecrets_README.md) | Sealed Secrets chart parameter reference |
