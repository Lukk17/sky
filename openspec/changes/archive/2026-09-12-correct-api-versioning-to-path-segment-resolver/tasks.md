## 1. Establish the evidence

- [x] 1.1 Confirm the resolver is a path segment by reading
  `sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java` and verifying it calls
  `usePathSegment(1, ...)` with a predicate accepting the second segment only when it is `v` followed by a digit, then
  `setDefaultVersion("1")` and `addSupportedVersions("1")`
- [x] 1.2 Confirm the resolver's observable behaviour from
  `sky-common/src/test/java/com/lukk/sky/common/web/ApiVersioningAutoConfigurationTest.java`, verifying that
  `/api/v1/offers` resolves, that `/api/offers/42` and `/api/version/offers` resolve to no version, that the default is
  version 1, and that version 2 is rejected
- [x] 1.3 Confirm the declared version by grepping the three REST services for `@RequestMapping` and verifying each
  controller carries `path = "${sky.apiPrefix}", version = "1"`, and that `sky.apiPrefix` is `/api/v1` in each
  service's `application.yaml`
- [x] 1.4 Confirm which framework exception rejects an unsupported version, and its status, by running `javap` over
  `DefaultApiVersionStrategy`, `InvalidApiVersionException` and `NotAcceptableApiVersionException` from the
  `spring-web` jar on the current version line, and verifying that `validateVersion` constructs
  `InvalidApiVersionException`, that its status is `BAD_REQUEST`, and that `NotAcceptableApiVersionException` is a
  subclass thrown from `VersionRequestCondition` instead
- [x] 1.5 Disprove the claim in archived task `2026-06-25-api-cleanup` 5.1 that every controller test sends
  `X-API-Version: 1`, by grepping the six module source trees for the string and verifying zero matches, and by running
  `git log --all -S"X-API-Version"` over `*.java`, `*.yaml`, `*.yml`, `*.bru` and `*.json` and verifying no commit ever
  added it
- [x] 1.6 Disprove the claim in archived task `2026-06-25-api-cleanup` 6.1 that the Bruno collection carries the header
  as a collection-level default, by reading `docs/api/request/opencollection.yml` and verifying it holds no headers
  block at all, and by reading `docs/api/request/offer/get-offer-owner.yml` and verifying its only headers are
  `Authorization` and `Accept`
- [x] 1.7 Confirm the internal namespace is gone by grepping the six module source trees plus `config`, `docs`, `e2e`,
  `buildSrc` and `gradle` for `api/internal` and verifying zero matches, and by reading commit c459904 and verifying it
  deleted `OfferInternalController` and moved the handler into `OfferApiController`
- [x] 1.8 Confirm the real owner route and its guard by reading
  `sky-offer/src/main/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiController.java` for
  `@GetMapping("/offers/{offerId}/owner")` and
  `sky-offer/src/main/java/com/lukk/sky/offer/config/SecurityConfig.java` for the `.authenticated()` matcher on
  `/api/v1/offers/*/owner` ordered ahead of the `permitAll` on `/api/v1/offers/**`
- [x] 1.9 Confirm the token is forwarded rather than minted by reading
  `sky-booking/src/main/java/com/lukk/sky/booking/adapters/outbound/rest/OfferServiceCaller.java` and verifying it
  copies the caller's `JwtAuthenticationToken` value into the outbound `Authorization` header, and
  `sky-booking/src/main/java/com/lukk/sky/booking/adapters/outbound/rest/OfferRestClient.java` plus
  `offerOwnerEndpoint` in `sky-booking/src/main/resources/application.yaml` for the path it builds
- [x] 1.10 Confirm both scenarios of the second requirement are covered by committed tests, by verifying
  `getOfferOwner_whenNoJwt_thenReturn401` and `getAllOffers_whenOffersExist_thenReturnPagedOffers` in
  `sky-offer/src/test/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiControllerTest.java`, that the second sends
  no token and asserts 2xx, and that both go through the production filter chain because `TestSecurityConfig` replaces
  only the `JwtDecoder`
- [x] 1.11 Read commits 77a6f44 and c459904 in full and record the stated reason from each, so the reasoning written
  into the requirements is the author's rather than a reconstruction

