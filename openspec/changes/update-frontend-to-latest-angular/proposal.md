## Why

`apps/frontend/sky-view/` is pinned to Angular 16.1.x, TypeScript 5.1 and Node 16 or 18 era tooling, while the local runtime is Node 26. Theposition is end of life, the lint and e2e builders (`tslint`, `codelyzer`, `protractor`) are dead upstream, and the app cannot be built or served reliably on the current toolchain.

## What Changes

- Upgrade Angular from 16.1.x to 22.0.x, including `@angular/cdk`, `@angular/material`, `@angular/cli` and `@angular-devkit/build-angular`.
- Upgrade TypeScript to >=6.0.0 <6.1.0, `rxjs` to ^6.5.3 or ^7.4.0, `zone.js` to ~0.15.0 or ~0.16.0, Node to ^22.22.3 or ^24.15.0 or >=26.0.0, and `@types/node` to the versions Angular 22.0.x requires.
- Replace dead tooling: `tslint` plus `codelyzer` move to `eslint` with angular-eslint, `protractor` e2e moves to a supported runner.
- Migrate `apps/frontend/sky-view/angular.json` off removed builders (`browser`, `tslint`, `protractor`) onto their supported replacements.
- Refresh third-party UI dependencies (`angular-calendar`, `bootstrap`, `@auth0/auth0-angular`, `@stomp/stompjs`, `ngx-cookie-service`, `date-fns`) to versions compatible with the new Angular major.
- Regenerate `apps/frontend/sky-view/package-lock.json` from the upgraded manifest.
- Fix `apps/frontend/sky-view/Dockerfile` to pin a concrete Node major and use the supported production build invocation.

## Capabilities

### New Capabilities

- `frontend-toolchain`: pins Angular 22.0.x, TypeScript >=6.0.0 <6.1.0, Node ^22.22.3 or ^24.15.0 or >=26.0.0, `zone.js` ~0.15.0 or ~0.16.0, `rxjs` ^6.5.3 or ^7.4.0, and build tooling for `apps/frontend`, so the app builds, serves and lints on the current runtime.

### Modified Capabilities

None. Existing specs (`framework-version`, `docker-build`) are backend-only and their requirements do not change.

## Impact

- Affected code: everything under `apps/frontend/sky-view/`, principally `package.json`, `package-lock.json`, `angular.json`, sources using removed APIs, and `Dockerfile`.
- Systems: local `ng serve` flow, frontend container image build, any CI job covering the frontend.
- No backend service, chart, contract or migration is touched by this change.
