## Why

The three documents under `docs/api/openapi/` are produced from the running services by the `sky.openapi-conventions` Gradle plugin, and they are the documents the repository keeps. They are large, they are generated from a forked boot under a `local,openapi` profile pair, and they are regenerated on every build. That is the right shape for a service document.

This change does not touch them. It adds a second artefact beside them: a small, versioned, bundled OpenAPI contract under `contracts/sky-api/` whose purpose is to be the thing a CI job can re-bundle and diff. The three service documents answer the question "what does this service publish"; the contract bundle answers the question "is the published contract still the one we committed".

The gap is that nothing currently diffs the published documents against anything. A contributor can edit a controller, regenerate the three documents, and commit a changed contract with no gate noticing that the change happened. The `api-contract` capability merged on 2026-09-17 states that a published document is produced from the service, is never edited directly, and that the kept copy equals what the current source produces. What it does not state is that there is a machine check on the last clause, and there is not one today: `build` regenerates the documents, and nothing compares the result against the previous commit.

## What Changes

- Scaffold `openspec/changes/phase-4a-contract-skeleton/` mirroring the archived layout (proposal, design, tasks, README, `.openspec.yaml`).
- Create `contracts/sky-api/src/openapi.yaml` as a skeleton OpenAPI 3.1 document carrying `info`, `servers`, and a single offer path stub (`/api/v1/offers`), written by hand as the source of truth for the contract bundle.
- Create `contracts/sky-api/redocly.yaml` mirroring the sibling lint pattern: `extends: recommended` with the minimal rule relaxations the skeleton needs to lint clean.
- Bundle the source into `contracts/sky-api/openapi.yaml` with `npx redocly bundle` and commit the bundle.
- Comply with the `verify-changelog` CI job: the changed-file switch in that job has no case for `contracts/`, so no module is marked affected and no changelog bump is demanded. That is recorded here rather than assumed.
- Add a CI job that re-bundles `contracts/sky-api/src/openapi.yaml` and diffs the output against the committed `contracts/sky-api/openapi.yaml`, failing on drift.

## Capabilities

### New Capabilities

None. No specification changes; this is build and CI wiring plus a versioned artefact.

### Modified Capabilities

None.

## Impact

- Affected files: `contracts/sky-api/src/openapi.yaml`, `contracts/sky-api/redocly.yaml`, `contracts/sky-api/openapi.yaml`, `.github/workflows/ci.yaml`, and the six `CHANGELOG.md` files under `apps/backend/` (bumps only if the gate requires them, which it does not for a `contracts/` change).
- The three service documents under `docs/api/openapi/` are not touched and are not bundled by this change.
- Risk: low. The bundle is from a hand-written skeleton with one path stub, and the new CI job is a pure diff against a committed file.