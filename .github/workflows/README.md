# Workflows

Two workflows live here. `ci.yaml` is the merge gate every pull request must pass. `release.yaml` publishes versioned service images and cuts the stack tag.

## Triggers

| Workflow | Trigger | What runs |
|---|---|---|
| `ci.yaml` | `pull_request` (`opened`, `reopened`, `synchronize`) | Full gate, path-filtered per changed module. |
| `ci.yaml` | `workflow_dispatch` | Full pipeline, no path filtering. Manual run from the Actions tab. |
| `ci.yaml` | `workflow_call` from `release.yaml` | Full pipeline, no path filtering. The release gate. |
| `release.yaml` | `workflow_dispatch` with service booleans | Builds and pushes only the ticked services. |

`ready_for_review` is deliberately absent from `ci.yaml`. A draft already raises `opened` and `synchronize`, so the head commit is tested before it is marked ready.

Concurrency is one in-flight run per pull request. A new push cancels the superseded run. Non-PR triggers each get their own group and are never cancelled, so a release waiting on tests is never killed by a push to a pull request.

## Gates in ci.yaml

| Job | Runs when | What it checks |
|---|---|---|
| `detect-changes` | Always | Maps changed files to outputs (`booking`, `offer`, `message`, `notify`, `gateway`, `common`, `shared`, `contracts`, `frontend`, `workflows`) plus `backend` (any backend file) and `docs-only` (none of backend, contracts, frontend, workflows). Non-PR events report `backend=true`, `docs-only=false`. |
| `verify-changelog` | `pull_request` only | Every touched module has a new top `## [x.y.z]` entry with content. Compares HEAD against the base branch. |
| `dockerfile-parity` | Always | Runs `scripts/check-dockerfile-parity.sh`. Fails when a Gradle module is missing from a Dockerfile `COPY` list. Takes seconds. |
| `build-and-test` | `backend == true` | Builds and tests only the affected modules (`:<service>:build`). A `sky-common`, `buildSrc`, catalogue, or wrapper change builds everything. Skipped entirely on a docs-only pull request. |
| `e2e-collection` | After `build-and-test` | Builds the five service images from their Dockerfiles, starts the compose stack, runs the Bruno collection. Skipped when `build-and-test` is skipped. |
| `owasp-dependency-check` | `pull_request` and `backend == true` | Runs `dependencyCheckAggregate`. Skipped on docs-only. |
| `bundle-contract` | Non-PR, or `contracts == true` | Re-bundles `contracts/sky-api/src/openapi.yaml` and diffs against the committed bundle. Skipped on docs-only. |
| `client-freshness` | Non-PR, or `contracts`/`frontend` changed | Regenerates the Angular client and diffs against the committed one. Skipped on docs-only. |

## Docs-only fast path

A pull request that touches only markdown, `docs/`, `openspec/`, or e2e runbooks sets `backend=false` and `docs-only=true`. The only jobs that run are `detect-changes`, `verify-changelog` (exits 0 with nothing to verify), and `dockerfile-parity` (shell, seconds). No JDK, no Gradle, no Node, no containers. The run finishes in under two minutes.

## Secrets

| Secret | Used by | Purpose |
|---|---|---|
| `DOCKERHUB_USERNAME` | `release.yaml` | Docker Hub login for image push. |
| `DOCKERHUB_TOKEN` | `release.yaml` | Docker Hub token for image push. |
| `GITHUB_TOKEN` | `release.yaml`, `ci.yaml` artifact upload | Automatic token. Push to GHCR and upload test reports. No setup needed. |

No long-lived cloud key lives in CI. Cloud authentication, where needed, uses OIDC rather than a stored secret. Every job declares minimum `permissions`, never `write-all`.

## Test reports

`build-and-test` uploads the Gradle test and coverage reports as an artifact only on failure:

- `apps/backend/**/build/reports/tests/**`
- `apps/backend/**/build/reports/jacoco/**`
- `apps/backend/**/build/test-results/**`

Artifact name is `test-reports-<run_id>-<run_attempt>`, retained seven days. A green run uploads nothing.

## Parity script

`scripts/check-dockerfile-parity.sh` is the executable form of one rule: every service Dockerfile must `COPY` its own module, every `project(":...")` dependency named in its `build.gradle.kts` (today `sky-common` everywhere), and the shared build inputs (`settings.gradle.kts`, `build.gradle.kts`, `gradle`, `buildSrc`), and must build `:<service>:bootJar`. It also checks that every service with a Dockerfile is listed in `settings.gradle.kts`. Run it locally before pushing a Dockerfile change:

```bash
bash scripts/check-dockerfile-parity.sh
```
