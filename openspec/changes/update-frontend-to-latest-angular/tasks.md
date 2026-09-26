## 1. Baseline

- [x] 1.1 Record the exact upgrade path by reading the Angular update guide for 16 to latest, and verify the target major plus the required Node and TypeScript versions are written down in the change
- [x] 1.2 Decide the fate of `@nrwl/angular` by searching `apps/frontend` for `nx` usage, and verify the decision (keep with version or remove) is recorded before any upgrade runs
- [x] 1.3 Check the support matrix of `angular-calendar`, `@auth0/auth0-angular`, `bootstrap` and `@stomp/stompjs` against the target major, and verify replacements are named for anything without a compatible release

## 2. Stepwise upgrade

- [ ] 2.1 Run `ng update` one major at a time from 16, resolving peer conflicts at each step, and verify `ng version` reports the new major after each step
- [x] 2.2 Fix breaking API changes in `apps/frontend/sky-view/src` after each major, and verify the application compiles with no error before moving to the next major
- [x] 2.3 Regenerate `apps/frontend/sky-view/package-lock.json` at the final major, and verify `npm ci` completes with no peer dependency error

## 3. Tooling replacement

- [x] 3.1 Migrate `apps/frontend/sky-view/angular.json` off the `browser`, `tslint` and `protractor` builders, and verify no dead builder name remains in the file
- [x] 3.2 Replace `tslint` plus `codelyzer` with `eslint` plus angular-eslint configuration, and verify the lint script runs green
- [x] 3.3 Replace the `protractor` e2e setup with the supported runner, and verify the e2e command starts and executes its specs
- [x] 3.4 Remove `jquery` script tags or migrate Bootstrap integration to the supported path for the new builder, and verify the production build emits the bundle with no error

## 4. Container and verification

- [x] 4.1 Pin `apps/frontend/sky-view/Dockerfile` to a concrete Node major and use the named production build configuration, and verify the image builds from the repository root
- [x] 4.2 Run the production build, lint and e2e on the final toolchain, and verify all three pass on the pinned Node major
- [ ] 4.3 Smoke test every route (offers, search, owned offers, bookings, messages, auth) against the compose stack, and verify no route regressed visually or functionally
