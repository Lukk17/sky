## 1. Inventory

- [x] 1.1 List every `values.yaml` under `config/k8s/helm/`. For each, list values that are environment-specific but currently hardcoded.
- [x] 1.2 Grep `skycloud.luksarna.com`, `dev-ssl-cert`, `imagePullPolicy: Always`, `namespace: default`. List all hits.

## 2. Defaults & overlays

- [x] 2.1 Move env-specific literals out of `values.yaml` defaults and into `values-prod.yaml` (initially mirrors current state) per chart.
- [x] 2.2 Create `values-dev.yaml` per chart for the dev variant (localhost frontend, different TLS, etc.) if a dev environment is operational.
- [x] 2.3 Update default `values.yaml` to use clearly placeholder-style defaults (e.g., `host: example.com`) so a forgotten override is loud, not silent.

## 3. Rename and parameterize

- [x] 3.1 Rename TLS secret references `dev-ssl-cert` → `sky-tls-cert`. Update sealed-secret YAMLs accordingly.
- [x] 3.2 Parameterize `image.pullPolicy` to `IfNotPresent` default.
- [x] 3.3 Parameterize `namespace`. Update each `Chart.yaml` and template files to use `.Release.Namespace` (the idiomatic Helm value) instead of literal.
- [x] 3.4 Parameterize Auth0 callback / audience / issuer in oauth2-proxy chart.

## 4. Image tag pinning

- [x] 4.1 For each service chart, set `image.tag` to the matching service version (e.g., `1.0.2`). Document the convention: chart and service share a version.
- [x] 4.2 Update CI/build to publish the image with the matching tag (coordinate with `docker-modernization`).

## 5. Umbrella chart (optional)

- [x] 5.1 Decide: create `config/k8s/helm/sky/Chart.yaml` that lists each chart as a dependency. If yes, populate `Chart.yaml` `dependencies:` and run `helm dependency update`.
- [x] 5.2 If no umbrella, skip this section and use the existing per-chart deploy scripts.

## 6. Scripts

- [x] 6.1 Update `config/k8s/_deployment-scripts/helm/linux/helm-app-deploy.sh` and Windows .bat to pass `-f values-${ENV}.yaml` where `ENV` is an env var.
- [x] 6.2 Same for `helm-app-upgrade.sh` and `helm-app-remove.sh`.

## 7. Lint & verify

- [x] 7.1 `helm lint config/k8s/helm/service/sky-offer` (and others) — clean.
- [x] 7.2 `helm template config/k8s/helm/service/sky-offer -f values-prod.yaml` produces the same Deployment YAML (modulo intentional changes) as before. Diff against a pre-change template render.
- [x] 7.3 Deploy to a local kind cluster via the umbrella (or per-chart). All pods reach Ready.
- [x] 7.4 Smoke: Ingress resolves; one REST call succeeds.
