## 1. Establish the evidence

- [x] 1.1 Read both commits that changed the behaviour, `e2f4c41` and `4970536`, and record which value each moved out
  of which chart default
- [x] 1.2 Render every chart with no overlay and record the exit status, the byte count on standard output and the
  verbatim error, with `helm template <name> config/k8s/helm/<path>` for each of the eleven top-level charts
- [x] 1.3 Confirm helm names one missing value at a time by rendering `sky-booking` with `--set` supplying the first
  missing value and verifying the error moves to the next
- [x] 1.4 Confirm the six categories currently held out of defaults by grepping every `required` call under
  `config/k8s/helm/**/templates/` and matching each to an empty default in that chart's `values.yaml`
- [x] 1.5 Confirm the namespace claim by rendering `sky-booking` twice with `--namespace sky-prod` and
  `--namespace sky-dev` and verifying the offer-service address differs by the namespace alone
- [x] 1.6 Confirm the optional-feature case by reading `config/k8s/helm/infra/floci/templates/floci-ingress.yaml` and
  its three values files, and verifying the ingress is off by default, on in local and off in production
- [x] 1.7 Confirm the two requirements not being touched still hold: the image tag against every module's `version` in
  `build.gradle.kts`, and every `secretName` across every values file
- [x] 1.8 Confirm no environment literal survives in any default by grepping every `values.yaml` for the production
  and local hostnames and for a literal namespace
- [x] 1.9 Confirm `s3.presignEndpoint`, the one remaining empty default with no `required` behind it, is a deliberate
  fallback rather than a missed value, by reading `S3Properties` in `sky-offer` and verifying a blank value falls back
  to the internal endpoint

## 2. Write the change artifacts

- [x] 2.1 Write `proposal.md` naming `helm-charts` as the one modified capability
- [x] 2.2 Write `specs/helm-charts/spec.md`, first as a `## MODIFIED Requirements` block carrying the entire
  requirement, and verify the requirement header matches the merged file byte for byte with `od -c`
- [x] 2.3 Rework the delta into `## ADDED Requirements` plus `## REMOVED Requirements` after
  `openspec validate --strict` rejected the `MODIFIED` block for omitting the scenario name being replaced, and record
  the rejection and the alternatives in `design.md`
- [x] 2.4 Write `design.md` recording the retire-and-replace decision, the closed-list decision, the mechanism
  sentence, the namespace carve-out and the alternatives each beat
- [x] 2.5 Verify every artifact holds no em dash and no en dash, using a matcher first validated against a fixture
  containing an em dash, an en dash, an arrow and a bullet, and confirming it reports the first two and stays silent
  on the last two
- [x] 2.6 Verify the delta matches the merged file's wrap style, which is one physical line per paragraph and per
  bullet with no hard wrap
- [x] 2.7 Run `openspec validate correct-helm-defaults-requirement-to-failing-render --strict` and verify it reports
  no error
- [x] 2.8 Dry-run the archive against a copy of the `openspec` tree in a scratch directory, and diff the resulting
  merged file to see exactly what the real archive will write before writing it

## 3. Archive and sync

- [x] 3.1 Archive the change and let the CLI perform the merged-file rewrite
- [x] 3.2 Verify `openspec/specs/helm-charts/spec.md` no longer contains `example.com` or the phrase
  `obviously-placeholder`, and now carries the verbatim helm error
- [x] 3.3 Verify by reading the merged file that the `Image tags are pinned` and `TLS secret names are not misleading`
  requirements are byte-identical to their pre-archive text, and that the capability `## Purpose` is unchanged
- [x] 3.4 Run `openspec validate --specs --strict` and verify it reports no error
- [x] 3.5 Verify `git status` shows no file changed outside `openspec/` and `sky-message/AGENTS.md`, and report any
  entry under `openspec/changes/` that is not `archive/` and not this change

## 4. Notes from the run

- The evidence render is `helm template <release> config/k8s/helm/<chart>` with no `-f`. Six charts exit 1 with zero
  bytes on standard output: the four service charts, `infra/keycloak` and `api-gateway/oauth2-proxy`. Five exit 0 with
  a full manifest set: `api-gateway/sealed-secrets-controller`, `db/postgres`,
  `db/database-persistent-volume-claim`, `kafka` and `infra/floci`.
- Helm names one missing value per run. With the host supplied the same chart fails on
  `ingress.tls.secretName must be set by a values-<env>.yaml overlay` at `ingress.yaml:21:21`, and with both supplied
  it fails on the next ingress, `ingress-swagger.yaml:19:13`. The scenario says so rather than implying the render
  lists everything that is missing.
- `openspec validate --strict` rejected the first draft, which used `## MODIFIED Requirements`, because a MODIFIED
  block may not omit a scenario name the merged spec still holds. That is what forced the retire-and-replace shape.
  The rejection and the three alternatives are in design.md.
- The archive was rehearsed first against a copy of the whole `openspec` tree in a scratch directory. The merged file
  the real archive wrote is identical to the rehearsal, line for line.
- `Image tags are pinned, pull policy is cache-friendly` is byte-identical after the archive.
  `TLS secret names are not misleading` differs by exactly one trailing newline, because it is no longer the last
  block in the file, and its text is byte-identical when stripped. The capability `## Purpose` is untouched.
- One thing the evidence pass turned up that is not a defect and is recorded so the next reader does not chase it:
  `s3.presignEndpoint` in `sky-offer/values.yaml` is the one empty default with no `required` behind it. It is a
  deliberate fallback, not a missed value: `S3Properties.effectivePresignEndpoint()` returns `s3.endpoint` when it is
  blank, which is what the cluster wants, and only the local overlay sets it.
- `openspec/changes/` holds `archive/` and one active change that is not this one,
  `resolve-notify-outbound-adapter-package-deviation`, created by another agent while this change was open. It targets
  `architecture` and `spring-boot-hygiene`, so it does not touch `helm-charts`.
