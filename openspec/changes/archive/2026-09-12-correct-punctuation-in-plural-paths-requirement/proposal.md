## Why

The repository formatting rule forbids joining two independent clauses with a semicolon, and `api-versioning` carries
three such lines, at scenario lines 11, 22 and 29. Only one of them can be corrected right now, and this change
corrects that one.

The reason the other two are left alone is the more important half of this proposal. A delta replaces a requirement
block by matching its header, so fixing a scenario means restating its whole requirement, and restating a requirement
means standing behind every fact inside it. Two of this capability's three requirements describe a mechanism that was
never built.

The requirement `Header-based API versioning via Spring Framework 7 native mechanism` says the `X-API-Version` request
header is the primary resolver. The string `X-API-Version` appears in no Java file, no YAML file and no Bruno request
anywhere in this repository, and `git log -S` finds no commit that ever added it to one. What was built instead, in
commit 77a6f44, is a path-segment resolver:
`sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java` calls
`configurer.usePathSegment(1, ...)` with `setDefaultVersion("1")` and `addSupportedVersions("1")`, and that commit
message states the choice and its reason, which was to keep the public URLs at `/api/v1` so the frontend was unaffected.

The requirement `Internal endpoints are versioned and namespaced` says service-to-service endpoints live under
`/api/internal/...`. They did. `OfferInternalController` existed and commit c459904 deleted it, moving the owner lookup
to `GET /api/v1/offers/{offerId}/owner`, and that commit message states the reason, which was to model the lookup as a
sub-resource and keep access concerns out of the URL. The string `api/internal` now appears in no source or
configuration file.

Both are therefore corrections somebody has to authorise rather than corrections this pass can make. Each has two
possible resolutions, which are to restate the requirement toward the code or to build what the requirement asks for,
and picking one is a decision about the API contract rather than a repair of a stale fact. They are reported to the
owner and left exactly as they are, so the record keeps asking the question.

The third requirement, `REST resource paths use plural nouns consistently`, needs no decision. Every collection
endpoint across the three REST services uses a plural segment, verified by reading all sixteen request mappings, so the
requirement is true and its scenario can be repunctuated on the spot.

## What Changes

- Restate the plural-noun requirement with the punctuation fixed, and with two boundaries named that the original left
  implicit: a single-valued sub-resource keeps its singular name, and a namespace segment is not a collection at all.
  Both exist today and both would look like violations to someone auditing the rule literally.
- Leave the other two requirements in this capability untouched, and record in this proposal why, so the next reader
  finds the analysis rather than the three unexplained semicolons.

No code, configuration file or controller is touched, and no build is run. Every path comes from reading a mapping
annotation.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `api-versioning`: punctuation in the path-auditing scenario, and the two boundaries of the plural-noun rule.

## Impact

- Affected file: `openspec/specs/api-versioning/spec.md`, rewritten at archive time from the delta in this change. One
  of its three requirements is touched and the other two are deliberately not.
- Two open decisions are handed to the owner, both named above with the commit that made the code diverge. Until they
  are taken, `api-versioning` keeps two semicolon-joined scenario lines, which is the one place in this pass where the
  formatting rule is knowingly left broken rather than fixed.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low. The requirement restated here is the only one in the capability that the code already satisfies.
