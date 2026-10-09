# phase-4a-contract-skeleton

Scaffold a bundled OpenAPI contract for the sky API under `contracts/sky-api/`, with a redocly lint config and a CI job that re-bundles and diffs against the committed bundle. The published documents under `docs/api/openapi/` remain the generated service documents; this change adds a separate, versioned contract bundle that the CI keeps in sync with its source.