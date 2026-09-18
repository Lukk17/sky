## Why

Three requirement corrections archived earlier this evening left two merged specifications whose Purpose contradicts
their own requirements, so each file now opens by asserting something its requirement list denies.

`spring-boot-hygiene` opens with "no credential committed as a default". Its credential requirement now carries one
named exemption for the `local` profile, and three local-profile files use it, so the Purpose states a rule the
capability no longer holds. The same sentence says the cross-origin policy "does not answer every origin", which is
still true but reads as a description of the old production-only rule rather than of the wildcard prohibition that
replaced it.

`architecture` opens with "holds every service to the hexagonal layout, with the dependency direction and the package
structure checked by a test rather than by review". Its layout requirement now names `sky-gateway` and `sky-common` as
outside the rule, so "every service" is wrong, and it names exactly three locations that an ArchUnit test asserts, so
"the package structure checked by a test" claims more enforcement than exists. The honest version says which parts a
build checks.

A Purpose is the only part of a specification that says what the capability is for, so a Purpose that contradicts the
requirements below it is worse than no Purpose at all: a reader who trusts it stops reading.

## What Changes

- Rewrite the Purpose of `openspec/specs/spring-boot-hygiene/spec.md` so it names the local-profile exemption and the
  wildcard prohibition, which are the rules the capability now carries.
- Rewrite the Purpose of `openspec/specs/architecture/spec.md` so it names the four hexagonal services rather than every
  service, and says that each service's own ArchUnit test asserts the parts a build can check rather than implying the
  whole layout is checked.
- Leave the Purpose of `openspec/specs/api-versioning/spec.md` alone. Its requirements changed this evening and its
  Purpose still describes them correctly, because it speaks about endpoints being versioned and consistently named and
  says nothing about how the version is carried.

No requirement text changes, so no behaviour changes and nothing is built, tested or deployed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None at requirement level, which is why this change sets `skip_specs: true` in its `.openspec.yaml`.

A Purpose section is not a requirement and no delta verb addresses it. `openspec instructions specs` is explicit: a
delta for an existing capability must not carry a `## Purpose` section, because the merged specification already has
one and the delta's copy is ignored at archive time, and the documented route for changing an existing Purpose is to
edit the merged specification directly. So this change carries no delta specification, and the rewrite is a task rather
than a delta. The precedent is `2026-09-12-replace-spec-purpose-placeholders`, which replaced all eighteen Purpose
placeholders the same way.

## Impact

- Affected files: the `## Purpose` line of `openspec/specs/spring-boot-hygiene/spec.md` and of
  `openspec/specs/architecture/spec.md`. No other line in either file is touched.
- Kept separate from the three requirement corrections rather than folded into them. Each of those is reviewable as a
  requirement diff, and a Purpose rewrite cannot travel in a delta at all, so folding it in would have meant one change
  writing to a merged file through two different routes.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low, and bounded by the fact that a Purpose is descriptive. It carries no MUST and nothing validates against its
  content, so a poorly worded one misleads a reader without breaking a build. The guard is that each rewrite is checked
  against the requirement list directly below it.
