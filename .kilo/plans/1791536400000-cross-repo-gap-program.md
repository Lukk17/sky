# Cross-repo gap program for sky

Adopt the proven practices of the sibling repos (Pharmacy pha-pharma and pha-cloud, AscendAI) that sky lacks.
Scope decided with the owner: full program, full contract-first OpenAPI, release images to Docker Hub plus GHCR.
SonarQube is explicitly out of scope (owner skipped it). Formatting gates, secret scanning, and the dependency-analysis plugin are explicitly out of scope (owner cut them).

## Source evidence (verified by reading the sibling repos)

- Pharmacy applies owasp dependency-check in its root build file; derives version from the top CHANGELOG entry; sets -parameters on all compiles; defines an aggregated JaCoCo report plus verification gate at 0.90 line and branch.
- Pharmacy CI runs path-filtered per-service matrix builds, changelog bump gate, redocly bundle diff, mock route and body parity scripts, served-contract boot plus Bruno run, Dockerfile-module parity, and IDE config verification.
- Pharmacy contracts folder holds a hand-written split contract with a committed bundled openapi file and a redocly lint config.
- Pharmacy observability folder provisions Prometheus, Grafana, Loki, and Alloy with dashboards and a usage README; services expose actuator prometheus via micrometer.
- AscendAI documents a changelog-gated CI, a selective release workflow pushing multi-arch images to Docker Hub plus GHCR with a per-registry bump guard, and a separate live-stack Bruno workflow.
- Sky facts: apps/backend/buildSrc has no OWASP wiring; micrometer Prometheus registry is applied but nothing scrapes it; release images are local-only; sky-view hand-writes API models; CI builds every module on every run; the Bruno collection misses three REST endpoints.

## Process rules for every phase below

- Run each phase through the repo OpenSpec flow (propose, then apply, then archive).
- One PR per phase against the module changelog gate (every touched service gets a changelog entry).
- Never push, never open a PR, never commit without the owner saying so in that conversation.

## Prerequisites (owner provides before the release phase)

- DOCKERHUB_USERNAME and DOCKERHUB_TOKEN repository secrets (GHCR uses the automatic GITHUB_TOKEN, no secret needed).
- After the first GHCR publish of each image, set package visibility to public by hand once per image.

## Phase 1: dependency vulnerability scanning (fills the removed-Dependabot hole)

- Add the OWASP dependency-check Gradle plugin via the version catalog and apply it in apps/backend/build.gradle.kts.
- Add a CI job that runs the OWASP check on every PR and fails on CVSS above the agreed threshold (propose 7.0, owner confirms in the phase proposal).
- Acceptance: owaspDependencyCheck runs locally and in CI; a deliberately vulnerable test dependency fails the gate.

## Phase 2: build hygiene

- Derive the build version from the top CHANGELOG entry (copy the versionFromChangelog pattern), removing the cosmetic drift between build files and changelogs.
- Add an aggregated JaCoCo report plus an aggregated 0.90 line and branch verification gate.
- Set -parameters on all JavaCompile tasks and configure test logging to show failed and skipped tests with full stack traces.
- Acceptance: version prints from the changelog; aggregate coverage gate runs in check; failing tests print causes.

## Phase 3: missing Bruno endpoint requests

- Add GET single offer by id, PUT gallery cover (set main photo), and GET gateway session to docs/api/request, with value assertions in the existing style (status plus body values, chaining ids where the flow needs them).
- Websocket notify stays out of Bruno by design (needs a code test with a websocket client).
- Error-branch assertions (booking 404, 409, 502, 503; gallery 404, 409; validation 400s; 401 and 403 per service) stay a separate future pass, not this phase.
- Acceptance: the collection covers all 21 REST endpoints and passes green on compose and on the cluster env.

## Phase 4: contract-first OpenAPI (largest phase, split in three)

- 4a: create contracts/sky-api/src/ with the hand-written contract skeleton and a committed bundled openapi file; add the redocly bundle plus diff gate to CI; add a contracts path filter output.
- 4b: generate TypeScript API clients for apps/frontend/sky-view from the bundled contract and replace the hand-written models; add a client freshness check to CI.
- 4c: migrate server side per service (offer, booking, message; notify and gateway have no REST contract surface to generate) to generated stubs and DTOs; keep ArchUnit hexagonal rules green throughout.
- Add a served-contract CI job that boots the stack and diffs the served document against the committed bundle.
- Acceptance per sub-phase: bundle diff green; frontend builds against generated clients; all module tests plus ArchUnit green.

## Phase 5: local observability stack

- Add a compose-embedded Prometheus, Grafana, Loki, and Alloy set with provisioned datasources, one overview dashboard, and one alert rule file, modeled on pha-cloud observability.
- Point Prometheus at the existing actuator prometheus endpoints of all four services plus the gateway.
- Document scrape verification and two starter PromQL queries in the stack README.
- Acceptance: prometheus targets shows all services UP; logs for every service are queryable in Grafana Explore.

## Phase 6: registry release publishing

- Extend .github/workflows/release.yaml to the AscendAI model: selective per-service release, multi-arch build once, push version plus latest to Docker Hub and GHCR, per-registry bump guard that fails closed on ambiguity.
- Keep the changelog as the single version source and keep the workflow commit-free.
- Acceptance: a dry image-only run publishes to both registries; a same-version rerun is blocked by the guard.

## Phase 7: CI maturity

- Add path-filtered change detection so untouched modules skip build and test.
- Add a Dockerfile-module parity script that fails when a Gradle module is missing from its Dockerfile copy list.
- Add a workflows README documenting triggers, gates, and secrets.
- Upload test reports as artifacts on failure.
- Acceptance: a docs-only PR finishes CI in under two minutes with an empty build matrix.

## Phase 8: host and DNS verification script

- Add a script that verifies every name the stacks depend on (localhost, wildcard localhost subdomains, keycloak.test) resolves to loopback on the machine, plus the gateway ports answer.
- Run it as the first step of the local runbooks and reference it from the k3d and compose guides. This is the direct lesson of the unproven k3d photo-fetch failures.
- Acceptance: the script fails with a naming-the-host message on a machine where wildcard localhost does not resolve.

## Explicitly out of scope

- SonarQube server and analysis (owner decision).
- Formatting gates, secret scanning, dependency-analysis plugin (owner cut).
- Pact broker (superseded by Phase 4 contract-first).
- CodeQL (noted as a future idea only).
