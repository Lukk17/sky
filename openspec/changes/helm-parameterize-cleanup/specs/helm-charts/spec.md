## ADDED Requirements

### Requirement: Helm charts have no environment-specific values in defaults
Default `values.yaml` files in every Helm chart MUST NOT contain environment-specific literals (hostnames, TLS secret names, namespaces, Auth0 callbacks). Environment specifics MUST live in `values-<env>.yaml` overlay files passed via `-f` at deploy time.

#### Scenario: Switching environments
- **WHEN** an operator deploys the same chart to a different environment with a different hostname
- **THEN** the only change required is selecting a different `values-<env>.yaml` overlay; no chart-internal values need editing

#### Scenario: Default values are obviously placeholders
- **WHEN** an operator runs `helm template` without an overlay
- **THEN** rendered manifests carry obviously-placeholder values (e.g., `host: example.com`) so a forgotten overlay fails loudly, not silently

### Requirement: Image tags are pinned, pull policy is cache-friendly
Helm chart values MUST set `image.tag` to a specific version (not `latest`) matching the service's Gradle `version` field, and `image.pullPolicy` MUST default to `IfNotPresent`.

#### Scenario: Pod scheduling reuses cached images
- **WHEN** a pod is scheduled on a node that already has the image
- **THEN** Kubernetes does not re-pull from the registry; the pod starts immediately

### Requirement: TLS secret names are not misleading
TLS secret references in chart values MUST be named for what they are (e.g., `sky-tls-cert`), not legacy environment-specific names (e.g., `dev-ssl-cert` in a prod context).

#### Scenario: Inspecting prod TLS configuration
- **WHEN** an operator runs `kubectl get secrets -n sky-prod`
- **THEN** the TLS secret has a name that reflects its purpose, not its historical environment of origin
