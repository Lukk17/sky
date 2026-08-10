# kubernetes-deployment Specification

## Purpose
TBD - created by archiving change remove-vanilla-k8s. Update Purpose after archive.
## Requirements
### Requirement: Helm is the only Kubernetes deployment path
The repository MUST maintain exactly one Kubernetes deployment path: Helm charts under `config/k8s/helm/`. Hand-written `kubectl apply -f` YAML files (the historical `config/k8s/vanilla/` tree) MUST NOT exist.

#### Scenario: Searching for vanilla manifests
- **WHEN** a contributor runs `find config/k8s -type d -name vanilla`
- **THEN** zero matches; the entire vanilla tree and its deployment scripts have been removed

#### Scenario: Documentation pointers
- **WHEN** the root `README.md` or `config/k8s/_deployment-scripts/deployment_README.md` references the deployment process
- **THEN** all links resolve to Helm-side documents; no link targets a path under `config/k8s/vanilla/`

