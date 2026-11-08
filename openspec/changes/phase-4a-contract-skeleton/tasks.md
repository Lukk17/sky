## 1. Scaffold the change

- [x] 1.1 Create `openspec/changes/phase-4a-contract-skeleton/` mirroring the archived layout: `proposal.md`, `design.md`, `tasks.md`, `.openspec.yaml`, `README.md`.
- [x] 1.2 Confirm the archived layout by reading `openspec/changes/archive/2026-09-17-state-the-generated-api-contract-and-correct-what-it-falsified/` and `openspec/changes/archive/2026-09-12-correct-canonical-package-layout/`, and mirror the five files plus the `specs/` directory shape.

## 2. Read the published document head and the contracts layout

- [x] 2.1 Read the head of `docs/api/openapi/sky-offer.openapi.yaml` and confirm it carries `openapi: 3.1.0`, an `info` block with `title: sky-offer` and `version: v1`, a `servers` block naming `http://localhost:5777` and `http://localhost:5552`, and paths under `/api/v1/offers`.
- [x] 2.2 Confirm no `contracts/` directory exists anywhere in the repository, so the layout is new rather than mirroring an existing one.

## 3. Write the contract skeleton and lint config

- [x] 3.1 Create `contracts/sky-api/src/openapi.yaml` carrying `openapi: 3.1.0`, an `info` block (title `sky-api`, version `v1`, description naming the bundle's purpose), the two `servers` from the published document, and one path stub at `/api/v1/offers` with a `get` operation whose operation id is `getContractStub`.
- [x] 3.2 Create `contracts/sky-api/redocly.yaml` with `extends: recommended` and the minimal rule relaxations the skeleton needs to lint clean.
- [x] 3.3 Confirm the skeleton lints clean under `npx redocly lint`.

## 4. Bundle and commit the bundle

- [x] 4.1 Run `npx redocly bundle contracts/sky-api/src/openapi.yaml -o contracts/sky-api/openapi.yaml` and confirm it produces a single-file bundle.
- [x] 4.2 Confirm the bundle is deterministic by bundling twice and diffing the outputs.

## 5. Comply with the verify-changelog gate

- [x] 5.1 Read `.github/workflows/ci.yaml` `verify-changelog` job and confirm its changed-file switch has no case for `contracts/`, so no module is marked affected and no changelog bump is demanded.
- [x] 5.2 Record that finding in proposal.md so it is not silently assumed.

## 6. Add the drift-diff CI job

- [x] 6.1 Add a job to `.github/workflows/ci.yaml` that re-bundles `contracts/sky-api/src/openapi.yaml` to a temp file and diffs it against the committed `contracts/sky-api/openapi.yaml`, failing on any difference.
- [x] 6.2 Confirm the job runs on the same triggers as the rest of the workflow and does not need the Gradle build.

## 7. Report

- [x] 7.1 Report files created, bundle output and gate compliance. Do NOT commit. Do NOT touch other phases or the session 500 diagnosis.