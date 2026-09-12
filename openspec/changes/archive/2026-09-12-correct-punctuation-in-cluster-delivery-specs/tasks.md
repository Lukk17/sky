## 1. Establish the punctuation scope

- [x] 1.1 Count the semicolon lines in `openspec/specs/helm-charts/spec.md` and
  `openspec/specs/kubernetes-deployment/spec.md` and verify the count is two in each
- [x] 1.2 Read each of the four lines and classify it as two independent clauses, a serial list separator or shell
  syntax, including the one whose first half is a bare fragment rather than a clause
- [x] 1.3 Record which requirement block each line belongs to, since a scenario cannot be corrected without restating
  its requirement

## 2. Check the chart-defaults claims

- [x] 2.1 Read every default `values.yaml` under `config/k8s/helm/service/` and list every environment-specific literal
  it carries, with the line of each
- [x] 2.2 Verify whether a namespace literal is among them, so the requirement's list is not restated with an item
  nothing illustrates
- [x] 2.3 Verify what the Auth0 example should have become, by finding the annotations in the charts that carry the same
  concern
- [x] 2.4 Read the `values-local.yaml` and `values-prod.yaml` overlays of one chart to establish what an overlay
  actually overrides, and whether the defaults are meant as placeholders
- [x] 2.5 Decide the handling for the placeholder-defaults scenario, which the repository fails, and record why
- [x] 2.6 Read the requirement this change does not touch, `TLS secret names are not misleading`, and establish whether
  it holds once the prod overlay is applied, without restating it

## 3. Check the image-tag claims

- [x] 3.1 Record the `image.tag` and `image.pullPolicy` of all four service charts
- [x] 3.2 Record the `version` field of every module build file and compare it with the chart tags, including any prefix
- [x] 3.3 Record what the local overlay sets for both values, and decide whether the rule is restated or the divergence
  reported

## 4. Check the single-deployment-path claims

- [x] 4.1 Run the exact `find` the scenario names and record the result
- [x] 4.2 Search every markdown, shell and PowerShell file under `config/k8s` for `kubectl apply` outside the Helm tree
  and list every hit with its file and line
- [x] 4.3 For each hit, establish whether a Helm chart already installs the same component, because a second path to a
  charted component is the case the requirement is most clearly about
- [x] 4.4 Verify no documentation link targets a path under `config/k8s/vanilla/`, by searching every markdown file for
  the string rather than only the two the scenario names
- [x] 4.5 Decide the handling: keep the rule absolute and report the paths, rather than scoping it to charted components
- [x] 4.6 Verify the second draft of the delta did not quietly narrow the rule, by comparing its prohibition with the
  merged one clause by clause

## 5. Write the delta specifications

- [x] 5.1 Read both current merged specifications rather than assuming their content
- [x] 5.2 Write `specs/helm-charts/spec.md` and `specs/kubernetes-deployment/spec.md` under `## MODIFIED Requirements`,
  carrying each whole requirement block
- [x] 5.3 Verify every requirement header and every existing scenario name matches the merged file character for
  character, so the archive step cannot drop a scenario
- [x] 5.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a
  fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 5.5 Verify both deltas respect the wrap width of their merged files, which is one physical line per paragraph and
  per scenario bullet
- [x] 5.6 Run `openspec validate correct-punctuation-in-cluster-delivery-specs --strict` and verify it reports no error

## 6. Archive and verify the merged files

- [x] 6.1 Archive the change and verify both merged specifications carry the corrected blocks
- [x] 6.2 Verify no semicolon joining two clauses remains in either merged file
- [x] 6.3 Verify Auth0 appears in neither merged file
- [x] 6.4 Verify the untouched requirement is unchanged in its own text
- [x] 6.5 Verify the two rules this change deliberately did not weaken are still as strict in the merged files as they
  were before
- [x] 6.6 Verify `openspec validate --specs --strict` still reports eighteen passed and none failed
- [x] 6.7 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 7. Notes from the run

