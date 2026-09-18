## MODIFIED Requirements

### Requirement: Helm charts have no environment-specific values in defaults
Default `values.yaml` files in every Helm chart MUST NOT contain environment-specific literals. That covers hostnames, TLS secret names, namespaces, and the oauth2-proxy `auth-url` and `auth-signin` ingress annotations, each of which names a concrete environment's front door. Environment specifics MUST live in `values-<env>.yaml` overlay files passed via `-f` at deploy time, so the chart itself carries no answer to the question of which environment it is for.

#### Scenario: Switching environments
- **WHEN** an operator deploys the same chart to a different environment with a different hostname
- **THEN** the only change required is selecting a different `values-<env>.yaml` overlay, and no value inside the chart needs editing

#### Scenario: Default values are obviously placeholders
- **WHEN** an operator runs `helm template` without an overlay
- **THEN** rendered manifests carry obviously-placeholder values, for example `host: example.com`, so a forgotten overlay fails loudly rather than silently deploying into a real environment

### Requirement: Image tags are pinned, pull policy is cache-friendly
Helm chart values MUST set `image.tag` to a specific version rather than to `latest`, and that version MUST correspond to the `version` field of the module the image is built from, so a deployed pod can be traced back to a source revision. The four service charts set `tag: "v2.0.0"` and every module declares `version = "2.0.0"`, which is the same value carrying a `v` prefix in the registry. `image.pullPolicy` MUST default to `IfNotPresent`, so a node that already holds the image does not go back to the registry for it.

#### Scenario: Pod scheduling reuses cached images
- **WHEN** a pod is scheduled on a node that already has the image
- **THEN** Kubernetes does not re-pull from the registry, and the pod starts immediately from the cached image
