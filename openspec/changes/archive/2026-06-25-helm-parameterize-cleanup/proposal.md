## Why

The Helm charts work but carry environment specifics in places they shouldn't:

- **Hostname `skycloud.luksarna.com`** hardcoded in every service chart's `values.yaml` ingress section. Switching environments means copy-editing N values files.
- **TLS secret name `dev-ssl-cert`** hardcoded across charts. Even prod uses a secret called `dev-ssl-cert` — misleading at minimum.
- **Namespace `default`** hardcoded as the deployment namespace.
- **`imagePullPolicy: Always`** hardcoded — guarantees a pull on every pod schedule, defeats Docker image caching, slows rollouts.
- **Auth0 OAuth callback URLs** embedded in ingress annotations — env-specific, should be parameterized.

A small but real bonus: charts use mixed style for templating (sometimes plain Helm `{{ .Values.x }}`, sometimes pinned literals next to templated values in the same block). Standardize on full parameterization with sensible defaults in `values.yaml` and `values-prod.yaml` / `values-dev.yaml` overlays.

## What Changes

- **Add** environment-specific values files per service: `values-dev.yaml`, `values-prod.yaml` (or a single `values-local.yaml` if only one prod env exists). Defaults stay in `values.yaml`.
- **Parameterize**:
  - `ingress.host` (default `skycloud.luksarna.com` in `values.yaml`)
  - `ingress.tlsSecretName` (rename `dev-ssl-cert` → `sky-tls-cert` as part of this; document the cert rotation/rename procedure)
  - `namespace` (default `default`; chart accepts override)
  - `image.pullPolicy` (default `IfNotPresent`; `Always` only via override)
  - `auth0.callbackUrl`, `auth0.audience`, `auth0.issuer` — pulled into a shared block consumed by the oauth2-proxy chart.
- **Add** `Chart.lock` files where missing (Bitnami Kafka subchart in particular).
- **Pin image tags**: every service deployment uses an explicit chart-version-matched image tag rather than `latest`. Coordinate with the service Gradle `version` field.
- **Add** a top-level umbrella chart `config/k8s/helm/sky/` that depends on the individual charts so one `helm install sky ./sky` deploys the whole stack. (Optional; nice-to-have if not too invasive.)
- **Update** deployment scripts under `config/k8s/_deployment-scripts/helm/` to pass `-f values-<env>.yaml` and use the umbrella chart if added.
- **Lint**: run `helm lint` against every chart; fix warnings.

## Capabilities

### New Capabilities
- `helm-deployment`: A single canonical Helm path with per-environment overlay files, pinned image tags, parameterized hosts/secrets/namespaces, and no environment specifics baked into the default values.

### Modified Capabilities
- _None._ External behavior unchanged; same workloads deploy to the same cluster.

## Impact

- **Touched files**: every chart's `values.yaml`, new overlay files, possibly new umbrella chart, deployment shell/bat scripts under `_deployment-scripts/helm/`.
- **Operationally**: existing deploys re-deploy with new pull policy (faster restarts), same image versions. Secret rename requires a one-shot kubectl secret rename or recreate.
- **TLS secret rename**: requires sealing the cert under the new name and `helm upgrade`; brief reconciliation window. Document in tasks.
- **Risk**: low-medium. Mostly chart hygiene. Smoke-test cluster reachability after deploy.
- **Dependency order**: after `remove-vanilla-k8s` (so we're not parameterizing two paths). Independent of code-level changes.
