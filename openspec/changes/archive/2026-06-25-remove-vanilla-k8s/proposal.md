## Why

`config/k8s/` carries two parallel deployment paths:
- **`vanilla/`** — 24 hand-written YAML files (Deployments, Services, ConfigMaps, Ingresses) deployed via `kubectl apply -f`.
- **`helm/`** — full chart hierarchy: per-service charts, Bitnami Kafka subchart, MySQL chart, oauth2-proxy chart, sealed-secrets-controller chart with RBAC/CRDs/ServiceMonitor.

Helm is strictly a superset:
- Everything vanilla deploys, Helm deploys.
- Helm adds NetworkPolicy, RBAC, ServiceMonitor (Prometheus), PodDisruptionBudget that vanilla lacks.
- Helm parameterizes; vanilla hardcodes.

Maintaining both means every K8s-touching change has to be made twice, and one path always lags behind. Vanilla is the lagging one. Delete it.

## What Changes

- **Delete** `config/k8s/vanilla/` directory tree (24 YAML files + 2 READMEs).
- **Delete** `config/k8s/_deployment-scripts/vanilla/` (4 shell + 2 bat scripts).
- **Update** root `README.md` to remove the two doc links pointing at vanilla READMEs (ingress, kafka). Replace with pointers to Helm equivalents.
- **Update** `config/k8s/_deployment-scripts/deployment_README.md` — remove the 8 `kubectl apply -f vanilla/...` commands and the "Clearing" section that targets vanilla resources.
- **Update** `config/k8s/k8s_README.md` if it references vanilla.
- **Update** `config/k8s/vanilla/api-gateway/ingress/ingress_README.md` content — the routing detail in there is still useful as documentation. Migrate the useful parts to `config/k8s/helm/helm_README.md` or a new `INGRESS.md` under helm; then delete the vanilla version.
- **Migrate** the Kafka `vanilla/kafka/kafka_README.md` notes the same way.

## Capabilities

### New Capabilities
- _None._

### Modified Capabilities
- _None._ Removing a redundant deployment path; the canonical Helm path is unchanged.

## Impact

- **Touched files**: ~31 files deleted, 2–3 docs updated.
- **Operationally**: anyone running the legacy `kubectl apply -f vanilla/...` will fail. Single user (you); pre-warned.
- **CI/CD**: no impact — no pipeline references vanilla (verified by grep in the audit).
- **Service code**: zero impact.
- **Risk**: low. Pure deletion of unused parallel infrastructure.
- **Dependency order**: independent. Can land any time. Best ordered before `helm-parameterize-cleanup` so Helm work has the spotlight.