- Task 1.2 classification. Three of the four lines join two independent clauses. The fourth,
  `openspec/specs/kubernetes-deployment/spec.md` line 11, reads "zero matches; the entire vanilla tree ... have been
  removed", whose first half is a noun phrase rather than a clause. It was corrected anyway, because the semicolon is
  doing the work of joining two statements with the subject of the first elided, and naming what returns the zero made
  the scenario more checkable rather than less.
- Task 2.1 found three kinds of environment literal in every default `values.yaml` under
  `config/k8s/helm/service/`. Read in `sky-booking`: `secretName: dev-ssl-cert` at line 43, the oauth2-proxy
  `auth-url` and `auth-signin` annotations carrying `https://skycloud.luksarna.com` at lines 51 and 52, and
  `host: "skycloud.luksarna.com"` at lines 59, 77 and 96. The other three charts carry the same set at the same lines.
- Task 2.2 found no namespace literal in any default values file, so the requirement keeps namespaces in its list as a
  prohibition rather than as a description of something present.
- Task 2.4 is where the divergence stopped looking accidental. The header comment of
  `config/k8s/helm/service/sky-booking/values-prod.yaml` says the defaults are production-ready today and that the
  overlay exists so future non-production environments can fork. The prod overlay therefore overrides almost nothing,
  setting `secretName: sky-tls-cert` and restating the pull policy, while `values-local.yaml` overrides the image
  coordinates, the issuer URI and the ingress annotations.
- Task 2.5 decided to keep the placeholder-defaults scenario exactly as strict as it was. The reasoning is in design.md
  under Decisions, and the short version is that a comment in a values file records a convenience rather than the
  retirement of a contract.
- Task 2.6 found the TLS-naming requirement holds where its own scenario looks. The default is `dev-ssl-cert` in all
  four charts, and `values-prod.yaml` overrides it to `sky-tls-cert`, which is the name the requirement asks for and
  what a `kubectl get secrets -n sky-prod` would therefore show.
- Task 3.2 found agreement modulo the prefix. All four service charts set `tag: "v2.0.0"` and all six modules declare
  `version = "2.0.0"`, and the topmost `## [2.0.0]` entry in each module CHANGELOG agrees with both.
- Task 3.3 found the local overlay setting `tag: "latest"` with `pullPolicy: "Never"`, which the rule as written
  forbids and which makes sense for an image built on the machine and never pulled. Reported rather than exempted,
  because exempting it is a narrowing decision with an owner.
- Task 4.1 returned zero, so the vanilla tree really is gone.
- Task 4.2 and 4.3 found three non-Helm paths. The one that most clearly breaks the rule is
  `config/k8s/_deployment-scripts/deployment_README.md` line 172, which applies
  `config/k8s/secret/sealed-secrets-controller.yaml` while
  `config/k8s/helm/api-gateway/sealed-secrets-controller/` is a chart for the same controller, and the runbook itself
  calls the manifest a vendored plain one. The second is `config/k8s/local_README.md` lines 129, 141 and 221, applying
  `config/k8s/local/sky-secrets-local.yaml`, `config/k8s/secret/ssl/dev-ssl-cert.yaml` and
  `config/k8s/local/kafka-local.yaml`, where a Kafka chart also exists at `config/k8s/helm/kafka/`. The third is
  `config/k8s/Xperimantal/`, holding Keycloak, Kong and PostgreSQL manifests with their own apply scripts, and its own
  `keycloak_README.md` opens by saying the path is an experiment that is not in use and that the platform runs the Helm
  chart instead.
- Task 4.5 and 4.6 together caught a mistake made inside this change. The first draft of the delta scoped the
  prohibition to components that already have a chart, which read better and quietly legalised two of the three paths
  above. It was rewritten to keep the absolute form before validation, and the comparison in 4.6 was run clause by
  clause against the merged text to confirm the second draft is no weaker than what it replaces.
- Task 6.5 holds. The merged `helm-charts` file still requires obviously-placeholder defaults and the merged
  `kubernetes-deployment` file still says hand-written apply YAML must not exist.
- `openspec archive` emitted the same non-blocking warning as the changes archived before it today, that the proposal's
  Why section exceeds 1000 characters. Left as written, because each corrected claim needs the file and the line it was
  read from.
