## Purpose

Pins the supported Angular major, language level and build tooling for the `apps/frontend/sky-view` application, so the storefront builds, serves and lints on the current Node runtime instead of on end of life Angular 16 tooling.

## ADDED Requirements

### Requirement: Frontend runs on a supported Angular major
`apps/frontend/sky-view` MUST build and serve on Angular 22.0.x, with matching `@angular/cli`, `@angular-devkit/build-angular`, `@angular/cdk` and `@angular/material` versions. TypeScript MUST be >=6.0.0 <6.1.0, `rxjs` MUST be ^6.5.3 or ^7.4.0, `zone.js` MUST be ~0.15.0 or ~0.16.0, Node MUST be ^22.22.3 or ^24.15.0 or >=26.0.0, and `@types/node` MUST be the version Angular 22.0.x requires. No dependency pinned to the Angular 16 era may remain in `apps/frontend/sky-view/package.json`.

#### Scenario: Clean install and production build
- **WHEN** a developer runs `npm ci` followed by the production build inside `apps/frontend/sky-view` on the pinned Node major
- **THEN** the install completes with no peer dependency error and the build emits the application bundle with no error

#### Scenario: Dev server starts
- **WHEN** a developer runs the `localDev` serve configuration
- **THEN** the dev server starts and serves the application with hot reload and no build error

### Requirement: Dead frontend tooling is replaced
`apps/frontend/sky-view` MUST NOT depend on `tslint`, `codelyzer` or `protractor`. Linting MUST run on `eslint` with Angular support, `apps/frontend/sky-view/angular.json` MUST NOT reference the `browser`, `tslint` or `protractor` builders, and the lockfile MUST be regenerated from the upgraded manifest.

#### Scenario: Lint run
- **WHEN** a developer runs the lint script inside `apps/frontend/sky-view`
- **THEN** the run completes on the eslint toolchain and reports no configuration error about a missing builder or plugin

#### Scenario: No dead builder reference
- **WHEN** any agent or human searches `apps/frontend/sky-view` for `tslint`, `codelyzer` or `protractor`
- **THEN** zero configuration references match, excluding historical notes in change archives

### Requirement: Frontend image builds from a pinned toolchain
`apps/frontend/sky-view/Dockerfile` MUST pin a concrete Node major image instead of `node:latest` and MUST invoke the supported production build configuration. The runtime stage MUST remain a static file server image with no compiler.

#### Scenario: Image build
- **WHEN** the frontend image is built from the repository root with `apps/frontend/sky-view` as its context input
- **THEN** the build completes reproducibly and the resulting image serves the built application
