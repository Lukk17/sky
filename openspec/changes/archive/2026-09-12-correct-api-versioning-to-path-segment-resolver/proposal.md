## Why

Two of the three requirements in `api-versioning` describe a mechanism nobody ever built, so the capability cannot be
used to judge whether a change to the versioning scheme regressed anything. The previous pass over this capability,
archived as `2026-09-12-correct-punctuation-in-plural-paths-requirement`, found both and deliberately left them alone
because restating them is a decision about the API contract rather than a repair of a stale fact. The owner has now
taken that decision: restate both toward what exists, because a requirement describing a mechanism nobody built is a
leftover rather than a contract.

The requirement `Header-based API versioning via Spring Framework 7 native mechanism` names the `X-API-Version` request
header as the primary resolver. The string appears in no Java file, no YAML file, no Bruno request and no OpenAPI
contract, and `git log --all -S` finds no commit that ever added it to one. What commit 77a6f44 built instead is a
path-segment resolver: `sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java` calls
`usePathSegment(1, ...)` with a predicate that accepts the second path segment only when it reads `v` followed by a
digit, then `setDefaultVersion("1")` and `addSupportedVersions("1")`. That commit message states the reason, which was
to keep the public URLs at `/api/v1` so the frontend was unaffected.

The requirement `Internal endpoints are versioned and namespaced` says service-to-service endpoints live under
`/api/internal/...`. They did. `OfferInternalController` existed and commit c459904 deleted it, moving the owner lookup
to `GET /api/v1/offers/{offerId}/owner`, and that commit message states the reason, which was to model the lookup as a
sub-resource so the version sits right after `/api` like every other endpoint and the URL carries no access-concern
naming. The string `api/internal` now appears in no source or configuration file.

## What Changes

- Replace the versioning-mechanism requirement with one describing the path-segment resolver that ships, including why
  the header was considered and not chosen, so a future reader sees a decision rather than an omission.
- Replace the internal-endpoint requirement with the sub-resource rule it became: a service-to-service route carries
  the same `/api/v1` prefix as a public one, and what keeps it from being a public surface is the authorization rule in
  front of it rather than a separate URL namespace.
- Record both as a removal with a Reason and a Migration followed by an addition, rather than as a modification,
  because each one recorded a decision that was later reversed rather than a rule that needed refining. The shape and
  the constraint that forces it are in design.md.
- Correct, in the same two blocks, four stale details that cannot be carried over unchanged:
  - The example paths `GET /api/offers` and `/api/internal/owner/offers/{id}`. No service serves either. Every
    collection path sits under `/api/v1`, because `@RequestMapping(path = "${sky.apiPrefix}", version = "1")` on all
    three controllers resolves `sky.apiPrefix` to `/api/v1`.
  - The named exception. The scenario credits Spring Framework 7's `NotAcceptableApiVersionException` for rejecting an
    unsupported version. `DefaultApiVersionStrategy.validateVersion` throws `InvalidApiVersionException`, which carries
    `HttpStatus.BAD_REQUEST`, and the committed test asserts exactly that.
    `NotAcceptableApiVersionException` is a subclass of it, thrown from `VersionRequestCondition` when a mapping
    mismatches on version alone, so the original sentence names the wrong half of the pair.
  - The claim that the internal route `is not exposed at the root path`. It is reachable at the same public prefix as
    everything else and is gated by `.authenticated()` in `sky-offer`'s `SecurityConfig`, ordered ahead of the
    `permitAll` on public offers.
  - The two scenario lines that join two independent clauses with a semicolon, at `api-versioning` lines 13 and 31.
    These are the last two such lines in any merged specification, so this change closes the punctuation pass that
    `2026-09-12-correct-punctuation-in-plural-paths-requirement` had to leave open.

Nothing here changes code, configuration, a Bruno request or an OpenAPI contract. Both requirements are being aligned
to behaviour that already ships and is already covered by tests, so no endpoint and no test moves.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `api-versioning`: the resolver the versioning requirement names, and the shape the internal-endpoint requirement
  requires of a service-to-service route.

## Impact

- Affected file: `openspec/specs/api-versioning/spec.md`, rewritten at archive time from the delta in this change. Two
  of its three requirements are touched. The third, the plural-noun rule corrected earlier today, is not.
- The `2026-06-25-api-cleanup` archived record carries two task lines that were never true, 5.1 claiming every
  controller test sends the version header and 6.1 claiming the Bruno collection carries it as a collection-level
  default. Neither is rewritten, and the evidence against both is recorded in this change's tasks instead. The
  reasoning is in design.md.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected, so there is nothing to
  build and nothing to deploy.
- Risk: low. Both requirements move toward a resolver and a route that are committed, exercised by
  `sky-common/src/test/java/com/lukk/sky/common/web/ApiVersioningAutoConfigurationTest.java`, and unchanged by this
  work.
