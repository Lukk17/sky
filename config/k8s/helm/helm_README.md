# Helm deployment guide

Charts live under [config/k8s/helm/](.). The stack installs in dependency order: Sealed Secrets controller, then
`oauth2-proxy`, then Kafka and MySQL, then the four service charts. Each step is independent; partial installs are
safe to resume.

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

To create new or rotated credentials, see
[config/k8s/_deployment-scripts/deployment_README.md](../_deployment-scripts/deployment_README.md#create-new-sealed-secrets).

---

### 2. oauth2-proxy (API gateway)

`oauth2-proxy` sits in front of authenticated routes. It validates OIDC sessions with Keycloak and forwards the
caller's identity in `x-auth-request-email`, `x-auth-request-access-token`, and `authorization` headers so the
downstream JWT resource-servers can verify the bearer token independently.

**Before deploying, re-seal the Keycloak credentials.** The chart reads three keys from `sky-secrets`:
`keycloak-client-id`, `keycloak-client-secret`, and `keycloak-client-cookie-secret`. Seal them with kubeseal:

```shell
kubectl create secret generic sky-secrets \
  --from-literal=keycloak-client-id=<client-id> \
  --from-literal=keycloak-client-secret=<client-secret> \
  --from-literal=keycloak-client-cookie-secret=<32-byte-random-base64> \
  --dry-run=client -o yaml \
  | kubeseal --controller-namespace sealed-secrets --controller-name sealed-secrets-controller \
  --cert config/k8s/secret/sealed-public.crt -o yaml \
  > config/k8s/secret/sealed/sealed-secrets.yaml
```

Then apply:

```shell
kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
```

Install oauth2-proxy with the production overlay:

```shell
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/ -f ./config/k8s/helm/api-gateway/oauth2-proxy/values.yaml -f ./config/k8s/helm/api-gateway/oauth2-proxy/values-prod.yaml
```

---

### 3. Kafka

Install the bundled Bitnami chart (v3.5.0):

```shell
helm install kafka ./config/k8s/helm/kafka/
```

Install the latest upstream chart:

```shell
helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka
```

---

### 4. MySQL

Install the bundled chart (MySQL v8.0.33):

```shell
helm install mysql ./config/k8s/helm/mysql/
```

---

### 5. Service charts

Each service chart lives under [config/k8s/helm/service/](./service/). Install them in any order after the
infrastructure charts are healthy.

```shell
helm install sky-offer ./config/k8s/helm/service/sky-offer
```

```shell
helm install sky-booking ./config/k8s/helm/service/sky-booking
```

```shell
helm install sky-message ./config/k8s/helm/service/sky-message
```

```shell
helm install sky-notify ./config/k8s/helm/service/sky-notify
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
| [../k8s_README.md](../k8s_README.md) | kubectl reference, secrets, cluster access, Auth0 login |
| [../../local-dev/local_README.md](../../local-dev/local_README.md) | Local dev: Gradle, Docker, Minikube |
| [../../../README.md](../../../README.md) | Root README: platform overview, modules, build |
| [api-gateway/sealed-secrets-controller/sealedSecrets_README.md](api-gateway/sealed-secrets-controller/sealedSecrets_README.md) | Sealed Secrets chart parameter reference |
