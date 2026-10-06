# kubernetes-deployment Specification

## Purpose
Makes one tool the only path into the cluster, so what is running is described in a single place rather than by a chart and a set of hand-maintained manifests that drift apart.

## Requirements

### Requirement: Helm is the only Kubernetes deployment path
The repository MUST maintain exactly one Kubernetes deployment path, which is the Helm charts under `config/k8s/helm/`. Hand-written YAML meant to be installed with `kubectl apply -f` MUST NOT exist, and that includes the historical `config/k8s/vanilla/` tree, which MUST NOT exist at all. The reason the rule is absolute rather than scoped to components that already have a chart is that a second installation path drifts from the first, and a cluster carrying both then matches neither description.

#### Scenario: Searching for vanilla manifests
- **WHEN** a contributor runs `find config/k8s -type d -name vanilla`
- **THEN** the search returns zero matches, because the entire vanilla tree and its deployment scripts have been removed

#### Scenario: Documentation pointers
- **WHEN** the root `README.md` or `config/k8s/_deployment-scripts/deployment_README.md` references the deployment process
- **THEN** every link resolves to a Helm-side document, and no link targets a path under `config/k8s/vanilla/`
