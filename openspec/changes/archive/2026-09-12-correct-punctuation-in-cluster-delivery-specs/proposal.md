## Why

The repository formatting rule forbids joining two independent clauses with a semicolon, and twenty lines across nine
merged specifications still do it. Four of those lines are in the two capabilities that govern how a built image reaches
the cluster: `helm-charts` scenarios at lines 11 and 22, and `kubernetes-deployment` scenarios at lines 11 and 15.

The punctuation is the occasion rather than the work. A delta replaces a requirement block by matching its header, so
fixing a scenario means restating its whole requirement, and restating a requirement means standing behind every fact
inside it. The three requirements touched here were checked against the charts, the deployment runbooks and the module
build files, and the check turned up one stale fact and two divergences where the specification is right and the
repository is not.

The stale fact is the identity provider, again. The `helm-charts` requirement names "Auth0 callbacks" as an example of
an environment-specific literal that must stay out of a default. The charts carry no Auth0 anything. What they do carry,
and what the example should have named once the provider changed, is the oauth2-proxy pair
`nginx.ingress.kubernetes.io/auth-url` and `nginx.ingress.kubernetes.io/auth-signin`, at
`config/k8s/helm/service/sky-booking/values.yaml` lines 51 and 52 and at the same lines of the other three service
charts.

The first divergence is that those very annotations are in the defaults, with the production hostname inside them. The
requirement forbids a hostname or a TLS secret name in a default `values.yaml`, and its second scenario asks that
`helm template` without an overlay render obviously-placeholder values so a forgotten overlay fails loudly. Read from
`config/k8s/helm/service/sky-booking/values.yaml`: `host: "skycloud.luksarna.com"` at lines 59, 77 and 96,
`secretName: dev-ssl-cert` at line 43, and the production hostname again inside the two oauth2-proxy annotations. The
chart says so deliberately, in the header comment of
`config/k8s/helm/service/sky-booking/values-prod.yaml`: "Defaults in values.yaml are also production-ready today". So a
`helm template` with no overlay renders a production hostname rather than a placeholder, which is the opposite of what
the scenario asks for, and it fails quietly rather than loudly.

The second divergence is that Helm is not the only path into the cluster. The literal prohibition holds, and
`find config/k8s -type d -name vanilla` returns zero matches, so the historical tree really is gone. Three other paths
are not: `config/k8s/_deployment-scripts/deployment_README.md` line 172 applies
`config/k8s/secret/sealed-secrets-controller.yaml` with `kubectl apply -f` while a Helm chart for the same controller
sits at `config/k8s/helm/api-gateway/sealed-secrets-controller/`, `config/k8s/local_README.md` lines 129, 141 and 221
apply three hand-written manifests the same way, and `config/k8s/Xperimantal/` holds Keycloak, Kong and PostgreSQL
manifests with their own `kubectl apply` scripts.

Neither divergence is resolved by rewriting the requirement that names it. Both rules are kept at full force and both
gaps are reported.

## What Changes

- Restate the no-environment-specifics requirement with the punctuation fixed, the Auth0 example replaced by the
  oauth2-proxy annotations the charts carry, and the rule itself unchanged.
- Report, without weakening that requirement, that three kinds of environment literal are in the defaults today and
  that `helm template` with no overlay renders the production hostname.
- Restate the image-tag requirement with the punctuation fixed and the pin stated precisely: the four service charts set
  `tag: "v2.0.0"`, every module's Gradle `version` is `2.0.0`, so the two agree apart from the leading `v`.
- Report, without weakening that requirement, that `values-local.yaml` deliberately sets `tag: "latest"` and
  `pullPolicy: "Never"` for a locally built image.
- Restate the single-deployment-path requirement with the punctuation fixed in both scenarios, and with the one check
  that holds stated as what it checks.
- Report, without weakening that requirement, the three non-Helm apply paths named above.
- Leave the requirements in these files that carry no semicolon alone. That is the TLS-secret-naming requirement in
  `helm-charts`, which holds through the prod overlay and is worth reading with the note in Impact.

No chart, script, manifest or runbook is touched, and nothing is deployed, templated or applied. Every value comes from
reading a committed file.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `helm-charts`: punctuation in the environment-switching scenario and in the image-pull scenario, the Auth0 example,
  and the precision of the tag-to-version claim.
- `kubernetes-deployment`: punctuation in both scenarios of the single-path requirement.

## Impact

- Affected files: `openspec/specs/helm-charts/spec.md` and `openspec/specs/kubernetes-deployment/spec.md`, both
  rewritten at archive time from the deltas in this change. Two of three requirements in the first and the only
  requirement in the second.
- Grouped together because they are one subject from two distances: what a chart may hold and what may reach the cluster
  at all. Both divergences found here are the same failure in two places, which is an environment-specific fact living
  somewhere it should not, once as a hostname in a default and once as a manifest beside a chart that already renders
  it.
- Read and left alone, with the nuance worth recording: the `helm-charts` requirement `TLS secret names are not
  misleading` looks violated at first glance, because all four default `values.yaml` files set
  `secretName: dev-ssl-cert`. It is not, because `values-prod.yaml` overrides it to `sky-tls-cert`, which is the name
  the requirement asks for, and the requirement's own scenario is about what a prod namespace shows. The name in the
  default is still an environment-specific literal, which is the other requirement's problem and is reported above.
- No source, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low for the corrections. The two open items are left as failing contracts on purpose, because closing either
  means editing charts or runbooks, or relaxing a rule that exists to stop a forgotten overlay from deploying quietly,
  and neither belongs in a punctuation pass.
