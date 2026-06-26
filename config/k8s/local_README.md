# Local k3d cluster — sky platform

This document is the single source of commands needed to stand up the full sky backend stack
on a local k3d cluster and verify it with the Bruno collection.

The cluster is named `k3d-sky`. Host port 5777 maps to the cluster load balancer port 80.
All traffic enters through nginx-ingress. Keycloak is the OIDC provider, available at
`http://keycloak.127.0.0.1.nip.io:5777`. The nip.io domain resolves to 127.0.0.1 on the
host without touching the hosts file, and CoreDNS resolves it to the nginx-ingress ClusterIP
inside the cluster so services can reach Keycloak for OIDC discovery.

---

### Prerequisites

- k3d >= 5 (`k3d version`)
- kubectl (`kubectl version --client`)
- Helm >= 3.12 (`helm version`)
- Bruno CLI >= 1 (`bru --version`)
- Docker with local images: `sky-offer:e2e`, `sky-booking:e2e`, `sky-message:e2e`, `sky-notify:e2e`

---

### 1. Create the k3d cluster

Run once. Skip if the cluster already exists.

PowerShell:
```powershell
k3d cluster create sky --port "5777:80@loadbalancer" --k3s-arg "--disable=traefik@server:0"
```

Bash:
```bash
k3d cluster create sky --port "5777:80@loadbalancer" --k3s-arg "--disable=traefik@server:0"
```

---

### 2. Import local Docker images into k3d

```bash
k3d image import sky-offer:e2e sky-booking:e2e sky-message:e2e sky-notify:e2e -c sky
```

---

### 3. Install nginx-ingress

```bash
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer
```

Wait for the controller to be ready before continuing:

```bash
kubectl wait pod -n ingress-nginx -l app.kubernetes.io/component=controller \
  --for=condition=Ready --timeout=120s
```

---

### 4. Create the sky-secrets Secret

All credentials are plain-text dev values. Never use these in production.

```bash
kubectl create secret generic sky-secrets \
  --from-literal=postgres-user=sky_user \
  --from-literal=postgres-password=sky_pass \
  --from-literal=s3-access-key=admin \
  --from-literal=s3-secret-key=password \
  --from-literal=spring-security-user=sky_user \
  --from-literal=spring-security-pass=sky_pass \
  --from-literal=keycloak-admin=admin \
  --from-literal=keycloak-admin-password=admin \
  --from-literal=keycloak-db-user=keycloak_user \
  --from-literal=keycloak-db-password=keycloak_pass \
  --from-literal=keycloak-client-secret=dev-only-change-in-prod
```

---

### 5. Create the TLS Secret (self-signed, dev only)

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /tmp/tls.key -out /tmp/tls.crt \
  -subj "//CN=localhost/O=k3d-sky-local"
kubectl create secret tls dev-ssl-cert --cert=/tmp/tls.crt --key=/tmp/tls.key
```

---

### 6. Patch CoreDNS for in-cluster Keycloak resolution

Get the nginx-ingress ClusterIP:

```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller \
  -o jsonpath='{.spec.clusterIP}'
```

Replace `NGINX_CLUSTER_IP` in the command below with that value (e.g. `10.43.245.67`):

```bash
kubectl patch configmap coredns -n kube-system \
  --type merge \
  -p '{"data":{"NodeHosts":"NGINX_CLUSTER_IP keycloak.127.0.0.1.nip.io\n"}}'
kubectl rollout restart deployment coredns -n kube-system
```

---

### 7. Deploy infra charts

Run all commands from the repo root.

```bash
helm upgrade --install database-persistent-volume-claim \
  config/k8s/helm/db/db-volume-claim -n default
```

```bash
helm upgrade --install postgres config/k8s/helm/db/postgres -n default
```

```bash
helm upgrade --install minio config/k8s/helm/infra/minio -n default
```

```bash
helm upgrade --install keycloak config/k8s/helm/infra/keycloak \
  -f config/k8s/helm/infra/keycloak/values-local.yaml -n default
```

Deploy Kafka using the standalone KRaft manifest (the Bitnami chart image is no longer
available on Docker Hub):

```bash
kubectl apply -f config/k8s/local/kafka-local.yaml
```

Wait for infra to be ready:

```bash
kubectl wait pod -l component=postgres --for=condition=Ready --timeout=120s
kubectl wait pod -l component=keycloak-postgres --for=condition=Ready --timeout=120s
kubectl wait pod -l component=keycloak --for=condition=Ready --timeout=180s
kubectl wait pod -l component=minio --for=condition=Ready --timeout=120s
kubectl wait statefulset/kafka-service --for=condition=Available=true --timeout=120s
```

Fix the Postgres readiness probe (the exec pg_isready probe does not work in the k8s exec
context; switch to tcpSocket which is consistent with startup and liveness):

```bash
kubectl patch statefulset postgres-deployment --type='json' \
  -p='[{"op":"replace","path":"/spec/template/spec/containers/0/readinessProbe","value":{"tcpSocket":{"port":5432},"initialDelaySeconds":0,"periodSeconds":10,"failureThreshold":3}}]'

kubectl patch statefulset keycloak-postgres --type='json' \
  -p='[{"op":"replace","path":"/spec/template/spec/containers/0/readinessProbe","value":{"tcpSocket":{"port":5432},"initialDelaySeconds":0,"periodSeconds":10,"failureThreshold":3}}]'
