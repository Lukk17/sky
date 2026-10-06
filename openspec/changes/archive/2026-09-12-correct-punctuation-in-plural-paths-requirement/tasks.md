## 1. Establish what can and cannot be corrected here

- [x] 1.1 Count the semicolon lines in `openspec/specs/api-versioning/spec.md` and verify the count is three
- [x] 1.2 Read each line and record which requirement block it belongs to
- [x] 1.3 Verify the header resolver the first requirement mandates against the code, by searching every source,
  configuration and request file in the repository for the header name
- [x] 1.4 Establish whether the header resolver ever existed, using a content search over the whole history rather than
  inferring it from the current tree
- [x] 1.5 Record what the versioning mechanism actually is, from the auto-configuration in `sky-common`, and find the
  commit that chose it and the reason its message gives
- [x] 1.6 Verify the internal-endpoint namespace the third requirement mandates, by searching for `api/internal`
- [x] 1.7 Establish whether it ever existed, and if it was removed, find the commit and the reason its message gives
- [x] 1.8 Decide the handling for both: report to the owner rather than restating either, and record why

## 2. Check the plural-path claims

- [x] 2.1 List every request mapping across the three REST services and record the full path of each
- [x] 2.2 Verify every collection segment is plural
- [x] 2.3 Identify every path that would look like a violation under a literal reading of the rule, and decide whether
  each is a violation or a shape the rule was never about
- [x] 2.4 Verify no singular collection path of the kind the requirement names survives anywhere

## 3. Write the delta specification

- [x] 3.1 Read the current merged specification rather than assuming its content
- [x] 3.2 Write `specs/api-versioning/spec.md` under `## MODIFIED Requirements`, carrying the one whole requirement
  block and nothing else from the file
- [x] 3.3 Verify the requirement header and the existing scenario name match the merged file character for character
- [x] 3.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a
  fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 3.5 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and per
  scenario bullet
- [x] 3.6 Run the strict validation for this change and verify it reports no error

## 4. Archive and verify the merged file

- [x] 4.1 Archive the change and verify the merged specification carries the corrected block
- [x] 4.2 Verify the merged file still carries exactly two semicolon lines, the two this change deliberately did not
  touch, and that their requirement text is unchanged
- [x] 4.3 Verify the strict specification validation still reports eighteen passed and none failed
- [x] 4.4 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 5. Notes from the run

- Task 1.3 and 1.4 are the findings that blocked two thirds of this file. The header name appears in no Java, YAML or
  Bruno file anywhere in the repository, and a content search over the whole history for it across source and
  configuration files returns no commit at all. It was specified and never built. The archived change that specified it,
  `2026-06-25-api-cleanup`, nonetheless has its tasks 5.1 and 6.1 checked off, which claimed the controller tests and
  the Bruno collection were updated to send it. They were not.
- Task 1.5 found the mechanism in
  `sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java`, which calls
  `configurer.usePathSegment(1, ...)` with `setDefaultVersion("1")` and `addSupportedVersions("1")`, and matches a
  segment beginning with `v` followed by a digit. Commit 77a6f44 introduced it and its message states the choice and the
  reason: a path-segment strategy wired once in `sky-common`, keeping the public URLs at `/api/v1` so the frontend was
  unaffected.
- Task 1.7 found the internal namespace removed on purpose. `OfferInternalController` existed and commit c459904 deleted
  it, moving the owner lookup to `GET /api/v1/offers/{offerId}/owner`, gating that path as authenticated because it
  returns an email address, and updating the caller, the tests, the OpenAPI document and the Bruno request with it. The
  message states the reason: the version then sits right after `/api` like every other endpoint and the URL carries no
  access-concern naming.
- Task 1.8 decided to report rather than restate. Both requirements could be corrected toward the code on the strength
  of those two commit messages, which is how the MySQL and Auth0 corrections were justified. The difference is that
  those two described a fact that had changed, while these two describe an API contract, and choosing between correcting
  the contract and building it is the owner's call. The embedded Kafka rule was left strict earlier today for the same
  reason.
- Task 2.1 and 2.2 read sixteen request mappings across the three REST services. Every collection segment is plural:
  `/bookings`, `/user/bookings`, `/offers`, `/owner/offers`, `/messages`, `/messages/received` and `/messages/sent`,
  plus the identified forms of each.
- Task 2.3 found two shapes that a literal audit would flag wrongly. `/offers/{offerId}/owner` names one owner of one
  offer, and the `/owner` prefix in `/owner/offers` is a namespace. Both are now named in the requirement as outside the
  rule. `POST /search` was also considered and is not a collection endpoint at all, so it needed no mention.
- Task 4.2 holds. The merged file still carries exactly two semicolon lines, at lines 13 and 31 after the archive step
  shifted the numbering by two, and the diff shows no change to either requirement's text.
- `openspec archive` emitted the same non-blocking warning as the changes archived before it today, that the proposal's
  Why section exceeds 1000 characters. Left as written, because each blocked requirement needs the commit that diverged
  from it.
