# helm-charts Specification

## Purpose
Keeps the charts deployable in more than one environment by holding environment-specific values out of the defaults, pinning image tags rather than tracking a moving one, and naming a secret for what it actually contains.

## Requirements

### Requirement: Image tags are pinned, pull policy is cache-friendly
Helm chart values MUST set `image.tag` to a specific version rather than to `latest`, and that version MUST correspond to the `version` field of the module the image is built from, so a deployed pod can be traced back to a source revision. The four service charts set `tag: "v2.0.0"` and every module declares `version = "2.0.0"`, which is the same value carrying a `v` prefix in the registry. `image.pullPolicy` MUST default to `IfNotPresent`, so a node that already holds the image does not go back to the registry for it.

#### Scenario: Pod scheduling reuses cached images
- **WHEN** a pod is scheduled on a node that already has the image
- **THEN** Kubernetes does not re-pull from the registry, and the pod starts immediately from the cached image

### Requirement: TLS secret names are not misleading
TLS secret references in chart values MUST be named for what they are (e.g., `sky-tls-cert`), not legacy environment-specific names (e.g., `dev-ssl-cert` in a prod context).

#### Scenario: Inspecting prod TLS configuration
- **WHEN** an operator runs `kubectl get secrets -n sky-prod`
- **THEN** the TLS secret has a name that reflects its purpose, not its historical environment of origin

### Requirement: Chart defaults name no environment and a missing overlay fails the render
Default `values.yaml` files in every Helm chart MUST NOT contain environment-specific literals. Six categories are held out of the defaults and the list is exhaustive as written: ingress hostnames, TLS secret names, namespaces, the oauth2-proxy `auth-url` and `auth-signin` ingress annotations, the identity provider issuer and redirect URL, and the cross origin allow list. Each of the six ties a rendered manifest to one concrete environment. A value that names no environment keeps its safe default instead, which is why the oauth2-proxy cookie security and TLS redirect flags default to `"true"` and are relaxed by the local overlay rather than the reverse. Environment specifics MUST live in `values-<env>.yaml` overlay files passed via `-f` at deploy time, so the chart itself carries no answer to the question of which environment it is for.

A value in one of those six categories that a chart needs in order to render MUST default to empty and MUST be wrapped in Helm's `required` where a template uses it, so a render with no overlay writes no manifest and exits naming the value. A placeholder literal MUST NOT stand in for it. A placeholder renders a manifest set `kubectl` accepts, so a forgotten overlay surfaces days later as an ingress nobody can reach, and a `helm upgrade` missing its `-f` silently rewrites a live release to the placeholder. A value a chart does not need in order to render MAY instead be left out of the defaults altogether, which is what the three service charts that sit behind oauth2-proxy do with the two auth annotations: the default annotation map does not hold them, the production overlay adds them and the local overlay sets them to null. An optional feature MAY likewise default off, which is what the `infra/floci` chart does with its ingress: the default disables it and only the local overlay turns it on. Neither route names an environment.

The namespace is the one of the six not held behind `required`, because it is not a value an overlay supplies: it comes from the `helm` invocation. A chart that has to name a namespace MUST carry `{{ .Release.Namespace }}` in the value and render it through `tpl`, so the address follows the release. Today that is `sky-booking` addressing `sky-offer` across the cluster. Every other in-cluster address in these charts is a bare service name that resolves inside the release namespace and so names no namespace at all.

#### Scenario: Switching environments
- **WHEN** an operator deploys the same chart to a different environment with a different hostname
- **THEN** the only change required is selecting a different `values-<env>.yaml` overlay, and no value inside the chart needs editing

#### Scenario: A render with no overlay fails and names the missing value
- **WHEN** an operator runs `helm template sky-booking config/k8s/helm/service/sky-booking` with no `-f` overlay
- **THEN** helm exits 1, writes no manifest to standard output, and prints `Error: execution error at (sky-booking/templates/ingress.yaml:19:13): ingress hosts[].host must be set by a values-<env>.yaml overlay`, naming one missing value at a time, so supplying that one moves the failure to the next rather than clearing it

#### Scenario: A chart whose defaults name no environment still renders
- **WHEN** an operator runs `helm template` with no overlay against a chart whose defaults name no environment, which today is `db/postgres`, `db/database-persistent-volume-claim`, `kafka`, `infra/floci` or `api-gateway/sealed-secrets-controller`
- **THEN** helm exits 0 and renders the full manifest set, because the requirement holds environment specifics out of defaults rather than making an overlay mandatory for every chart

#### Scenario: The same chart renders into two namespaces
- **WHEN** an operator renders `sky-booking` with the same overlay twice, once with `--namespace sky-prod` and once with `--namespace sky-dev`
- **THEN** the address it calls `sky-offer` on renders as `http://sky-offer-service.sky-prod.svc.cluster.local` and as `http://sky-offer-service.sky-dev.svc.cluster.local`, so neither the chart nor the overlay carries a namespace literal
