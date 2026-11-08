# Design

## Context

Sky backend is a multi-module Gradle project. The changelog gate already exists and requires bumping changelogs for build-root changes. OWASP dependency-check Gradle plugin needs to be wired via version catalog and applied at root build level. CI needs a new job to run the check on PRs.

## Goals / Non-Goals

**Goals:**
- Add OWASP dependency-check via libs.versions.toml and apply in apps/backend/build.gradle.kts
- Add CI job that runs owaspDependencyCheck on PRs with failure threshold CVSS >= 7.0
- Follow existing repo conventions (version catalog usage, changelog bump requirements)

**Non-Goals:**
- Changing other phases of cross-repo gap program
- Modifying existing CI structure beyond adding the new job
- Committing vulnerable test dependencies

## Decisions

**Decision 1: Use official OWASP dependency-check Gradle plugin**
- Rationale: Standard, integrates with Gradle, allows setting CVSS threshold via configuration
- Alternatives considered: Other scanners (out of scope per owner constraints)

**Decision 2: Apply at root backend build (apps/backend/build.gradle.kts)**
- Rationale: Covers all submodules in the composite build
- Alternatives considered: Per-module (less consistent)

**Decision 3: CI job runs on PRs only**
- Rationale: Matches verify-changelog pattern; avoids running on release dispatches
- Alternatives considered: Run on all pushes (slower, NVD cache considerations)

## Risks / Trade-offs

- [Risk] First run downloads NVD data (slow) → Mitigation: Acceptable for CI/local as noted in requirements
- [Risk] False positives → Mitigation: Can be tuned later if needed; threshold 7.0 is reasonable
