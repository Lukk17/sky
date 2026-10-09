# Tasks

## 1. Gradle plugin wiring

- [ ] 1.1 Add OWASP dependency-check plugin version to apps/backend/gradle/libs.versions.toml and verify version is referenced correctly
- [ ] 1.2 Apply OWASP dependency-check plugin in apps/backend/build.gradle.kts with failOnCVSS = 7.0 and verify build loads
- [ ] 1.3 Bump changelogs for affected modules (all 6 modules due to build-root change) and verify verify-changelog will pass

## 2. CI integration

- [ ] 2.1 Add owasp-dependency-check job to .github/workflows/ci.yaml that runs on PRs, sets up Gradle, and executes owaspDependencyCheck with fail threshold; verify job syntax is valid

## 3. Local verification

- [ ] 3.1 Run owaspDependencyCheck locally via backend wrapper and confirm it executes (first run may download NVD data); document output summary
- [ ] 3.2 Verify that introducing a deliberately vulnerable test dependency would cause the check to fail (simulate/test understanding without committing)