## 2. Write the delta specification

- [x] 2.1 Establish the delta shape before writing it, by drafting the correction as `## RENAMED` plus
  `## MODIFIED`, running the validator, and recording that it refuses a MODIFIED block which drops a scenario name the
  merged requirement has, confirmed by reading `findMissingCurrentScenarios` in the installed CLI
- [x] 2.2 Write `openspec/changes/correct-api-versioning-to-path-segment-resolver/specs/api-versioning/spec.md` with a
  `## REMOVED Requirements` block carrying both old requirements, each with a `**Reason**` naming the commit that
  reversed the decision and a `**Migration**` naming the replacement requirement and what it keeps, and verify each
  removed header matches the merged file character for character
- [x] 2.3 Write both replacement requirements under `## ADDED Requirements`, each with at least one scenario, and
  verify every rule carried by the removed requirement appears in its replacement or is named in the Migration line as
  deliberately dropped
- [x] 2.4 Verify the delta holds no em dash, no en dash, no semicolon, and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers the delta format requires, using a byte-exact matcher first validated against a fixture containing
  an em dash, an en dash, an arrow, a bullet and a box-drawing character
- [x] 2.5 Verify the delta respects the merged file's wrap width, which is one physical line per requirement paragraph
  and one per scenario line
- [x] 2.6 Run `openspec validate correct-api-versioning-to-path-segment-resolver --strict` and verify it reports no
  error

## 3. Archive and sync

- [x] 3.1 Archive the change, letting the CLI perform the merged-file rewrite, and verify
  `openspec/specs/api-versioning/spec.md` no longer contains `X-API-Version`, and that the only remaining occurrence of
  `api/internal` is the one inside the replacement requirement that forbids it
- [x] 3.2 Verify the merged file now carries both replacement requirement headers and no longer carries either removed
  header, and that its requirement count is unchanged at three
- [x] 3.3 Verify the merged file holds no semicolon, and record what remains elsewhere in `openspec/specs/`
- [x] 3.4 Verify the plural-noun requirement is unchanged by this archive, by diffing the merged file and confirming the
  block `REST resource paths use plural nouns consistently` and its scenario appear in the diff as context rather than
  as changed lines
- [x] 3.5 Verify the change directory moved to
  `openspec/changes/archive/2026-09-12-correct-api-versioning-to-path-segment-resolver` and that `openspec list`
  reports it as no longer active
- [x] 3.6 Verify `git status` shows no file outside `openspec/changes` and `openspec/specs`

## 4. Notes from the run

- The corrections to the two archived task lines 5.1 and 6.1 of `2026-06-25-api-cleanup` live here rather than in that
  directory. Tasks 1.5 and 1.6 above carry the evidence. The archived record is left byte-identical, for the reasons in
  design.md.
- Task 3.1 was written expecting `api/internal` to disappear from the merged file entirely. It does not, and should not:
  the replacement requirement names the namespace in order to forbid it. The task was corrected rather than passed on a
  reading that would have been wrong either way.
- A removal plus an addition appends the new requirements at the end, so the merged file's requirement order changed:
  the plural-noun rule is now first and the two replacements follow it. The requirement count is unchanged at three and
  the plural-noun block is byte-identical to what `2026-09-12-correct-punctuation-in-plural-paths-requirement` left,
  confirmed by diffing the block out of `git show HEAD:` against the rewritten file. Order carries no meaning in this
  format, so the reordering is recorded rather than corrected.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. The
  section is longer because it carries the evidence for two requirements. Left as written.
- Task 3.3 holds for this capability and one semicolon remains in `openspec/specs/`, in the first requirement of
  `architecture`. Those three semicolons are serial separators inside a list introduced by a colon rather than two
  independent clauses joined, so the formatting rule does not reach them. That requirement has its own factual defects,
  reported to the owner and not corrected here.
- Task 3.6 found the working tree carrying another agent's in-flight Kafka producer work in `sky-booking`, `sky-offer`
  and `sky-notify`. None of it is from this change, and re-reading the cross-origin and credential lines of the three
  `application.yaml` files after those edits confirmed none of the lines this pass relies on moved.
