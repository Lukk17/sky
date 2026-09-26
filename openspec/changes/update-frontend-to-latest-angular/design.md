## Context

`apps/frontend` is an Angular 16.1 application using the removed `browser` builder, `tslint` plus `codelyzer` for lint and `protractor` for e2e. The machine runs Node 26, outside the Angular 16 support matrix. See `proposal.md` for motivation and `specs/frontend-toolchain/spec.md` for requirements.

## Goals / Non-Goals

Goals: upgrade one major at a time with the Angular update guide until the latest stable major, keeping the app compiling at each step. Replace dead builders and linters with their supported equivalents. Keep every existing route and component working with no visual change.

Non-Goals: no rewrite to standalone components unless a migration step requires it. No endpoint or auth flow change, that belongs to the `frontend-backend-alignment` change. No backend, chart or contract change.

## Decisions

- Step through majors with `ng update` rather than jumping 16 to latest in one edit, because peer ranges of `angular-calendar`, `@auth0/auth0-angular` and `@nrwl/angular` only resolve stepwise. Alternative of a fresh scaffold plus file copy was rejected because it loses the existing lint and serve configurations silently.
- Drop `@nrwl/angular` if no workspace feature uses it, rather than carrying an Nx major alongside plain Angular CLI. Decide by searching for `nx` references in `apps/frontend` before upgrading.
- Adopt `application` builder plus `angular-eslint` plus a supported e2e runner, the paths the Angular team documents, rather than community forks of the dead builders.
- Pin the Dockerfile to the Node major that matches local development, and build with the named production configuration instead of the removed `--prod` flag.

## Risks / Trade-offs

- Risk: `angular-calendar` 0.31 has no release for the latest Angular major, so the calendar view may need a replacement library. Mitigation is to check its support matrix at the first major step and timebox the search before choosing an alternative.
- Risk: `jquery` plus `bootstrap.min.js` script tags in `angular.json` break under the `application` builder. Mitigation is to move to the Bootstrap package imports or an `ng-bootstrap` version matching the target major.
- Risk: `@auth0/auth0-angular` 2.x may not support the latest Angular, while the backend expects Keycloak-issued JWTs. Mitigation is to upgrade the wrapper first, and the alignment change later decides whether the wrapper stays at all.

## Migration Plan

Upgrade on a branch, one major per commit, running build plus lint plus the manual smoke route after each. Rollback is the previous commit, since every step keeps the app serving. Regenerate the lockfile only at the final major to avoid intermediate churn in review.

## Open Questions

None. The target major is read from the Angular releases at implementation time.

## Accepted deviation: stepwise history (tasks 2.1, 4.3)

The tree moved 16 to 22 directly instead of one major per commit, so task 2.1
per-step `ng version` evidence was not recorded. Outcome equivalence holds on
the final tree: `apps/frontend/sky-view/package.json` pins `@angular/*` to
`~22.1.7` with `@angular/cli` and `@angular-devkit/build-angular` at `~22.1.8`,
and tasks 2.2, 2.3 plus 3.1 to 4.2 are checked with build, lint, unit, and e2e
green on that toolchain. Task 4.3 (compose smoke) stays open and is not claimed.
History is not redone: replaying each major only to regenerate intermediate
`ng version` lines would add no signal about the shipped tree.