```

Delete the pods so the patched spec takes effect:

```bash
kubectl delete pod postgres-deployment-0 keycloak-postgres-0
kubectl wait pod -l component=postgres --for=condition=Ready --timeout=90s
kubectl wait pod -l component=keycloak-postgres --for=condition=Ready --timeout=90s
```

---

### 8. Deploy service charts

```bash
helm upgrade --install sky-offer config/k8s/helm/service/sky-offer \
  -f config/k8s/helm/service/sky-offer/values-local.yaml -n default

helm upgrade --install sky-booking config/k8s/helm/service/sky-booking \
  -f config/k8s/helm/service/sky-booking/values-local.yaml -n default

helm upgrade --install sky-message config/k8s/helm/service/sky-message \
  -f config/k8s/helm/service/sky-message/values-local.yaml -n default

helm upgrade --install sky-notify config/k8s/helm/service/sky-notify \
  -f config/k8s/helm/service/sky-notify/values-local.yaml -n default
```

Wait for all service pods to be ready:

```bash
kubectl wait pod -l app=sky-offer --for=condition=Ready --timeout=180s
kubectl wait pod -l app=sky-booking --for=condition=Ready --timeout=180s
kubectl wait pod -l app=sky-message --for=condition=Ready --timeout=180s
kubectl wait pod -l app=sky-notify --for=condition=Ready --timeout=180s
```

---

### 9. Verify the stack

Check all pods are 1/1 Running:

```bash
kubectl get pods
```

Expected output (8 pods, all 1/1):

```
kafka-service-0                  1/1 Running
keycloak-deployment-*            1/1 Running
keycloak-postgres-0              1/1 Running
minio-deployment-0               1/1 Running
postgres-deployment-0            1/1 Running
sky-booking-deployment-*         1/1 Running
sky-message-deployment-*         1/1 Running
sky-notify-deployment-*          1/1 Running
sky-offer-deployment-*           1/1 Running
```

Verify Keycloak OIDC issuer:

```bash
curl -s "http://keycloak.127.0.0.1.nip.io:5777/realms/sky/.well-known/openid-configuration" \
  | grep '"issuer"'
```

Expected: `"issuer":"http://keycloak.127.0.0.1.nip.io/realms/sky"`

---

### 10. Run the Bruno collection

```bash
cd docs/api/request
bru run -r --env k3d --insecure
```

Expected result: 16/16 requests pass, 66/66 assertions green.

---

### Bruno k3d environment

The environment file is at `docs/api/request/environments/k3d.yml`:

- `baseUrl`: `http://localhost:5777`
- `keycloakUrl`: `http://keycloak.127.0.0.1.nip.io:5777`
- `keycloakClientId`: `sky-backend`
- `keycloakClientSecret`: `dev-only-change-in-prod`
- `keycloakUsername`: `lukk`
- `keycloakPassword`: `test1234`

---

### Credentials (dev-only, never use in production)

| Service | Username | Password |
|---|---|---|
| Postgres (sky) | sky_user | sky_pass |
| MinIO | admin | password |
| Keycloak admin | admin | admin |
| Keycloak realm user | lukk | test1234 |
| Keycloak client secret | sky-backend | dev-only-change-in-prod |

---

### Tear down

Remove all chart releases:

```bash
helm uninstall sky-offer sky-booking sky-message sky-notify \
  keycloak minio postgres database-persistent-volume-claim -n default
kubectl delete -f config/k8s/local/kafka-local.yaml
kubectl delete secret sky-secrets dev-ssl-cert
kubectl delete pvc --all -n default
```

Destroy the cluster completely:

```bash
k3d cluster delete sky
```

---

### Known issues and design notes

1. Postgres readiness probe: the Helm charts use `exec: pg_isready -U $(POSTGRES_USER)` but
   Kubernetes does not expand env vars in probe exec commands; `$(POSTGRES_USER)` is passed
   literally and `pg_isready` without a resolvable Unix socket path returns exit code 3
   ("no attempt"). The tcpSocket patch in step 7 works around this permanently for the local
   cluster. The chart templates should be updated to use `tcpSocket` as a follow-up.

2. Keycloak ingress class: the keycloak chart uses the deprecated `kubernetes.io/ingress.class`
   annotation. This was patched to `spec.ingressClassName: nginx` in the template at
   `config/k8s/helm/infra/keycloak/templates/keycloak-ingress.yaml`.

3. Bitnami Kafka image: `bitnami/kafka:3.5.0-debian-11-r7` is no longer available on Docker Hub.
   The cluster uses a standalone KRaft-mode Kafka defined in `config/k8s/local/kafka-local.yaml`
   using `apache/kafka:3.7.1`.

4. Spring Boot 4 breaking change: `SPRING_SECURITY_USER` env var maps to `spring.security.user`,
   which Spring Boot 4 now requires to be a structured object rather than a plain string. The
   env var was a dead leftover from the basic-auth era and was removed from all four service
   deployment templates.

5. Auth annotations: the production `values.yaml` files contain nginx auth-url/auth-signin
   annotations that route to the production oauth2-proxy. These are nulled out in
   `values-local.yaml` for every affected ingress section. The ingress templates were updated
   to skip nil-valued annotations so the null override takes effect.

6. Image non-numeric user: the `sky-*:e2e` images use a named user `sky`. Kubernetes requires
   a numeric UID when `runAsNonRoot: true` is set. The deployment templates were updated to
   add `runAsUser: 1000` alongside `runAsNonRoot: true`.
