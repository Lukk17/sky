# Proposal

## Why

Sky currently lacks automated dependency vulnerability scanning in CI. Pharmacy (sibling repo) applies OWASP dependency-check to surface CVE findings early in pull requests. Adding OWASP dependency-check to the backend build and CI will catch vulnerable dependencies before merge with a controlled failure threshold.

## What Changes

- Add the OWASP dependency-check Gradle plugin version to pps/backend/gradle/libs.versions.toml
- Apply the OWASP dependency-check plugin in pps/backend/build.gradle.kts
- Add a CI job that runs OWASP dependency-check on every PR and fails on CVSS >= 7.0
- Bump affected module CHANGELOG.md files as required by the changelog gate for build-root changes

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None.

## Impact

- Backend build (Gradle): adds dependency-check configuration via version catalog and root build
- CI workflow: adds new job gated to PRs
- All backend modules affected by shared build changes require changelog bumps per CI gate
